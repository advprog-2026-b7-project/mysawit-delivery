package id.ac.ui.cs.advprog.mysawit.delivery.dto.external;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class PlantationDetailQueryResponse {
    private PlantationDetailData data;

    @Getter
    @Setter
    public static class PlantationDetailData {
        private UUID id;
        private String name;
        private String code;
        private DriverPageData drivers;
    }

    @Getter
    @Setter
    public static class DriverPageData {
        private List<DriverItem> content;
    }

    @Getter
    @Setter
    public static class DriverItem {
        private UUID id;
        private String driverName;
    }
}