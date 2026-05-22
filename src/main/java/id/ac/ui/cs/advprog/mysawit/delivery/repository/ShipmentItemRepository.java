package id.ac.ui.cs.advprog.mysawit.delivery.repository;

import id.ac.ui.cs.advprog.mysawit.delivery.entity.ShipmentItems;
import id.ac.ui.cs.advprog.mysawit.delivery.entity.ShipmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ShipmentItemRepository extends JpaRepository<ShipmentItems, UUID> {

    List<ShipmentItems> findByShipmentId(UUID shipmentId);

    boolean existsByHarvestId(UUID harvestId);

    /**
     * Returns {@code true} when the given harvest is already attached to an
     * <em>active</em> shipment — i.e. a shipment whose status is not one of
     * the provided terminal statuses.
     */
    @Query("SELECT CASE WHEN COUNT(si) > 0 THEN true ELSE false END "
            + "FROM ShipmentItems si "
            + "WHERE si.harvestId = :harvestId "
            + "AND si.shipmentId IN ("
            + "    SELECT s.id FROM Shipment s "
            + "    WHERE s.status NOT IN :terminalStatuses"
            + ")")
    boolean existsInActiveShipment(
            @Param("harvestId") UUID harvestId,
            @Param("terminalStatuses") List<ShipmentStatus> terminalStatuses);
}
