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
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * UC7 gestionar asignaturas y UC8 asignar asignatura a profesor. Cada asignatura pertenece al
 * departamento que la ofrece (en posgrado, a su programa); los programas en que se cursa salen de
 * los planes de estudio.
 */
@Service
public class AsignaturaService {

    private static final String ENTIDAD = "subject";

    private final AsignaturaRepository asignaturas;
    private final PlanAsignaturaRepository planes;
    private final CatalogoService catalogo;
    private final UsuarioRepository usuarios;
    private final ScopeService scopeService;
    private final ApplicationEventPublisher eventos;

    public AsignaturaService(AsignaturaRepository asignaturas, PlanAsignaturaRepository planes,
                             CatalogoService catalogo, UsuarioRepository usuarios, ScopeService scopeService,
                             ApplicationEventPublisher eventos) {
        this.asignaturas = asignaturas;
        this.planes = planes;
        this.catalogo = catalogo;
        this.usuarios = usuarios;
        this.scopeService = scopeService;
        this.eventos = eventos;
    }

    /** departmentId: departamento que la ofrece; en posgrado se indica programId en su lugar. */
    public record AsignaturaRequest(
            @NotBlank @Size(max = 200) String name,
            @NotBlank @Size(max = 20) String code,
            @Min(1) @Max(30) int credits,
            UUID departmentId,
            UUID programId,
            @Size(max = 30) String modality,
            @Min(0) @Max(40) Integer hoursPresencial,
            @Min(0) @Max(40) Integer hoursDirigido,
            @Min(0) @Max(40) Integer hoursIndependiente,
            @Size(max = 300) String prerequisites,
            /** Falso en espacios del plan sin syllabus propio (curso libre, electiva...); nulo = verdadero. */
            Boolean requiresSyllabus,
            Boolean isActive) {}

    /**
     * departmentId: departamento que la ofrece; programId: programa en cuyos planes aparece.
     * manageable: solo las asignaturas del alcance de gestión (p. ej. para crear su syllabus).
     */
    public record Filtros(UUID departmentId, UUID programId, UUID professorId, String search, boolean includeInactive,
                          boolean manageable) {}

    /** Lista las asignaturas dentro del alcance de lectura (o de gestión, si se pide) del usuario. */
    @Transactional(readOnly = true)
    public List<AsignaturaResponse> listar(UUID actor, Filtros f) {
        Alcance alcance = scopeService.de(actor, f.manageable());
        Set<UUID> delPrograma = f.programId() == null ? null : planes.asignaturasDelPrograma(f.programId());
        List<Asignatura> lista = asignaturas.findAll(especificacion(f, alcance, delPrograma), Sort.by("code"));
        Map<UUID, Long> syllabus = CatalogoService.conteos(asignaturas.contarSyllabusPorAsignatura());
        Map<UUID, List<PlanAsignatura>> enPlanes = enPlanes(lista.stream().map(Asignatura::getId).toList());
        return lista.stream()
                .map(a -> AsignaturaResponse.from(a, syllabus.getOrDefault(a.getId(), 0L),
                        enPlanes.getOrDefault(a.getId(), List.of())))
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
        validarAlcanceEscritura(actor, a.getDepartamento(), a.getPrograma());
        validarDesactivar(id);
        a.setActive(false);
        publicar(actor, "DELETE", a, Map.of());
    }

    /** UC8: el profesor debe estar activo, tener el rol y estar afiliado a un programa o al departamento. */
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
        Programa posgrado = req.programId() == null ? null : posgradoActivo(req.programId());
        Departamento departamento = posgrado == null ? departamentoActivo(req.departmentId()) : null;
        validarAlcanceEscritura(actor, departamento, posgrado);
        if (a.getId() != null) {
            validarAlcanceEscritura(actor, a.getDepartamento(), a.getPrograma());
        }

