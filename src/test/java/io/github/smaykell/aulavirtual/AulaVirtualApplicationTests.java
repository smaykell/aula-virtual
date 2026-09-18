package io.github.smaykell.aulavirtual;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Comprueba que el contexto completo levanta: JPA valida el esquema, Flyway corre
 * sus migraciones y la cadena de seguridad se construye.
 *
 * <p>Necesita un Postgres accesible, asi que se omite (no falla) cuando no lo hay.
 * De ese modo {@code ./gradlew build} funciona en una maquina recien clonada y, en
 * cuanto la base existe, el test se ejecuta de verdad. Ver el README para crearla.
 */
@SpringBootTest
@ActiveProfiles("test")
@EnabledIf("hayBaseDeDatos")
class AulaVirtualApplicationTests {

    @Test
    void el_contexto_arranca() {
        // El propio arranque del contexto es la asercion.
    }

    static boolean hayBaseDeDatos() {
        String url = variable("DB_URL", "jdbc:postgresql://localhost:5432/aula_virtual_test");
        String usuario = variable("DB_USER", "postgres");
        String clave = variable("DB_PASSWORD", "postgres");

        DriverManager.setLoginTimeout(3);
        try (Connection conexion = DriverManager.getConnection(url, usuario, clave)) {
            return conexion.isValid(3);
        } catch (SQLException ex) {
            return false;
        }
    }

    private static String variable(String nombre, String porDefecto) {
        String valor = System.getenv(nombre);
        return valor == null || valor.isBlank() ? porDefecto : valor;
    }
}
