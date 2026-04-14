package id.ac.ui.cs.advprog.mysawit.delivery.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreateShipmentRequest {
    private UUID plantationId;
    private UUID mandorId;
    private UUID driverId;
    private BigDecimal totalWeightKg;
}
