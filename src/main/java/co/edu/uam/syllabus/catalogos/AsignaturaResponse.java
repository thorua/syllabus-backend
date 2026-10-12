package co.edu.uam.syllabus.catalogos;

import co.edu.uam.syllabus.usuarios.Usuario;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Asignatura con su responsable (departamento o, en posgrado, programa) y los programas en cuyos
 * planes de estudio aparece, con el semestre.
 */
public record AsignaturaResponse(
        UUID id, String name, String code, int credits,
        DepartamentoResponse department, ProgramaResponse program, List<EnPlan> programs, Profesor professor,
        String modality, Integer hoursPresencial, Integer hoursDirigido, Integer hoursIndependiente,
        String prerequisites, boolean isActive, long syllabusCount, Instant createdAt, Instant updatedAt) {

    public record Profesor(UUID id, String name, String email) {}

    /** Programa que incluye la asignatura en uno de sus planes de estudio. */
    public record EnPlan(UUID id, String name, String code, Programa.Nivel level, UUID planId, String plan,
                         boolean planActive, int semester) {

        public static EnPlan de(PlanAsignatura pa) {
            Programa p = pa.getPlan().getPrograma();
            return new EnPlan(p.getId(), p.getName(), p.getCode(), p.getLevel(), pa.getPlan().getId(),
                    pa.getPlan().getName(), pa.getPlan().isActive(), pa.getSemester());
        }
    }

    public static AsignaturaResponse from(Asignatura a, long syllabus, List<PlanAsignatura> planes) {
        Usuario p = a.getProfesor();
        return new AsignaturaResponse(a.getId(), a.getName(), a.getCode(), a.getCredits(),
                DepartamentoResponse.from(a.getDepartamento()), a.getPrograma() == null ? null
                        : ProgramaResponse.from(a.getPrograma()),
                planes.stream().map(EnPlan::de).toList(),
                p == null ? null : new Profesor(p.getId(), p.getName(), p.getEmail()),
                a.getModality(), a.getHoursPresencial(), a.getHoursDirigido(), a.getHoursIndependiente(),
                a.getPrerequisites(), a.isActive(), syllabus, a.getCreatedAt(), a.getUpdatedAt());
    }
}
