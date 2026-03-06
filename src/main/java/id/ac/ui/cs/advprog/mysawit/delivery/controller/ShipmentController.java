package id.ac.ui.cs.advprog.mysawit.delivery.controller;

import id.ac.ui.cs.advprog.mysawit.delivery.dto.CreateShipmentRequest;
import id.ac.ui.cs.advprog.mysawit.delivery.dto.ShipmentResponse;
import id.ac.ui.cs.advprog.mysawit.delivery.repository.ShipmentRepository;
import id.ac.ui.cs.advprog.mysawit.delivery.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/deliveries")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService shipmentService;

    @PostMapping
    public ResponseEntity<ShipmentResponse> createShipment(@RequestBody CreateShipmentRequest request){
        ShipmentResponse response = shipmentService.createShipment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}/assign-driver")
    public ResponseEntity<ShipmentResponse> assignDriver(@PathVariable("id") UUID shipmentId,
                                                         @RequestBody Map<String, UUID> requestBody){
        UUID driverId = requestBody.get("driverId");
        if (driverId == null) {
            throw new IllegalArgumentException("Driver ID tidak boleh kosong!");
        }

        ShipmentResponse response = shipmentService.assignDriver(shipmentId, driverId);
        return ResponseEntity.ok(response);
    }
}
