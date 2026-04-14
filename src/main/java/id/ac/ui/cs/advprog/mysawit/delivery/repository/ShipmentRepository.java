package id.ac.ui.cs.advprog.mysawit.delivery.repository;

import id.ac.ui.cs.advprog.mysawit.delivery.entity.Shipment;
import id.ac.ui.cs.advprog.mysawit.delivery.entity.ShipmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {
    List<Shipment> findByDriverId(UUID driverId);
    List<Shipment> findByMandorIdAndStatus(UUID mandorId, ShipmentStatus status);
    List<Shipment> findByMandorId(UUID mandorId);
}
