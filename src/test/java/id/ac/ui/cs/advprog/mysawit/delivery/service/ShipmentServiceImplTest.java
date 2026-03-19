package id.ac.ui.cs.advprog.mysawit.delivery.service;

import id.ac.ui.cs.advprog.mysawit.delivery.dto.CreateShipmentRequest;
import id.ac.ui.cs.advprog.mysawit.delivery.dto.ShipmentResponse;
import id.ac.ui.cs.advprog.mysawit.delivery.entity.Shipment;
import id.ac.ui.cs.advprog.mysawit.delivery.entity.ShipmentStatus;
import id.ac.ui.cs.advprog.mysawit.delivery.repository.ShipmentRepository;
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
public class ShipmentServiceImplTest {
    @Mock
    ShipmentRepository shipmentRepository;

    @InjectMocks
    ShipmentServiceImpl shipmentService;

    private UUID shipmentId;
    private UUID plantationId;
    private UUID mandorId;
    private UUID driverId;
    private Shipment dummyShipment;

    @BeforeEach
    void setUp(){
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
    }

    @Test
    void testCreateShipmentSuccess(){
        CreateShipmentRequest request = new CreateShipmentRequest();
        request.setPlantationId(plantationId);
        request.setMandorId(mandorId);
        request.setTotalWeightKg(new BigDecimal("350.00"));

        when(shipmentRepository.save(any(Shipment.class))).thenReturn(dummyShipment);

        ShipmentResponse response = shipmentService.createShipment(request);

        assertNotNull(response);
        assertEquals(plantationId, response.getPlantationId());
        assertEquals(new BigDecimal("350.00"), response.getTotalWeightKg());
        assertEquals(ShipmentStatus.MEMUAT, response.getStatus());
        verify(shipmentRepository, times(1)).save(any(Shipment.class));
    }

    @Test
    void testCreateShipmentFailedExceedWeightLimit(){
        CreateShipmentRequest request = new CreateShipmentRequest();
        request.setTotalWeightKg(new BigDecimal("450.00"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            shipmentService.createShipment(request);
        });

        assertEquals("Berat muatan tidak boleh melebihi 400 kg!", exception.getMessage());
        verify(shipmentRepository, never()).save(any(Shipment.class));
    }

    @Test
    void testCreateShipmentFailedZeroWeight(){
        CreateShipmentRequest request = new CreateShipmentRequest();
        request.setTotalWeightKg(BigDecimal.ZERO);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            shipmentService.createShipment(request);
        });

        assertEquals("Berat muatan harus lebih dari 0 kg!", exception.getMessage());
        verify(shipmentRepository, never()).save(any(Shipment.class));
    }

    @Test
    void testCreateShipmentNullWeight(){
        CreateShipmentRequest request = new CreateShipmentRequest();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            shipmentService.createShipment(request);
        });

        assertEquals("Berat muatan tidak boleh kosong!", exception.getMessage());
        verify(shipmentRepository, never()).save(any(Shipment.class));
    }

    @Test
    void testAssignDriverSuccess(){
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(dummyShipment));
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(dummyShipment);

        ShipmentResponse response = shipmentService.assignDriver(shipmentId, driverId);

        assertNotNull(response);
        assertEquals(driverId, dummyShipment.getDriverId());
        verify(shipmentRepository, times(1)).save(dummyShipment);
    }

    @Test
    void testAssignDriverFailedShipmentNotFound() {
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            shipmentService.assignDriver(shipmentId, driverId);
        });

        assertEquals("Data Pengiriman Tidak Ditemukan!", exception.getMessage());
        verify(shipmentRepository, never()).save(any(Shipment.class));
    }

    @Test
    void testPartialRejectShipmentSuccess(){
        BigDecimal acceptedWeight = new BigDecimal("200");
        String reason = "Kualitas buah sebagian buruk";

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(dummyShipment));
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(dummyShipment);

        ShipmentResponse response = shipmentService.partialRejectShipment(shipmentId, acceptedWeight, reason);

        assertNotNull(response);
        assertEquals(ShipmentStatus.DISETUJUI_PARSIAL, dummyShipment.getStatus());
        assertEquals(acceptedWeight, dummyShipment.getTotalWeightKg());
        assertEquals(reason, dummyShipment.getRejectedReason());
        verify(shipmentRepository, times(1)).save(dummyShipment);
    }

    @Test
    void testPartialRejectShipmentFailedShipmentNotFound(){
        BigDecimal acceptedWeight = new BigDecimal("200");
        String reason = "Kualitas buah sebagian buruk";

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            shipmentService.partialRejectShipment(shipmentId, acceptedWeight, reason);
        });

        assertEquals("Data Pengiriman Tidak Ditemukan!", exception.getMessage());
        verify(shipmentRepository, never()).save(any(Shipment.class));
    }


}
