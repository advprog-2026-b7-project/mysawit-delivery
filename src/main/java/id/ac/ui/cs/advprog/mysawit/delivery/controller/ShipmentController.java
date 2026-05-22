package id.ac.ui.cs.advprog.mysawit.delivery.controller;

import id.ac.ui.cs.advprog.mysawit.delivery.client.HarvestClient;
import id.ac.ui.cs.advprog.mysawit.delivery.client.PlantationClient;
import id.ac.ui.cs.advprog.mysawit.delivery.dto.AdminRejectRequest;
import id.ac.ui.cs.advprog.mysawit.delivery.dto.CreateShipmentRequest;
import id.ac.ui.cs.advprog.mysawit.delivery.dto.ShipmentResponse;
import id.ac.ui.cs.advprog.mysawit.delivery.dto.external.PlantationDetailQueryResponse;
import id.ac.ui.cs.advprog.mysawit.delivery.dto.response.ApiSuccessResponse;
import id.ac.ui.cs.advprog.mysawit.delivery.service.ShipmentService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import id.ac.ui.cs.advprog.mysawit.delivery.entity.ShipmentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/deliveries")
public class ShipmentController {

    private final ShipmentService shipmentService;
    private final HarvestClient harvestClient;
    private final PlantationClient plantationClient;

    public ShipmentController(ShipmentService shipmentService, HarvestClient harvestClient,
                              PlantationClient plantationClient) {
        this.shipmentService = shipmentService;
        this.harvestClient = harvestClient;
        this.plantationClient = plantationClient;
    }

    @PostMapping
    public ResponseEntity<ApiSuccessResponse<ShipmentResponse>> createShipment(
            @RequestHeader(value = "Authorization") String authHeader,
            @Valid @RequestBody CreateShipmentRequest request
    ) {
        ShipmentResponse data = shipmentService.createShipment(request, authHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiSuccessResponse<>(data));
    }

