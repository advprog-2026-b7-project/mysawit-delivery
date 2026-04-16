package id.ac.ui.cs.advprog.mysawit.delivery.service;

import id.ac.ui.cs.advprog.mysawit.delivery.dto.CreateShipmentRequest;
import id.ac.ui.cs.advprog.mysawit.delivery.dto.ShipmentResponse;
import id.ac.ui.cs.advprog.mysawit.delivery.entity.Shipment;
import id.ac.ui.cs.advprog.mysawit.delivery.entity.ShipmentStatus;
import id.ac.ui.cs.advprog.mysawit.delivery.mapper.ShipmentMapper;
import id.ac.ui.cs.advprog.mysawit.delivery.repository.ShipmentRepository;
import id.ac.ui.cs.advprog.mysawit.delivery.validator.WeightValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShipmentServiceImpl implements ShipmentService{

    private final ShipmentRepository shipmentRepository;
    private final WeightValidator weightValidator;
    private final ShipmentMapper shipmentMapper;

    @Override
    public ShipmentResponse createShipment(CreateShipmentRequest request){
        weightValidator.validate(request.getTotalWeightKg());

        Shipment shipment = Shipment.builder()
                            .plantationId(request.getPlantationId())
                            .mandorId(request.getMandorId())
                            .totalWeightKg(request.getTotalWeightKg())
                            .status(ShipmentStatus.MEMUAT)
                            .build();

        Shipment savedShipment = shipmentRepository.save(shipment);
        return shipmentMapper.toResponse(savedShipment);
    }

    @Override
    public ShipmentResponse assignDriver(UUID shipmentId, UUID driverId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Data Pengiriman Tidak Ditemukan!"));
        if (shipment.getStatus() != ShipmentStatus.MEMUAT) {
            throw new IllegalStateException("Driver hanya bisa di-assign saat status MEMUAT!");
        }
        shipment.setDriverId(driverId);
        Shipment updatedShipment = shipmentRepository.save(shipment);
        return shipmentMapper.toResponse(updatedShipment);
    }

    @Override
    public ShipmentResponse partialRejectShipment(
            UUID shipmentId, BigDecimal acceptedWeight, String reason){

        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Data Pengiriman Tidak Ditemukan!"));

        if (shipment.getStatus() != ShipmentStatus.MEMUAT) {
            throw new IllegalStateException("Partial reject hanya bisa saat status MEMUAT!");
        }

        if (acceptedWeight.compareTo(BigDecimal.ZERO) <= 0
                || acceptedWeight.compareTo(shipment.getTotalWeightKg()) >= 0) {
            throw new IllegalArgumentException("Berat yang diterima tidak valid!");
        }
        shipment.setStatus(ShipmentStatus.DISETUJUI_PARSIAL);
        shipment.setTotalWeightKg(acceptedWeight);
        shipment.setRejectedReason(reason);

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

        // VALIDASI TRANSISI
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
}
