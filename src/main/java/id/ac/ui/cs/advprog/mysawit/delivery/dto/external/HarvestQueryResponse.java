package id.ac.ui.cs.advprog.mysawit.delivery.dto.external;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class HarvestQueryResponse {
    private String message;
    private HarvestPageData data;

    @Getter
    @Setter
    public static class HarvestPageData {
        private List<HarvestItemResponse> content;
    }

    @Getter
    @Setter
    public static class HarvestItemResponse {
        private UUID id;
        private String plantationId;
        private BigDecimal weightKg;
        private String status;
    }
}