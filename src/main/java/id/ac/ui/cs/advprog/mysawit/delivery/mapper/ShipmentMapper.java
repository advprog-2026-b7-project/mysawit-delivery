package id.ac.ui.cs.advprog.mysawit.delivery.mapper;

import id.ac.ui.cs.advprog.mysawit.delivery.dto.ShipmentResponse;
import id.ac.ui.cs.advprog.mysawit.delivery.entity.Shipment;
import org.springframework.stereotype.Component;

@Component
public class ShipmentMapper {

    public ShipmentResponse toResponse(Shipment shipment) {
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