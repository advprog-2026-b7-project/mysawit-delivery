package id.ac.ui.cs.advprog.mysawit.delivery.dto.external;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class HarvestQueryResponse {
    private String message; // Menangkap field "success" atau pesan status dari ApiSuccessResponse
    private HarvestPageData data;

    @Getter
    @Setter
    public static class HarvestPageData {
        // Menangkap list harvest yang ada di dalam HarvestPageResponse
        // Catatan: Pastikan nama field 'harvests' ini sama dengan nama list yang ada di kelas HarvestPageResponse kamu
        private List<HarvestItemResponse> content;
    }

    @Getter
    @Setter
    public static class HarvestItemResponse {
        private UUID id;
        private String plantationId;
        private BigDecimal weightKg; // COCOK: sama dengan nama field di model Harvest kamu
        private String status;       // "APPROVED"
    }
}