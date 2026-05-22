package id.ac.ui.cs.advprog.mysawit.delivery.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.mysawit.delivery.client.HarvestClient;
import id.ac.ui.cs.advprog.mysawit.delivery.client.PlantationClient;
import id.ac.ui.cs.advprog.mysawit.delivery.dto.AdminRejectRequest;
import id.ac.ui.cs.advprog.mysawit.delivery.dto.CreateShipmentRequest;
import id.ac.ui.cs.advprog.mysawit.delivery.dto.ShipmentResponse;
import id.ac.ui.cs.advprog.mysawit.delivery.dto.external.PlantationDetailQueryResponse;
import id.ac.ui.cs.advprog.mysawit.delivery.entity.ShipmentStatus;
import id.ac.ui.cs.advprog.mysawit.delivery.service.ShipmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

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

@WebMvcTest(
        controllers = ShipmentController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class}
)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser
class ShipmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ShipmentService shipmentService;

    @MockBean
    private HarvestClient harvestClient;

    @MockBean
    private PlantationClient plantationClient;

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
        request.setDriverId(driverId);
        request.setHarvestIds(List.of(UUID.randomUUID()));

        when(shipmentService.createShipment(any(CreateShipmentRequest.class), anyString()))
                .thenReturn(dummyResponse);

        mockMvc.perform(post("/api/v1/deliveries")
                        .header("Authorization", "Bearer dummy-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(shipmentId.toString()))
                .andExpect(jsonPath("$.data.plantationId").value(plantationId.toString()))
                .andExpect(jsonPath("$.data.totalWeightKg").value(350.00))
                .andExpect(jsonPath("$.data.status").value("MEMUAT"));
    }

    @Test
    void testCreateShipmentFailedWeightExceededLimit() throws Exception {
        CreateShipmentRequest request = new CreateShipmentRequest();
        request.setDriverId(driverId);
        request.setHarvestIds(List.of(UUID.randomUUID(), UUID.randomUUID()));

        when(shipmentService.createShipment(any(CreateShipmentRequest.class), anyString()))
                .thenThrow(
                        new IllegalArgumentException("Berat muatan tidak boleh melebihi 400 kg!"));

        mockMvc.perform(post("/api/v1/deliveries")
                        .header("Authorization", "Bearer dummy-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(result -> assertTrue(
                        result.getResolvedException() instanceof IllegalArgumentException))
                .andExpect(result -> assertEquals("Berat muatan tidak boleh melebihi 400 kg!",
                        result.getResolvedException().getMessage()));
    }

    @Test
    void testAssignDriverSuccess() throws Exception {
        dummyResponse.setDriverId(driverId);

        when(shipmentService.assignDriver(any(UUID.class), any(UUID.class)))
                .thenReturn(dummyResponse);

        Map<String, UUID> requestBody = new HashMap<>();
        requestBody.put("driverId", driverId);

        mockMvc.perform(patch("/api/v1/deliveries/{id}/assign-driver", shipmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(shipmentId.toString()))
                .andExpect(jsonPath("$.driverId").value(driverId.toString()));
    }

    @Test
    void testAssignDriverFailedNullDriverId() throws Exception {
        Map<String, UUID> requestBody = new HashMap<>();

        mockMvc.perform(patch("/api/v1/deliveries/{id}/assign-driver", shipmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(
                        status().isBadRequest())
                .andExpect(result -> assertTrue(
                        result.getResolvedException() instanceof IllegalArgumentException))
                .andExpect(result -> assertEquals("Driver ID tidak boleh kosong!",
                        result.getResolvedException().getMessage()));
    }

    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111")
    void testUpdateStatusSuccess() throws Exception {
        dummyResponse.setStatus(ShipmentStatus.MENGIRIM);

        when(shipmentService.updateStatus(
                any(UUID.class), any(ShipmentStatus.class), any(UUID.class)))
                .thenReturn(dummyResponse);

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("status", "MENGIRIM");

        mockMvc.perform(patch("/api/v1/deliveries/{id}/status", shipmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MENGIRIM"));
    }

    @Test
    void testUpdateStatusFailedNullStatus() throws Exception {
        Map<String, String> requestBody = new HashMap<>();

        mockMvc.perform(patch("/api/v1/deliveries/{id}/status", shipmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(result -> assertTrue(
                        result.getResolvedException() instanceof IllegalArgumentException))
                .andExpect(result -> assertEquals("Status tidak boleh kosong!",
                        result.getResolvedException().getMessage()));
    }

    @Test
    void testUpdateStatusFailedInvalidStatusString() throws Exception {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("status", "STATUS_TIDAK_VALID");

        mockMvc.perform(patch("/api/v1/deliveries/{id}/status", shipmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(result -> assertTrue(
                        result.getResolvedException() instanceof IllegalArgumentException))
                .andExpect(result -> assertEquals("Status tidak valid!",
                        result.getResolvedException().getMessage()));
    }

    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111")
    void testApproveByMandorSuccess() throws Exception {
        dummyResponse.setStatus(ShipmentStatus.DISETUJUI_MANDOR);

        when(shipmentService.approveByMandor(any(UUID.class), any(UUID.class)))
                .thenReturn(dummyResponse);

        mockMvc.perform(patch("/api/v1/deliveries/{id}/approve-mandor", shipmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISETUJUI_MANDOR"));
    }

    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111")
    void testRejectByMandorSuccess() throws Exception {
        dummyResponse.setStatus(ShipmentStatus.DITOLAK_MANDOR);

        when(shipmentService.rejectByMandor(any(UUID.class), any(String.class), any(UUID.class)))
                .thenReturn(dummyResponse);

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("reason", "Kualitas tidak memenuhi standar");

        mockMvc.perform(patch("/api/v1/deliveries/{id}/reject-mandor", shipmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DITOLAK_MANDOR"));
    }

    @Test
    void testApproveByAdminSuccess() throws Exception {
        dummyResponse.setStatus(ShipmentStatus.DISETUJUI_ADMIN);

        when(shipmentService.approveByAdmin(any(UUID.class))).thenReturn(dummyResponse);

        mockMvc.perform(patch("/api/v1/deliveries/{id}/approve-admin", shipmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISETUJUI_ADMIN"));
    }

    @Test
    void testRejectByAdminFullRejectionSuccess() throws Exception {
        dummyResponse.setStatus(ShipmentStatus.DITOLAK_ADMIN);

        when(shipmentService.rejectByAdmin(any(UUID.class), any(AdminRejectRequest.class)))
                .thenReturn(dummyResponse);

        AdminRejectRequest request = new AdminRejectRequest();
        request.setPartial(false);
        request.setReason("Sawit busuk");

        mockMvc.perform(patch("/api/v1/deliveries/{id}/reject-admin", shipmentId)
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
        request.setReason("Sebagian rusak");

        mockMvc.perform(patch("/api/v1/deliveries/{id}/reject-admin", shipmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISETUJUI_PARSIAL"));
    }

    @Test
    void testGetAssignedDeliveriesForDriver() throws Exception {
        when(shipmentService.getAssignedDeliveriesForDriver(any(UUID.class)))
                .thenReturn(List.of(dummyResponse));

        mockMvc.perform(get("/api/v1/deliveries/driver/{driverId}/assigned", driverId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(shipmentId.toString()));
    }

    @Test
    void testGetDriverHistory() throws Exception {
        when(shipmentService.getDriverHistory(any(UUID.class), any(LocalDateTime.class),
                any(LocalDateTime.class)))
                .thenReturn(List.of(dummyResponse));

        mockMvc.perform(get("/api/v1/deliveries/driver/{driverId}/history", driverId)
                        .param("startDate", "2026-01-01T00:00:00")
                        .param("endDate", "2026-12-31T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(shipmentId.toString()));
    }

    @Test
    void testGetOngoingDeliveriesForMandor() throws Exception {
        when(shipmentService.getOngoingDeliveriesForMandor(any(UUID.class)))
                .thenReturn(List.of(dummyResponse));

        mockMvc.perform(get("/api/v1/deliveries/mandor/{mandorId}/ongoing", mandorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(shipmentId.toString()));
    }

    @Test
    void testGetAllShipments() throws Exception {
        when(shipmentService.getAllShipments()).thenReturn(List.of(dummyResponse));

        mockMvc.perform(get("/api/v1/deliveries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(shipmentId.toString()));
    }

    // ==================== NEW ENDPOINTS UNIT TESTS ====================

    @Test
    void testGetApprovedWeight() throws Exception {
        BigDecimal totalApprovedWeight = new BigDecimal("1500.50");
        when(shipmentService.getAvailableHarvestWeight(anyString()))
                .thenReturn(totalApprovedWeight);

        mockMvc.perform(get("/api/v1/deliveries/harvest/approved-weight")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(1500.50));
    }

    @Test
    void testGetAvailableDriversSuccess() throws Exception {
        // Construct nested DTO mock responses
        PlantationDetailQueryResponse mockResponse = new PlantationDetailQueryResponse();
        PlantationDetailQueryResponse.PlantationDetailData detailData =
                new PlantationDetailQueryResponse.PlantationDetailData();
        PlantationDetailQueryResponse.DriverPageData driverPage =
                new PlantationDetailQueryResponse.DriverPageData();
        PlantationDetailQueryResponse.DriverItem driverItem =
                new PlantationDetailQueryResponse.DriverItem();

        driverItem.setId(driverId);
        driverItem.setName("tesdriver");
        driverPage.setContent(List.of(driverItem));
        detailData.setDrivers(driverPage);
        mockResponse.setData(detailData);

        when(plantationClient.getPlantationDetail(plantationId)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/deliveries/drivers/available")
                        .header("Authorization", "Bearer valid-token")
                        .param("plantationId", plantationId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(driverId.toString()))
                .andExpect(jsonPath("$[0].name").value("tesdriver"));
    }

    @Test
    void testGetAvailableDriversEmptyResponse() throws Exception {
        // Simulate null data wrapper
        when(plantationClient.getPlantationDetail(plantationId)).thenReturn(null);

        mockMvc.perform(get("/api/v1/deliveries/drivers/available")
                        .header("Authorization", "Bearer valid-token")
                        .param("plantationId", plantationId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void testGetMyPlantationIdSuccess() throws Exception {
        when(plantationClient.getPlantationIdByMandor("Bearer valid-token")).thenReturn(
                plantationId);

        mockMvc.perform(get("/api/v1/deliveries/my-plantation")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plantationId").value(plantationId.toString()));
    }

    @Test
    void testGetMyPlantationIdNotFound() throws Exception {
        when(plantationClient.getPlantationIdByMandor("Bearer invalid-token"))
                .thenThrow(new IllegalStateException(
                        "Data kebun (Plantation) tidak ditemukan untuk akun Mandor ini."));

        mockMvc.perform(get("/api/v1/deliveries/my-plantation")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(
                        "Data kebun (Plantation) tidak ditemukan untuk akun Mandor ini."));
    }

    // ==================== MANDOR /me/mandor ENDPOINT TESTS ====================

    @Test
    @WithMockUser(username = "22222222-2222-2222-2222-222222222222")
    void testGetMyOngoingDeliveriesForMandor() throws Exception {
        when(shipmentService.getOngoingDeliveriesForMandor(any(UUID.class)))
                .thenReturn(List.of(dummyResponse));

        mockMvc.perform(get("/api/v1/deliveries/me/mandor/ongoing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(shipmentId.toString()));
    }

    @Test
    @WithMockUser(username = "22222222-2222-2222-2222-222222222222")
    void testGetMyDriverDeliveriesForMandor() throws Exception {
        when(shipmentService.getDriverDeliveriesForMandor(any(UUID.class), any(UUID.class)))
                .thenReturn(List.of(dummyResponse));

        mockMvc.perform(get("/api/v1/deliveries/me/mandor/driver/{driverId}", driverId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(shipmentId.toString()));
    }

    // ==================== SUPIR /me ENDPOINT TESTS ====================

    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111")
    void testGetMyAssignedDeliveries() throws Exception {
        when(shipmentService.getAssignedDeliveriesForDriver(any(UUID.class)))
                .thenReturn(List.of(dummyResponse));

        mockMvc.perform(get("/api/v1/deliveries/me/assigned"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(shipmentId.toString()));
    }

    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111")
    void testGetMyHistory() throws Exception {
        when(shipmentService.getDriverHistory(
                any(UUID.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(dummyResponse));

        mockMvc.perform(get("/api/v1/deliveries/me/history")
                        .param("startDate", "2026-01-01T00:00:00")
                        .param("endDate", "2026-12-31T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(shipmentId.toString()));
    }
}