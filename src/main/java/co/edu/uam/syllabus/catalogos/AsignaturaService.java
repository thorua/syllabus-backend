package co.edu.uam.syllabus.catalogos;

import static co.edu.uam.syllabus.catalogos.CatalogoService.conflicto;
import static co.edu.uam.syllabus.catalogos.CatalogoService.esPosgrado;
import static co.edu.uam.syllabus.catalogos.CatalogoService.normalizarCodigo;

import co.edu.uam.syllabus.auditoria.AuditEvent;
import co.edu.uam.syllabus.common.security.Alcance;
import co.edu.uam.syllabus.common.security.ScopeService;
import co.edu.uam.syllabus.usuarios.Rol;
import co.edu.uam.syllabus.usuarios.Usuario;
import co.edu.uam.syllabus.usuarios.UsuarioRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** UC7 gestionar asignaturas y UC8 asignar asignatura a profesor. */
@Service
public class AsignaturaService {

    private static final String ENTIDAD = "subject";

    private final AsignaturaRepository asignaturas;
    private final CatalogoService catalogo;
    private final UsuarioRepository usuarios;
    private final ScopeService scopeService;
    private final ApplicationEventPublisher eventos;

    public AsignaturaService(AsignaturaRepository asignaturas, CatalogoService catalogo, UsuarioRepository usuarios,
                             ScopeService scopeService, ApplicationEventPublisher eventos) {
        this.asignaturas = asignaturas;
        this.catalogo = catalogo;
        this.usuarios = usuarios;
        this.scopeService = scopeService;
        this.eventos = eventos;
    }

    public record AsignaturaRequest(
            @NotBlank @Size(max = 200) String name,
            @NotBlank @Size(max = 20) String code,
            @Min(1) @Max(30) int credits,
            @Min(1) @Max(12) int semester,
            @NotNull UUID programId,
            UUID departmentId,
            @Size(max = 30) String modality,
            @Min(0) @Max(40) Integer hoursPresencial,
            @Min(0) @Max(40) Integer hoursDirigido,
            @Min(0) @Max(40) Integer hoursIndependiente,
            @Size(max = 300) String prerequisites,
            Boolean isActive) {}

    /** manageable: solo las asignaturas del alcance de gestión (p. ej. para crear su syllabus). */
    public record Filtros(UUID departmentId, UUID programId, UUID professorId, String search, boolean includeInactive,
                          boolean manageable) {}

