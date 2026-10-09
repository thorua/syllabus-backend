package co.edu.uam.syllabus.common;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuración propia de la aplicación; todos los valores llegan por variables de entorno. */
@ConfigurationProperties("app")
public record AppProperties(String url, Jwt jwt, String mailFrom, Duration passwordResetExpiresIn, Admin admin) {

    public record Jwt(String secret, Duration expiresIn) {}

    public record Admin(String email, String password) {}
}
