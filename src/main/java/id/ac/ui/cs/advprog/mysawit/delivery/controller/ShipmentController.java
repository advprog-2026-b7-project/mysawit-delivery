package id.ac.ui.cs.advprog.mysawit.delivery.controller;

import id.ac.ui.cs.advprog.mysawit.delivery.dto.CreateShipmentRequest;
import id.ac.ui.cs.advprog.mysawit.delivery.dto.ShipmentResponse;
import id.ac.ui.cs.advprog.mysawit.delivery.service.ShipmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import id.ac.ui.cs.advprog.mysawit.delivery.entity.ShipmentStatus;

import java.math.BigDecimal;
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
    @PatchMapping("/{id}/partial-reject")
    public ResponseEntity<ShipmentResponse> partialRejectShipment(
            @PathVariable("id") UUID shipmentId,
            @RequestBody Map<String, Object> requestBody) {

        BigDecimal acceptedWeight = new BigDecimal(requestBody.get("acceptedWeight").toString());
        String reason = (String) requestBody.get("reason");

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Alasan penolakan tidak boleh kosong!");
        }

        ShipmentResponse response = shipmentService
                .partialRejectShipment(shipmentId, acceptedWeight, reason);
        return ResponseEntity.ok(response);
    }

}
