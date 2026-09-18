package io.github.smaykell.aulavirtual;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// Se omite en lugar de fallar cuando no hay Postgres, para que el build funcione recien clonado.
@SpringBootTest
@ActiveProfiles("test")
@EnabledIf("databaseIsReachable")
class AulaVirtualApplicationTests {

    private static final int LOGIN_TIMEOUT_SECONDS = 3;

    @Test
    void the_context_starts() {
    }

    static boolean databaseIsReachable() {
        String url = environmentOrDefault("DB_URL",
                "jdbc:postgresql://localhost:5432/aula_virtual_test");
        String user = environmentOrDefault("DB_USER", "aula_virtual");
        String password = environmentOrDefault("DB_PASSWORD", "aula_virtual");

        DriverManager.setLoginTimeout(LOGIN_TIMEOUT_SECONDS);
        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            return connection.isValid(LOGIN_TIMEOUT_SECONDS);
        } catch (SQLException ex) {
            return false;
        }
    }

    private static String environmentOrDefault(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
