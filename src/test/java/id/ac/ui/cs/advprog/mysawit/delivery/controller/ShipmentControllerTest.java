package id.ac.ui.cs.advprog.mysawit.delivery.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.mysawit.delivery.dto.CreateShipmentRequest;
import id.ac.ui.cs.advprog.mysawit.delivery.dto.ShipmentResponse;
import id.ac.ui.cs.advprog.mysawit.delivery.entity.ShipmentStatus;
import id.ac.ui.cs.advprog.mysawit.delivery.service.ShipmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ShipmentController.class)
class ShipmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ShipmentService shipmentService;

    private UUID shipmentId;
    private UUID plantationId;
    private UUID mandorId;
    private UUID driverId;
    private ShipmentResponse dummyResponse;

    @BeforeEach
    void setUp() {
        shipmentId = UUID.randomUUID();
        plantationId = UUID.randomUUID();
        mandorId = UUID.randomUUID();
        driverId = UUID.randomUUID();

        dummyResponse = ShipmentResponse.builder()
                .id(shipmentId)
                .plantationId(plantationId)
                .mandorId(mandorId)
                .totalWeightKg(new BigDecimal("350.00"))
                .status(ShipmentStatus.MEMUAT)
                .build();
    }

    @Test
    void testCreateShipmentSuccess() throws Exception {
        CreateShipmentRequest request = new CreateShipmentRequest();
        request.setPlantationId(plantationId);
        request.setMandorId(mandorId);
        request.setTotalWeightKg(new BigDecimal("350.00"));

        // Dienter sebelum .thenReturn agar < 100 karakter
        when(shipmentService.createShipment(any(CreateShipmentRequest.class)))
                .thenReturn(dummyResponse);

        mockMvc.perform(post("/deliveries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(shipmentId.toString()))
                .andExpect(jsonPath("$.plantationId").value(plantationId.toString()))
                .andExpect(jsonPath("$.totalWeightKg").value(350.00))
                .andExpect(jsonPath("$.status").value("MEMUAT"));
    }

    @Test
    void testCreateShipmentFailedWeightExceededLimit() throws Exception {
        CreateShipmentRequest request = new CreateShipmentRequest();
        request.setPlantationId(plantationId);
        request.setMandorId(mandorId);
        request.setTotalWeightKg(new BigDecimal("500.00"));

        when(shipmentService.createShipment(any(CreateShipmentRequest.class)))
                .thenThrow(new IllegalArgumentException(
                        "Berat muatan tidak boleh melebihi 400 kg!"));

        mockMvc.perform(post("/deliveries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Dienter dan dipecah ke bawah
                .andExpect(result -> assertTrue(
                        result.getResolvedException() instanceof IllegalArgumentException))
                .andExpect(result -> assertEquals(
                        "Berat muatan tidak boleh melebihi 400 kg!",
                        result.getResolvedException().getMessage()));
    }

    @Test
    void testAssignDriverSuccess() throws Exception {
        dummyResponse.setDriverId(driverId);

        // Dienter sebelum .thenReturn
        when(shipmentService.assignDriver(any(UUID.class), any(UUID.class)))
                .thenReturn(dummyResponse);

        Map<String, UUID> requestBody = new HashMap<>();
        requestBody.put("driverId", driverId);

        mockMvc.perform(patch("/deliveries/{id}/assign-driver", shipmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(shipmentId.toString()))
                .andExpect(jsonPath("$.driverId").value(driverId.toString()));
    }

    @Test
    void testAssignDriverFailedNullDriverId() throws Exception {
        Map<String, UUID> requestBody = new HashMap<>();

        mockMvc.perform(patch("/deliveries/{id}/assign-driver", shipmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                // Dienter dan dipecah ke bawah
                .andExpect(result -> assertTrue(
                        result.getResolvedException() instanceof IllegalArgumentException))
                .andExpect(result -> assertEquals(
                        "Driver ID tidak boleh kosong!",
                        result.getResolvedException().getMessage()));
    }
    @Test
    void testUpdateStatusSuccess() throws Exception {
        dummyResponse.setStatus(ShipmentStatus.MENGIRIM);

        when(shipmentService.updateStatus(any(UUID.class), any(ShipmentStatus.class)))
                .thenReturn(dummyResponse);

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("status", "MENGIRIM");

        mockMvc.perform(patch("/deliveries/{id}/status", shipmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MENGIRIM"));
    }

    @Test
    void testUpdateStatusFailedNullStatus() throws Exception {
        Map<String, String> requestBody = new HashMap<>();

        mockMvc.perform(patch("/deliveries/{id}/status", shipmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(result -> assertTrue(
                        result.getResolvedException() instanceof IllegalArgumentException))
                .andExpect(result -> assertEquals(
                        "Status tidak boleh kosong!",
                        result.getResolvedException().getMessage()));
    }

    @Test
    void testUpdateStatusFailedInvalidStatusString() throws Exception {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("status", "STATUS_TIDAK_VALID");

        mockMvc.perform(patch("/deliveries/{id}/status", shipmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(result -> assertTrue(
                        result.getResolvedException() instanceof IllegalArgumentException))
                .andExpect(result -> assertEquals(
                        "Status tidak valid!",
                        result.getResolvedException().getMessage()));
    }
}