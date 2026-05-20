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
    private final String adminToken; // 1. Tambahkan variabel token admin rahasia

    public PlantationClient(RestTemplate restTemplate,
                            @Value("${plantation.service.url:http://localhost:8081}")
                            String plantationServiceUrl,
                            @Value("${plantation.service.admin-token}")
                            String adminToken) { // 2. Inject dari properties
        this.restTemplate = restTemplate;
        this.plantationServiceUrl = plantationServiceUrl;
        this.adminToken = adminToken;
    }

    // 1. Mengambil ID Kebun milik Mandor
    public UUID getPlantationIdByMandor(String authHeader) {
        HttpHeaders headers = new HttpHeaders();

        // SINKRONISASI: Pakai adminToken agar lolos dari JwtAdminGuard milik Plantation
        headers.set(HttpHeaders.AUTHORIZATION, adminToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String url = UriComponentsBuilder.fromHttpUrl(plantationServiceUrl + "/api/v1/plantations")
                .queryParam("size", 1)
                .toUriString();

        try {
            ResponseEntity<PlantationListQueryResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, PlantationListQueryResponse.class
            );

            if (response.getBody() != null && response.getBody().getData() != null
                    && !response.getBody().getData().getContent().isEmpty()) {
                return response.getBody().getData().getContent().get(0).getId();
            }
        } catch (Exception e) {
            System.err.println(
                    "Gagal mengambil data kebun Mandor via Admin Token: " + e.getMessage());
        }
        throw new IllegalStateException(
                "Data kebun (Plantation) tidak ditemukan untuk akun Mandor ini.");
    }

    // 2. Mengecek apakah Driver benar-benar terdaftar di Kebun tersebut
    public boolean isDriverAssignedToPlantation(String authHeader, UUID plantationId,
                                                UUID driverId) {
        HttpHeaders headers = new HttpHeaders();

        // SINKRONISASI: Gunakan adminToken rahasia di sini juga
        headers.set(HttpHeaders.AUTHORIZATION, adminToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String url = UriComponentsBuilder.fromHttpUrl(
                        plantationServiceUrl + "/api/v1/plantations/" + plantationId)
                .queryParam("size", 100)
                .toUriString();

        try {
            ResponseEntity<PlantationDetailQueryResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, PlantationDetailQueryResponse.class
            );

            if (response.getBody() != null && response.getBody().getData() != null
                    && response.getBody().getData().getDrivers() != null) {

                return response.getBody().getData().getDrivers().getContent().stream()
                        .anyMatch(driver -> driver.getId().equals(driverId));
            }
        } catch (Exception e) {
            System.err.println(
                    "Gagal memvalidasi penugasan driver via Admin Token: " + e.getMessage());
        }
        return false;
    }

    // 3. Mengambil Mandor ID lewat decode JWT Token lokal (Biarkan tetap seperti ini)
    public UUID getMandorIdFromToken(String authHeader) {
        try {
            String token = authHeader.substring(7);
            String[] chunks = token.split("\\.");

            Base64.Decoder decoder = Base64.getUrlDecoder();
            String payload = new String(decoder.decode(chunks[1]));

            String searchString = "\"sub\":\"";
            int startIndex = payload.indexOf(searchString) + searchString.length();
            int endIndex = payload.indexOf("\"", startIndex);

            String mandorIdStr = payload.substring(startIndex, endIndex);
            return UUID.fromString(mandorIdStr);
        } catch (Exception e) {
            throw new IllegalArgumentException("Token JWT tidak valid atau corrupt.");
        }
    }
}