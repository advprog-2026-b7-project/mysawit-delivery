package id.ac.ui.cs.advprog.mysawit.delivery.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * Payload sent by Mandor to create a new shipment.
 * plantationId and mandorId are derived server-side from the JWT.
 * totalWeightKg is derived server-side by summing selected harvest records.
 */
@Data
public class CreateShipmentRequest {
    private UUID driverId;
    private List<UUID> harvestIds;
}
