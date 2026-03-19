package id.ac.ui.cs.advprog.mysawit.delivery.validator;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DefaultWeightValidator implements WeightValidator{
    private static final BigDecimal MAX_WEIGHT_LIMIT = new BigDecimal("400.00");

    @Override
    public void validate(BigDecimal weight){
        if (weight == null) {
            throw new IllegalArgumentException("Berat muatan tidak boleh kosong!");
        }
        if (weight.compareTo(MAX_WEIGHT_LIMIT) > 0) {
            throw new IllegalArgumentException("Berat muatan tidak boleh melebihi 400 kg!");
        }
        if (weight.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Berat muatan harus lebih dari 0 kg!");
        }
    }
}
