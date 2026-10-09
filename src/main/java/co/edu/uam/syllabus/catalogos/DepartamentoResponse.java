package co.edu.uam.syllabus.catalogos;

import java.time.Instant;
import java.util.UUID;

public record DepartamentoResponse(
        UUID id, String name, String code, boolean isActive, Instant createdAt, Instant updatedAt) {

    public static DepartamentoResponse from(Departamento d) {
        return d == null ? null
                : new DepartamentoResponse(d.getId(), d.getName(), d.getCode(), d.isActive(), d.getCreatedAt(), d.getUpdatedAt());
    }
}
