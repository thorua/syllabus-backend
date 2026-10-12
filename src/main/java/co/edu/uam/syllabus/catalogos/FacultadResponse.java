package co.edu.uam.syllabus.catalogos;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;

/** departmentCount y programCount solo se incluyen en el listado de facultades. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FacultadResponse(
        UUID id, String name, String code, boolean isActive, Instant createdAt, Instant updatedAt,
        Long departmentCount, Long programCount) {

    public static FacultadResponse from(Facultad f) {
        return f == null ? null : conConteos(f, null, null);
    }

    static FacultadResponse conConteos(Facultad f, Long departamentos, Long programas) {
        return new FacultadResponse(f.getId(), f.getName(), f.getCode(), f.isActive(),
                f.getCreatedAt(), f.getUpdatedAt(), departamentos, programas);
    }
}
