package co.edu.uam.syllabus.syllabus;

import co.edu.uam.syllabus.common.PageResponse;
import co.edu.uam.syllabus.common.security.UsuarioAutenticado;
import co.edu.uam.syllabus.syllabus.versionado.VersionResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Syllabus (UC10–UC13) y su historial de versiones (UC14, UC22). */
@RestController
@RequestMapping("/api/syllabus")
public class SyllabusController {

    private final SyllabusService service;

    public SyllabusController(SyllabusService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('syllabus:read')")
    public PageResponse<SyllabusResponse> listar(@AuthenticationPrincipal UsuarioAutenticado actor,
                                                 @RequestParam(required = false) String search,
                                                 @RequestParam(required = false) UUID departmentId,
                                                 @RequestParam(required = false) UUID programId,
                                                 @RequestParam(required = false) UUID subjectId,
                                                 @RequestParam(required = false) String academicPeriod,
                                                 @RequestParam(required = false) Syllabus.Estado status,
                                                 @RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int limit) {
        return service.listar(actor,
                new SyllabusService.Filtros(search, departmentId, programId, subjectId, academicPeriod, status),
                page, limit);
    }

    @GetMapping("/periods")
    @PreAuthorize("hasAuthority('syllabus:read')")
    public List<String> periodos() {
        return service.periodos();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permisos.puede(authentication, 'syllabus:read', #id)")
    public SyllabusResponse obtener(@AuthenticationPrincipal UsuarioAutenticado actor, @PathVariable UUID id) {
        return service.obtener(actor, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('syllabus:create')")
    public SyllabusResponse crear(@AuthenticationPrincipal UsuarioAutenticado actor,
                                  @Valid @RequestBody SyllabusService.CrearRequest request) {
        return service.crear(actor, request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permisos.puede(authentication, 'syllabus:update', #id)")
    public SyllabusResponse actualizar(@AuthenticationPrincipal UsuarioAutenticado actor, @PathVariable UUID id,
                                       @Valid @RequestBody SyllabusService.ActualizarRequest request) {
        return service.actualizar(actor, id, request);
    }

    /** Copia el syllabus a otro período sin ajustes (guía GFR-GUI-031); queda registrado en su historial. */
    @PostMapping("/{id}/renew")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('syllabus:create') and @permisos.puede(authentication, 'syllabus:update', #id)")
    public SyllabusResponse renovar(@AuthenticationPrincipal UsuarioAutenticado actor, @PathVariable UUID id,
                                    @Valid @RequestBody SyllabusService.RenovarRequest request) {
        return service.renovar(actor, id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permisos.puede(authentication, 'syllabus:delete', #id)")
    public Map<String, String> desactivar(@AuthenticationPrincipal UsuarioAutenticado actor, @PathVariable UUID id) {
        service.desactivar(actor, id);
        return Map.of("message", "Syllabus desactivado correctamente");
    }

    @GetMapping("/{id}/versions")
    @PreAuthorize("@permisos.puede(authentication, 'syllabus:versions', #id)")
    public List<VersionResponse> versiones(@PathVariable UUID id) {
        return service.versiones(id);
    }

    @GetMapping("/{id}/versions/{versionId}")
    @PreAuthorize("@permisos.puede(authentication, 'syllabus:versions', #id)")
    public VersionResponse version(@PathVariable UUID id, @PathVariable UUID versionId) {
        return service.version(id, versionId);
    }
}
