package co.edu.uam.syllabus.catalogos;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;

/** programCount y subjectCount solo se incluyen en el listado de departamentos. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DepartamentoResponse(
        UUID id, String name, String code, boolean isActive, Instant createdAt, Instant updatedAt,
        Long programCount, Long subjectCount) {

    public static DepartamentoResponse from(Departamento d) {
        return d == null ? null : conConteos(d, null, null);
    }

    static DepartamentoResponse conConteos(Departamento d, Long programas, Long asignaturas) {
        return new DepartamentoResponse(d.getId(), d.getName(), d.getCode(), d.isActive(),
                d.getCreatedAt(), d.getUpdatedAt(), programas, asignaturas);
    }
}
