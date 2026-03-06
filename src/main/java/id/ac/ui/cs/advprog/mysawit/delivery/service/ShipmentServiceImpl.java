package id.ac.ui.cs.advprog.mysawit.delivery.service;

import id.ac.ui.cs.advprog.mysawit.delivery.dto.CreateShipmentRequest;
import id.ac.ui.cs.advprog.mysawit.delivery.dto.ShipmentResponse;
import id.ac.ui.cs.advprog.mysawit.delivery.entity.Shipment;
import id.ac.ui.cs.advprog.mysawit.delivery.entity.ShipmentStatus;
import id.ac.ui.cs.advprog.mysawit.delivery.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShipmentServiceImpl implements ShipmentService{
    private final ShipmentRepository shipmentRepository;
    private static final BigDecimal MAX_WEIGHT_LIMIT = new BigDecimal("400.00");

    @Override
    public ShipmentResponse createShipment(CreateShipmentRequest request){
        validateWeightLimit(request.getTotalWeightKg());

        Shipment shipment = Shipment.builder()
                            .plantationId(request.getPlantationId())
                            .mandorId(request.getMandorId())
                            .totalWeightKg(request.getTotalWeightKg())
                            .status(ShipmentStatus.MEMUAT)
                            .build();

        Shipment savedShipment = shipmentRepository.save(shipment);
        return convertToResponse(savedShipment);
    }

    private void validateWeightLimit(BigDecimal weight){
        if (weight == null) {
            throw new IllegalArgumentException("Berat muatan tidak boleh kosong!");
        }
        if (weight.compareTo(MAX_WEIGHT_LIMIT) > 0) {
            throw new IllegalArgumentException("Berat muatan tidak boleh melebihi 400 kg!");
        }
        if (weight.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Berat muatan harus lebih dari 0 kg!");
        }
    }

    @Override
    public ShipmentResponse assignDriver(UUID shipmentId, UUID driverId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new IllegalArgumentException("Data Pengiriman Tidak Ditemukan!"));

        shipment.setDriverId(driverId);
        Shipment updatedShipment = shipmentRepository.save(shipment);
        return convertToResponse(updatedShipment);
    }

    @Override
    public ShipmentResponse partialRejectShipment(UUID shipmentId, BigDecimal acceptedWeight, String reason){
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new IllegalArgumentException("Data Pengiriman Tidak Ditemukan!"));

        shipment.setStatus(ShipmentStatus.DISETUJUI_PARSIAL);
        shipment.setTotalWeightKg(acceptedWeight);
        shipment.setRejectedReason(reason);

        Shipment updatedShipment = shipmentRepository.save(shipment);
        return convertToResponse(updatedShipment);
    }

    private ShipmentResponse convertToResponse(Shipment shipment){
        return ShipmentResponse.builder()
                .id(shipment.getId())
                .plantationId(shipment.getPlantationId())
                .mandorId(shipment.getMandorId())
                .driverId(shipment.getDriverId())
                .totalWeightKg(shipment.getTotalWeightKg())
                .status(shipment.getStatus())
                .rejectionReason(shipment.getRejectedReason())
                .createdAt(shipment.getCreatedAt())
                .updatedAt(shipment.getUpdatedAt())
                .build();
    }
}
