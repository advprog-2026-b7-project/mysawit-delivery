package id.ac.ui.cs.advprog.mysawit.delivery.service;

import id.ac.ui.cs.advprog.mysawit.delivery.dto.CreateShipmentRequest;
import id.ac.ui.cs.advprog.mysawit.delivery.dto.ShipmentResponse;
import id.ac.ui.cs.advprog.mysawit.delivery.entity.Shipment;
import id.ac.ui.cs.advprog.mysawit.delivery.entity.ShipmentStatus;
import id.ac.ui.cs.advprog.mysawit.delivery.mapper.ShipmentMapper;
import id.ac.ui.cs.advprog.mysawit.delivery.repository.ShipmentRepository;
import id.ac.ui.cs.advprog.mysawit.delivery.validator.WeightValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShipmentServiceImpl implements ShipmentService{

    private final ShipmentRepository shipmentRepository;
    private final WeightValidator weightValidator;
    private final ShipmentMapper shipmentMapper;

    @Override
    public ShipmentResponse createShipment(CreateShipmentRequest request){
        weightValidator.validate(request.getTotalWeightKg());

        Shipment shipment = Shipment.builder()
                            .plantationId(request.getPlantationId())
                            .mandorId(request.getMandorId())
                            .totalWeightKg(request.getTotalWeightKg())
                            .status(ShipmentStatus.MEMUAT)
                            .build();

        Shipment savedShipment = shipmentRepository.save(shipment);
        return shipmentMapper.toResponse(savedShipment);
    }

    @Override
    public ShipmentResponse assignDriver(UUID shipmentId, UUID driverId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Data Pengiriman Tidak Ditemukan!"));

        shipment.setDriverId(driverId);
        Shipment updatedShipment = shipmentRepository.save(shipment);
        return shipmentMapper.toResponse(updatedShipment);
    }

    @Override
    public ShipmentResponse partialRejectShipment(
            UUID shipmentId, BigDecimal acceptedWeight, String reason){

        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Data Pengiriman Tidak Ditemukan!"));

        shipment.setStatus(ShipmentStatus.DISETUJUI_PARSIAL);
        shipment.setTotalWeightKg(acceptedWeight);
        shipment.setRejectedReason(reason);

        Shipment updatedShipment = shipmentRepository.save(shipment);
        return shipmentMapper.toResponse(updatedShipment);
    }
}
