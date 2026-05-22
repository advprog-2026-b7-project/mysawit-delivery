package id.ac.ui.cs.advprog.mysawit.delivery.service;

import id.ac.ui.cs.advprog.mysawit.delivery.dto.AdminRejectRequest;
import id.ac.ui.cs.advprog.mysawit.delivery.dto.CreateShipmentRequest;
import id.ac.ui.cs.advprog.mysawit.delivery.dto.ShipmentResponse;
import id.ac.ui.cs.advprog.mysawit.delivery.entity.ShipmentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ShipmentService {
    ShipmentResponse createShipment(CreateShipmentRequest request, String authHeader);

    ;

    ShipmentResponse assignDriver(UUID shipmentId, UUID driverId);

    ShipmentResponse updateStatus(UUID shipmentId, ShipmentStatus newStatus, UUID callerDriverId);

    List<ShipmentResponse> getAllShipments();

    ShipmentResponse approveByMandor(UUID id, UUID callerMandorId);

    default ShipmentResponse approveByMandor(
            UUID id,
            UUID callerMandorId,
            String authHeader) {
        return approveByMandor(id, callerMandorId);
    }

    ShipmentResponse rejectByMandor(UUID id, String reason, UUID callerMandorId);

    List<ShipmentResponse> getDriverDeliveriesForMandor(UUID driverId, UUID callerMandorId);

    ShipmentResponse approveByAdmin(UUID id);

    default ShipmentResponse approveByAdmin(UUID id, String authHeader) {
        return approveByAdmin(id);
    }

    ShipmentResponse rejectByAdmin(UUID id, AdminRejectRequest request);

    default ShipmentResponse rejectByAdmin(
            UUID id,
            AdminRejectRequest request,
            String authHeader) {
        return rejectByAdmin(id, request);
    }

    List<ShipmentResponse> getApprovedByMandorShipments(
            LocalDateTime startDate, LocalDateTime endDate);

    List<ShipmentResponse> getAssignedDeliveriesForDriver(UUID driverId);

    List<ShipmentResponse> getDriverHistory(
            UUID driverId,
            LocalDateTime startDate,
            LocalDateTime endDate);

    List<ShipmentResponse> getOngoingDeliveriesForMandor(UUID mandorId);

    List<ShipmentResponse> getSpecificDriverDeliveries(UUID driverId);

    List<ShipmentResponse> getSpecificMandorDeliveries(UUID mandorId);

    BigDecimal getAvailableHarvestWeight(String authHeader);

}
