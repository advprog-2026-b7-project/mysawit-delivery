package id.ac.ui.cs.advprog.mysawit.delivery.service;

import id.ac.ui.cs.advprog.mysawit.delivery.dto.CreateShipmentRequest;
import id.ac.ui.cs.advprog.mysawit.delivery.entity.Shipment;

import java.util.List;
import java.util.UUID;

public interface ShipmentService {
    Shipment createShipment(CreateShipmentRequest request);
    Shipment assignDriver(UUID shipmentId, UUID driverId);
}
