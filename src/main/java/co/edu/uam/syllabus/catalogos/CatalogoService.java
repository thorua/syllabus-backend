package co.edu.uam.syllabus.catalogos;

import co.edu.uam.syllabus.auditoria.AuditEvent;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** UC5 departamentos y UC6 programas (pregrado con departamento, posgrado sin él). */
@Service
public class CatalogoService {

    private static final Sort POR_NOMBRE = Sort.by("name");

    private final DepartamentoRepository departamentos;
    private final ProgramaRepository programas;
    private final AsignaturaRepository asignaturas;
    private final ApplicationEventPublisher eventos;

    public CatalogoService(DepartamentoRepository departamentos, ProgramaRepository programas,
                           AsignaturaRepository asignaturas, ApplicationEventPublisher eventos) {
        this.departamentos = departamentos;
        this.programas = programas;
        this.asignaturas = asignaturas;
        this.eventos = eventos;
    }

    /** isActive nulo = sin cambio; true permite reactivar un registro desactivado. */
    public record DepartamentoRequest(
            @NotBlank @Size(max = 150) String name,
            @NotBlank @Size(max = 20) String code,
            Boolean isActive) {}

    public record ProgramaRequest(
            @NotBlank @Size(max = 200) String name,
            @NotBlank @Size(max = 20) String code,
            @NotNull Programa.Nivel level,
            UUID departmentId,
            @Email @Size(max = 150) String coordinationEmail,
            Boolean isActive) {}

    // ---------- Departamentos (UC5) ----------

