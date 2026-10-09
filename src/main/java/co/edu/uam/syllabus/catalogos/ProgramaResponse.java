package co.edu.uam.syllabus.catalogos;

import java.time.Instant;
import java.util.UUID;

public record ProgramaResponse(
        UUID id, String name, String code, Programa.Nivel level, DepartamentoResponse department,
        boolean isActive, Instant createdAt, Instant updatedAt) {

    public static ProgramaResponse from(Programa p) {
        return new ProgramaResponse(p.getId(), p.getName(), p.getCode(), p.getLevel(),
                DepartamentoResponse.from(p.getDepartamento()), p.isActive(), p.getCreatedAt(), p.getUpdatedAt());
    }
}
