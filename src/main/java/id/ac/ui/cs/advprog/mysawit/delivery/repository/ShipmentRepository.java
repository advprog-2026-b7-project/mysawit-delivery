package id.ac.ui.cs.advprog.mysawit.delivery.repository;

import id.ac.ui.cs.advprog.mysawit.delivery.entity.Shipment;
import id.ac.ui.cs.advprog.mysawit.delivery.entity.ShipmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {
    List<Shipment> findByDriverIdAndStatusIn(UUID driverId, List<ShipmentStatus> statuses);

    List<Shipment> findByMandorIdAndStatusIn(UUID mandorId, List<ShipmentStatus> statuses);

    List<Shipment> findByMandorId(UUID mandorId);

    List<Shipment> findByDriverId(UUID driverId);

    @Query("SELECT s FROM Shipment s WHERE s.driverId = :driverId " +
            "AND (:startDate IS NULL OR s.createdAt >= :startDate) " +
            "AND (:endDate IS NULL OR s.createdAt <= :endDate)")
    List<Shipment> findDriverHistory(
            @Param("driverId") UUID driverId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT COALESCE(SUM(s.totalWeightKg), 0) FROM Shipment s " +
            "WHERE s.plantationId = :plantationId " +
            "AND s.status NOT IN :excludedStatuses")
    BigDecimal sumActiveWeightByPlantation(
            @Param("plantationId") UUID plantationId,
            @Param("excludedStatuses") List<ShipmentStatus> excludedStatuses
    );
}
