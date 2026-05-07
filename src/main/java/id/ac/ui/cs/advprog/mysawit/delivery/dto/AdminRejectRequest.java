package id.ac.ui.cs.advprog.mysawit.delivery.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminRejectRequest {
    @NotBlank(message = "Alasan penolakan tidak boleh kosong")
    private String reason;
    private boolean isPartial;
    @PositiveOrZero(message = "Berat yang diakui tidak boleh minus")
    private BigDecimal recognizedKg;
}
