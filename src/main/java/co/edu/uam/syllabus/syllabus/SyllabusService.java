package co.edu.uam.syllabus.syllabus;

import co.edu.uam.syllabus.auditoria.AuditEvent;
import co.edu.uam.syllabus.catalogos.Asignatura;
import co.edu.uam.syllabus.catalogos.AsignaturaRepository;
import co.edu.uam.syllabus.catalogos.Departamento;
import co.edu.uam.syllabus.common.PageResponse;
import co.edu.uam.syllabus.common.security.Alcance;
import co.edu.uam.syllabus.common.security.ScopeService;
import co.edu.uam.syllabus.common.security.UsuarioAutenticado;
import co.edu.uam.syllabus.configuracion.ConfiguracionService;
import co.edu.uam.syllabus.configuracion.Evaluacion;
import co.edu.uam.syllabus.usuarios.Usuario;
import co.edu.uam.syllabus.usuarios.UsuarioRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * UC10 consultar, UC11 crear, UC12 editar, UC13 eliminar (lógico) y UC14 historial de syllabus.
 * Cada creación, edición o desactivación registra una versión inmutable (UC22).
 */
@Service
public class SyllabusService {

    public static final String EVALUACION = "evaluation";

    private static final String ENTIDAD = "syllabus";
    private static final String TIPOS_SECCION =
            "identification|justification|performance|contents|teaching_strategies|evaluation|bibliography";

    private final SyllabusRepository syllabus;
    private final VersionSyllabusRepository versiones;
    private final AsignaturaRepository asignaturas;
    private final UsuarioRepository usuarios;
    private final ScopeService scopeService;
    private final ConfiguracionService configuracion;
    private final ApplicationEventPublisher eventos;

    public SyllabusService(SyllabusRepository syllabus, VersionSyllabusRepository versiones,
                           AsignaturaRepository asignaturas, UsuarioRepository usuarios, ScopeService scopeService,
                           ConfiguracionService configuracion, ApplicationEventPublisher eventos) {
        this.syllabus = syllabus;
        this.versiones = versiones;
        this.asignaturas = asignaturas;
        this.usuarios = usuarios;
        this.scopeService = scopeService;
        this.configuracion = configuracion;
        this.eventos = eventos;
    }

    public record SeccionRequest(
            @NotBlank @Pattern(regexp = TIPOS_SECCION, message = "Tipo de sección inválido.") String sectionType,
            @NotBlank @Size(max = 200) String title,
            @Size(max = 20000) String content,
            List<Evaluacion.Corte> evaluation) {}

    public record CrearRequest(
            @NotNull UUID subjectId,
            @NotBlank @Pattern(regexp = "\\d{4}-0[1-3]", message = "Use el formato AAAA-01, AAAA-02 o AAAA-03.")
            String academicPeriod,
            @NotBlank @Size(max = 250) String title,
            Syllabus.Estado status,
            @Size(max = 200) String headerTitle,
            @Size(max = 50) String documentCode,
            @Size(max = 20) String documentDate,
            @NotEmpty @Size(max = 20) List<@Valid SeccionRequest> sections) {}

    public record ActualizarRequest(
            @NotBlank @Size(max = 250) String title,
            @NotNull Syllabus.Estado status,
            @Size(max = 200) String headerTitle,
            @Size(max = 50) String documentCode,
            @Size(max = 20) String documentDate,
            @NotEmpty @Size(max = 20) List<@Valid SeccionRequest> sections,
            @NotBlank @Size(max = 500) String changeDescription,
            /** Versión sobre la que se editó; si cambió entretanto, se rechaza para no pisar cambios ajenos. */
            Integer baseVersion) {}

    public record Filtros(String search, UUID departmentId, UUID programId, UUID subjectId, String academicPeriod,
                          Syllabus.Estado status) {}

    /**
     * Lista los syllabus del alcance de lectura. Los no vigentes (borrador o inactivo) solo los ve
     * quien puede editarlos; el estudiante ve únicamente los activos (UC10).
     */
    @Transactional(readOnly = true)
    public PageResponse<SyllabusResponse> listar(UsuarioAutenticado actor, Filtros f, int page, int limit) {
        Alcance lectura = scopeService.de(actor.id(), false);
        Alcance escritura = scopeService.de(actor.id(), true);
        var pagina = PageRequest.of(Math.max(page, 1) - 1, Math.clamp(limit, 1, 100),
                Sort.by(Sort.Direction.DESC, "updatedAt"));
        var spec = especificacion(f, lectura, puedeEditar(actor) ? escritura : null);
        return PageResponse.of(syllabus.findAll(spec, pagina),
                s -> SyllabusResponse.resumen(s, enAlcance(escritura, s.getAsignatura())));
    }