    /** Lista las asignaturas dentro del alcance de lectura (o de gestión, si se pide) del usuario. */
    @Transactional(readOnly = true)
    public List<AsignaturaResponse> listar(UUID actor, Filtros f) {
        Alcance alcance = scopeService.de(actor, f.manageable());
        Map<UUID, Long> syllabus = CatalogoService.conteos(asignaturas.contarSyllabusPorAsignatura());
        return asignaturas.findAll(especificacion(f, alcance), Sort.by("code")).stream()
                .map(a -> AsignaturaResponse.from(a, syllabus.getOrDefault(a.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public AsignaturaResponse obtener(UUID id) {
        return respuesta(buscar(id));
    }

    @Transactional
    public AsignaturaResponse crear(UUID actor, AsignaturaRequest req) {
        String codigo = normalizarCodigo(req.code());
        if (asignaturas.existsByCodeIgnoreCase(codigo)) {
            throw conflicto("Ya existe una asignatura con ese código.");
        }
        Asignatura a = new Asignatura();
        aplicar(actor, a, req, codigo);
        asignaturas.save(a);
        publicar(actor, "CREATE", a, Map.of());
        return respuesta(a);
    }

    @Transactional
    public AsignaturaResponse actualizar(UUID actor, UUID id, AsignaturaRequest req) {
        Asignatura a = buscar(id);
        String codigo = normalizarCodigo(req.code());
        if (asignaturas.existsByCodeIgnoreCaseAndIdNot(codigo, id)) {
            throw conflicto("Ya existe una asignatura con ese código.");
        }
        if (Boolean.FALSE.equals(req.isActive()) && a.isActive()) {
            validarDesactivar(id);
        }
        aplicar(actor, a, req, codigo);
        publicar(actor, "UPDATE", a, Map.of());
        return respuesta(a);
    }

    /** Borrado lógico: solo si no tiene syllabus activos. */
    @Transactional
    public void desactivar(UUID actor, UUID id) {
        Asignatura a = buscar(id);
        validarDesactivar(id);
        a.setActive(false);
        publicar(actor, "DELETE", a, Map.of());
    }

    /** UC8: el profesor debe estar activo, tener el rol y estar afiliado al programa o departamento. */
    @Transactional
    public AsignaturaResponse asignarProfesor(UUID actor, UUID id, UUID profesorId) {
        Asignatura a = buscar(id);
        Usuario profesor = profesorId == null ? null : profesorValido(a, profesorId);
        a.setProfesor(profesor);
        publicar(actor, "ASSIGN", a, profesor == null
                ? Map.of("professor", "Sin profesor")
                : Map.of("professor", profesor.getEmail()));
        return respuesta(a);
    }

    private void aplicar(UUID actor, Asignatura a, AsignaturaRequest req, String codigo) {
        Programa programa = catalogo.buscarPrograma(req.programId());
        if (!programa.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El programa seleccionado está inactivo.");
        }
        Departamento departamento = esPosgrado(programa.getLevel()) ? null : departamentoActivo(req.departmentId());
        validarAlcanceEscritura(actor, departamento);

        a.setName(req.name().trim());
        a.setCode(codigo);
        a.setCredits(req.credits());
        a.setSemester(req.semester());
        a.setPrograma(programa);
        a.setDepartamento(departamento);
        a.setModality(StringUtils.hasText(req.modality()) ? req.modality().trim() : "Presencial");
        a.setHoursPresencial(req.hoursPresencial());
        a.setHoursDirigido(req.hoursDirigido());
        a.setHoursIndependiente(req.hoursIndependiente());
        a.setPrerequisites(StringUtils.hasText(req.prerequisites()) ? req.prerequisites().trim() : null);
        if (req.isActive() != null) {
            a.setActive(req.isActive());
        }
    }

    /** Coord. de Departamento: solo sus departamentos. Posgrado (sin departamento): solo alcance total. */
    private void validarAlcanceEscritura(UUID actor, Departamento departamento) {
        Alcance alcance = scopeService.de(actor, true);
        if (departamento == null && !alcance.total()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Las asignaturas de posgrado solo las gestiona el Administrador o Registro Académico.");
        }
        if (departamento != null && !alcance.incluyeDepartamento(departamento.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo puede gestionar asignaturas de sus departamentos.");
        }
    }

    private Departamento departamentoActivo(UUID id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Una asignatura de pregrado requiere departamento.");
        }
        Departamento d = catalogo.buscarDepartamento(id);
        if (!d.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El departamento seleccionado está inactivo.");
        }
        return d;
    }

    private Usuario profesorValido(Asignatura a, UUID profesorId) {
        Usuario profesor = usuarios.findDetalleById(profesorId)
                .filter(Usuario::isActive)
                .filter(u -> u.getRoles().contains(Rol.PROFESOR))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El usuario seleccionado no es un profesor activo."));
        boolean afiliado = profesor.getProgramas().stream().anyMatch(p -> p.getId().equals(a.getPrograma().getId()))
                || (a.getDepartamento() != null && profesor.getDepartamentos().stream()
                        .anyMatch(d -> d.getId().equals(a.getDepartamento().getId())));
        if (!afiliado) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El profesor no está afiliado al programa ni al departamento de la asignatura.");
        }
        return profesor;
    }

    private void validarDesactivar(UUID id) {
        if (asignaturas.tieneSyllabusActivos(id)) {
            throw conflicto("La asignatura tiene syllabus activos; desactívelos primero.");
        }
    }

    private Asignatura buscar(UUID id) {
        return asignaturas.findDetalleById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asignatura no encontrada."));
    }

    private AsignaturaResponse respuesta(Asignatura a) {
        return AsignaturaResponse.from(a, asignaturas.contarSyllabus(a.getId()));
    }

    private void publicar(UUID actor, String accion, Asignatura a, Map<String, Object> extra) {
        Map<String, Object> detalles = new HashMap<>(extra);
        detalles.put("code", a.getCode());
        detalles.put("name", a.getName());
        eventos.publishEvent(AuditEvent.de(actor, accion, ENTIDAD, a.getId()).con(detalles));
    }

    private static Specification<Asignatura> especificacion(Filtros f, Alcance alcance) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (!f.includeInactive()) p.add(cb.isTrue(root.get("active")));
            if (f.departmentId() != null) p.add(cb.equal(root.get("departamento").get("id"), f.departmentId()));
            if (f.programId() != null) p.add(cb.equal(root.get("programa").get("id"), f.programId()));
            if (f.professorId() != null) p.add(cb.equal(root.get("profesor").get("id"), f.professorId()));
            if (StringUtils.hasText(f.search())) {
                String patron = "%" + f.search().trim().toLowerCase() + "%";
                p.add(cb.or(cb.like(cb.lower(root.get("name")), patron), cb.like(cb.lower(root.get("code")), patron)));
            }
            if (!alcance.total()) {
                p.add(cb.or(
                        en(cb, root.get("id"), alcance.subjectIds()),
                        en(cb, root.get("programa").get("id"), alcance.programIds()),
                        en(cb, root.get("departamento").get("id"), alcance.departmentIds())));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }

    private static Predicate en(CriteriaBuilder cb, Expression<UUID> campo, Set<UUID> ids) {
        return ids.isEmpty() ? cb.disjunction() : campo.in(ids);
    }
}
