package co.edu.uam.syllabus.catalogos;

import static co.edu.uam.syllabus.catalogos.CatalogoService.conflicto;

import co.edu.uam.syllabus.auditoria.AuditEvent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Planes de estudio de cada programa y su malla curricular por semestre, con el estado del
 * syllabus de cada asignatura en un período: verificado (firmado por la coordinación y por
 * Registro), pendiente de firma, en borrador o sin syllabus.
 */
@Service
public class PlanEstudiosService {

    private static final String ENTIDAD = "study_plan";

    /** Estado del syllabus de una asignatura en la malla. */
    public enum EstadoMalla { VERIFICADO, PENDIENTE_REGISTRO, PENDIENTE_FIRMA, BORRADOR, SIN_SYLLABUS }

    public record PlanRequest(@NotNull UUID programId, @NotBlank @Size(max = 100) String name, Boolean isActive) {}

    public record AsignaturaEnPlanRequest(@Min(1) @Max(12) int semester) {}

    public record PlanResponse(UUID id, UUID programId, String programName, String name, boolean isActive,
                               int subjectCount) {
        static PlanResponse de(PlanEstudios p) {
            return new PlanResponse(p.getId(), p.getPrograma().getId(), p.getPrograma().getName(), p.getName(),
                    p.isActive(), p.getAsignaturas().size());
        }
    }

    public record Celda(UUID subjectId, String code, String name, int credits, String offeredBy, boolean isActive,
                        UUID syllabusId, EstadoMalla state) {}

    public record Semestre(int number, int credits, List<Celda> subjects) {}

    public record MallaResponse(PlanResponse plan, String academicPeriod, List<Semestre> semesters) {}

    private record SyllabusEnMalla(UUID id, EstadoMalla estado) {}

    private final PlanEstudiosRepository planes;
    private final AsignaturaRepository asignaturas;
    private final CatalogoService catalogo;
    private final JdbcClient jdbc;
    private final ApplicationEventPublisher eventos;

    public PlanEstudiosService(PlanEstudiosRepository planes, AsignaturaRepository asignaturas,
                               CatalogoService catalogo, JdbcClient jdbc, ApplicationEventPublisher eventos) {
        this.planes = planes;
        this.asignaturas = asignaturas;
        this.catalogo = catalogo;
        this.jdbc = jdbc;
        this.eventos = eventos;
    }

    @Transactional(readOnly = true)
    public List<PlanResponse> listar(UUID programaId) {
        return planes.findByProgramaIdOrderByName(programaId).stream().map(PlanResponse::de).toList();
    }

    /** Malla del plan con el estado del syllabus de cada asignatura en el período. */
    @Transactional(readOnly = true)
    public MallaResponse malla(UUID id, String periodo) {
        PlanEstudios plan = buscar(id);
        Map<UUID, SyllabusEnMalla> syllabus = new HashMap<>();
        if (!plan.getAsignaturas().isEmpty()) {
            jdbc.sql("""
                    SELECT y.subject_id, y.id, y.status,
                           bool_or(f.step = 'REGISTRO') AS registro, bool_or(f.step = 'DEPARTAMENTO') AS departamento
                    FROM syllabus y
                    LEFT JOIN syllabus_signatures f ON f.syllabus_id = y.id AND f.version_number =
                        (SELECT max(v.version_number) FROM syllabus_versions v WHERE v.syllabus_id = y.id)
                    WHERE y.academic_period = :periodo AND y.subject_id IN (:ids)
                    GROUP BY y.subject_id, y.id, y.status""")
                    .param("periodo", periodo)
                    .param("ids", plan.getAsignaturas().stream().map(pa -> pa.getAsignatura().getId()).toList())
                    .query((rs, i) -> syllabus.put(rs.getObject("subject_id", UUID.class), new SyllabusEnMalla(
                            rs.getObject("id", UUID.class), estado(rs.getString("status"),
                                    rs.getBoolean("registro"), rs.getBoolean("departamento")))))
                    .list();
        }
        Map<Integer, List<Celda>> porSemestre = new TreeMap<>();
        plan.getAsignaturas().stream()
                .sorted(Comparator.comparing((PlanAsignatura pa) -> pa.getAsignatura().getCode()))
                .forEach(pa -> {
                    Asignatura a = pa.getAsignatura();
                    SyllabusEnMalla y = syllabus.get(a.getId());
                    porSemestre.computeIfAbsent(pa.getSemester(), s -> new ArrayList<>()).add(new Celda(
                            a.getId(), a.getCode(), a.getName(), a.getCredits(), a.responsable(), a.isActive(),
                            y == null ? null : y.id(), y == null ? EstadoMalla.SIN_SYLLABUS : y.estado()));
                });
        List<Semestre> semestres = porSemestre.entrySet().stream()
                .map(e -> new Semestre(e.getKey(), e.getValue().stream().mapToInt(Celda::credits).sum(), e.getValue()))
                .toList();
        return new MallaResponse(PlanResponse.de(plan), periodo, semestres);
    }

