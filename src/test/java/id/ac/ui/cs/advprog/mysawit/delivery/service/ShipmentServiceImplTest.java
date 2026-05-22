package id.ac.ui.cs.advprog.mysawit.delivery.service;

import id.ac.ui.cs.advprog.mysawit.delivery.client.HarvestClient;
import id.ac.ui.cs.advprog.mysawit.delivery.client.PlantationClient;
import id.ac.ui.cs.advprog.mysawit.delivery.dto.AdminRejectRequest;
import id.ac.ui.cs.advprog.mysawit.delivery.dto.CreateShipmentRequest;
import id.ac.ui.cs.advprog.mysawit.delivery.dto.ShipmentResponse;
import id.ac.ui.cs.advprog.mysawit.delivery.dto.external.HarvestQueryResponse;
import id.ac.ui.cs.advprog.mysawit.delivery.entity.Shipment;
import id.ac.ui.cs.advprog.mysawit.delivery.entity.ShipmentItems;
import id.ac.ui.cs.advprog.mysawit.delivery.entity.ShipmentStatus;
import id.ac.ui.cs.advprog.mysawit.delivery.mapper.ShipmentMapper;
import id.ac.ui.cs.advprog.mysawit.delivery.repository.ShipmentItemRepository;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ShipmentServiceImplTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private ShipmentItemRepository shipmentItemRepository;

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

    /** Builds a valid HarvestItemResponse belonging to the test plantation. */
    private HarvestQueryResponse.HarvestItemResponse approvedHarvest(
            UUID id, BigDecimal weight) {
        HarvestQueryResponse.HarvestItemResponse h =
                new HarvestQueryResponse.HarvestItemResponse();
        h.setId(id);
        h.setPlantationId(plantationId.toString());
        h.setWeightKg(weight);
        h.setStatus("APPROVED");
        return h;
    }

    @Test
    void testCreateShipment_Success() {
        UUID harvestId = UUID.randomUUID();
        CreateShipmentRequest request = new CreateShipmentRequest();
        request.setDriverId(driverId);
        request.setHarvestIds(List.of(harvestId));
        String token = "Bearer dummyToken";

        when(plantationClient.getMandorIdFromToken(token)).thenReturn(mandorId);
        when(plantationClient.getPlantationIdByMandor(token)).thenReturn(plantationId);
        when(harvestClient.getHarvestById(harvestId, token))
                .thenReturn(approvedHarvest(harvestId, new BigDecimal("200.00")));
        when(shipmentItemRepository.existsInActiveShipment(eq(harvestId), anyList()))
                .thenReturn(false);
        doNothing().when(weightValidator).validate(any());
        when(plantationClient.isDriverAssignedToPlantation(token, plantationId, driverId))
                .thenReturn(true);
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(dummyShipment);
        when(shipmentItemRepository.save(any(ShipmentItems.class)))
                .thenReturn(new ShipmentItems());
        when(shipmentMapper.toResponse(any(Shipment.class), anyList())).thenReturn(dummyResponse);

        ShipmentResponse response = shipmentService.createShipment(request, token);

        assertNotNull(response);
        verify(shipmentRepository, times(1)).save(any(Shipment.class));
        verify(shipmentItemRepository, times(1)).save(any(ShipmentItems.class));
    }

    @Test
    void testCreateShipment_EmptyHarvestIds_Rejected() {
        CreateShipmentRequest request = new CreateShipmentRequest();
        request.setDriverId(driverId);
        request.setHarvestIds(List.of());
        String token = "Bearer dummyToken";

        when(plantationClient.getMandorIdFromToken(token)).thenReturn(mandorId);
        when(plantationClient.getPlantationIdByMandor(token)).thenReturn(plantationId);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> shipmentService.createShipment(request, token));

        assertTrue(ex.getMessage().contains("harvest harus dipilih"));
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void testCreateShipment_HarvestNotFound_Rejected() {
        UUID missingId = UUID.randomUUID();
        CreateShipmentRequest request = new CreateShipmentRequest();
        request.setDriverId(driverId);
        request.setHarvestIds(List.of(missingId));
        String token = "Bearer dummyToken";

        when(plantationClient.getMandorIdFromToken(token)).thenReturn(mandorId);
        when(plantationClient.getPlantationIdByMandor(token)).thenReturn(plantationId);
        when(harvestClient.getHarvestById(missingId, token)).thenReturn(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> shipmentService.createShipment(request, token));

        assertTrue(ex.getMessage().contains("tidak ditemukan"));
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void testCreateShipment_HarvestNotApproved_Rejected() {
        UUID harvestId = UUID.randomUUID();
        HarvestQueryResponse.HarvestItemResponse pending =
                new HarvestQueryResponse.HarvestItemResponse();
        pending.setId(harvestId);
        pending.setPlantationId(plantationId.toString());
        pending.setWeightKg(new BigDecimal("100.00"));
        pending.setStatus("PENDING");

        CreateShipmentRequest request = new CreateShipmentRequest();
        request.setDriverId(driverId);
        request.setHarvestIds(List.of(harvestId));
        String token = "Bearer dummyToken";

        when(plantationClient.getMandorIdFromToken(token)).thenReturn(mandorId);
        when(plantationClient.getPlantationIdByMandor(token)).thenReturn(plantationId);
        when(harvestClient.getHarvestById(harvestId, token)).thenReturn(pending);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> shipmentService.createShipment(request, token));

        assertTrue(ex.getMessage().contains("belum disetujui"));
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void testCreateShipment_HarvestNotFromSamePlantation_Rejected() {
        UUID harvestId = UUID.randomUUID();
        UUID otherPlantation = UUID.randomUUID();
        HarvestQueryResponse.HarvestItemResponse wrongPlantation =
                new HarvestQueryResponse.HarvestItemResponse();
        wrongPlantation.setId(harvestId);
        wrongPlantation.setPlantationId(otherPlantation.toString());
        wrongPlantation.setWeightKg(new BigDecimal("100.00"));
        wrongPlantation.setStatus("APPROVED");

        CreateShipmentRequest request = new CreateShipmentRequest();
        request.setDriverId(driverId);
        request.setHarvestIds(List.of(harvestId));
        String token = "Bearer dummyToken";

        when(plantationClient.getMandorIdFromToken(token)).thenReturn(mandorId);
        when(plantationClient.getPlantationIdByMandor(token)).thenReturn(plantationId);
        when(harvestClient.getHarvestById(harvestId, token)).thenReturn(wrongPlantation);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> shipmentService.createShipment(request, token));

        assertTrue(ex.getMessage().contains("bukan milik kebun"));
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void testCreateShipment_HarvestAlreadyInActiveShipment_Rejected() {
        UUID harvestId = UUID.randomUUID();
        CreateShipmentRequest request = new CreateShipmentRequest();
        request.setDriverId(driverId);
        request.setHarvestIds(List.of(harvestId));
        String token = "Bearer dummyToken";

        when(plantationClient.getMandorIdFromToken(token)).thenReturn(mandorId);
        when(plantationClient.getPlantationIdByMandor(token)).thenReturn(plantationId);
        when(harvestClient.getHarvestById(harvestId, token))
                .thenReturn(approvedHarvest(harvestId, new BigDecimal("100.00")));
        when(shipmentItemRepository.existsInActiveShipment(eq(harvestId), anyList()))
                .thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> shipmentService.createShipment(request, token));

        assertTrue(ex.getMessage().contains("pengiriman aktif"));
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void testCreateShipment_WeightExceeds400kg_Rejected() {
        UUID h1 = UUID.randomUUID();
        UUID h2 = UUID.randomUUID();
        CreateShipmentRequest request = new CreateShipmentRequest();
        request.setDriverId(driverId);
        request.setHarvestIds(List.of(h1, h2));
        String token = "Bearer dummyToken";

        when(plantationClient.getMandorIdFromToken(token)).thenReturn(mandorId);
        when(plantationClient.getPlantationIdByMandor(token)).thenReturn(plantationId);
        when(harvestClient.getHarvestById(h1, token))
                .thenReturn(approvedHarvest(h1, new BigDecimal("250.00")));
        when(harvestClient.getHarvestById(h2, token))
                .thenReturn(approvedHarvest(h2, new BigDecimal("250.00")));
        when(shipmentItemRepository.existsInActiveShipment(any(), anyList())).thenReturn(false);
        doThrow(new IllegalArgumentException("Berat muatan tidak boleh melebihi 400 kg!"))
                .when(weightValidator).validate(any(BigDecimal.class));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> shipmentService.createShipment(request, token));

        assertTrue(ex.getMessage().contains("400"));
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void testCreateShipment_DriverOutsidePlantation_Rejected() {
        UUID harvestId = UUID.randomUUID();
        CreateShipmentRequest request = new CreateShipmentRequest();
        request.setDriverId(driverId);
        request.setHarvestIds(List.of(harvestId));
        String token = "Bearer dummyToken";

        when(plantationClient.getMandorIdFromToken(token)).thenReturn(mandorId);
        when(plantationClient.getPlantationIdByMandor(token)).thenReturn(plantationId);
        when(harvestClient.getHarvestById(harvestId, token))
                .thenReturn(approvedHarvest(harvestId, new BigDecimal("100.00")));
        when(shipmentItemRepository.existsInActiveShipment(eq(harvestId), anyList()))
                .thenReturn(false);
        doNothing().when(weightValidator).validate(any());
        when(plantationClient.isDriverAssignedToPlantation(token, plantationId, driverId))
                .thenReturn(false);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> shipmentService.createShipment(request, token));

        assertTrue(ex.getMessage().contains("tidak terdaftar di kebun"));
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void testCreateShipment_ShipmentItemsPersisted() {
        UUID h1 = UUID.randomUUID();
        UUID h2 = UUID.randomUUID();
        CreateShipmentRequest request = new CreateShipmentRequest();
        request.setDriverId(driverId);
        request.setHarvestIds(List.of(h1, h2));
        String token = "Bearer dummyToken";

        when(plantationClient.getMandorIdFromToken(token)).thenReturn(mandorId);
        when(plantationClient.getPlantationIdByMandor(token)).thenReturn(plantationId);
        when(harvestClient.getHarvestById(h1, token))
                .thenReturn(approvedHarvest(h1, new BigDecimal("100.00")));
        when(harvestClient.getHarvestById(h2, token))
                .thenReturn(approvedHarvest(h2, new BigDecimal("100.00")));
        when(shipmentItemRepository.existsInActiveShipment(any(), anyList())).thenReturn(false);
        doNothing().when(weightValidator).validate(any());
        when(plantationClient.isDriverAssignedToPlantation(token, plantationId, driverId))
                .thenReturn(true);
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(dummyShipment);
        when(shipmentItemRepository.save(any(ShipmentItems.class)))
                .thenReturn(new ShipmentItems());
        when(shipmentMapper.toResponse(any(Shipment.class), anyList())).thenReturn(dummyResponse);

        shipmentService.createShipment(request, token);

        // Two harvests → two ShipmentItems rows persisted.
        verify(shipmentItemRepository, times(2)).save(any(ShipmentItems.class));
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
                shipmentService.updateStatus(shipmentId, ShipmentStatus.MENGIRIM, driverId);

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
                shipmentService.updateStatus(shipmentId, ShipmentStatus.TIBA_DI_TUJUAN, driverId);

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
                shipmentService.updateStatus(shipmentId, ShipmentStatus.MENGIRIM, driverId);

        assertNotNull(response);
        assertEquals(ShipmentStatus.MENGIRIM, dummyShipment.getStatus());
        verify(shipmentRepository, times(1)).save(dummyShipment);
    }

    @Test
    void testUpdateStatusFailedDriverNotAssigned() {
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(dummyShipment));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                shipmentService.updateStatus(shipmentId, ShipmentStatus.MENGIRIM, driverId)
        );

        assertEquals("Driver belum di-assign!", exception.getMessage());
        verify(shipmentRepository, never()).save(any(Shipment.class));
    }

    @Test
    void testUpdateStatusFailedOwnershipViolation() {
        dummyShipment.setDriverId(driverId);
        UUID otherDriver = UUID.randomUUID();

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(dummyShipment));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                shipmentService.updateStatus(shipmentId, ShipmentStatus.MENGIRIM, otherDriver)
        );

        assertTrue(exception.getMessage().contains("tidak berhak"));
        verify(shipmentRepository, never()).save(any(Shipment.class));
    }

    @Test
    void testUpdateStatusFailedInvalidTransition() {
        dummyShipment.setDriverId(driverId);

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(dummyShipment));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                shipmentService.updateStatus(shipmentId, ShipmentStatus.TIBA_DI_TUJUAN, driverId)
        );

        assertTrue(exception.getMessage().contains("Transisi status tidak valid"));
        verify(shipmentRepository, never()).save(any(Shipment.class));
    }

    @Test
    void testUpdateStatusFailedShipmentNotFound() {
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                shipmentService.updateStatus(shipmentId, ShipmentStatus.MENGIRIM, driverId)
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

        ShipmentResponse response = shipmentService.approveByMandor(shipmentId, mandorId);

        assertNotNull(response);
        assertEquals(ShipmentStatus.DISETUJUI_MANDOR, dummyShipment.getStatus());
        verify(shipmentRepository, times(1)).save(dummyShipment);
    }

    @Test
    void testApproveByMandorFailedOwnershipViolation() {
        dummyShipment.setStatus(ShipmentStatus.TIBA_DI_TUJUAN);
        UUID otherMandor = UUID.randomUUID();

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(dummyShipment));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                shipmentService.approveByMandor(shipmentId, otherMandor)
        );

        assertTrue(exception.getMessage().contains("tidak berhak"));
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void testApproveByMandorFailedWrongStatus() {
        dummyShipment.setStatus(ShipmentStatus.MENGIRIM);

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(dummyShipment));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                shipmentService.approveByMandor(shipmentId, mandorId)
        );

        assertEquals("Hanya pengiriman yang telah tiba yang dapat diapprove Mandor.",
                exception.getMessage());
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void testApproveByMandorFailedNotFound() {
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () ->
                shipmentService.approveByMandor(shipmentId, mandorId)
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

        ShipmentResponse response = shipmentService.rejectByMandor(shipmentId, reason, mandorId);

        assertNotNull(response);
        assertEquals(ShipmentStatus.DITOLAK_MANDOR, dummyShipment.getStatus());
        assertEquals(reason, dummyShipment.getRejectedReason());
        verify(shipmentRepository, times(1)).save(dummyShipment);
    }

    @Test
    void testRejectByMandorFailedOwnershipViolation() {
        dummyShipment.setStatus(ShipmentStatus.TIBA_DI_TUJUAN);
        UUID otherMandor = UUID.randomUUID();

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(dummyShipment));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                shipmentService.rejectByMandor(shipmentId, "alasan", otherMandor)
        );

        assertTrue(exception.getMessage().contains("tidak berhak"));
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void testRejectByMandorFailedWrongStatus() {
        dummyShipment.setStatus(ShipmentStatus.MENGIRIM);

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(dummyShipment));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                shipmentService.rejectByMandor(shipmentId, "alasan", mandorId)
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
                shipmentService.rejectByMandor(shipmentId, "", mandorId)
        );

        assertEquals("Alasan penolakan tidak boleh kosong.", exception.getMessage());
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void testRejectByMandorFailedNullReason() {
        dummyShipment.setStatus(ShipmentStatus.TIBA_DI_TUJUAN);

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(dummyShipment));

        assertThrows(IllegalArgumentException.class, () ->
                shipmentService.rejectByMandor(shipmentId, null, mandorId)
        );

        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void testRejectByMandorFailedNotFound() {
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () ->
                shipmentService.rejectByMandor(shipmentId, "alasan", mandorId)
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
        // recognizedWeightKg must be set to totalWeightKg when previously null
        assertEquals(dummyShipment.getTotalWeightKg(), dummyShipment.getRecognizedWeightKg());
        verify(shipmentRepository, times(1)).save(dummyShipment);
    }

    @Test
    void testApproveByAdminFailedWrongStatus() {
        dummyShipment.setStatus(ShipmentStatus.TIBA_DI_TUJUAN);

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(dummyShipment));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                shipmentService.approveByAdmin(shipmentId)
        );

        assertTrue(exception.getMessage().contains("disetujui Mandor"));
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
    void testRejectByAdminFullRejectionFailedNullReason() {
        dummyShipment.setStatus(ShipmentStatus.DISETUJUI_MANDOR);

        AdminRejectRequest request = new AdminRejectRequest();
        request.setPartial(false);
        request.setReason(null);

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(dummyShipment));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                shipmentService.rejectByAdmin(shipmentId, request)
        );

        assertEquals("Alasan penolakan tidak boleh kosong.", exception.getMessage());
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void testRejectByAdminFullRejectionFailedBlankReason() {
        dummyShipment.setStatus(ShipmentStatus.DISETUJUI_MANDOR);

        AdminRejectRequest request = new AdminRejectRequest();
        request.setPartial(false);
        request.setReason("   ");

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(dummyShipment));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                shipmentService.rejectByAdmin(shipmentId, request)
        );

        assertEquals("Alasan penolakan tidak boleh kosong.", exception.getMessage());
        verify(shipmentRepository, never()).save(any());
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
        assertEquals("Sebagian sawit rusak", dummyShipment.getRejectedReason());
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
    void testRejectByAdminPartialFailedWeightExceedsTotal() {
        // dummyShipment.totalWeightKg = 350.00 — recognizedKg 500 should be rejected
        dummyShipment.setStatus(ShipmentStatus.DISETUJUI_MANDOR);

        AdminRejectRequest request = new AdminRejectRequest();
        request.setPartial(true);
        request.setRecognizedKg(new BigDecimal("500.00"));
        request.setReason("Parsial");

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(dummyShipment));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                shipmentService.rejectByAdmin(shipmentId, request)
        );

        assertTrue(exception.getMessage().contains("tidak boleh melebihi"));
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void testRejectByAdminFailedWrongStatus() {
        dummyShipment.setStatus(ShipmentStatus.TIBA_DI_TUJUAN);

        AdminRejectRequest request = new AdminRejectRequest();
        request.setPartial(false);
        request.setReason("alasan");

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(dummyShipment));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                shipmentService.rejectByAdmin(shipmentId, request)
        );

        assertTrue(exception.getMessage().contains("disetujui Mandor"));
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

    // ==================== ADMIN LIST QUERY ====================

    @Test
    void testGetApprovedByMandorShipments_NoFilter() {
        List<Shipment> shipments = List.of(dummyShipment);

        when(shipmentRepository.findByStatusWithDateRange(
                ShipmentStatus.DISETUJUI_MANDOR, null, null))
                .thenReturn(shipments);
        when(shipmentMapper.toResponse(any(Shipment.class))).thenReturn(dummyResponse);

        List<ShipmentResponse> result =
                shipmentService.getApprovedByMandorShipments(null, null);

        assertEquals(1, result.size());
        verify(shipmentRepository, times(1)).findByStatusWithDateRange(
                ShipmentStatus.DISETUJUI_MANDOR, null, null);
    }

    @Test
    void testGetApprovedByMandorShipments_WithDateFilter() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 12, 31, 23, 59);
        List<Shipment> shipments = List.of(dummyShipment);

        when(shipmentRepository.findByStatusWithDateRange(
                ShipmentStatus.DISETUJUI_MANDOR, start, end))
                .thenReturn(shipments);
        when(shipmentMapper.toResponse(any(Shipment.class))).thenReturn(dummyResponse);

        List<ShipmentResponse> result =
                shipmentService.getApprovedByMandorShipments(start, end);

        assertEquals(1, result.size());
        verify(shipmentRepository, times(1)).findByStatusWithDateRange(
                ShipmentStatus.DISETUJUI_MANDOR, start, end);
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

    @Test
    void testGetDriverDeliveriesForMandor() {
        dummyShipment.setDriverId(driverId);
        List<Shipment> shipments = List.of(dummyShipment);

        when(shipmentRepository.findByDriverIdAndMandorId(driverId, mandorId))
                .thenReturn(shipments);
        when(shipmentMapper.toResponse(any(Shipment.class))).thenReturn(dummyResponse);

        List<ShipmentResponse> result =
                shipmentService.getDriverDeliveriesForMandor(driverId, mandorId);

        assertEquals(1, result.size());
        verify(shipmentRepository, times(1)).findByDriverIdAndMandorId(driverId, mandorId);
    }

    @Test
    void testGetDriverDeliveriesForMandor_OtherMandorSeesNothing() {
        UUID otherMandor = UUID.randomUUID();

        when(shipmentRepository.findByDriverIdAndMandorId(driverId, otherMandor))
                .thenReturn(List.of());

        List<ShipmentResponse> result =
                shipmentService.getDriverDeliveriesForMandor(driverId, otherMandor);

        assertTrue(result.isEmpty());
        verify(shipmentRepository, times(1)).findByDriverIdAndMandorId(driverId, otherMandor);
    }
}