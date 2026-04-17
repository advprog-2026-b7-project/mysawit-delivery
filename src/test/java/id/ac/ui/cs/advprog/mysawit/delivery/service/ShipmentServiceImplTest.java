package id.ac.ui.cs.advprog.mysawit.delivery.service;

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

    @InjectMocks
    ShipmentServiceImpl shipmentService;

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

    @Test
    void testCreateShipmentSuccess() {
        CreateShipmentRequest request = new CreateShipmentRequest();
        request.setPlantationId(plantationId);
        request.setMandorId(mandorId);
        request.setTotalWeightKg(new BigDecimal("350.00"));

        doNothing().when(weightValidator).validate(any(BigDecimal.class));
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(dummyShipment);
        when(shipmentMapper.toResponse(any(Shipment.class))).thenReturn(dummyResponse);

        ShipmentResponse response = shipmentService.createShipment(request);

        assertNotNull(response);
        assertEquals(plantationId, response.getPlantationId());
        assertEquals(new BigDecimal("350.00"), response.getTotalWeightKg());
        assertEquals(ShipmentStatus.MEMUAT, response.getStatus());

        verify(weightValidator, times(1)).validate(any(BigDecimal.class));
        verify(shipmentRepository, times(1)).save(any(Shipment.class));
        verify(shipmentMapper, times(1)).toResponse(any(Shipment.class));
    }

    @Test
    void testCreateShipmentFailedValidation() {
        CreateShipmentRequest request = new CreateShipmentRequest();
        request.setTotalWeightKg(new BigDecimal("450.00"));

        doThrow(new IllegalArgumentException("Berat muatan tidak valid!"))
                .when(weightValidator).validate(any(BigDecimal.class));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            shipmentService.createShipment(request);
        });

        assertEquals("Berat muatan tidak valid!", exception.getMessage());

        verify(shipmentRepository, never()).save(any(Shipment.class));
        verify(shipmentMapper, never()).toResponse(any(Shipment.class));
    }

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

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            shipmentService.assignDriver(shipmentId, driverId);
        });

        assertEquals("Data Pengiriman Tidak Ditemukan!", exception.getMessage());
        verify(shipmentRepository, never()).save(any(Shipment.class));
        verify(shipmentMapper, never()).toResponse(any(Shipment.class));
    }

    @Test
    void testPartialRejectShipmentSuccess() {
        BigDecimal acceptedWeight = new BigDecimal("200");
        String reason = "Kualitas buah sebagian buruk";

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(dummyShipment));
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(dummyShipment);
        when(shipmentMapper.toResponse(any(Shipment.class))).thenReturn(dummyResponse);

        ShipmentResponse response = shipmentService.partialRejectShipment(
                shipmentId, acceptedWeight, reason);

        assertNotNull(response);
        assertEquals(ShipmentStatus.DISETUJUI_PARSIAL, dummyShipment.getStatus());
        assertEquals(acceptedWeight, dummyShipment.getTotalWeightKg());
        assertEquals(reason, dummyShipment.getRejectedReason());

        verify(shipmentRepository, times(1)).save(dummyShipment);
        verify(shipmentMapper, times(1)).toResponse(dummyShipment);
    }

    @Test
    void testPartialRejectShipmentFailedShipmentNotFound() {
        BigDecimal acceptedWeight = new BigDecimal("200");
        String reason = "Kualitas buah sebagian buruk";

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            shipmentService.partialRejectShipment(shipmentId, acceptedWeight, reason);
        });

        assertEquals("Data Pengiriman Tidak Ditemukan!", exception.getMessage());
        verify(shipmentRepository, never()).save(any(Shipment.class));
        verify(shipmentMapper, never()).toResponse(any(Shipment.class));
    }
    @Test
    void testUpdateStatusSuccessMemuatToMengirim() {
        dummyShipment.setDriverId(driverId);

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(dummyShipment));
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(dummyShipment);
        when(shipmentMapper.toResponse(any(Shipment.class))).thenReturn(dummyResponse);

        ShipmentResponse response = shipmentService
                .updateStatus(shipmentId, ShipmentStatus.MENGIRIM);

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

        ShipmentResponse response = shipmentService
                .updateStatus(shipmentId, ShipmentStatus.TIBA_DI_TUJUAN);;

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

        ShipmentResponse response = shipmentService
                .updateStatus(shipmentId, ShipmentStatus.MENGIRIM);

        assertNotNull(response);
        assertEquals(ShipmentStatus.MENGIRIM, dummyShipment.getStatus());
        verify(shipmentRepository, times(1)).save(dummyShipment);
    }

    @Test
    void testUpdateStatusFailedDriverNotAssigned() {
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(dummyShipment));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            shipmentService.updateStatus(shipmentId, ShipmentStatus.MENGIRIM);
        });

        assertEquals("Driver belum di-assign!", exception.getMessage());
        verify(shipmentRepository, never()).save(any(Shipment.class));
    }

    @Test
    void testUpdateStatusFailedInvalidTransition() {
        dummyShipment.setDriverId(driverId);

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(dummyShipment));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            shipmentService.updateStatus(shipmentId, ShipmentStatus.TIBA_DI_TUJUAN);
        });

        assertTrue(exception.getMessage().contains("Transisi status tidak valid"));
        verify(shipmentRepository, never()).save(any(Shipment.class));
    }

    @Test
    void testUpdateStatusFailedShipmentNotFound() {
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            shipmentService.updateStatus(shipmentId, ShipmentStatus.MENGIRIM);
        });

        assertEquals("Data Pengiriman Tidak Ditemukan!", exception.getMessage());
        verify(shipmentRepository, never()).save(any(Shipment.class));
    }
}