package id.ac.ui.cs.advprog.mysawit.delivery.service;

import id.ac.ui.cs.advprog.mysawit.delivery.dto.CreateShipmentRequest;
import id.ac.ui.cs.advprog.mysawit.delivery.dto.ShipmentResponse;
import id.ac.ui.cs.advprog.mysawit.delivery.entity.Shipment;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ShipmentService {
    ShipmentResponse createShipment(CreateShipmentRequest request);
    ShipmentResponse assignDriver(UUID shipmentId, UUID driverId);
    ShipmentResponse partialRejectShipment(
            UUID shipmentId, BigDecimal acceptedWeight, String reason);
}
