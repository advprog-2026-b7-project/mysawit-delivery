package id.ac.ui.cs.advprog.mysawit.delivery.dto.external;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class PlantationListQueryResponse {
    private PlantationPageData data;

    @Getter
    @Setter
    public static class PlantationPageData {
        private List<PlantationItem> content; // Menangkap daftar kebun
    }

    @Getter
    @Setter
    public static class PlantationItem {
        private UUID id; // Ini Plantation ID
        private String name;
        private String code;
    }
}