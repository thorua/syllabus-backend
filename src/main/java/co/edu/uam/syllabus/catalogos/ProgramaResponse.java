package co.edu.uam.syllabus.catalogos;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;

/** subjectCount solo se incluye en el listado de programas. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProgramaResponse(
        UUID id, String name, String code, Programa.Nivel level, DepartamentoResponse department,
        boolean isActive, Instant createdAt, Instant updatedAt, Long subjectCount) {

    public static ProgramaResponse from(Programa p) {
        return conConteo(p, null);
    }

    static ProgramaResponse conConteo(Programa p, Long asignaturas) {
        return new ProgramaResponse(p.getId(), p.getName(), p.getCode(), p.getLevel(),
                DepartamentoResponse.from(p.getDepartamento()), p.isActive(), p.getCreatedAt(), p.getUpdatedAt(),
                asignaturas);
    }
}
