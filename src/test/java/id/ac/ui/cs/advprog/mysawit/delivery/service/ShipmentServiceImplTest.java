package id.ac.ui.cs.advprog.mysawit.delivery.service;

import id.ac.ui.cs.advprog.mysawit.delivery.client.HarvestClient;
import id.ac.ui.cs.advprog.mysawit.delivery.client.PlantationClient;
import id.ac.ui.cs.advprog.mysawit.delivery.dto.AdminRejectRequest;
import id.ac.ui.cs.advprog.mysawit.delivery.dto.CreateShipmentRequest;
import id.ac.ui.cs.advprog.mysawit.delivery.dto.ShipmentResponse;
import id.ac.ui.cs.advprog.mysawit.delivery.entity.Shipment;
import id.ac.ui.cs.advprog.mysawit.delivery.entity.ShipmentStatus;
import id.ac.ui.cs.advprog.mysawit.delivery.mapper.ShipmentMapper;
import id.ac.ui.cs.advprog.mysawit.delivery.repository.ShipmentRepository;
import id.ac.ui.cs.advprog.mysawit.delivery.validator.WeightValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ShipmentServiceImplTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private WeightValidator weightValidator;

    @Mock
    private ShipmentMapper shipmentMapper;

    @Mock
    private HarvestClient harvestClient;

    @Mock
    private PlantationClient plantationClient;

    @InjectMocks
    private ShipmentServiceImpl shipmentService;

    private UUID shipmentId;
    private UUID plantationId;
    private UUID mandorId;
    private UUID driverId;
    private Shipment dummyShipment;
    private ShipmentResponse dummyResponse;

    @BeforeEach
    void setUp() {
        shipmentId = UUID.randomUUID();
        plantationId = UUID.randomUUID();
        mandorId = UUID.randomUUID();
        driverId = UUID.randomUUID();

        dummyShipment = Shipment.builder()
                .id(shipmentId)
                .plantationId(plantationId)
                .mandorId(mandorId)
                .totalWeightKg(new BigDecimal("350.00"))
                .status(ShipmentStatus.MEMUAT)
                .build();

        dummyResponse = ShipmentResponse.builder()
                .id(shipmentId)
                .plantationId(plantationId)
                .totalWeightKg(new BigDecimal("350.00"))
                .status(ShipmentStatus.MEMUAT)
                .build();
    }

    // ==================== CREATE SHIPMENT TESTS ====================

    @Test
    void testCreateShipment_Success() {
        CreateShipmentRequest request = new CreateShipmentRequest();
        request.setDriverId(driverId);
        request.setTotalWeightKg(new BigDecimal("100.00"));
        String token = "Bearer dummyToken";

        doNothing().when(weightValidator).validate(any());
        when(harvestClient.getTotalApprovedWeight(token)).thenReturn(new BigDecimal("500.00"));
        when(plantationClient.getPlantationIdByMandor(token)).thenReturn(plantationId);
        when(plantationClient.getMandorIdFromToken(token)).thenReturn(mandorId);
        when(plantationClient.isDriverAssignedToPlantation(token, plantationId,
                driverId)).thenReturn(true);

        Shipment shipment = new Shipment();
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(shipment);
        when(shipmentMapper.toResponse(any())).thenReturn(dummyResponse);

        ShipmentResponse response = shipmentService.createShipment(request, token);

        assertNotNull(response);
        verify(shipmentRepository, times(1)).save(any(Shipment.class));
    }

    @Test
    void testCreateShipment_FailedWeightExceeded() {
        CreateShipmentRequest request = new CreateShipmentRequest();
        request.setDriverId(driverId);
        request.setTotalWeightKg(new BigDecimal("600.00"));
        String token = "Bearer dummyToken";

        when(harvestClient.getTotalApprovedWeight(token)).thenReturn(new BigDecimal("500.00"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                shipmentService.createShipment(request, token)
        );

        assertTrue(exception.getMessage().contains("melebihi total hasil panen"));
        verify(shipmentRepository, never()).save(any(Shipment.class));
    }

    @Test
    void testCreateShipment_FailedDriverNotAssignedToPlantation() {
        CreateShipmentRequest request = new CreateShipmentRequest();
        request.setDriverId(driverId);
        request.setTotalWeightKg(new BigDecimal("100.00"));
        String token = "Bearer dummyToken";

        when(harvestClient.getTotalApprovedWeight(token)).thenReturn(new BigDecimal("500.00"));
        when(plantationClient.getPlantationIdByMandor(token)).thenReturn(plantationId);
        when(plantationClient.isDriverAssignedToPlantation(token, plantationId,
                driverId)).thenReturn(false);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                shipmentService.createShipment(request, token)
        );

        assertTrue(exception.getMessage().contains("tidak terdaftar di kebun"));
        verify(shipmentRepository, never()).save(any(Shipment.class));
    }

    // ==================== ASSIGN DRIVER TESTS ====================

    @Test
    void testAssignDriverSuccess() {
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(dummyShipment));
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(dummyShipment);
        when(shipmentMapper.toResponse(any(Shipment.class))).thenReturn(dummyResponse);

        ShipmentResponse response = shipmentService.assignDriver(shipmentId, driverId);

        assertNotNull(response);
        verify(shipmentRepository, times(1)).findById(shipmentId);
        verify(shipmentRepository, times(1)).save(dummyShipment);
        verify(shipmentMapper, times(1)).toResponse(dummyShipment);
    }

    @Test
    void testAssignDriverFailedShipmentNotFound() {
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                shipmentService.assignDriver(shipmentId, driverId)
        );

        assertEquals("Data Pengiriman Tidak Ditemukan!", exception.getMessage());
        verify(shipmentRepository, never()).save(any(Shipment.class));
        verify(shipmentMapper, never()).toResponse(any(Shipment.class));
    }

    // ==================== UPDATE STATUS TESTS ====================

    @Test
    void testUpdateStatusSuccessMemuatToMengirim() {
        dummyShipment.setDriverId(driverId);

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(dummyShipment));
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(dummyShipment);
        when(shipmentMapper.toResponse(any(Shipment.class))).thenReturn(dummyResponse);

        ShipmentResponse response =
                shipmentService.updateStatus(shipmentId, ShipmentStatus.MENGIRIM);

        assertNotNull(response);
        assertEquals(ShipmentStatus.MENGIRIM, dummyShipment.getStatus());
        verify(shipmentRepository, times(1)).save(dummyShipment);
    }

    @Test
    void testUpdateStatusSuccessMengirimToTibaDiTujuan() {
        dummyShipment.setDriverId(driverId);
        dummyShipment.setStatus(ShipmentStatus.MENGIRIM);

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(dummyShipment));
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(dummyShipment);
        when(shipmentMapper.toResponse(any(Shipment.class))).thenReturn(dummyResponse);

        ShipmentResponse response =
                shipmentService.updateStatus(shipmentId, ShipmentStatus.TIBA_DI_TUJUAN);

        assertNotNull(response);
        assertEquals(ShipmentStatus.TIBA_DI_TUJUAN, dummyShipment.getStatus());
        verify(shipmentRepository, times(1)).save(dummyShipment);
    }

    @Test
    void testUpdateStatusSuccessDisetujuiParsialToMengirim() {
        dummyShipment.setDriverId(driverId);
        dummyShipment.setStatus(ShipmentStatus.DISETUJUI_PARSIAL);

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(dummyShipment));
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(dummyShipment);
        when(shipmentMapper.toResponse(any(Shipment.class))).thenReturn(dummyResponse);

        ShipmentResponse response =
                shipmentService.updateStatus(shipmentId, ShipmentStatus.MENGIRIM);

        assertNotNull(response);
        assertEquals(ShipmentStatus.MENGIRIM, dummyShipment.getStatus());
        verify(shipmentRepository, times(1)).save(dummyShipment);
    }

    @Test
    void testUpdateStatusFailedDriverNotAssigned() {
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(dummyShipment));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                shipmentService.updateStatus(shipmentId, ShipmentStatus.MENGIRIM)
        );

        assertEquals("Driver belum di-assign!", exception.getMessage());
        verify(shipmentRepository, never()).save(any(Shipment.class));
    }

    @Test
    void testUpdateStatusFailedInvalidTransition() {
        dummyShipment.setDriverId(driverId);

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(dummyShipment));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                shipmentService.updateStatus(shipmentId, ShipmentStatus.TIBA_DI_TUJUAN)
        );

        assertTrue(exception.getMessage().contains("Transisi status tidak valid"));
        verify(shipmentRepository, never()).save(any(Shipment.class));
    }

    @Test
    void testUpdateStatusFailedShipmentNotFound() {
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                shipmentService.updateStatus(shipmentId, ShipmentStatus.MENGIRIM)
        );

        assertEquals("Data Pengiriman Tidak Ditemukan!", exception.getMessage());
        verify(shipmentRepository, never()).save(any(Shipment.class));
    }

    // ==================== APPROVE BY MANDOR ====================

    @Test
    void testApproveByMandorSuccess() {
        dummyShipment.setStatus(ShipmentStatus.TIBA_DI_TUJUAN);

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(dummyShipment));
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(dummyShipment);
        when(shipmentMapper.toResponse(any(Shipment.class))).thenReturn(dummyResponse);

        ShipmentResponse response = shipmentService.approveByMandor(shipmentId);

        assertNotNull(response);
        assertEquals(ShipmentStatus.DISETUJUI_MANDOR, dummyShipment.getStatus());
        verify(shipmentRepository, times(1)).save(dummyShipment);
    }

    @Test
    void testApproveByMandorFailedWrongStatus() {
        dummyShipment.setStatus(ShipmentStatus.MENGIRIM);

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(dummyShipment));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                shipmentService.approveByMandor(shipmentId)
        );

        assertEquals("Hanya pengiriman yang telah tiba yang dapat diapprove Mandor.",
                exception.getMessage());
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void testApproveByMandorFailedNotFound() {
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () ->
                shipmentService.approveByMandor(shipmentId)
        );

        verify(shipmentRepository, never()).save(any());
    }

    // ==================== REJECT BY MANDOR ====================

    @Test
    void testRejectByMandorSuccess() {
        dummyShipment.setStatus(ShipmentStatus.TIBA_DI_TUJUAN);
        String reason = "Kualitas tidak memenuhi standar";

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(dummyShipment));
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(dummyShipment);
        when(shipmentMapper.toResponse(any(Shipment.class))).thenReturn(dummyResponse);

        ShipmentResponse response = shipmentService.rejectByMandor(shipmentId, reason);

        assertNotNull(response);
        assertEquals(ShipmentStatus.DITOLAK_MANDOR, dummyShipment.getStatus());
        assertEquals(reason, dummyShipment.getRejectedReason());
        verify(shipmentRepository, times(1)).save(dummyShipment);
    }

    @Test
    void testRejectByMandorFailedWrongStatus() {
        dummyShipment.setStatus(ShipmentStatus.MENGIRIM);

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(dummyShipment));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                shipmentService.rejectByMandor(shipmentId, "alasan")
        );

        assertEquals("Hanya pengiriman yang telah tiba yang dapat ditolak Mandor.",
                exception.getMessage());
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void testRejectByMandorFailedEmptyReason() {
        dummyShipment.setStatus(ShipmentStatus.TIBA_DI_TUJUAN);

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(dummyShipment));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                shipmentService.rejectByMandor(shipmentId, "")
        );

        assertEquals("Alasan penolakan tidak boleh kosong.", exception.getMessage());
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void testRejectByMandorFailedNullReason() {
        dummyShipment.setStatus(ShipmentStatus.TIBA_DI_TUJUAN);

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(dummyShipment));

        assertThrows(IllegalArgumentException.class, () ->
                shipmentService.rejectByMandor(shipmentId, null)
        );

        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void testRejectByMandorFailedNotFound() {
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () ->
                shipmentService.rejectByMandor(shipmentId, "alasan")
        );

        verify(shipmentRepository, never()).save(any());
    }

    // ==================== APPROVE BY ADMIN ====================

    @Test
    void testApproveByAdminSuccess() {
        dummyShipment.setStatus(ShipmentStatus.DISETUJUI_MANDOR);

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(dummyShipment));
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(dummyShipment);
        when(shipmentMapper.toResponse(any(Shipment.class))).thenReturn(dummyResponse);

        ShipmentResponse response = shipmentService.approveByAdmin(shipmentId);

        assertNotNull(response);
        assertEquals(ShipmentStatus.DISETUJUI_ADMIN, dummyShipment.getStatus());
        verify(shipmentRepository, times(1)).save(dummyShipment);
    }

    @Test
    void testApproveByAdminFailedWrongStatus() {
        dummyShipment.setStatus(ShipmentStatus.TIBA_DI_TUJUAN);

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(dummyShipment));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                shipmentService.approveByAdmin(shipmentId)
        );

        assertEquals("Hanya pengiriman yang telah tiba yang dapat diapprove Admin.",
                exception.getMessage());
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void testApproveByAdminFailedNotFound() {
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () ->
                shipmentService.approveByAdmin(shipmentId)
        );

        verify(shipmentRepository, never()).save(any());
    }

    // ==================== REJECT BY ADMIN ====================

    @Test
    void testRejectByAdminFullRejectionSuccess() {
        dummyShipment.setStatus(ShipmentStatus.DISETUJUI_MANDOR);

        AdminRejectRequest request = new AdminRejectRequest();
        request.setPartial(false);
        request.setReason("Sawit tidak memenuhi standar pabrik");

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(dummyShipment));
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(dummyShipment);
        when(shipmentMapper.toResponse(any(Shipment.class))).thenReturn(dummyResponse);

        ShipmentResponse response = shipmentService.rejectByAdmin(shipmentId, request);

        assertNotNull(response);
        assertEquals(ShipmentStatus.DITOLAK_ADMIN, dummyShipment.getStatus());
        assertEquals(BigDecimal.ZERO, dummyShipment.getRecognizedWeightKg());
        assertEquals("Sawit tidak memenuhi standar pabrik", dummyShipment.getRejectedReason());
        verify(shipmentRepository, times(1)).save(dummyShipment);
    }

    @Test
    void testRejectByAdminPartialSuccess() {
        dummyShipment.setStatus(ShipmentStatus.DISETUJUI_MANDOR);

        AdminRejectRequest request = new AdminRejectRequest();
        request.setPartial(true);
        request.setRecognizedKg(new BigDecimal("200.00"));
        request.setReason("Sebagian sawit rusak");

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(dummyShipment));
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(dummyShipment);
        when(shipmentMapper.toResponse(any(Shipment.class))).thenReturn(dummyResponse);

        ShipmentResponse response = shipmentService.rejectByAdmin(shipmentId, request);

        assertNotNull(response);
        assertEquals(ShipmentStatus.DISETUJUI_PARSIAL, dummyShipment.getStatus());
        assertEquals(new BigDecimal("200.00"), dummyShipment.getRecognizedWeightKg());
        verify(shipmentRepository, times(1)).save(dummyShipment);
    }

    @Test
    void testRejectByAdminPartialFailedZeroWeight() {
        dummyShipment.setStatus(ShipmentStatus.DISETUJUI_MANDOR);

        AdminRejectRequest request = new AdminRejectRequest();
        request.setPartial(true);
        request.setRecognizedKg(BigDecimal.ZERO);

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(dummyShipment));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                shipmentService.rejectByAdmin(shipmentId, request)
        );

        assertEquals("Berat yang diakui harus lebih dari 0 untuk penolakan parsial.",
                exception.getMessage());
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void testRejectByAdminFailedWrongStatus() {
        dummyShipment.setStatus(ShipmentStatus.TIBA_DI_TUJUAN);

        AdminRejectRequest request = new AdminRejectRequest();
        request.setPartial(false);
        request.setReason("alasan");

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(dummyShipment));

        assertThrows(IllegalStateException.class, () ->
                shipmentService.rejectByAdmin(shipmentId, request)
        );

        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void testRejectByAdminFailedNotFound() {
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.empty());

        AdminRejectRequest request = new AdminRejectRequest();
        assertThrows(NoSuchElementException.class, () ->
                shipmentService.rejectByAdmin(shipmentId, request)
        );
    }

    // ==================== QUERY METHODS ====================

    @Test
    void testGetAssignedDeliveriesForDriver() {
        List<Shipment> shipments = List.of(dummyShipment);

        when(shipmentRepository.findByDriverIdAndStatusIn(eq(driverId), anyList())).thenReturn(
                shipments);
        when(shipmentMapper.toResponse(any(Shipment.class))).thenReturn(dummyResponse);

        List<ShipmentResponse> result = shipmentService.getAssignedDeliveriesForDriver(driverId);

        assertEquals(1, result.size());
        verify(shipmentRepository, times(1)).findByDriverIdAndStatusIn(eq(driverId), anyList());
    }

    @Test
    void testGetDriverHistory() {
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();
        List<Shipment> shipments = List.of(dummyShipment);

        when(shipmentRepository.findDriverHistory(driverId, start, end)).thenReturn(shipments);
        when(shipmentMapper.toResponse(any(Shipment.class))).thenReturn(dummyResponse);

        List<ShipmentResponse> result = shipmentService.getDriverHistory(driverId, start, end);

        assertEquals(1, result.size());
        verify(shipmentRepository, times(1)).findDriverHistory(driverId, start, end);
    }

    @Test
    void testGetOngoingDeliveriesForMandor() {
        List<Shipment> shipments = List.of(dummyShipment);

        when(shipmentRepository.findByMandorIdAndStatusIn(eq(mandorId), anyList())).thenReturn(
                shipments);
        when(shipmentMapper.toResponse(any(Shipment.class))).thenReturn(dummyResponse);

        List<ShipmentResponse> result = shipmentService.getOngoingDeliveriesForMandor(mandorId);

        assertEquals(1, result.size());
        verify(shipmentRepository, times(1)).findByMandorIdAndStatusIn(eq(mandorId), anyList());
    }

    @Test
    void testGetSpecificDriverDeliveries() {
        List<Shipment> shipments = List.of(dummyShipment);

        when(shipmentRepository.findByDriverId(driverId)).thenReturn(shipments);
        when(shipmentMapper.toResponse(any(Shipment.class))).thenReturn(dummyResponse);

        List<ShipmentResponse> result = shipmentService.getSpecificDriverDeliveries(driverId);

        assertEquals(1, result.size());
        verify(shipmentRepository, times(1)).findByDriverId(driverId);
    }

    @Test
    void testGetSpecificMandorDeliveries() {
        List<Shipment> shipments = List.of(dummyShipment);

        when(shipmentRepository.findByMandorId(mandorId)).thenReturn(shipments);
        when(shipmentMapper.toResponse(any(Shipment.class))).thenReturn(dummyResponse);

        List<ShipmentResponse> result = shipmentService.getSpecificMandorDeliveries(mandorId);

        assertEquals(1, result.size());
        verify(shipmentRepository, times(1)).findByMandorId(mandorId);
    }
}