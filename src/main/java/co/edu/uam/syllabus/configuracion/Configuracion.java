package co.edu.uam.syllabus.configuracion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

/**
 * Configuración institucional: plantilla del documento (UC18), versión del formato Almera (UC19)
 * y estrategia de evaluación (UC20). Cada campo es una clave de institutional_config.
 */
public record Configuracion(
        @NotBlank @Size(max = 200) String universityName,
        @Size(max = 300) String universityLogoUrl,
        @Size(max = 300) String universityAddress,
        @Size(max = 50) String universityPhone,
        @Size(max = 100) String universityEmail,
        @Size(max = 200) String facultyName,
        @Size(max = 200) String departmentName,
        @NotBlank @Size(max = 200) String syllabusHeader,
        @NotBlank @Size(max = 50) String documentCode,
        @NotBlank @Size(max = 20) String documentVersion,
        @NotBlank @Size(max = 20) String documentDate,
        @Size(max = 200) String syllabusSubtitle,
        @Size(max = 300) String footerText,
        @NotNull List<Evaluacion.Corte> evaluationTemplate) {

    static Configuracion de(Map<String, String> v, List<Evaluacion.Corte> plantilla) {
        return new Configuracion(v.get("universityName"), v.get("universityLogoUrl"), v.get("universityAddress"),
                v.get("universityPhone"), v.get("universityEmail"), v.get("facultyName"), v.get("departmentName"),
                v.get("syllabusHeader"), v.get("documentCode"), v.get("documentVersion"), v.get("documentDate"),
                v.get("syllabusSubtitle"), v.get("footerText"), plantilla);
    }
}
