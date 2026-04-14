package id.ac.ui.cs.advprog.mysawit.delivery;

import org.junit.jupiter.api.Test;

class MainTest {

    @Test
    void mainRuns() {
        System.setProperty("spring.profiles.active", "test");
        MysawitDeliveryApplication.main(new String[]{});
    }
}