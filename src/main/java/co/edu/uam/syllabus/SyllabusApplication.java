package co.edu.uam.syllabus;

import java.util.Locale;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SyllabusApplication {

    public static void main(String[] args) {
        // Mensajes de validación (Bean Validation) en español.
        Locale.setDefault(Locale.forLanguageTag("es-CO"));
        SpringApplication.run(SyllabusApplication.class, args);
    }
}
