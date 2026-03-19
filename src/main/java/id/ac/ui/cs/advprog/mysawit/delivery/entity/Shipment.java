package id.ac.ui.cs.advprog.mysawit.delivery.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "shipments")
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "plantation_id", nullable = false)
    private UUID plantationId;

    @Column(name = "mandor_id", nullable = false)
    private UUID mandorId;

    @Column(name = "driver_id")
    private UUID driverId;

    @Column(name = "total_weight_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalWeightKg;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ShipmentStatus status = ShipmentStatus.MEMUAT;

    @Column(name = "rejected_reason", columnDefinition = "TEXT")
    private String rejectedReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
