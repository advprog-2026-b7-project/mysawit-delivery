package id.ac.ui.cs.advprog.mysawit.delivery.client;

import id.ac.ui.cs.advprog.mysawit.delivery.dto.external.PlantationDetailQueryResponse;
import id.ac.ui.cs.advprog.mysawit.delivery.dto.external.PlantationListQueryResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Base64;
import java.util.UUID;

@Component
public class PlantationClient {

    private final RestTemplate restTemplate;
    private final String plantationServiceUrl;

    @Value("${inter.service.api-key}")
    private String internalApiKey;

    public PlantationClient(RestTemplate restTemplate,
                            @Value("${plantation.service.url:http://localhost:8081}")
                            String plantationServiceUrl) {
        this.restTemplate = restTemplate;
        this.plantationServiceUrl = plantationServiceUrl;
    }

    private HttpEntity<Void> internalRequest() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, internalApiKey);
        return new HttpEntity<>(headers);
    }

    public UUID getPlantationIdByMandor(String authHeader) {
        UUID mandorId = getMandorIdFromToken(authHeader);

        String url = UriComponentsBuilder.fromHttpUrl(plantationServiceUrl + "/api/v1/plantations")
                .queryParam("mandorId", mandorId)
                .queryParam("size", 1)
                .toUriString();

        try {
            ResponseEntity<PlantationListQueryResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, internalRequest(), PlantationListQueryResponse.class);

            if (response.getBody() != null && response.getBody().getData() != null
                    && response.getBody().getData().getContent() != null
                    && !response.getBody().getData().getContent().isEmpty()) {
                return response.getBody().getData().getContent().get(0).getId();
            }
        } catch (Exception e) {
            System.err.println("Gagal mengambil data kebun: " + e.getMessage());
        }
        throw new IllegalStateException("Data kebun tidak ditemukan untuk akun Mandor ini.");
    }

    public boolean isDriverAssignedToPlantation(String authHeader, UUID plantationId,
                                                UUID driverId) {
        String url = UriComponentsBuilder.fromHttpUrl(
                        plantationServiceUrl + "/api/v1/plantations/" + plantationId)
                .queryParam("size", 100)
                .toUriString();
        try {
            ResponseEntity<PlantationDetailQueryResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, internalRequest(), PlantationDetailQueryResponse.class);
            if (response.getBody() != null && response.getBody().getData() != null
                    && response.getBody().getData().getDrivers() != null) {
                return response.getBody().getData().getDrivers().getContent().stream()
                        .anyMatch(driver -> driver.getId().equals(driverId));
            }
        } catch (Exception e) {
            System.err.println("Gagal memvalidasi driver: " + e.getMessage());
        }
        return false;
    }

    public PlantationDetailQueryResponse getPlantationDetail(UUID plantationId) {
        String url = UriComponentsBuilder.fromHttpUrl(
                        plantationServiceUrl + "/api/v1/plantations/" + plantationId)
                .queryParam("size", 100)
                .toUriString();
        try {
            ResponseEntity<PlantationDetailQueryResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, internalRequest(), PlantationDetailQueryResponse.class);
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Gagal mengambil detail kebun: " + e.getMessage());
            return null;
        }
    }

    public UUID getMandorIdFromToken(String authHeader) {
        try {
            String token = authHeader.substring(7);
            String[] chunks = token.split("\\.");
            if (chunks.length < 2) {
                throw new IllegalArgumentException("Token JWT tidak valid");
            }
            Base64.Decoder decoder = Base64.getUrlDecoder();
            String payload = new String(decoder.decode(chunks[1]));
            String searchKey = "\"sub\":";
            int keyIndex = payload.indexOf(searchKey);
            if (keyIndex < 0) {
                throw new IllegalArgumentException("Token JWT tidak memiliki sub claim");
            }
            int startIndex = keyIndex + searchKey.length();
            String remaining = payload.substring(startIndex).trim();
            if (remaining.startsWith("\"")) {
                startIndex = keyIndex + searchKey.length() + 1;
                int endIndex = payload.indexOf("\"", startIndex);
                return UUID.fromString(payload.substring(startIndex, endIndex));
            } else {
                int endIndex = remaining.indexOf(",");
                if (endIndex < 0) endIndex = remaining.indexOf("}");
                if (endIndex < 0) endIndex = remaining.length();
                return UUID.fromString(remaining.substring(0, endIndex).trim());
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Token JWT tidak valid atau corrupt: " + e.getMessage());
        }
    }
}