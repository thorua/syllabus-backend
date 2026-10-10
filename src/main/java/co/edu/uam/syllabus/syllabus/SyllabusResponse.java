package co.edu.uam.syllabus.syllabus;

import co.edu.uam.syllabus.catalogos.Asignatura;
import co.edu.uam.syllabus.catalogos.AsignaturaResponse;
import co.edu.uam.syllabus.catalogos.DepartamentoResponse;
import co.edu.uam.syllabus.catalogos.ProgramaResponse;
import co.edu.uam.syllabus.configuracion.Evaluacion;
import co.edu.uam.syllabus.usuarios.Usuario;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Syllabus; en el listado no incluye secciones ni versión actual. canManage indica si está dentro
 * del alcance de gestión del usuario (editar, desactivar, ver versiones, según sus permisos).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SyllabusResponse(
        UUID id, String title, String academicPeriod, Syllabus.Estado status,
        String headerTitle, String documentCode, String documentDate,
        Materia subject, Autor createdBy, List<Seccion> sections, Integer currentVersion,
        boolean canManage, Instant createdAt, Instant updatedAt) {

    /** Datos de la asignatura que necesitan la vista del syllabus y su encabezado. */
    public record Materia(UUID id, String name, String code, int credits, int semester,
                          ProgramaResponse program, DepartamentoResponse department,
                          AsignaturaResponse.Profesor professor, String modality, Integer hoursPresencial,
                          Integer hoursDirigido, Integer hoursIndependiente, String prerequisites) {

        static Materia de(Asignatura a) {
            Usuario p = a.getProfesor();
            return new Materia(a.getId(), a.getName(), a.getCode(), a.getCredits(), a.getSemester(),
                    ProgramaResponse.from(a.getPrograma()), DepartamentoResponse.from(a.getDepartamento()),
                    p == null ? null : new AsignaturaResponse.Profesor(p.getId(), p.getName(), p.getEmail()),
                    a.getModality(), a.getHoursPresencial(), a.getHoursDirigido(), a.getHoursIndependiente(),
                    a.getPrerequisites());
        }
    }

    public record Autor(UUID id, String name) {}

    public record Seccion(UUID id, String sectionType, String title, String content,
                          List<Evaluacion.Corte> evaluation, int order) {}

    static SyllabusResponse resumen(Syllabus s, boolean gestionable) {
        return of(s, null, null, gestionable);
    }

    static SyllabusResponse detalle(Syllabus s, int versionActual, boolean gestionable) {
        List<Seccion> secciones = s.getSecciones().stream()
                .map(x -> new Seccion(x.getId(), x.getSectionType(), x.getTitle(), x.getContent(),
                        x.getEvaluation(), x.getOrden()))
                .toList();
        return of(s, secciones, versionActual, gestionable);
    }

    private static SyllabusResponse of(Syllabus s, List<Seccion> secciones, Integer versionActual,
                                       boolean gestionable) {
        return new SyllabusResponse(s.getId(), s.getTitle(), s.getAcademicPeriod(), s.getStatus(),
                s.getHeaderTitle(), s.getDocumentCode(), s.getDocumentDate(), Materia.de(s.getAsignatura()),
                new Autor(s.getCreatedBy().getId(), s.getCreatedBy().getName()), secciones, versionActual,
                gestionable, s.getCreatedAt(), s.getUpdatedAt());
    }
}
