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
        private DriverPageData drivers; // Drivers di sini bertipe page response!
    }

    @Getter
    @Setter
    public static class DriverPageData {
        private List<DriverItem> content; // Menangkap daftar driver di dalam kebun
    }

    @Getter
    @Setter
    public static class DriverItem {
        private UUID id;
                // Driver ID (sesuaikan jika di modul plantation menggunakan nama field lain seperti driverId)
        private String driverName;
    }
}