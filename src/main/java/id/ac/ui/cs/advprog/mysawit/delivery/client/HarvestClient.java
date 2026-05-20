package id.ac.ui.cs.advprog.mysawit.delivery.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
public class HarvestClient {

    private final RestTemplate restTemplate;
    private final String harvestServiceUrl;

    public HarvestClient(RestTemplate restTemplate,
                         @Value("${harvest.service.url:http://localhost:8083}")
                         String harvestServiceUrl) {
        this.restTemplate = restTemplate;
        this.harvestServiceUrl = harvestServiceUrl;
    }

    public BigDecimal getTotalApprovedWeight(String authHeader) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authHeader);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // Memanggil API Harvest bawaan dengan menyaring status APPROVED
        String url = UriComponentsBuilder.fromHttpUrl(harvestServiceUrl + "/api/v1/harvests")
                .queryParam("status", "APPROVED")
                .queryParam("size", 100)
                .toUriString();

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, Map.class
            );

            if (response.getBody() != null && response.getBody().get("data") != null) {
                Map<?, ?> rootData = (Map<?, ?>) response.getBody().get("data");
                List<?> contentList = null;
                if (rootData.get("content") != null) {
                    contentList = (List<?>) rootData.get("content");
                } else if (rootData.get("harvests") != null) {
                    contentList = (List<?>) rootData.get("harvests");
                }

                if (contentList != null) {
                    return contentList.stream()
                            .map(item -> {
                                if (item instanceof Map) {
                                    Object weight = ((Map<?, ?>) item).get("weightKg");
                                    return weight != null ? new BigDecimal(weight.toString()) :
                                            BigDecimal.ZERO;
                                }
                                return BigDecimal.ZERO;
                            })
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                }
            }
        } catch (Exception e) {
            System.err.println("Gagal mengalkulasi data dari Harvest Service: " + e.getMessage());
        }

        return BigDecimal.ZERO;
    }
}