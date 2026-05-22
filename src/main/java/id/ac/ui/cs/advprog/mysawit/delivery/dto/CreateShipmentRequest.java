package id.ac.ui.cs.advprog.mysawit.delivery.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
    @NotNull(message = "Driver ID tidak boleh kosong")
    private UUID driverId;

    @NotEmpty(message = "Minimal satu harvest harus dipilih")
    private List<UUID> harvestIds;
}
