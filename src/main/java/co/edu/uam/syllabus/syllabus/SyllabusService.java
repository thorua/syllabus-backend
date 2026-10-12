package co.edu.uam.syllabus.syllabus;

import co.edu.uam.syllabus.auditoria.AuditEvent;
import co.edu.uam.syllabus.catalogos.Asignatura;
import co.edu.uam.syllabus.catalogos.AsignaturaRepository;
import co.edu.uam.syllabus.catalogos.PlanAsignaturaRepository;
import co.edu.uam.syllabus.common.PageResponse;
import co.edu.uam.syllabus.common.security.Alcance;
import co.edu.uam.syllabus.common.security.ScopeService;
import co.edu.uam.syllabus.common.security.UsuarioAutenticado;
import co.edu.uam.syllabus.configuracion.ConfiguracionService;
import co.edu.uam.syllabus.configuracion.Evaluacion;
import co.edu.uam.syllabus.syllabus.versionado.VersionResponse;
import co.edu.uam.syllabus.syllabus.versionado.VersionSyllabus;
import co.edu.uam.syllabus.syllabus.firma.FirmaSyllabusRepository;
import co.edu.uam.syllabus.syllabus.firma.SyllabusPorFirmar;
import co.edu.uam.syllabus.syllabus.firma.Verificacion;
import co.edu.uam.syllabus.syllabus.versionado.VersionadoService;
import co.edu.uam.syllabus.usuarios.Usuario;
import co.edu.uam.syllabus.usuarios.UsuarioRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.HashMap;
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
 * Cada creación, edición o desactivación registra una versión inmutable (UC22). Renovar copia un
 * syllabus a otro período sin ajustes y deja ese registro en el historial (guía GFR-GUI-031).
 */
@Service
public class SyllabusService {

    public static final String EVALUACION = "evaluation";
    private static final String IDENTIFICACION = "identification";

    private static final String ENTIDAD = "syllabus";
    private static final String TIPOS_SECCION =
            "identification|justification|performance|contents|teaching_strategies|evaluation|bibliography";

    private final SyllabusRepository syllabus;
    private final VersionadoService versionado;
    private final AsignaturaRepository asignaturas;
    private final PlanAsignaturaRepository planes;
    private final FirmaSyllabusRepository firmas;
    private final UsuarioRepository usuarios;
    private final ScopeService scopeService;
    private final ConfiguracionService configuracion;
    private final ApplicationEventPublisher eventos;

    public SyllabusService(SyllabusRepository syllabus, VersionadoService versionado,
                           AsignaturaRepository asignaturas, PlanAsignaturaRepository planes,
                           FirmaSyllabusRepository firmas, UsuarioRepository usuarios, ScopeService scopeService,
                           ConfiguracionService configuracion, ApplicationEventPublisher eventos) {
        this.syllabus = syllabus;
        this.versionado = versionado;
        this.asignaturas = asignaturas;
        this.planes = planes;
        this.firmas = firmas;
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

    public record RenovarRequest(
            @NotBlank @Pattern(regexp = "\\d{4}-0[1-3]", message = "Use el formato AAAA-01, AAAA-02 o AAAA-03.")
            String academicPeriod) {}

    /** departmentId: departamento que ofrece la asignatura; programId: programa en cuya malla está. */
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
        var spec = especificacion(f, delPrograma(f), lectura, puedeEditar(actor) ? escritura : null);
        var encontrados = syllabus.findAll(spec, pagina);
        Map<UUID, Verificacion.Estado> verificacion = verificaciones(
                encontrados.getContent().stream().map(Syllabus::getId).toList());
        return PageResponse.of(encontrados, s -> SyllabusResponse.resumen(s,
                verificacion.getOrDefault(s.getId(), Verificacion.Estado.SIN_FIRMAS),
                escritura.incluyeAsignatura(s.getAsignatura().getId())));
    }

