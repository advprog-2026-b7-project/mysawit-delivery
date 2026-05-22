package id.ac.ui.cs.advprog.mysawit.delivery.service;

import id.ac.ui.cs.advprog.mysawit.delivery.client.HarvestClient;
import id.ac.ui.cs.advprog.mysawit.delivery.client.PaymentPayrollClient;
import id.ac.ui.cs.advprog.mysawit.delivery.client.PlantationClient;
import id.ac.ui.cs.advprog.mysawit.delivery.dto.AdminRejectRequest;
import id.ac.ui.cs.advprog.mysawit.delivery.dto.CreateShipmentRequest;
import id.ac.ui.cs.advprog.mysawit.delivery.dto.ShipmentResponse;
import id.ac.ui.cs.advprog.mysawit.delivery.dto.external.HarvestQueryResponse;
import id.ac.ui.cs.advprog.mysawit.delivery.entity.Shipment;
import id.ac.ui.cs.advprog.mysawit.delivery.entity.ShipmentItems;
import id.ac.ui.cs.advprog.mysawit.delivery.entity.ShipmentStatus;
import id.ac.ui.cs.advprog.mysawit.delivery.mapper.ShipmentMapper;
import id.ac.ui.cs.advprog.mysawit.delivery.repository.ShipmentItemRepository;
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

    /** Terminal statuses — shipments in these states are considered closed/rejected. */
    private static final List<ShipmentStatus> TERMINAL_STATUSES = List.of(
            ShipmentStatus.DITOLAK_MANDOR,
            ShipmentStatus.DITOLAK_ADMIN,
            ShipmentStatus.DISETUJUI_ADMIN,
            ShipmentStatus.DISETUJUI_PARSIAL
    );

    /** Completed statuses — terminal statuses that represent successful delivery completion */
    private static final List<ShipmentStatus> COMPLETED_STATUSES = List.of(
            ShipmentStatus.DISETUJUI_ADMIN,
            ShipmentStatus.DISETUJUI_PARSIAL,
            ShipmentStatus.DITOLAK_MANDOR,
            ShipmentStatus.DITOLAK_ADMIN
    );

    private final ShipmentRepository shipmentRepository;
    private final ShipmentItemRepository shipmentItemRepository;
    private final WeightValidator weightValidator;
    private final ShipmentMapper shipmentMapper;
    private final HarvestClient harvestClient;
    private final PlantationClient plantationClient;
    private final PaymentPayrollClient paymentPayrollClient;

    @Override
    @Transactional
    public ShipmentResponse createShipment(CreateShipmentRequest request, String authHeader) {
        // 1. Derive Mandor identity and plantation from JWT — never trust the request body.
        UUID mandorId = plantationClient.getMandorIdFromToken(authHeader);
        UUID plantationId = plantationClient.getPlantationIdByMandor(authHeader);

        // 2. Basic input guards.
        List<UUID> harvestIds = request.getHarvestIds();
        if (harvestIds == null || harvestIds.isEmpty()) {
            throw new IllegalArgumentException("Minimal satu harvest harus dipilih!");
        }
        if (request.getDriverId() == null) {
            throw new IllegalArgumentException("Driver harus dipilih!");
        }

        // 3. Validate every selected harvest record.
        BigDecimal totalWeight = BigDecimal.ZERO;
        for (UUID harvestId : harvestIds) {
            HarvestQueryResponse.HarvestItemResponse harvest =
                    harvestClient.getHarvestById(harvestId, authHeader);

            if (harvest == null) {
                throw new IllegalArgumentException(
                        "Harvest " + harvestId + " tidak ditemukan!");
            }
            if (!"APPROVED".equals(harvest.getStatus())) {
                throw new IllegalArgumentException(
                        "Harvest " + harvestId + " belum disetujui (status: "
                                + harvest.getStatus() + ")!");
            }
            // Only enforce plantation ownership when the harvest carries a parseable
            // plantation UUID.  Legacy records may store "UNSPECIFIED" (set by the
            // harvest service when the buruh's assignment lacked a plantation ID at
            // submission time).  The mandor's ability to see the harvest via the
            // harvest-history API already implies they supervise the creating buruh,
            // so skipping the check for those records is safe.
            UUID harvestPlantationId = tryParseUUID(harvest.getPlantationId());
            if (harvestPlantationId != null && !plantationId.equals(harvestPlantationId)) {
                throw new IllegalArgumentException(
                        "Harvest " + harvestId + " bukan milik kebun Anda!");
            }
            if (shipmentItemRepository.existsInActiveShipment(harvestId, TERMINAL_STATUSES)) {
                throw new IllegalStateException(
                        "Harvest " + harvestId + " sudah terdaftar dalam pengiriman aktif!");
            }
            totalWeight = totalWeight.add(harvest.getWeightKg());
        }

        // 4. Validate summed weight (delegates <= 0 and > 400 kg to WeightValidator).
        weightValidator.validate(totalWeight);

        // 5. Validate selected driver belongs to this plantation.
        boolean driverValid = plantationClient.isDriverAssignedToPlantation(
                authHeader, plantationId, request.getDriverId());
        if (!driverValid) {
            throw new IllegalStateException(
                    "Driver yang dipilih tidak terdaftar di kebun Anda!");
        }

        // 6. Persist Shipment and ShipmentItems atomically.
        Shipment shipment = Shipment.builder()
                .plantationId(plantationId)
                .mandorId(mandorId)
                .driverId(request.getDriverId())
                .totalWeightKg(totalWeight)
                .status(ShipmentStatus.MEMUAT)
                .build();
        Shipment saved = shipmentRepository.save(shipment);

        for (UUID harvestId : harvestIds) {
            ShipmentItems item = new ShipmentItems();
            item.setShipmentId(saved.getId());
            item.setHarvestId(harvestId);
            shipmentItemRepository.save(item);
        }

        return shipmentMapper.toResponse(saved, harvestIds);
    }

    /**
     * Attempts to parse a UUID string.  Returns {@code null} if the value is
     * null, blank, or not a valid UUID (e.g. the legacy sentinel "UNSPECIFIED").
     */
    private UUID tryParseUUID(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
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
    public ShipmentResponse updateStatus(
            UUID shipmentId, ShipmentStatus newStatus, UUID callerDriverId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Data Pengiriman Tidak Ditemukan!"));
        if (shipment.getDriverId() == null) {
            throw new IllegalStateException("Driver belum di-assign!");
        }
        if (!callerDriverId.equals(shipment.getDriverId())) {
            throw new IllegalStateException(
                    "Anda tidak berhak mengubah status pengiriman ini!");
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
    public ShipmentResponse approveByMandor(UUID id, UUID callerMandorId) {
        return approveByMandor(id, callerMandorId, null);
    }

    @Override
    @Transactional
    public ShipmentResponse approveByMandor(
            UUID id,
            UUID callerMandorId,
            String authHeader) {
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException(NOT_FOUND_MSG));
        if (!callerMandorId.equals(shipment.getMandorId())) {
            throw new IllegalStateException(
                    "Anda tidak berhak menyetujui pengiriman ini!");
        }
        if (shipment.getStatus() != ShipmentStatus.TIBA_DI_TUJUAN) {
            throw new IllegalStateException(
                    "Hanya pengiriman yang telah tiba yang dapat diapprove Mandor.");
        }
        shipment.setStatus(ShipmentStatus.DISETUJUI_MANDOR);
        shipment.setCompletedAt(LocalDateTime.now());
        shipmentRepository.save(shipment);
        paymentPayrollClient.createDriverPayroll(shipment, authHeader);
        return shipmentMapper.toResponse(shipment);
    }

    @Override
    @Transactional
    public ShipmentResponse rejectByMandor(UUID id, String reason, UUID callerMandorId) {
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException(NOT_FOUND_MSG));
        if (!callerMandorId.equals(shipment.getMandorId())) {
            throw new IllegalStateException(
                    "Anda tidak berhak menolak pengiriman ini!");
        }
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
    @Transactional(readOnly = true)
    public List<ShipmentResponse> getDriverDeliveriesForMandor(
            UUID driverId, UUID callerMandorId) {
        return shipmentRepository.findByDriverIdAndMandorId(driverId, callerMandorId)
                .stream()
                .map(shipmentMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ShipmentResponse approveByAdmin(UUID id) {
        return approveByAdmin(id, null);
    }

    @Override
    @Transactional
    public ShipmentResponse approveByAdmin(UUID id, String authHeader) {
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException(NOT_FOUND_MSG));
        if (shipment.getStatus() != ShipmentStatus.DISETUJUI_MANDOR) {
            throw new IllegalStateException(
                    "Hanya pengiriman yang disetujui Mandor yang dapat diapprove Admin.");
        }
        if (shipment.getRecognizedWeightKg() == null) {
            shipment.setRecognizedWeightKg(shipment.getTotalWeightKg());
        }
        shipment.setStatus(ShipmentStatus.DISETUJUI_ADMIN);
        shipment.setCompletedAt(LocalDateTime.now());
        shipmentRepository.save(shipment);
        paymentPayrollClient.createMandorPayroll(shipment, authHeader);
        return shipmentMapper.toResponse(shipment);
    }

    @Override
    @Transactional
    public ShipmentResponse rejectByAdmin(UUID id, AdminRejectRequest request) {
        return rejectByAdmin(id, request, null);
    }

    @Override
    @Transactional
    public ShipmentResponse rejectByAdmin(
            UUID id,
            AdminRejectRequest request,
            String authHeader) {
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException(NOT_FOUND_MSG));
        if (shipment.getStatus() != ShipmentStatus.DISETUJUI_MANDOR) {
            throw new IllegalStateException(
                    "Hanya pengiriman yang disetujui Mandor yang dapat ditolak Admin.");
        }
        if (request.isPartial()) {
            if (request.getRecognizedKg() == null ||
                    request.getRecognizedKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                        "Berat yang diakui harus lebih dari 0 untuk penolakan parsial.");
            }
            if (request.getRecognizedKg().compareTo(shipment.getTotalWeightKg()) > 0) {
                throw new IllegalArgumentException(
                        "Berat yang diakui tidak boleh melebihi total berat pengiriman.");
            }
            if (request.getReason() == null || request.getReason().trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "Alasan penolakan tidak boleh kosong.");
            }
            shipment.setRecognizedWeightKg(request.getRecognizedKg());
            shipment.setRejectedReason(request.getReason());
            shipment.setStatus(ShipmentStatus.DISETUJUI_PARSIAL);
        } else {
            if (request.getReason() == null || request.getReason().trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "Alasan penolakan tidak boleh kosong.");
            }
            shipment.setRecognizedWeightKg(BigDecimal.ZERO);
            shipment.setStatus(ShipmentStatus.DITOLAK_ADMIN);
            shipment.setRejectedReason(request.getReason());
        }
        shipmentRepository.save(shipment);
        if (shipment.getStatus() == ShipmentStatus.DISETUJUI_PARSIAL) {
            paymentPayrollClient.createMandorPayroll(shipment, authHeader);
        }
        return shipmentMapper.toResponse(shipment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShipmentResponse> getApprovedByMandorShipments(
            LocalDateTime startDate, LocalDateTime endDate) {
        return shipmentRepository
                .findByStatusWithDateRange(
                        ShipmentStatus.DISETUJUI_MANDOR, startDate, endDate)
                .stream()
                .map(shipmentMapper::toResponse)
                .toList();
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
                findDriverHistory(driverId, startDate, endDate, COMPLETED_STATUSES)
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

    @Override
    public BigDecimal getAvailableHarvestWeight(String authHeader) {
        UUID plantationId = plantationClient.getPlantationIdByMandor(authHeader);
        BigDecimal totalHarvest = harvestClient.getTotalApprovedWeight(authHeader);
        BigDecimal totalShipped = shipmentRepository.sumActiveWeightByPlantation(
                plantationId,
                List.of(ShipmentStatus.DITOLAK_MANDOR, ShipmentStatus.DITOLAK_ADMIN)
        );
        System.out.println("plantationId: " + plantationId);
        System.out.println("totalHarvest: " + totalHarvest);
        System.out.println("totalShipped: " + totalShipped);
        BigDecimal available = totalHarvest.subtract(totalShipped);
        return available.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : available;
    }
}
