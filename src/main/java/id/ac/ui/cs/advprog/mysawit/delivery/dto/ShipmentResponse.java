package id.ac.ui.cs.advprog.mysawit.delivery.dto;

import id.ac.ui.cs.advprog.mysawit.delivery.entity.ShipmentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ShipmentResponse {
    private UUID id;
    private UUID plantationId;
    private UUID mandorId;
    private UUID driverId;
    private BigDecimal totalWeightKg;

    private ShipmentStatus status;
    private String rejectionReason;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
