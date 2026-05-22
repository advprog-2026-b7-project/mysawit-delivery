package id.ac.ui.cs.advprog.mysawit.delivery.client;

import id.ac.ui.cs.advprog.mysawit.delivery.dto.external.HarvestDetailResponse;
import id.ac.ui.cs.advprog.mysawit.delivery.dto.external.HarvestQueryResponse;
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
import java.util.UUID;

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

        BigDecimal total = BigDecimal.ZERO;
        int page = 0;
        int pageSize = 100;
        boolean hasMore = true;

        while (hasMore) {
            String url = UriComponentsBuilder.fromHttpUrl(harvestServiceUrl + "/api/v1/harvests")
                    .queryParam("status", "APPROVED")
                    .queryParam("page", page)
                    .queryParam("size", pageSize)
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

                    if (contentList != null && !contentList.isEmpty()) {
                        BigDecimal pageTotal = contentList.stream()
                                .map(item -> {
                                    if (item instanceof Map) {
                                        Object weight = ((Map<?, ?>) item).get("weightKg");
                                        return weight != null ? new BigDecimal(weight.toString()) :
                                                BigDecimal.ZERO;
                                    }
                                    return BigDecimal.ZERO;
                                })
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        total = total.add(pageTotal);
                        page++;
                        hasMore = contentList.size() >= pageSize;
                    } else {
                        hasMore = false;
                    }
                } else {
                    hasMore = false;
                }
            } catch (Exception e) {
                System.err.println("Gagal mengalkulasi data dari Harvest Service: " + e.getMessage());
                hasMore = false;
            }
        }

        return total;
    }

    /**
     * Fetch a single harvest record by its ID.
     * Returns {@code null} if the record cannot be fetched (not found, service down, etc.).
     */
    public HarvestQueryResponse.HarvestItemResponse getHarvestById(
            UUID harvestId, String authHeader) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authHeader);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        String url = harvestServiceUrl + "/api/v1/harvests/" + harvestId;
        try {
            ResponseEntity<HarvestDetailResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, HarvestDetailResponse.class);
            if (response.getBody() != null) {
                return response.getBody().getData();
            }
        } catch (Exception e) {
            System.err.println(
                    "Gagal mengambil harvest " + harvestId + ": " + e.getMessage());
        }
        return null;
    }
}