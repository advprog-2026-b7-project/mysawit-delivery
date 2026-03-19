package id.ac.ui.cs.advprog.mysawit.delivery.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class DefaultWeightValidatorTest {

    private DefaultWeightValidator validator;

    @BeforeEach
    void setUp() {
        validator = new DefaultWeightValidator();
    }

    @Test
    void testValidateSuccess() {
        // Berat normal (harus lolos tanpa error)
        assertDoesNotThrow(() -> validator.validate(new BigDecimal("200.00")));
        assertDoesNotThrow(() -> validator.validate(new BigDecimal("400.00")));
    }

    @Test
    void testValidateFailedNullWeight() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> validator.validate(null));
        assertEquals("Berat muatan tidak boleh kosong!", exception.getMessage());
    }

    @Test
    void testValidateFailedWeightExceedsLimit() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> validator.validate(new BigDecimal("400.01")));
        assertEquals("Berat muatan tidak boleh melebihi 400 kg!", exception.getMessage());
    }

    @Test
    void testValidateFailedZeroWeight() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> validator.validate(BigDecimal.ZERO));
        assertEquals("Berat muatan harus lebih dari 0 kg!", exception.getMessage());
    }

    @Test
    void testValidateFailedNegativeWeight() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> validator.validate(new BigDecimal("-50.00")));
        assertEquals("Berat muatan harus lebih dari 0 kg!", exception.getMessage());
    }
}