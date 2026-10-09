package co.edu.uam.syllabus.catalogos;

import co.edu.uam.syllabus.usuarios.Usuario;
import java.time.Instant;
import java.util.UUID;

public record AsignaturaResponse(
        UUID id, String name, String code, int credits, int semester,
        ProgramaResponse program, DepartamentoResponse department, Profesor professor,
        String modality, Integer hoursPresencial, Integer hoursDirigido, Integer hoursIndependiente,
        String prerequisites, boolean isActive, long syllabusCount, Instant createdAt, Instant updatedAt) {

    public record Profesor(UUID id, String name, String email) {}

    public static AsignaturaResponse from(Asignatura a, long syllabus) {
        Usuario p = a.getProfesor();
        return new AsignaturaResponse(a.getId(), a.getName(), a.getCode(), a.getCredits(), a.getSemester(),
                ProgramaResponse.from(a.getPrograma()), DepartamentoResponse.from(a.getDepartamento()),
                p == null ? null : new Profesor(p.getId(), p.getName(), p.getEmail()),
                a.getModality(), a.getHoursPresencial(), a.getHoursDirigido(), a.getHoursIndependiente(),
                a.getPrerequisites(), a.isActive(), syllabus, a.getCreatedAt(), a.getUpdatedAt());
    }
}
