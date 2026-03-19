package id.ac.ui.cs.advprog.mysawit.delivery.mapper;

import id.ac.ui.cs.advprog.mysawit.delivery.dto.ShipmentResponse;
import id.ac.ui.cs.advprog.mysawit.delivery.entity.Shipment;
import id.ac.ui.cs.advprog.mysawit.delivery.entity.ShipmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ShipmentMapperTest {

    private ShipmentMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ShipmentMapper();
    }

    @Test
    void testToResponseSuccess() {
        // Siapkan data dummy
        UUID id = UUID.randomUUID();
        UUID plantationId = UUID.randomUUID();
        UUID mandorId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();

        Shipment shipment = Shipment.builder()
                .id(id)
                .plantationId(plantationId)
                .mandorId(mandorId)
                .driverId(driverId)
                .totalWeightKg(new BigDecimal("300.50"))
                .status(ShipmentStatus.MEMUAT)
                .rejectedReason("Aman")
                .build();

        // Eksekusi fungsi mapper
        ShipmentResponse response = mapper.toResponse(shipment);

        // Verifikasi hasilnya
        assertNotNull(response);
        assertEquals(id, response.getId());
        assertEquals(plantationId, response.getPlantationId());
        assertEquals(mandorId, response.getMandorId());
        assertEquals(driverId, response.getDriverId());
        assertEquals(new BigDecimal("300.50"), response.getTotalWeightKg());
        assertEquals(ShipmentStatus.MEMUAT, response.getStatus());
        assertEquals("Aman", response.getRejectionReason());
    }
}