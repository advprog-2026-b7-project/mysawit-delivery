package id.ac.ui.cs.advprog.mysawit.delivery.repository;

import id.ac.ui.cs.advprog.mysawit.delivery.entity.ShipmentItems;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ShipmentItemRepository extends JpaRepository<ShipmentItems, UUID> {
    List<ShipmentItems> findByShipmentId(UUID shipmentId);
    boolean existsByHarvestId(UUID harvestId);
}
