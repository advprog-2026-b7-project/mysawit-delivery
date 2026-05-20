package id.ac.ui.cs.advprog.mysawit.delivery.service;

import id.ac.ui.cs.advprog.mysawit.delivery.client.HarvestClient;
import id.ac.ui.cs.advprog.mysawit.delivery.client.PlantationClient;
import id.ac.ui.cs.advprog.mysawit.delivery.dto.AdminRejectRequest;
import id.ac.ui.cs.advprog.mysawit.delivery.dto.CreateShipmentRequest;
import id.ac.ui.cs.advprog.mysawit.delivery.dto.ShipmentResponse;
import id.ac.ui.cs.advprog.mysawit.delivery.entity.Shipment;
import id.ac.ui.cs.advprog.mysawit.delivery.entity.ShipmentStatus;
import id.ac.ui.cs.advprog.mysawit.delivery.mapper.ShipmentMapper;
import id.ac.ui.cs.advprog.mysawit.delivery.repository.ShipmentRepository;
import id.ac.ui.cs.advprog.mysawit.delivery.validator.WeightValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShipmentServiceImpl implements ShipmentService {

    private static final String NOT_FOUND_MSG = "Shipment tidak ditemukan";

    private final ShipmentRepository shipmentRepository;
    private final WeightValidator weightValidator;
    private final ShipmentMapper shipmentMapper;
    private final HarvestClient harvestClient;
    private final PlantationClient plantationClient;

    @Override
    @Transactional
    public ShipmentResponse createShipment(CreateShipmentRequest request, String authHeader) {
        BigDecimal totalAvailableWeight = harvestClient.getTotalApprovedWeight(authHeader);

        // 2. Validasi: Apakah berat yang mau dikirim melebihi total panen APPROVED yang ada di kebun?
        if (request.getTotalWeightKg().compareTo(totalAvailableWeight) > 0) {
            throw new IllegalArgumentException(
                    "Berat pengiriman (" + request.getTotalWeightKg() +
                            " kg) melebihi total hasil panen yang disetujui (" +
                            totalAvailableWeight + " kg)!"
            );
        }

        // 3. Validasi aturan bisnis maksimal berat truk (misal aturan 400 Kg)
        weightValidator.validate(request.getTotalWeightKg());

        // 4. OTOMATISASI: Ambil data kebun (Plantation) & Mandor berdasarkan token JWT
        // (Asumsi PlantationClient kamu sudah di-inject dan memiliki method ini)
        UUID plantationId = plantationClient.getPlantationIdByMandor(authHeader);
        UUID mandorId = plantationClient.getMandorIdFromToken(authHeader);

        // 5. Validasi Tambahan: Pastikan Driver yang dipilih memang bertugas di kebun tersebut
        boolean isDriverValid =
                plantationClient.isDriverAssignedToPlantation(authHeader, plantationId,
                        request.getDriverId());
        if (!isDriverValid) {
            throw new IllegalStateException("Driver yang dipilih tidak terdaftar di kebun Anda!");
        }

        // 6. Bangun objek Shipment dengan data yang sudah tervalidasi aman
        Shipment shipment = Shipment.builder()
                .plantationId(plantationId) // Set otomatis dari backend via JWT!
                .mandorId(mandorId)         // Set otomatis dari backend via JWT!
                .driverId(
                        request.getDriverId()) // Diambil dari dropdown yang dipilih mandor di frontend
                .totalWeightKg(request.getTotalWeightKg())
                .status(ShipmentStatus.MEMUAT)
                .build();

        // 7. Simpan ke database dan kembalikan response
        Shipment savedShipment = shipmentRepository.save(shipment);
        return shipmentMapper.toResponse(savedShipment);
    }

    @Override
    public ShipmentResponse assignDriver(UUID shipmentId, UUID driverId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Data Pengiriman Tidak Ditemukan!"));
        if (shipment.getStatus() != ShipmentStatus.MEMUAT) {
            throw new IllegalStateException(
                    "Driver hanya bisa di-assign saat status MEMUAT!");
        }
        shipment.setDriverId(driverId);
        Shipment updatedShipment = shipmentRepository.save(shipment);
        return shipmentMapper.toResponse(updatedShipment);
    }

    @Override
    public ShipmentResponse updateStatus(UUID shipmentId, ShipmentStatus newStatus) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Data Pengiriman Tidak Ditemukan!"));
        if (shipment.getDriverId() == null) {
            throw new IllegalStateException("Driver belum di-assign!");
        }
        ShipmentStatus currentStatus = shipment.getStatus();
        if (!isValidTransition(currentStatus, newStatus)) {
            throw new IllegalStateException(
                    "Transisi status tidak valid dari " + currentStatus + " ke " + newStatus
            );
        }
        shipment.setStatus(newStatus);
        Shipment updatedShipment = shipmentRepository.save(shipment);
        return shipmentMapper.toResponse(updatedShipment);
    }

    @Override
    public List<ShipmentResponse> getAllShipments() {
        return shipmentRepository.findAll()
                .stream()
                .map(shipmentMapper::toResponse)
                .toList();
    }

    private boolean isValidTransition(ShipmentStatus current, ShipmentStatus next) {
        return switch (current) {
            case MEMUAT, DISETUJUI_PARSIAL -> next == ShipmentStatus.MENGIRIM;
            case MENGIRIM -> next == ShipmentStatus.TIBA_DI_TUJUAN;
            default -> false;
        };
    }

    @Override
    @Transactional
    public ShipmentResponse approveByMandor(UUID id) {
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException(NOT_FOUND_MSG));
        if (shipment.getStatus() != ShipmentStatus.TIBA_DI_TUJUAN) {
            throw new IllegalStateException(
                    "Hanya pengiriman yang telah tiba yang dapat diapprove Mandor.");
        }
        shipment.setStatus(ShipmentStatus.DISETUJUI_MANDOR);
        shipmentRepository.save(shipment);
        //todo: payroll driver
        return shipmentMapper.toResponse(shipment);
    }

    @Override
    @Transactional
    public ShipmentResponse rejectByMandor(UUID id, String reason) {
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException(NOT_FOUND_MSG));
        if (shipment.getStatus() != ShipmentStatus.TIBA_DI_TUJUAN) {
            throw new IllegalStateException(
                    "Hanya pengiriman yang telah tiba yang dapat ditolak Mandor.");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Alasan penolakan tidak boleh kosong.");
        }
        shipment.setStatus(ShipmentStatus.DITOLAK_MANDOR);
        shipment.setRejectedReason(reason);
        shipmentRepository.save(shipment);
        return shipmentMapper.toResponse(shipment);
    }

    @Override
    @Transactional
    public ShipmentResponse approveByAdmin(UUID id) {
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException(NOT_FOUND_MSG));
        if (shipment.getStatus() != ShipmentStatus.DISETUJUI_MANDOR) {
            throw new IllegalStateException(
                    "Hanya pengiriman yang telah tiba yang dapat diapprove Admin.");
        }
        shipment.setStatus(ShipmentStatus.DISETUJUI_ADMIN);
        shipmentRepository.save(shipment);
        //todo: payroll mandor
        return shipmentMapper.toResponse(shipment);
    }

    @Override
    @Transactional
    public ShipmentResponse rejectByAdmin(UUID id, AdminRejectRequest request) {
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException(NOT_FOUND_MSG));
        if (shipment.getStatus() != ShipmentStatus.DISETUJUI_MANDOR) {
            throw new IllegalStateException(
                    "Hanya pengiriman yang telah tiba yang dapat ditolak Admin.");
        }
        if (request.isPartial()) {
            if (request.getRecognizedKg() == null ||
                    request.getRecognizedKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                        "Berat yang diakui harus lebih dari 0 untuk penolakan parsial.");
            }
            shipment.setRecognizedWeightKg(request.getRecognizedKg());
            shipment.setStatus(ShipmentStatus.DISETUJUI_PARSIAL);
            //todo:payroll mandor sesuai berat
        } else {
            shipment.setRecognizedWeightKg(BigDecimal.ZERO);
            shipment.setStatus(ShipmentStatus.DITOLAK_ADMIN);
            shipment.setRejectedReason(request.getReason());
        }
        shipmentRepository.save(shipment);
        return shipmentMapper.toResponse(shipment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShipmentResponse> getAssignedDeliveriesForDriver(UUID driverId) {
        List<ShipmentStatus> statuses = List.of(ShipmentStatus.MEMUAT, ShipmentStatus.MENGIRIM);
        return shipmentRepository.
                findByDriverIdAndStatusIn(driverId, statuses)
                .stream()
                .map(shipmentMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShipmentResponse> getDriverHistory(UUID driverId, LocalDateTime startDate,
                                                   LocalDateTime endDate) {
        return shipmentRepository.
                findDriverHistory(driverId, startDate, endDate)
                .stream()
                .map(shipmentMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShipmentResponse> getOngoingDeliveriesForMandor(UUID mandorId) {
        List<ShipmentStatus> statuses = List.of(ShipmentStatus.MEMUAT, ShipmentStatus.MENGIRIM,
                ShipmentStatus.TIBA_DI_TUJUAN);
        return shipmentRepository
                .findByMandorIdAndStatusIn(mandorId, statuses)
                .stream()
                .map(shipmentMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShipmentResponse> getSpecificDriverDeliveries(UUID driverId) {
        return shipmentRepository
                .findByDriverId(driverId)
                .stream()
                .map(shipmentMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShipmentResponse> getSpecificMandorDeliveries(UUID mandorId) {
        return shipmentRepository.findByMandorId(mandorId)
                .stream().map(shipmentMapper::toResponse).toList();
    }

}
