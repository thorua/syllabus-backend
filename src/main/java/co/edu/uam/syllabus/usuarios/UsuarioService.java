package co.edu.uam.syllabus.usuarios;

import co.edu.uam.syllabus.auditoria.AuditEvent;
import co.edu.uam.syllabus.catalogos.Departamento;
import co.edu.uam.syllabus.catalogos.DepartamentoRepository;
import co.edu.uam.syllabus.catalogos.Programa;
import co.edu.uam.syllabus.catalogos.ProgramaRepository;
import co.edu.uam.syllabus.common.AppProperties;
import co.edu.uam.syllabus.common.PageResponse;
import co.edu.uam.syllabus.common.security.Alcance;
import co.edu.uam.syllabus.common.security.ScopeService;
import co.edu.uam.syllabus.common.security.UsuarioAutenticado;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** UC3 usuarios y roles, UC4 permisos por usuario, UC9 afiliaciones. */
@Service
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);
    private static final String ENTIDAD = "user";

    private final UsuarioRepository usuarios;
    private final DepartamentoRepository departamentos;
    private final ProgramaRepository programas;
    private final ScopeService scopeService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventos;
    private final AppProperties props;

    public UsuarioService(UsuarioRepository usuarios, DepartamentoRepository departamentos,
                          ProgramaRepository programas, ScopeService scopeService, PasswordEncoder passwordEncoder,
                          ApplicationEventPublisher eventos, AppProperties props) {
        this.usuarios = usuarios;
        this.departamentos = departamentos;
        this.programas = programas;
        this.scopeService = scopeService;
        this.passwordEncoder = passwordEncoder;
        this.eventos = eventos;
        this.props = props;
    }

    public record Filtros(String search, UUID departmentId, Rol role, Boolean isActive) {}

    public record PermisoOverrideRequest(String action, String resource, boolean allow) {
        String permiso() {
            return resource + ":" + action;
        }
    }

    /**
     * Quien gestiona usuarios (user:read) ve todos. Quien solo asigna asignaturas (UC8) ve
     * únicamente profesores activos afiliados a sus programas o departamentos.
     */
    @Transactional(readOnly = true)
    public PageResponse<UsuarioResponse> listar(UsuarioAutenticado actor, Filtros f, int page, int limit) {
        Filtros filtros = f;
        Alcance alcance = Alcance.TOTAL;
        if (!actor.permisos().contains("user:read")) {
            filtros = new Filtros(f.search(), f.departmentId(), Rol.PROFESOR, true);
            alcance = scopeService.de(actor.id(), false);
        }
        var pagina = PageRequest.of(Math.max(page, 1) - 1, Math.clamp(limit, 1, 100),
                Sort.by(Sort.Direction.ASC, "name"));
        return PageResponse.of(usuarios.findAll(especificacion(filtros, alcance), pagina), UsuarioResponse::from);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtener(UUID id) {
        return UsuarioResponse.from(buscar(id));
    }

    @Transactional
    public UsuarioResponse crear(UsuarioAutenticado actor, UsuarioRequest req) {
        if (!StringUtils.hasText(req.password())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña es obligatoria.");
        }
        if (usuarios.existsByEmailIgnoreCase(req.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un usuario con este correo.");
        }
        Usuario usuario = new Usuario();
        aplicar(usuario, req);
        usuarios.save(usuario);
        eventos.publishEvent(AuditEvent.de(actor.id(), "CREATE", ENTIDAD, usuario.getId()).con(resumen(usuario)));
        return UsuarioResponse.from(usuario);
    }

    @Transactional
    public UsuarioResponse actualizar(UsuarioAutenticado actor, UUID id, UsuarioRequest req) {
        Usuario usuario = buscar(id);
        if (usuarios.existsByEmailIgnoreCaseAndIdNot(req.email(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un usuario con este correo.");
        }
        if (actor.id().equals(id) && Boolean.FALSE.equals(req.isActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puede desactivar su propia cuenta.");
        }
        aplicar(usuario, req);
        eventos.publishEvent(AuditEvent.de(actor.id(), "UPDATE", ENTIDAD, id).con(resumen(usuario)));
        return UsuarioResponse.from(usuario);
    }

    /** Borrado lógico: el usuario conserva su historial pero no puede iniciar sesión. */
    @Transactional
    public void desactivar(UsuarioAutenticado actor, UUID id) {
        if (actor.id().equals(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puede desactivar su propia cuenta.");
        }
        Usuario usuario = buscar(id);
        usuario.setActive(false);
        eventos.publishEvent(AuditEvent.de(actor.id(), "DELETE", ENTIDAD, id).con(Map.of("email", usuario.getEmail())));
    }

    /** UC4: reemplaza los permisos otorgados/restringidos del usuario. */
    @Transactional
    public UsuarioResponse actualizarPermisos(UsuarioAutenticado actor, UUID id, List<PermisoOverrideRequest> pedidos) {
        Usuario usuario = buscar(id);
        Map<String, Boolean> nuevos = new LinkedHashMap<>();
        for (PermisoOverrideRequest p : pedidos) {
            if (!Permisos.CATALOGO.containsKey(p.permiso())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Permiso desconocido: " + p.permiso());
            }
            nuevos.put(p.permiso(), p.allow());
        }
        Set<PermisoOverride> actuales = usuario.getOverrides();
        actuales.removeIf(o -> !nuevos.containsKey(o.permiso()));
        Map<String, PermisoOverride> porPermiso = actuales.stream()
                .collect(Collectors.toMap(PermisoOverride::permiso, Function.identity()));
        nuevos.forEach((permiso, allow) -> {
            PermisoOverride existente = porPermiso.get(permiso);
            if (existente != null) {
                existente.setAllow(allow);
            } else {
                actuales.add(new PermisoOverride(usuario, permiso, allow, actor.id()));
            }
        });
        eventos.publishEvent(AuditEvent.de(actor.id(), "PERMISSIONS_UPDATE", ENTIDAD, id)
                .con(Map.of("email", usuario.getEmail(), "overrides", nuevos)));
        return UsuarioResponse.from(usuario);
    }

    /** UC9: afiliaciones del usuario a departamentos y programas (pregrado y posgrado). */
    @Transactional
    public UsuarioResponse actualizarAfiliaciones(UsuarioAutenticado actor, UUID id, Set<UUID> deptoIds,
                                                  Set<UUID> programaIds) {
        Usuario usuario = buscar(id);
        List<Departamento> deptos = departamentos.findAllById(deptoIds);
        List<Programa> progs = programas.findAllById(programaIds);
        if (deptos.size() != deptoIds.size() || progs.size() != programaIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Algún departamento o programa no existe.");
        }
        usuario.getDepartamentos().clear();
        usuario.getDepartamentos().addAll(deptos);
        usuario.getProgramas().clear();
        usuario.getProgramas().addAll(progs);
        eventos.publishEvent(AuditEvent.de(actor.id(), "AFFILIATIONS_UPDATE", ENTIDAD, id).con(Map.of(
                "email", usuario.getEmail(),
                "departments", deptos.stream().map(Departamento::getCode).sorted().toList(),
                "programs", progs.stream().map(Programa::getCode).sorted().toList())));
        return UsuarioResponse.from(usuario);
    }

    /** Crea el administrador inicial desde ADMIN_EMAIL / ADMIN_PASSWORD si aún no existe. */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void crearAdministradorInicial() {
        AppProperties.Admin admin = props.admin();
        if (!StringUtils.hasText(admin.email()) || !StringUtils.hasText(admin.password())
                || usuarios.existsByEmailIgnoreCase(admin.email())) {
            return;
        }
        Usuario usuario = new Usuario();
        usuario.setEmail(admin.email().toLowerCase());
        usuario.setName("Administrador");
        usuario.setPasswordHash(passwordEncoder.encode(admin.password()));
        usuario.getRoles().add(Rol.ADMINISTRADOR);
        usuarios.save(usuario);
        log.info("Administrador inicial creado: {}", usuario.getEmail());
    }

    private Usuario buscar(UUID id) {
        return usuarios.findDetalleById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado."));
    }

    private void aplicar(Usuario usuario, UsuarioRequest req) {
        usuario.setName(req.name().trim());
        usuario.setEmail(req.email().trim().toLowerCase());
        usuario.setGender(req.gender());
        usuario.setPhone(vacioANulo(req.phone()));
        usuario.setStudentCode(vacioANulo(req.studentCode()));
        usuario.setEmployeeCode(vacioANulo(req.employeeCode()));
        usuario.getRoles().clear();
        usuario.getRoles().addAll(req.roles());
        if (req.isActive() != null) {
            usuario.setActive(req.isActive());
        }
        if (StringUtils.hasText(req.password())) {
            usuario.setPasswordHash(passwordEncoder.encode(req.password()));
        }
    }

    private static Map<String, Object> resumen(Usuario u) {
        return Map.of("email", u.getEmail(), "name", u.getName(), "isActive", u.isActive(),
                "roles", u.getRoles().stream().sorted(Comparator.comparingInt(Rol::nivel)).map(Rol::name).toList());
    }

    private static String vacioANulo(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : null;
    }

    private static Specification<Usuario> especificacion(Filtros f, Alcance alcance) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (StringUtils.hasText(f.search())) {
                String patron = "%" + f.search().trim().toLowerCase() + "%";
                p.add(cb.or(cb.like(cb.lower(root.get("name")), patron), cb.like(cb.lower(root.get("email")), patron)));
            }
            if (f.role() != null) {
                p.add(cb.isMember(f.role(), root.<Set<Rol>>get("roles")));
            }
            if (f.isActive() != null) {
                p.add(cb.equal(root.get("active"), f.isActive()));
            }
            if (f.departmentId() != null) {
                p.add(root.get("id").in(afiliados(query, cb, "departamentos", Set.of(f.departmentId()))));
            }
            if (!alcance.total()) {
                p.add(cb.or(
                        root.get("id").in(afiliados(query, cb, "programas", alcance.programIds())),
                        root.get("id").in(afiliados(query, cb, "departamentos", alcance.departmentIds()))));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }

    /** Subconsulta: ids de usuarios afiliados a alguno de los ids dados (sin duplicar filas). */
    private static Subquery<UUID> afiliados(jakarta.persistence.criteria.CriteriaQuery<?> query,
                                            jakarta.persistence.criteria.CriteriaBuilder cb,
                                            String relacion, Set<UUID> ids) {
        Subquery<UUID> sub = query.subquery(UUID.class);
        Root<Usuario> u = sub.from(Usuario.class);
        var afiliacion = u.join(relacion);
        sub.select(u.get("id")).where(ids.isEmpty() ? cb.disjunction() : afiliacion.get("id").in(ids));
        return sub;
    }
}
