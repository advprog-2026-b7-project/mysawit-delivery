package id.ac.ui.cs.advprog.mysawit.delivery.client;

import id.ac.ui.cs.advprog.mysawit.delivery.entity.Shipment;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class PaymentPayrollClient {

    private static final BigDecimal NET_MULTIPLIER = new BigDecimal("0.90");
    private static final Logger logger = LoggerFactory.getLogger(PaymentPayrollClient.class);

    private final RestTemplate restTemplate;
    private final String paymentBaseUrl;

    public PaymentPayrollClient(
            RestTemplate restTemplate,
            @Value("${payment.service.base-url:http://localhost:8084}") String paymentBaseUrl) {
        this.restTemplate = restTemplate;
        this.paymentBaseUrl = paymentBaseUrl.replaceAll("/+$", "");
    }

    @Async
    public void createDriverPayroll(Shipment shipment, String authorizationHeader) {
        BigDecimal wage = readWageSetting("supirTruckWagePerKg");
        BigDecimal amount = calculateAmount(wage, shipment.getTotalWeightKg());
        Map<String, Object> body = Map.of(
                "driverId", shipment.getDriverId().toString(),
                "driverName", shipment.getDriverId().toString(),
                "driverAmount", amount,
                "deliveryId", shipment.getId().toString(),
                "driverDescription", "Delivery payout - " + shipment.getTotalWeightKg()
                        + " kg @ Rp" + wage + "/kg (after 10% fee)"
        );
        sendDeliveryPayroll(body, authorizationHeader, shipment.getId().toString());
    }

    @Async
    public void createMandorPayroll(Shipment shipment, String authorizationHeader) {
        BigDecimal recognizedWeight = shipment.getRecognizedWeightKg() == null
                ? shipment.getTotalWeightKg()
                : shipment.getRecognizedWeightKg();
        BigDecimal wage = readWageSetting("mandorWagePerKg");
        BigDecimal amount = calculateAmount(wage, recognizedWeight);
        Map<String, Object> body = Map.of(
                "mandorId", shipment.getMandorId().toString(),
                "mandorName", shipment.getMandorId().toString(),
                "mandorAmount", amount,
                "deliveryId", shipment.getId().toString(),
                "mandorDescription", "Delivery mandor payout - " + recognizedWeight
                        + " kg @ Rp" + wage + "/kg (after 10% fee)"
        );
        sendDeliveryPayroll(body, authorizationHeader, shipment.getId().toString());
    }

    private void sendDeliveryPayroll(
            Map<String, Object> body,
            String authorizationHeader,
            String deliveryId) {
        try {
            restTemplate.exchange(
                    paymentBaseUrl + "/api/payroll/delivery/create",
                    HttpMethod.POST,
                    new HttpEntity<>(body, authHeaders(authorizationHeader)),
                    Map.class
            );
        } catch (Exception ex) {
            logger.error("Failed to create delivery payroll for {}: {}",
                    deliveryId, ex.getMessage());
        }
    }

    private BigDecimal readWageSetting(String fieldName) {
        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.getForObject(
                paymentBaseUrl + "/api/payment/wage-settings", Map.class);
        Object data = response == null ? null : response.get("data");
        if (!(data instanceof Map<?, ?> dataMap)) {
            throw new IllegalStateException("Invalid wage settings response");
        }
        Object value = dataMap.get(fieldName);
        if (value == null) {
            throw new IllegalStateException("Missing wage setting: " + fieldName);
        }
        return new BigDecimal(String.valueOf(value));
    }

    private BigDecimal calculateAmount(BigDecimal wagePerKg, BigDecimal weightKg) {
        return wagePerKg.multiply(weightKg)
                .multiply(NET_MULTIPLIER)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private HttpHeaders authHeaders(String authorizationHeader) {
        HttpHeaders headers = new HttpHeaders();
        if (authorizationHeader != null && !authorizationHeader.isBlank()) {
            headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
        }
        return headers;
    }
}
