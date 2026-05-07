package id.ac.ui.cs.advprog.mysawit.delivery.controller;

import id.ac.ui.cs.advprog.mysawit.delivery.dto.AdminRejectRequest;
import id.ac.ui.cs.advprog.mysawit.delivery.dto.CreateShipmentRequest;
import id.ac.ui.cs.advprog.mysawit.delivery.dto.ShipmentResponse;
import id.ac.ui.cs.advprog.mysawit.delivery.service.ShipmentService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import id.ac.ui.cs.advprog.mysawit.delivery.entity.ShipmentStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/deliveries")
public class ShipmentController {

    private final ShipmentService shipmentService;

    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @PostMapping
    public ResponseEntity<ShipmentResponse> createShipment(
            @RequestBody CreateShipmentRequest request){
        ShipmentResponse response = shipmentService.createShipment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}/assign-driver")
    public ResponseEntity<ShipmentResponse> assignDriver(
            @PathVariable("id") UUID shipmentId,
            @RequestBody Map<String, UUID> requestBody){
        UUID driverId = requestBody.get("driverId");
        if (driverId == null) {
            throw new IllegalArgumentException("Driver ID tidak boleh kosong!");
        }
        ShipmentResponse response = shipmentService.assignDriver(shipmentId, driverId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ShipmentResponse> updateStatus(
            @PathVariable("id") UUID shipmentId,
            @RequestBody Map<String, String> requestBody) {
        String statusStr = requestBody.get("status");
        if (statusStr == null) {
            throw new IllegalArgumentException("Status tidak boleh kosong!");
        }
        ShipmentStatus newStatus;
        try {
            newStatus = ShipmentStatus.valueOf(statusStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Status tidak valid!");
        }
        ShipmentResponse response = shipmentService.updateStatus(shipmentId, newStatus);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<ShipmentResponse>> getAllShipments() {
        return ResponseEntity.ok(shipmentService.getAllShipments());
    }

    @PatchMapping("/{id}/approve-mandor")
    public ResponseEntity<ShipmentResponse> approveByMandor(
            @PathVariable("id") UUID shipmentId) {
        ShipmentResponse response = shipmentService.approveByMandor(shipmentId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/reject-mandor")
    public ResponseEntity<ShipmentResponse> rejectByMandor(
            @PathVariable("id") UUID shipmentId,
            @RequestBody Map<String, String> requestBody) {
        String reason = requestBody.get("reason");
        ShipmentResponse response = shipmentService.rejectByMandor(shipmentId, reason);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/approve-admin")
    public ResponseEntity<ShipmentResponse> approveByAdmin(@PathVariable("id") UUID shipmentId) {
        ShipmentResponse response = shipmentService.approveByAdmin(shipmentId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/reject-admin")
    public ResponseEntity<ShipmentResponse> rejectByAdmin(
            @PathVariable("id") UUID shipmentId,
            @RequestBody AdminRejectRequest request) {
        ShipmentResponse response = shipmentService.rejectByAdmin(shipmentId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/driver/{driverId}/assigned")
    public ResponseEntity<List<ShipmentResponse>> getAssignedDeliveriesForDriver(
            @PathVariable("driverId") UUID driverId) {
        return ResponseEntity.ok(shipmentService.getAssignedDeliveriesForDriver(driverId));
    }

    @GetMapping("/driver/{driverId}/history")
    public ResponseEntity<List<ShipmentResponse>> getDriverHistory(
            @PathVariable("driverId") UUID driverId,
            @RequestParam("startDate") @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam("endDate") @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(shipmentService.getDriverHistory(driverId, startDate, endDate));
    }

    @GetMapping("/mandor/{mandorId}/ongoing")
    public ResponseEntity<List<ShipmentResponse>> getOngoingDeliveriesForMandor(
            @PathVariable("mandorId") UUID mandorId) {
        return ResponseEntity.ok(shipmentService.getOngoingDeliveriesForMandor(mandorId));
    }

    @GetMapping("/driver/{driverId}")
    public ResponseEntity<List<ShipmentResponse>> getSpecificDriverDeliveries(
            @PathVariable("driverId") UUID driverId) {
        return ResponseEntity.ok(shipmentService.getSpecificDriverDeliveries(driverId));
    }

    @GetMapping("/mandor/{mandorId}")
    public ResponseEntity<List<ShipmentResponse>> getSpecificMandorDeliveries(
            @PathVariable("mandorId") UUID mandorId) {
        return ResponseEntity.ok(shipmentService.getSpecificMandorDeliveries(mandorId));
    }
}
