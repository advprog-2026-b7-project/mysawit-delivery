package id.ac.ui.cs.advprog.mysawit.delivery.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApiSuccessResponse<T> {
    private String status = "success"; // Default status agar seragam dengan modul lain
    private T data;

    // Constructor kosong (diperlukan untuk deserialisasi library seperti Jackson)
    public ApiSuccessResponse() {
    }

    // Constructor utama untuk membungkus data response
    public ApiSuccessResponse(T data) {
        this.data = data;
    }
}