    @PatchMapping("/{id}/assign-driver")
    public ResponseEntity<ShipmentResponse> assignDriver(
            @PathVariable("id") UUID shipmentId,
            @RequestBody Map<String, UUID> requestBody) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().equals("ROLE_MANDOR"))) {
            throw new IllegalStateException("Only MANDOR can assign drivers");
        }
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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UUID callerDriverId = UUID.fromString(authentication.getName());
        ShipmentResponse response =
                shipmentService.updateStatus(shipmentId, newStatus, callerDriverId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me/assigned")
    public ResponseEntity<List<ShipmentResponse>> getMyAssignedDeliveries() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UUID driverId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(shipmentService.getAssignedDeliveriesForDriver(driverId));
    }

    @GetMapping("/me/history")
    public ResponseEntity<List<ShipmentResponse>> getMyHistory(
            @RequestParam("startDate") @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam("endDate") @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UUID driverId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(shipmentService.getDriverHistory(driverId, startDate, endDate));
    }

    @GetMapping
    public ResponseEntity<List<ShipmentResponse>> getAllShipments() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            throw new IllegalStateException("Only ADMIN can view all shipments");
        }
        return ResponseEntity.ok(shipmentService.getAllShipments());
    }

    @PatchMapping("/{id}/approve-mandor")
    public ResponseEntity<ShipmentResponse> approveByMandor(
            @PathVariable("id") UUID shipmentId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UUID callerMandorId = UUID.fromString(authentication.getName());
        ShipmentResponse response =
                shipmentService.approveByMandor(shipmentId, callerMandorId, authHeader);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/reject-mandor")
    public ResponseEntity<ShipmentResponse> rejectByMandor(
            @PathVariable("id") UUID shipmentId,
            @RequestBody Map<String, String> requestBody) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UUID callerMandorId = UUID.fromString(authentication.getName());
        String reason = requestBody.get("reason");
        ShipmentResponse response =
                shipmentService.rejectByMandor(shipmentId, reason, callerMandorId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me/mandor/ongoing")
    public ResponseEntity<List<ShipmentResponse>> getMyOngoingDeliveries() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UUID mandorId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(shipmentService.getOngoingDeliveriesForMandor(mandorId));
    }

    @GetMapping("/me/mandor/driver/{driverId}")
    public ResponseEntity<List<ShipmentResponse>> getMyDriverDeliveries(
            @PathVariable("driverId") UUID driverId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UUID mandorId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(
                shipmentService.getDriverDeliveriesForMandor(driverId, mandorId));
    }

    @GetMapping("/admin/pending-review")
    public ResponseEntity<List<ShipmentResponse>> getAdminPendingReview(
            @RequestParam(required = false) @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            throw new IllegalStateException("Only ADMIN can view pending review shipments");
        }
        return ResponseEntity.ok(
                shipmentService.getApprovedByMandorShipments(startDate, endDate));
    }

    @PatchMapping("/{id}/approve-admin")
    public ResponseEntity<ShipmentResponse> approveByAdmin(
            @PathVariable("id") UUID shipmentId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            throw new IllegalStateException("Only ADMIN can approve shipments");
        }
        ShipmentResponse response = shipmentService.approveByAdmin(shipmentId, authHeader);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/reject-admin")
    public ResponseEntity<ShipmentResponse> rejectByAdmin(
            @PathVariable("id") UUID shipmentId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody AdminRejectRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            throw new IllegalStateException("Only ADMIN can reject shipments");
        }
        ShipmentResponse response = shipmentService.rejectByAdmin(shipmentId, request, authHeader);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/driver/{driverId}/assigned")
    public ResponseEntity<List<ShipmentResponse>> getAssignedDeliveriesForDriver(
            @PathVariable("driverId") UUID driverId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UUID callerId = UUID.fromString(auth.getName());
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && !callerId.equals(driverId)) {
            throw new IllegalStateException("You can only view your own assignments");
        }
        return ResponseEntity.ok(shipmentService.getAssignedDeliveriesForDriver(driverId));
    }

    @GetMapping("/driver/{driverId}/history")
    public ResponseEntity<List<ShipmentResponse>> getDriverHistory(
            @PathVariable("driverId") UUID driverId,
            @RequestParam("startDate") @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam("endDate") @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UUID callerId = UUID.fromString(auth.getName());
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && !callerId.equals(driverId)) {
            throw new IllegalStateException("You can only view your own history");
        }
        return ResponseEntity.ok(shipmentService.getDriverHistory(driverId, startDate, endDate));
    }

    @GetMapping("/mandor/{mandorId}/ongoing")
    public ResponseEntity<List<ShipmentResponse>> getOngoingDeliveriesForMandor(
            @PathVariable("mandorId") UUID mandorId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UUID callerId = UUID.fromString(auth.getName());
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && !callerId.equals(mandorId)) {
            throw new IllegalStateException("You can only view your own ongoing deliveries");
        }
        return ResponseEntity.ok(shipmentService.getOngoingDeliveriesForMandor(mandorId));
    }

    @GetMapping("/driver/{driverId}")
    public ResponseEntity<List<ShipmentResponse>> getSpecificDriverDeliveries(
            @PathVariable("driverId") UUID driverId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UUID callerId = UUID.fromString(auth.getName());
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && !callerId.equals(driverId)) {
            throw new IllegalStateException("You can only view your own deliveries");
        }
        return ResponseEntity.ok(shipmentService.getSpecificDriverDeliveries(driverId));
    }

    @GetMapping("/mandor/{mandorId}")
    public ResponseEntity<List<ShipmentResponse>> getSpecificMandorDeliveries(
            @PathVariable("mandorId") UUID mandorId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UUID callerId = UUID.fromString(auth.getName());
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && !callerId.equals(mandorId)) {
            throw new IllegalStateException("You can only view your own deliveries");
        }
        return ResponseEntity.ok(shipmentService.getSpecificMandorDeliveries(mandorId));
    }

    @GetMapping("/harvest/approved-weight")
    public ResponseEntity<BigDecimal> getApprovedWeight(
            @RequestHeader(value = "Authorization") String authHeader) {
        BigDecimal result = shipmentService.getAvailableHarvestWeight(authHeader);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/drivers/available")
    public ResponseEntity<?> getAvailableDrivers(
            @RequestHeader(value = "Authorization") String authHeader,
            @RequestParam("plantationId")
            UUID plantationId) {
        PlantationDetailQueryResponse response = plantationClient.getPlantationDetail(plantationId);

        if (response == null || response.getData() == null ||
                response.getData().getDrivers() == null) {
            return ResponseEntity.ok(java.util.List.of());
        }
        java.util.List<PlantationDetailQueryResponse.DriverItem> availableDrivers =
                response.getData().getDrivers().getContent();
        return ResponseEntity.ok(availableDrivers);
    }

    @GetMapping("/my-plantation")
    public ResponseEntity<?> getMyPlantationId(@RequestHeader("Authorization") String authHeader) {
        try {
            UUID plantationId = plantationClient.getPlantationIdByMandor(authHeader);
            return ResponseEntity.ok(java.util.Map.of("plantationId", plantationId));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(java.util.Map.of("message", e.getMessage()));
        }
    }
}
