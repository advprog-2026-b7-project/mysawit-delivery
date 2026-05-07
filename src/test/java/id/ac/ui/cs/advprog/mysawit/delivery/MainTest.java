package id.ac.ui.cs.advprog.mysawit.delivery;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ActiveProfiles("test")
class MainTest {

    @Test
    void mainRuns() {
        assertDoesNotThrow(() ->
                MysawitDeliveryApplication.main(new String[]{}));
    }
}