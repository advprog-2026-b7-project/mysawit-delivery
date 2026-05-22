package id.ac.ui.cs.advprog.mysawit.delivery.mapper;

import id.ac.ui.cs.advprog.mysawit.delivery.dto.ShipmentResponse;
import id.ac.ui.cs.advprog.mysawit.delivery.entity.Shipment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class ShipmentMapper {

    public ShipmentResponse toResponse(Shipment shipment) {
        return toResponse(shipment, null);
    }

    /** Maps a saved shipment together with the harvest IDs it contains. */
    public ShipmentResponse toResponse(Shipment shipment, List<UUID> harvestIds) {
        return ShipmentResponse.builder()
                .id(shipment.getId())
                .plantationId(shipment.getPlantationId())
                .mandorId(shipment.getMandorId())
                .driverId(shipment.getDriverId())
                .totalWeightKg(shipment.getTotalWeightKg())
                .recognizedWeightKg(shipment.getRecognizedWeightKg())
                .status(shipment.getStatus())
                .rejectionReason(shipment.getRejectedReason())
                .harvestIds(harvestIds)
                .createdAt(shipment.getCreatedAt())
                .updatedAt(shipment.getUpdatedAt())
                .build();
    }
}