    @Transactional
    public PlanResponse crear(UUID actor, PlanRequest req) {
        Programa programa = catalogo.buscarPrograma(req.programId());
        String nombre = req.name().trim();
        if (planes.existsByProgramaIdAndNameIgnoreCase(programa.getId(), nombre)) {
            throw conflicto("El programa ya tiene un plan de estudios con ese nombre.");
        }
        PlanEstudios plan = new PlanEstudios(programa);
        plan.setName(nombre);
        planes.save(plan);
        publicar(actor, "CREATE", plan, Map.of());
        return PlanResponse.de(plan);
    }

    @Transactional
    public PlanResponse actualizar(UUID actor, UUID id, PlanRequest req) {
        PlanEstudios plan = buscar(id);
        String nombre = req.name().trim();
        if (planes.existsByProgramaIdAndNameIgnoreCaseAndIdNot(plan.getPrograma().getId(), nombre, id)) {
            throw conflicto("El programa ya tiene un plan de estudios con ese nombre.");
        }
        plan.setName(nombre);
        if (req.isActive() != null) {
            plan.setActive(req.isActive());
        }
        publicar(actor, "UPDATE", plan, Map.of());
        return PlanResponse.de(plan);
    }

    /** Agrega la asignatura al plan en el semestre indicado o la mueve de semestre. */
    @Transactional
    public MallaResponse ubicarAsignatura(UUID actor, UUID id, UUID asignaturaId, int semestre, String periodo) {
        PlanEstudios plan = buscar(id);
        Asignatura a = asignaturas.findById(asignaturaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asignatura no encontrada."));
        if (!a.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La asignatura está inactiva.");
        }
        if (a.getPrograma() != null && !a.getPrograma().getId().equals(plan.getPrograma().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La asignatura pertenece al programa " + a.getPrograma().getName() + " y solo va en sus planes.");
        }
        plan.getAsignaturas().removeIf(pa -> pa.getAsignatura().getId().equals(asignaturaId));
        planes.flush();
        plan.getAsignaturas().add(new PlanAsignatura(plan, a, semestre));
        publicar(actor, "UPDATE", plan, Map.of("subject", a.getCode(), "semester", semestre));
        return malla(id, periodo);
    }

    @Transactional
    public MallaResponse quitarAsignatura(UUID actor, UUID id, UUID asignaturaId, String periodo) {
        PlanEstudios plan = buscar(id);
        PlanAsignatura quitada = plan.getAsignaturas().stream()
                .filter(pa -> pa.getAsignatura().getId().equals(asignaturaId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "La asignatura no está en el plan de estudios."));
        plan.getAsignaturas().remove(quitada);
        publicar(actor, "UPDATE", plan, Map.of("removedSubject", quitada.getAsignatura().getCode()));
        return malla(id, periodo);
    }

    private static EstadoMalla estado(String status, boolean registro, boolean departamento) {
        return switch (status) {
            case "ACTIVO" -> registro ? EstadoMalla.VERIFICADO
                    : departamento ? EstadoMalla.PENDIENTE_REGISTRO : EstadoMalla.PENDIENTE_FIRMA;
            case "BORRADOR" -> EstadoMalla.BORRADOR;
            default -> EstadoMalla.SIN_SYLLABUS;
        };
    }

    private PlanEstudios buscar(UUID id) {
        return planes.findDetalleById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Plan de estudios no encontrado."));
    }

    private void publicar(UUID actor, String accion, PlanEstudios plan, Map<String, Object> extra) {
        Map<String, Object> detalles = new HashMap<>(extra);
        detalles.put("name", plan.getPrograma().getName() + " · " + plan.getName());
        eventos.publishEvent(AuditEvent.de(actor, accion, ENTIDAD, plan.getId()).con(detalles));
    }
}