        a.setName(req.name().trim());
        a.setCode(codigo);
        a.setCredits(req.credits());
        a.setPrograma(posgrado);
        a.setDepartamento(departamento);
        a.setModality(StringUtils.hasText(req.modality()) ? req.modality().trim() : "Presencial");
        a.setHoursPresencial(req.hoursPresencial());
        a.setHoursDirigido(req.hoursDirigido());
        a.setHoursIndependiente(req.hoursIndependiente());
        a.setPrerequisites(StringUtils.hasText(req.prerequisites()) ? req.prerequisites().trim() : null);
        a.setRequiresSyllabus(!Boolean.FALSE.equals(req.requiresSyllabus()));
        if (req.isActive() != null) {
            a.setActive(req.isActive());
        }
    }

    /**
     * Cada asignatura la gestiona quien la ofrece: el Coordinador de su departamento o, en posgrado
     * (que no tiene departamento), el Coordinador del programa. Administración y Registro, todas.
     */
    private void validarAlcanceEscritura(UUID actor, Departamento departamento, Programa posgrado) {
        Alcance alcance = scopeService.de(actor, true);
        if (departamento != null && !alcance.incluyeDepartamento(departamento.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo puede gestionar asignaturas de sus departamentos.");
        }
        if (posgrado != null && !alcance.incluyePrograma(posgrado.getId(), null)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Las asignaturas de un posgrado las gestiona "
                    + "la coordinación de ese programa, Administración o Registro Académico.");
        }
    }

    private Programa posgradoActivo(UUID id) {
        Programa p = catalogo.buscarPrograma(id);
        if (!esPosgrado(p.getLevel())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Una asignatura de pregrado pertenece al "
                    + "departamento que la ofrece; se agrega al programa desde su plan de estudios.");
        }
        if (!p.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El programa seleccionado está inactivo.");
        }
        return p;
    }

    private Departamento departamentoActivo(UUID id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Indique el departamento que ofrece la asignatura (o el programa, si es de posgrado).");
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
        Set<UUID> programas = programas(a);
        boolean afiliado = profesor.getProgramas().stream().anyMatch(p -> programas.contains(p.getId()))
                || (a.getDepartamento() != null && profesor.getDepartamentos().stream()
                        .anyMatch(d -> d.getId().equals(a.getDepartamento().getId())));
        if (!afiliado) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El profesor no está afiliado al departamento de la asignatura ni a un programa que la incluya.");
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
        return AsignaturaResponse.from(a, asignaturas.contarSyllabus(a.getId()),
                planes.deAsignaturas(List.of(a.getId())));
    }

    /** Programas en que se cursa la asignatura: los de sus planes y, en posgrado, el propio. */
    private Set<UUID> programas(Asignatura a) {
        Set<UUID> ids = new HashSet<>();
        planes.deAsignaturas(List.of(a.getId())).forEach(pa -> ids.add(pa.getPlan().getPrograma().getId()));
        if (a.getPrograma() != null) {
            ids.add(a.getPrograma().getId());
        }
        return ids;
    }

    private Map<UUID, List<PlanAsignatura>> enPlanes(List<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return planes.deAsignaturas(ids).stream().collect(Collectors.groupingBy(pa -> pa.getAsignatura().getId()));
    }

    private void publicar(UUID actor, String accion, Asignatura a, Map<String, Object> extra) {
        Map<String, Object> detalles = new HashMap<>(extra);
        detalles.put("code", a.getCode());
        detalles.put("name", a.getName());
        eventos.publishEvent(AuditEvent.de(actor, accion, ENTIDAD, a.getId()).con(detalles));
    }

    private static Specification<Asignatura> especificacion(Filtros f, Alcance alcance, Set<UUID> delPrograma) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (!f.includeInactive()) p.add(cb.isTrue(root.get("active")));
            if (f.departmentId() != null) p.add(cb.equal(root.get("departamento").get("id"), f.departmentId()));
            if (delPrograma != null) p.add(en(cb, root.get("id"), delPrograma));
            if (f.professorId() != null) p.add(cb.equal(root.get("profesor").get("id"), f.professorId()));
            if (StringUtils.hasText(f.search())) {
                String patron = "%" + f.search().trim().toLowerCase() + "%";
                p.add(cb.or(cb.like(cb.lower(root.get("name")), patron), cb.like(cb.lower(root.get("code")), patron)));
            }
            if (!alcance.total()) {
                p.add(en(cb, root.get("id"), alcance.subjectIds()));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }

    private static Predicate en(CriteriaBuilder cb, Expression<UUID> campo, Set<UUID> ids) {
        return ids.isEmpty() ? cb.disjunction() : campo.in(ids);
    }
}
