package id.ac.ui.cs.advprog.mysawit.delivery.config;

import id.ac.ui.cs.advprog.mysawit.delivery.client.HarvestClient;
import id.ac.ui.cs.advprog.mysawit.delivery.client.PlantationClient;
import id.ac.ui.cs.advprog.mysawit.delivery.service.ShipmentService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that JwtFilter + SecurityConfig work together correctly:
 * - missing token          → 401
 * - invalid/expired token  → 401
 * - valid token, any role  → 200 on authenticated() routes
 * - MANDOR token           → 200 on hasRole("MANDOR") routes
 * - non-MANDOR token       → 403 on hasRole("MANDOR") routes
 *
 * Uses @SpringBootTest + @AutoConfigureMockMvc so the full servlet filter chain
 * is assembled (including FilterRegistrationBean.setEnabled(false) for JwtFilter),
 * which prevents the double-registration problem that occurs with @WebMvcTest slicing.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtFilterSecurityTest {

    // Must match jwt.secret in application-test.properties (pinned — not
    // subject to spring-dotenv .env override during test runs).
    private static final String TEST_SECRET = "dev-secret-key-dev-secret-key-123456";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ShipmentService shipmentService;

    @MockBean
    private HarvestClient harvestClient;

    @MockBean
    private PlantationClient plantationClient;

    // ── helpers ──────────────────────────────────────────────────────────────

    private String token(String userId, String role, long ttlMs) {
        SecretKey key = Keys.hmacShaKeyFor(
                TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(userId)
                .claim("role", role)
                .expiration(new Date(System.currentTimeMillis() + ttlMs))
                .signWith(key)
                .compact();
    }

    private String validToken(String role) {
        return token("user-" + role.toLowerCase(), role, 3_600_000L);
    }

    // ── 401 cases ────────────────────────────────────────────────────────────

    @Test
    void noAuthHeader_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/deliveries"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void malformedToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/deliveries")
                        .header("Authorization", "Bearer not.a.real.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredToken_returns401() throws Exception {
        String expired = token("user-1", "BURUH", -60_000L);
        mockMvc.perform(get("/api/v1/deliveries")
                        .header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongSecretToken_returns401() throws Exception {
        SecretKey otherKey = Keys.hmacShaKeyFor(
                "completely-different-secret-key-xyz".getBytes(StandardCharsets.UTF_8));
        String bad = Jwts.builder()
                .subject("user-1")
                .claim("role", "BURUH")
                .expiration(new Date(System.currentTimeMillis() + 3_600_000L))
                .signWith(otherKey)
                .compact();
        mockMvc.perform(get("/api/v1/deliveries")
                        .header("Authorization", "Bearer " + bad))
                .andExpect(status().isUnauthorized());
    }

    // ── authenticated() route ────────────────────────────────────────────────

    @Test
    void validBuruhToken_accessesAuthenticatedEndpoint() throws Exception {
        when(shipmentService.getAllShipments()).thenReturn(List.of());
        mockMvc.perform(get("/api/v1/deliveries")
                        .header("Authorization", "Bearer " + validToken("BURUH")))
                .andExpect(status().isOk());
    }

    @Test
    void validSupirToken_accessesAuthenticatedEndpoint() throws Exception {
        UUID driverId = UUID.randomUUID();
        when(shipmentService.getAssignedDeliveriesForDriver(driverId))
                .thenReturn(List.of());
        mockMvc.perform(get("/api/v1/deliveries/driver/{id}/assigned", driverId)
                        .header("Authorization", "Bearer " + validToken("SUPIR")))
                .andExpect(status().isOk());
    }

    // ── MANDOR-only route ────────────────────────────────────────────────────

    @Test
    void mandorToken_accessesMandorOnlyEndpoint() throws Exception {
        when(plantationClient.getPlantationIdByMandor(anyString()))
                .thenReturn(UUID.randomUUID());
        mockMvc.perform(get("/api/v1/deliveries/my-plantation")
                        .header("Authorization", "Bearer " + validToken("MANDOR")))
                .andExpect(status().isOk());
    }

    @Test
    void supirToken_forbiddenOnMandorOnlyEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/deliveries/my-plantation")
                        .header("Authorization", "Bearer " + validToken("SUPIR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void buruhToken_forbiddenOnMandorOnlyEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/deliveries/my-plantation")
                        .header("Authorization", "Bearer " + validToken("BURUH")))
                .andExpect(status().isForbidden());
    }
}