    /**
     * UC16: syllabus vigentes (activos) del alcance de lectura que cumplen los filtros, con sus
     * secciones. Si se indican ids, deben ser todos exportables.
     */
    @Transactional(readOnly = true)
    public List<SyllabusResponse> vigentesParaExportar(UsuarioAutenticado actor, Filtros f, List<UUID> ids) {
        Filtros activos = new Filtros(null, f.departmentId(), f.programId(), null, f.academicPeriod(),
                Syllabus.Estado.ACTIVO);
        Specification<Syllabus> spec = especificacion(activos, scopeService.de(actor.id(), false), null);
        if (ids != null && !ids.isEmpty()) {
            spec = spec.and((root, query, cb) -> root.get("id").in(ids));
        }
        List<Syllabus> encontrados = syllabus.findAll(spec, Sort.by("asignatura.code", "academicPeriod"));
        if (ids != null && !ids.isEmpty() && encontrados.size() != Set.copyOf(ids).size()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Algunos syllabus seleccionados no están vigentes o no están dentro de su alcance.");
        }
        Alcance escritura = scopeService.de(actor.id(), true);
        return encontrados.stream()
                .map(s -> SyllabusResponse.detalle(s, versionActual(s.getId()), enAlcance(escritura, s.getAsignatura())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> periodos() {
        return syllabus.periodos();
    }

    @Transactional(readOnly = true)
    public SyllabusResponse obtener(UsuarioAutenticado actor, UUID id) {
        Alcance escritura = scopeService.de(actor.id(), true);
        Syllabus s = syllabus.findDetalleById(id)
                .filter(x -> x.getStatus() == Syllabus.Estado.ACTIVO
                        || (puedeEditar(actor) && enAlcance(escritura, x.getAsignatura())))
                .orElseThrow(SyllabusService::noEncontrado);
        return SyllabusResponse.detalle(s, versionActual(id), enAlcance(escritura, s.getAsignatura()));
    }

    @Transactional
    public SyllabusResponse crear(UsuarioAutenticado actor, CrearRequest req) {
        Asignatura a = asignaturas.findDetalleById(req.subjectId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asignatura no encontrada."));
        if (!a.isActive()) {
            throw invalido("La asignatura seleccionada está inactiva.");
        }
        if (!enAlcance(scopeService.de(actor.id(), true), a)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo puede crear syllabus de las asignaturas a su cargo.");
        }
        String periodo = req.academicPeriod().trim();
        if (syllabus.existsByAsignaturaIdAndAcademicPeriod(a.getId(), periodo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un syllabus de " + a.getName() + " para el período " + periodo + ".");
        }
        Syllabus.Estado estado = req.status() == null ? Syllabus.Estado.BORRADOR : req.status();
        validarEstadoEditable(estado);

        Usuario autor = usuarios.getReferenceById(actor.id());
        Syllabus s = new Syllabus(a, periodo, autor);
        s.setTitle(req.title().trim());
        s.setStatus(estado);
        aplicarEncabezado(actor, s, req.headerTitle(), req.documentCode(), req.documentDate());
        s.reemplazarSecciones(secciones(req.sections(), estado));
        syllabus.saveAndFlush(s);

        registrarVersion(s, 1, "Creación del syllabus", autor);
        publicar(actor, "CREATE", s, 1);
        return SyllabusResponse.detalle(s, 1, true);
    }

    /** UC12: cada guardado con cambios genera una versión nueva (UC22). */
    @Transactional
    public SyllabusResponse actualizar(UsuarioAutenticado actor, UUID id, ActualizarRequest req) {
        Syllabus s = syllabus.findParaEditarById(id).orElseThrow(SyllabusService::noEncontrado);
        if (s.getStatus() == Syllabus.Estado.INACTIVO && !actor.permisos().contains("syllabus:delete")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "El syllabus está inactivo; solo un coordinador puede reactivarlo.");
        }
        validarEstadoEditable(req.status());
        int actual = versionActual(id);
        if (req.baseVersion() != null && req.baseVersion() != actual) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Otra persona guardó cambios en este syllabus "
                    + "(versión " + actual + "). Recargue la página para editar sobre la versión actual.");
        }

        VersionSyllabus.Snapshot anterior = VersionSyllabus.Snapshot.de(s);
        s.setTitle(req.title().trim());
        s.setStatus(req.status());
        aplicarEncabezado(actor, s, req.headerTitle(), req.documentCode(), req.documentDate());
        s.reemplazarSecciones(secciones(req.sections(), req.status()));
        if (anterior.equals(VersionSyllabus.Snapshot.de(s))) {
            throw invalido("No hay cambios para guardar.");
        }
        syllabus.flush();

        registrarVersion(s, actual + 1, req.changeDescription().trim(), usuarios.getReferenceById(actor.id()));
        publicar(actor, "UPDATE", s, actual + 1);
        return SyllabusResponse.detalle(s, actual + 1, true);
    }

    /** UC13: borrado lógico; el historial se conserva y la desactivación queda como versión. */
    @Transactional
    public void desactivar(UsuarioAutenticado actor, UUID id) {
        Syllabus s = syllabus.findParaEditarById(id).orElseThrow(SyllabusService::noEncontrado);
        if (s.getStatus() == Syllabus.Estado.INACTIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El syllabus ya está inactivo.");
        }
        s.setStatus(Syllabus.Estado.INACTIVO);
        syllabus.flush();
        int version = versionActual(id) + 1;
        registrarVersion(s, version, "Syllabus desactivado", usuarios.getReferenceById(actor.id()));
        publicar(actor, "DELETE", s, version);
    }

    @Transactional(readOnly = true)
    public List<VersionResponse> versiones(UUID id) {
        if (!syllabus.existsById(id)) {
            throw noEncontrado();
        }
        return versiones.findBySyllabusIdOrderByVersionNumberDesc(id).stream().map(VersionResponse::resumen).toList();
    }

    @Transactional(readOnly = true)
    public VersionResponse version(UUID id, UUID versionId) {
        return versiones.findByIdAndSyllabusId(versionId, id).map(VersionResponse::detalle)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Versión no encontrada."));
    }

    private List<SeccionSyllabus> secciones(List<SeccionRequest> req, Syllabus.Estado estado) {
        boolean completa = estado == Syllabus.Estado.ACTIVO;
        long evaluaciones = req.stream().filter(x -> EVALUACION.equals(x.sectionType())).count();
        if (evaluaciones > 1) {
            throw invalido("El syllabus solo puede tener una sección de evaluación.");
        }
        List<Evaluacion.Corte> plantilla = evaluaciones == 1 ? configuracion.plantillaEvaluacion() : List.of();
        List<SeccionSyllabus> resultado = new ArrayList<>();
        for (int i = 0; i < req.size(); i++) {
            SeccionRequest x = req.get(i);
            String titulo = x.title().trim();
            if (EVALUACION.equals(x.sectionType())) {
                List<Evaluacion.Corte> evaluacion = normalizar(x.evaluation());
                Evaluacion.validarSyllabus(plantilla, evaluacion, completa);
                resultado.add(new SeccionSyllabus(x.sectionType(), titulo, null, evaluacion, i + 1));
            } else {
                String contenido = x.content() == null ? "" : x.content().strip();
                if (completa && contenido.isEmpty()) {
                    throw invalido("La sección «" + titulo + "» no tiene contenido. Complétela o guarde el "
                            + "syllabus como borrador.");
                }
                resultado.add(new SeccionSyllabus(x.sectionType(), titulo, contenido, null, i + 1));
            }
        }
        return resultado;
    }

    private static List<Evaluacion.Corte> normalizar(List<Evaluacion.Corte> cortes) {
        if (cortes == null) {
            return null;
        }
        return cortes.stream()
                .map(c -> new Evaluacion.Corte(c.corte(), c.label(), c.porcentaje(), c.tipos() == null ? null
                        : c.tipos().stream()
                                .map(t -> new Evaluacion.Tipo(t.tipo() == null ? "" : t.tipo().trim(), t.porcentaje()))
                                .toList()))
                .toList();
    }

    /** UC19: el encabezado propio solo lo cambia quien tiene syllabus:header; el docente no lo edita. */
    private static void aplicarEncabezado(UsuarioAutenticado actor, Syllabus s, String titulo, String codigo,
                                          String fecha) {
        if (!actor.permisos().contains("syllabus:header")) {
            return;
        }
        s.setHeaderTitle(textoONulo(titulo));
        s.setDocumentCode(textoONulo(codigo));
        s.setDocumentDate(textoONulo(fecha));
    }

    private static void validarEstadoEditable(Syllabus.Estado estado) {
        if (estado == Syllabus.Estado.INACTIVO) {
            throw invalido("Para desactivar un syllabus use la opción Desactivar.");
        }
    }

    private void registrarVersion(Syllabus s, int numero, String descripcion, Usuario autor) {
        versiones.save(new VersionSyllabus(s, numero, descripcion, autor));
    }

    private int versionActual(UUID id) {
        return versiones.findFirstBySyllabusIdOrderByVersionNumberDesc(id)
                .map(VersionSyllabus::getVersionNumber).orElse(0);
    }

    private static boolean puedeEditar(UsuarioAutenticado actor) {
        return actor.permisos().contains("syllabus:update");
    }

    private static boolean enAlcance(Alcance alcance, Asignatura a) {
        Departamento d = a.getDepartamento();
        return alcance.incluyeAsignatura(a.getId(), a.getPrograma().getId(), d == null ? null : d.getId());
    }

    private void publicar(UsuarioAutenticado actor, String accion, Syllabus s, int version) {
        eventos.publishEvent(AuditEvent.de(actor.id(), accion, ENTIDAD, s.getId()).con(Map.of(
                "title", s.getTitle(),
                "academicPeriod", s.getAcademicPeriod(),
                "status", s.getStatus().name(),
                "versionNumber", version)));
    }

    private static Specification<Syllabus> especificacion(Filtros f, Alcance lectura, Alcance edicion) {
        return (root, query, cb) -> {
            Join<Syllabus, Asignatura> a = root.join("asignatura");
            Join<Asignatura, Departamento> depto = a.join("departamento", JoinType.LEFT);
            List<Predicate> p = new ArrayList<>();
            if (f.departmentId() != null) p.add(cb.equal(depto.get("id"), f.departmentId()));
            if (f.programId() != null) p.add(cb.equal(a.get("programa").get("id"), f.programId()));
            if (f.subjectId() != null) p.add(cb.equal(a.get("id"), f.subjectId()));
            if (StringUtils.hasText(f.academicPeriod())) p.add(cb.equal(root.get("academicPeriod"), f.academicPeriod()));
            if (f.status() != null) p.add(cb.equal(root.get("status"), f.status()));
            if (StringUtils.hasText(f.search())) {
                String patron = "%" + f.search().trim().toLowerCase() + "%";
                p.add(cb.or(cb.like(cb.lower(root.get("title")), patron),
                        cb.like(cb.lower(a.get("name")), patron),
                        cb.like(cb.lower(a.get("code")), patron)));
            }
            if (!lectura.total()) {
                p.add(alcance(cb, a, depto, lectura));
            }
            Predicate vigente = cb.equal(root.get("status"), Syllabus.Estado.ACTIVO);
            if (edicion == null) {
                p.add(vigente);
            } else if (!edicion.total()) {
                p.add(cb.or(vigente, alcance(cb, a, depto, edicion)));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }

    private static Predicate alcance(CriteriaBuilder cb, Join<Syllabus, Asignatura> a,
                                     Join<Asignatura, Departamento> depto, Alcance alcance) {
        return cb.or(
                en(cb, a.get("id"), alcance.subjectIds()),
                en(cb, a.get("programa").get("id"), alcance.programIds()),
                en(cb, depto.get("id"), alcance.departmentIds()));
    }

    private static Predicate en(CriteriaBuilder cb, Expression<UUID> campo, Set<UUID> ids) {
        return ids.isEmpty() ? cb.disjunction() : campo.in(ids);
    }

    private static String textoONulo(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : null;
    }

    private static ResponseStatusException noEncontrado() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Syllabus no encontrado.");
    }

    private static ResponseStatusException invalido(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }
}