    @Transactional(readOnly = true)
    public List<DepartamentoResponse> departamentos(boolean incluirInactivos) {
        Map<UUID, Long> porProgramas = conteos(programas.contarPorDepartamento());
        Map<UUID, Long> porAsignaturas = conteos(asignaturas.contarPorDepartamento());
        return departamentos.findAll(POR_NOMBRE).stream()
                .filter(d -> incluirInactivos || d.isActive())
                .map(d -> DepartamentoResponse.conConteos(d,
                        porProgramas.getOrDefault(d.getId(), 0L), porAsignaturas.getOrDefault(d.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public DepartamentoResponse departamento(UUID id) {
        return DepartamentoResponse.from(buscarDepartamento(id));
    }

    @Transactional
    public DepartamentoResponse crearDepartamento(UUID actor, DepartamentoRequest req) {
        String codigo = normalizarCodigo(req.code());
        if (departamentos.existsByCodeIgnoreCase(codigo) || departamentos.existsByNameIgnoreCase(req.name().trim())) {
            throw conflicto("Ya existe un departamento con ese nombre o código.");
        }
        Departamento d = new Departamento();
        aplicar(d, req, codigo);
        departamentos.save(d);
        publicar(actor, "CREATE", "department", d.getId(), d.getCode(), d.getName());
        return DepartamentoResponse.from(d);
    }

    @Transactional
    public DepartamentoResponse actualizarDepartamento(UUID actor, UUID id, DepartamentoRequest req) {
        Departamento d = buscarDepartamento(id);
        String codigo = normalizarCodigo(req.code());
        if (departamentos.existsByCodeIgnoreCaseAndIdNot(codigo, id)
                || departamentos.existsByNameIgnoreCaseAndIdNot(req.name().trim(), id)) {
            throw conflicto("Ya existe un departamento con ese nombre o código.");
        }
        if (Boolean.FALSE.equals(req.isActive()) && d.isActive()) {
            validarDesactivarDepartamento(id);
        }
        aplicar(d, req, codigo);
        publicar(actor, "UPDATE", "department", id, d.getCode(), d.getName());
        return DepartamentoResponse.from(d);
    }

    /** Borrado lógico: solo si ya no tiene programas ni asignaturas activas. */
    @Transactional
    public void desactivarDepartamento(UUID actor, UUID id) {
        Departamento d = buscarDepartamento(id);
        validarDesactivarDepartamento(id);
        d.setActive(false);
        publicar(actor, "DELETE", "department", id, d.getCode(), d.getName());
    }

    // ---------- Programas (UC6) ----------

    @Transactional(readOnly = true)
    public List<ProgramaResponse> programas(UUID departamentoId, boolean incluirInactivos) {
        Map<UUID, Long> porAsignaturas = conteos(asignaturas.contarPorPrograma());
        return programas.findAll(POR_NOMBRE).stream()
                .filter(p -> incluirInactivos || p.isActive())
                .filter(p -> departamentoId == null
                        || (p.getDepartamento() != null && p.getDepartamento().getId().equals(departamentoId)))
                .map(p -> ProgramaResponse.conConteo(p, porAsignaturas.getOrDefault(p.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProgramaResponse programa(UUID id) {
        return ProgramaResponse.from(buscarPrograma(id));
    }

    @Transactional
    public ProgramaResponse crearPrograma(UUID actor, ProgramaRequest req) {
        String codigo = normalizarCodigo(req.code());
        if (programas.existsByCodeIgnoreCase(codigo)) {
            throw conflicto("Ya existe un programa con ese código.");
        }
        Programa p = new Programa();
        aplicar(p, req, codigo);
        programas.save(p);
        publicar(actor, "CREATE", "program", p.getId(), p.getCode(), p.getName());
        return ProgramaResponse.from(p);
    }

    @Transactional
    public ProgramaResponse actualizarPrograma(UUID actor, UUID id, ProgramaRequest req) {
        Programa p = buscarPrograma(id);
        String codigo = normalizarCodigo(req.code());
        if (programas.existsByCodeIgnoreCaseAndIdNot(codigo, id)) {
            throw conflicto("Ya existe un programa con ese código.");
        }
        // Pasar de pregrado a posgrado (o al revés) cambia el departamento de sus asignaturas.
        if (esPosgrado(p.getLevel()) != esPosgrado(req.level()) && asignaturas.existsByProgramaId(id)) {
            throw conflicto("No se puede cambiar entre pregrado y posgrado un programa que ya tiene asignaturas.");
        }
        if (Boolean.FALSE.equals(req.isActive()) && p.isActive()) {
            validarDesactivarPrograma(id);
        }
        aplicar(p, req, codigo);
        publicar(actor, "UPDATE", "program", id, p.getCode(), p.getName());
        return ProgramaResponse.from(p);
    }

    /** Borrado lógico: solo si ya no tiene asignaturas activas. */
    @Transactional
    public void desactivarPrograma(UUID actor, UUID id) {
        Programa p = buscarPrograma(id);
        validarDesactivarPrograma(id);
        p.setActive(false);
        publicar(actor, "DELETE", "program", id, p.getCode(), p.getName());
    }

    static boolean esPosgrado(Programa.Nivel nivel) {
        return nivel != Programa.Nivel.PREGRADO;
    }

    /** Convierte filas [id, total] de una consulta agrupada en un mapa. */
    static Map<UUID, Long> conteos(List<Object[]> filas) {
        return filas.stream().collect(Collectors.toMap(f -> (UUID) f[0], f -> ((Number) f[1]).longValue()));
    }

    static String normalizarCodigo(String codigo) {
        return codigo.trim().toUpperCase();
    }

    static ResponseStatusException conflicto(String mensaje) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensaje);
    }

    Departamento buscarDepartamento(UUID id) {
        return departamentos.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Departamento no encontrado."));
    }

    Programa buscarPrograma(UUID id) {
        return programas.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Programa no encontrado."));
    }

    private void aplicar(Departamento d, DepartamentoRequest req, String codigo) {
        d.setName(req.name().trim());
        d.setCode(codigo);
        if (req.isActive() != null) {
            d.setActive(req.isActive());
        }
    }

    private void aplicar(Programa p, ProgramaRequest req, String codigo) {
        p.setName(req.name().trim());
        p.setCode(codigo);
        p.setLevel(req.level());
        p.setDepartamento(esPosgrado(req.level()) ? null : departamentoActivo(req.departmentId()));
        p.setCoordinationEmail(StringUtils.hasText(req.coordinationEmail())
                ? req.coordinationEmail().trim().toLowerCase(Locale.ROOT) : null);
        if (req.isActive() != null) {
            p.setActive(req.isActive());
        }
    }

    private Departamento departamentoActivo(UUID id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un programa de pregrado requiere departamento.");
        }
        Departamento d = buscarDepartamento(id);
        if (!d.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El departamento seleccionado está inactivo.");
        }
        return d;
    }

    private void validarDesactivarDepartamento(UUID id) {
        if (programas.existsByDepartamentoIdAndActiveTrue(id) || asignaturas.existsByDepartamentoIdAndActiveTrue(id)) {
            throw conflicto("El departamento tiene programas o asignaturas activas; desactívelos primero.");
        }
    }

    private void validarDesactivarPrograma(UUID id) {
        if (asignaturas.existsByProgramaIdAndActiveTrue(id)) {
            throw conflicto("El programa tiene asignaturas activas; desactívelas primero.");
        }
    }

    private void publicar(UUID actor, String accion, String entidad, UUID id, String codigo, String nombre) {
        eventos.publishEvent(AuditEvent.de(actor, accion, entidad, id).con(Map.of("code", codigo, "name", nombre)));
    }
}
