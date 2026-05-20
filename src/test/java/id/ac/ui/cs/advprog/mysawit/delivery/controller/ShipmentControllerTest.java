package id.ac.ui.cs.advprog.mysawit.delivery.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.mysawit.delivery.dto.AdminRejectRequest;
import id.ac.ui.cs.advprog.mysawit.delivery.dto.CreateShipmentRequest;
import id.ac.ui.cs.advprog.mysawit.delivery.dto.ShipmentResponse;
import id.ac.ui.cs.advprog.mysawit.delivery.entity.ShipmentStatus;
import id.ac.ui.cs.advprog.mysawit.delivery.service.ShipmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@WebMvcTest(
        controllers = ShipmentController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class}
)
@WithMockUser
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

        when(shipmentService.createShipment(any(CreateShipmentRequest.class), anyString()))
                .thenReturn(dummyResponse);

        mockMvc.perform(post("/deliveries")
                        .header("Authorization", "Bearer dummy-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(
                        shipmentId.toString()))          // <-- Tambah .data
                .andExpect(jsonPath("$.data.plantationId").value(
                        plantationId.toString())) // <-- Tambah .data
                .andExpect(jsonPath("$.data.totalWeightKg").value(
                        350.00))              // <-- Tambah .data
                .andExpect(jsonPath("$.data.status").value("MEMUAT"));
    }

    @Test
    void testCreateShipmentFailedWeightExceededLimit() throws Exception {
        CreateShipmentRequest request = new CreateShipmentRequest();
        request.setPlantationId(plantationId);
        request.setMandorId(mandorId);
        request.setTotalWeightKg(new BigDecimal("500.00"));

        when(shipmentService.createShipment(any(CreateShipmentRequest.class), anyString()))
                .thenThrow(new IllegalArgumentException(
                        "Berat muatan tidak boleh melebihi 400 kg!"));

        mockMvc.perform(post("/deliveries")
                        .header("Authorization", "Bearer dummy-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(result -> assertTrue(
                        result.getResolvedException() instanceof IllegalArgumentException))
                .andExpect(result -> assertEquals(
                        "Berat muatan tidak boleh melebihi 400 kg!",
                        result.getResolvedException().getMessage()));
    }

    @Test
    void testAssignDriverSuccess() throws Exception {
        dummyResponse.setDriverId(driverId);

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

    // ==================== APPROVE BY MANDOR ====================

    @Test
    void testApproveByMandorSuccess() throws Exception {
        dummyResponse.setStatus(ShipmentStatus.DISETUJUI_MANDOR);

        when(shipmentService.approveByMandor(any(UUID.class))).thenReturn(dummyResponse);

        mockMvc.perform(patch("/deliveries/{id}/approve-mandor", shipmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISETUJUI_MANDOR"));
    }

    @Test
    void testApproveByMandorFailedWrongStatus() throws Exception {
        when(shipmentService.approveByMandor(any(UUID.class)))
                .thenThrow(new IllegalStateException(
                        "Hanya pengiriman yang telah tiba yang dapat diapprove Mandor."));

        mockMvc.perform(patch("/deliveries/{id}/approve-mandor", shipmentId))
                .andExpect(result -> assertTrue(
                        result.getResolvedException() instanceof IllegalStateException))
                .andExpect(result -> assertEquals(
                        "Hanya pengiriman yang telah tiba yang dapat diapprove Mandor.",
                        result.getResolvedException().getMessage()));
    }

    // ==================== REJECT BY MANDOR ====================

    @Test
    void testRejectByMandorSuccess() throws Exception {
        dummyResponse.setStatus(ShipmentStatus.DITOLAK_MANDOR);

        when(shipmentService.rejectByMandor(any(UUID.class), any(String.class)))
                .thenReturn(dummyResponse);

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("reason", "Kualitas tidak memenuhi standar");

        mockMvc.perform(patch("/deliveries/{id}/reject-mandor", shipmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DITOLAK_MANDOR"));
    }

    @Test
    void testRejectByMandorFailedEmptyReason() throws Exception {
        when(shipmentService.rejectByMandor(any(UUID.class), any()))
                .thenThrow(new IllegalArgumentException("Alasan penolakan tidak boleh kosong."));

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("reason", "");

        mockMvc.perform(patch("/deliveries/{id}/reject-mandor", shipmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(result -> assertTrue(
                        result.getResolvedException() instanceof IllegalArgumentException))
                .andExpect(result -> assertEquals(
                        "Alasan penolakan tidak boleh kosong.",
                        result.getResolvedException().getMessage()));
    }

    // ==================== APPROVE BY ADMIN ====================

    @Test
    void testApproveByAdminSuccess() throws Exception {
        dummyResponse.setStatus(ShipmentStatus.DISETUJUI_ADMIN);

        when(shipmentService.approveByAdmin(any(UUID.class))).thenReturn(dummyResponse);

        mockMvc.perform(patch("/deliveries/{id}/approve-admin", shipmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISETUJUI_ADMIN"));
    }

    @Test
    void testApproveByAdminFailedWrongStatus() throws Exception {
        when(shipmentService.approveByAdmin(any(UUID.class)))
                .thenThrow(new IllegalStateException(
                        "Hanya pengiriman yang telah tiba yang dapat diapprove Admin."));

        mockMvc.perform(patch("/deliveries/{id}/approve-admin", shipmentId))
                .andExpect(result -> assertTrue(
                        result.getResolvedException() instanceof IllegalStateException))
                .andExpect(result -> assertEquals(
                        "Hanya pengiriman yang telah tiba yang dapat diapprove Admin.",
                        result.getResolvedException().getMessage()));
    }

    // ==================== REJECT BY ADMIN ====================

    @Test
    void testRejectByAdminFullRejectionSuccess() throws Exception {
        dummyResponse.setStatus(ShipmentStatus.DITOLAK_ADMIN);

        when(shipmentService.rejectByAdmin(any(UUID.class), any(AdminRejectRequest.class)))
                .thenReturn(dummyResponse);

        AdminRejectRequest request = new AdminRejectRequest();
        request.setPartial(false);
        request.setReason("Sawit tidak memenuhi standar pabrik");

        mockMvc.perform(patch("/deliveries/{id}/reject-admin", shipmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DITOLAK_ADMIN"));
    }

    @Test
    void testRejectByAdminPartialSuccess() throws Exception {
        dummyResponse.setStatus(ShipmentStatus.DISETUJUI_PARSIAL);

        when(shipmentService.rejectByAdmin(any(UUID.class), any(AdminRejectRequest.class)))
                .thenReturn(dummyResponse);

        AdminRejectRequest request = new AdminRejectRequest();
        request.setPartial(true);
        request.setRecognizedKg(new BigDecimal("200.00"));
        request.setReason("Sebagian sawit rusak");

        mockMvc.perform(patch("/deliveries/{id}/reject-admin", shipmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISETUJUI_PARSIAL"));
    }

    // ==================== GET ENDPOINTS ====================

    @Test
    void testGetAssignedDeliveriesForDriver() throws Exception {
        when(shipmentService.getAssignedDeliveriesForDriver(any(UUID.class)))
                .thenReturn(List.of(dummyResponse));

        mockMvc.perform(get("/deliveries/driver/{driverId}/assigned", driverId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(shipmentId.toString()));
    }

    @Test
    void testGetAssignedDeliveriesForDriverEmpty() throws Exception {
        when(shipmentService.getAssignedDeliveriesForDriver(any(UUID.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/deliveries/driver/{driverId}/assigned", driverId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void testGetDriverHistory() throws Exception {
        when(shipmentService.getDriverHistory(
                any(UUID.class),
                any(LocalDateTime.class),
                any(LocalDateTime.class)))
                .thenReturn(List.of(dummyResponse));

        mockMvc.perform(get("/deliveries/driver/{driverId}/history", driverId)
                        .param("startDate", "2025-01-01T00:00:00")
                        .param("endDate", "2025-12-31T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(shipmentId.toString()));
    }

    @Test
    void testGetOngoingDeliveriesForMandor() throws Exception {
        when(shipmentService.getOngoingDeliveriesForMandor(any(UUID.class)))
                .thenReturn(List.of(dummyResponse));

        mockMvc.perform(get("/deliveries/mandor/{mandorId}/ongoing", mandorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(shipmentId.toString()));
    }

    @Test
    void testGetOngoingDeliveriesForMandorEmpty() throws Exception {
        when(shipmentService.getOngoingDeliveriesForMandor(any(UUID.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/deliveries/mandor/{mandorId}/ongoing", mandorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void testGetSpecificDriverDeliveries() throws Exception {
        when(shipmentService.getSpecificDriverDeliveries(any(UUID.class)))
                .thenReturn(List.of(dummyResponse));

        mockMvc.perform(get("/deliveries/driver/{driverId}", driverId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(shipmentId.toString()));
    }

    @Test
    void testGetSpecificMandorDeliveries() throws Exception {
        when(shipmentService.getSpecificMandorDeliveries(any(UUID.class)))
                .thenReturn(List.of(dummyResponse));

        mockMvc.perform(get("/deliveries/mandor/{mandorId}", mandorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(shipmentId.toString()));
    }

    @Test
    void testGetAllShipments() throws Exception {
        when(shipmentService.getAllShipments()).thenReturn(List.of(dummyResponse));

        mockMvc.perform(get("/deliveries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(shipmentId.toString()));
    }
}