    /**
     * UC16: syllabus vigentes (activos) del alcance de lectura que cumplen los filtros, con sus
     * secciones. Si se indican ids, deben ser todos exportables.
     */
    @Transactional(readOnly = true)
    public List<SyllabusResponse> vigentesParaExportar(UsuarioAutenticado actor, Filtros f, List<UUID> ids) {
        Filtros activos = new Filtros(null, f.departmentId(), f.programId(), null, f.academicPeriod(),
                Syllabus.Estado.ACTIVO);
        Specification<Syllabus> spec = especificacion(activos, delPrograma(activos), scopeService.de(actor.id(), false),
                null);
        if (ids != null && !ids.isEmpty()) {
            spec = spec.and((root, query, cb) -> root.get("id").in(ids));
        }
        List<Syllabus> encontrados = syllabus.findAll(spec, Sort.by("asignatura.code", "academicPeriod"));
        if (ids != null && !ids.isEmpty() && encontrados.size() != Set.copyOf(ids).size()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Algunos syllabus seleccionados no están vigentes o no están dentro de su alcance.");
        }
        Alcance escritura = scopeService.de(actor.id(), true);
        return encontrados.stream().map(s -> detalle(s, escritura)).toList();
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
                        || (puedeEditar(actor) && escritura.incluyeAsignatura(x.getAsignatura().getId())))
                .orElseThrow(SyllabusService::noEncontrado);
        return detalle(s, escritura);
    }

    @Transactional
    public SyllabusResponse crear(UsuarioAutenticado actor, CrearRequest req) {
        Asignatura a = asignaturas.findDetalleById(req.subjectId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asignatura no encontrada."));
        if (!a.isActive()) {
            throw invalido("La asignatura seleccionada está inactiva.");
        }
        sinSyllabusPropio(a);
        if (!scopeService.de(actor.id(), true).incluyeAsignatura(a.getId())) {
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
        validarContacto(s);
        syllabus.saveAndFlush(s);

        int version = versionado.registrar(s, "Creación del syllabus", autor);
        publicar(actor, "CREATE", s, version);
        avisarSiQuedaPorFirmar(s);
        return respuesta(s, version);
    }

    /**
     * Copia el syllabus a otro período tal como está: el historial del nuevo registra que no tuvo
     * ajustes respecto al período de origen (guía GFR-GUI-031, actividades 3 y 7). Si queda activo,
     * debe cumplir las reglas vigentes (evaluación, contacto, longitud).
     */
    @Transactional
    public SyllabusResponse renovar(UsuarioAutenticado actor, UUID id, RenovarRequest req) {
        Syllabus origen = syllabus.findDetalleById(id).orElseThrow(SyllabusService::noEncontrado);
        Asignatura a = origen.getAsignatura();
        String periodo = req.academicPeriod().trim();
        if (origen.getStatus() == Syllabus.Estado.INACTIVO) {
            throw invalido("No se puede renovar un syllabus inactivo.");
        }
        sinSyllabusPropio(a);
        if (periodo.equals(origen.getAcademicPeriod())) {
            throw invalido("Elija un período distinto al del syllabus de origen.");
        }
        if (syllabus.existsByAsignaturaIdAndAcademicPeriod(a.getId(), periodo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un syllabus de " + a.getName() + " para el período " + periodo + ".");
        }
        Usuario autor = usuarios.getReferenceById(actor.id());
        Syllabus s = new Syllabus(a, periodo, autor);
        s.setTitle(origen.getTitle().replace(origen.getAcademicPeriod(), periodo));
        s.setStatus(origen.getStatus());
        s.setHeaderTitle(origen.getHeaderTitle());
        s.setDocumentCode(origen.getDocumentCode());
        s.setDocumentDate(origen.getDocumentDate());
        s.reemplazarSecciones(secciones(origen.getSecciones().stream()
                .map(x -> new SeccionRequest(x.getSectionType(), x.getTitle(), x.getContent(), x.getEvaluation()))
                .toList(), s.getStatus()));
        validarContacto(s);
        syllabus.saveAndFlush(s);

        int version = versionado.registrar(s, "Renovado sin ajustes respecto al período "
                + origen.getAcademicPeriod(), autor);
        eventos.publishEvent(AuditEvent.de(actor.id(), "CREATE", ENTIDAD, s.getId()).con(Map.of(
                "title", s.getTitle(), "academicPeriod", periodo, "status", s.getStatus().name(),
                "versionNumber", version, "renewedFrom", origen.getAcademicPeriod())));
        avisarSiQuedaPorFirmar(s);
        return respuesta(s, version);
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
        int actual = versionado.actual(id);
        if (req.baseVersion() != null && req.baseVersion() != actual) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Otra persona guardó cambios en este syllabus "
                    + "(versión " + actual + "). Recargue la página para editar sobre la versión actual.");
        }

        VersionSyllabus.Snapshot anterior = VersionSyllabus.Snapshot.de(s);
        s.setTitle(req.title().trim());
        s.setStatus(req.status());
        aplicarEncabezado(actor, s, req.headerTitle(), req.documentCode(), req.documentDate());
        s.reemplazarSecciones(secciones(req.sections(), req.status()));
        validarContacto(s);
        if (anterior.equals(VersionSyllabus.Snapshot.de(s))) {
            throw invalido("No hay cambios para guardar.");
        }
        syllabus.flush();

        int version = versionado.registrar(s, req.changeDescription().trim(), usuarios.getReferenceById(actor.id()));
        publicar(actor, "UPDATE", s, version);
        avisarSiQuedaPorFirmar(s);
        return respuesta(s, version);
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
        int version = versionado.registrar(s, "Syllabus desactivado", usuarios.getReferenceById(actor.id()));
        publicar(actor, "DELETE", s, version);
    }

    @Transactional(readOnly = true)
    public List<VersionResponse> versiones(UUID id) {
        if (!syllabus.existsById(id)) {
            throw noEncontrado();
        }
        return versionado.historial(id);
    }

    @Transactional(readOnly = true)
    public VersionResponse version(UUID id, UUID versionId) {
        return versionado.obtener(id, versionId);
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
                // La identificación se arma con los datos de la asignatura (PDF y vista web): no exige texto.
                if (completa && contenido.isEmpty() && !IDENTIFICACION.equals(x.sectionType())) {
                    throw invalido("La sección «" + titulo + "» no tiene contenido. Complétela o guarde el "
                            + "syllabus como borrador.");
                }
                resultado.add(new SeccionSyllabus(x.sectionType(), titulo, contenido, null, i + 1));
            }
        }
        return resultado;
    }

    /**
     * El syllabus vigente debe tener a quién contactar: el correo de la coordinación que ofrece la
     * asignatura (departamento o programa de posgrado) o, si no tiene, el docente asignado.
     */
    private static void validarContacto(Syllabus s) {
        Asignatura a = s.getAsignatura();
        if (s.getStatus() == Syllabus.Estado.ACTIVO && a.coordinationEmail() == null && a.getProfesor() == null) {
            throw invalido(a.responsable() + " no tiene correo de coordinación: asigne un docente a la asignatura "
                    + "antes de activar el syllabus.");
        }
    }

    /** Una versión vigente nueva no tiene firmas: se avisa a la coordinación que debe firmarla. */
    private void avisarSiQuedaPorFirmar(Syllabus s) {
        if (s.getStatus() == Syllabus.Estado.ACTIVO) {
            eventos.publishEvent(new SyllabusPorFirmar(s.getId()));
        }
    }

    /** Los espacios del plan (curso libre, electiva, énfasis, práctica) no tienen syllabus propio. */
    private static void sinSyllabusPropio(Asignatura a) {
        if (!a.isRequiresSyllabus()) {
            throw invalido(a.getName() + " es un espacio del plan sin syllabus propio (curso libre, electiva, "
                    + "énfasis o práctica).");
        }
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

    private static boolean puedeEditar(UsuarioAutenticado actor) {
        return actor.permisos().contains("syllabus:update");
    }

    private SyllabusResponse detalle(Syllabus s, Alcance escritura) {
        int version = versionado.actual(s.getId());
        return SyllabusResponse.detalle(s, version, verificacion(s.getId(), version),
                planes.deAsignaturas(List.of(s.getAsignatura().getId())),
                escritura.incluyeAsignatura(s.getAsignatura().getId()));
    }

    /** Respuesta tras crear o editar: la versión nueva aún no tiene firmas. */
    private SyllabusResponse respuesta(Syllabus s, int version) {
        return SyllabusResponse.detalle(s, version, Verificacion.Estado.SIN_FIRMAS,
                planes.deAsignaturas(List.of(s.getAsignatura().getId())), true);
    }

    private Verificacion.Estado verificacion(UUID id, int version) {
        return Verificacion.estado(firmas.pasosFirmados(id, version));
    }

    /** Estado de verificación de la versión actual de cada syllabus. */
    private Map<UUID, Verificacion.Estado> verificaciones(List<UUID> ids) {
        Map<UUID, List<String>> pasos = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Object[] fila : firmas.pasosDeVersionActual(ids)) {
                pasos.computeIfAbsent((UUID) fila[0], k -> new ArrayList<>()).add((String) fila[1]);
            }
        }
        Map<UUID, Verificacion.Estado> estados = new HashMap<>();
        pasos.forEach((id, p) -> estados.put(id, Verificacion.estado(p)));
        return estados;
    }

    private Set<UUID> delPrograma(Filtros f) {
        return f.programId() == null ? null : planes.asignaturasDelPrograma(f.programId());
    }

    private void publicar(UsuarioAutenticado actor, String accion, Syllabus s, int version) {
        eventos.publishEvent(AuditEvent.de(actor.id(), accion, ENTIDAD, s.getId()).con(Map.of(
                "title", s.getTitle(),
                "academicPeriod", s.getAcademicPeriod(),
                "status", s.getStatus().name(),
                "versionNumber", version)));
    }

    private static Specification<Syllabus> especificacion(Filtros f, Set<UUID> delPrograma, Alcance lectura,
                                                          Alcance edicion) {
        return (root, query, cb) -> {
            Join<Syllabus, Asignatura> a = root.join("asignatura");
            List<Predicate> p = new ArrayList<>();
            if (f.departmentId() != null) p.add(cb.equal(a.get("departamento").get("id"), f.departmentId()));
            if (delPrograma != null) p.add(en(cb, a.get("id"), delPrograma));
            if (f.subjectId() != null) p.add(cb.equal(a.get("id"), f.subjectId()));
            if (StringUtils.hasText(f.academicPeriod())) {
                p.add(cb.equal(root.get("academicPeriod"), f.academicPeriod()));
            }
            if (f.status() != null) p.add(cb.equal(root.get("status"), f.status()));
            if (StringUtils.hasText(f.search())) {
                String patron = "%" + f.search().trim().toLowerCase() + "%";
                p.add(cb.or(cb.like(cb.lower(root.get("title")), patron),
                        cb.like(cb.lower(a.get("name")), patron),
                        cb.like(cb.lower(a.get("code")), patron)));
            }
            if (!lectura.total()) {
                p.add(en(cb, a.get("id"), lectura.subjectIds()));
            }
            Predicate vigente = cb.equal(root.get("status"), Syllabus.Estado.ACTIVO);
            if (edicion == null) {
                p.add(vigente);
            } else if (!edicion.total()) {
                p.add(cb.or(vigente, en(cb, a.get("id"), edicion.subjectIds())));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
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
