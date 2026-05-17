package id.ac.ui.cs.advprog.mysawit.delivery;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ActiveProfiles("test")
class MainTest {

    @Test
    void mainRuns() {
        assertDoesNotThrow(() ->
                MysawitDeliveryApplication.main(new String[]{
                        "--spring.flyway.enabled=false",
                        "--spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
                        "--spring.datasource.driver-class-name=org.h2.Driver",
                        "--spring.datasource.username=sa",
                        "--spring.datasource.password=",
                        "--spring.jpa.hibernate.ddl-auto=create-drop",
                        "--spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                        "--spring.profiles.active=test"
                }));
    }
}