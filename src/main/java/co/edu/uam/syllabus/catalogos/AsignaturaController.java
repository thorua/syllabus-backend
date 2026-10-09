package co.edu.uam.syllabus.catalogos;

import co.edu.uam.syllabus.common.security.UsuarioAutenticado;
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

/** Asignaturas (UC7) y asignación de profesor (UC8), acotadas por el alcance del usuario. */
@RestController
@RequestMapping("/api/subjects")
public class AsignaturaController {

    private final AsignaturaService service;

    public AsignaturaController(AsignaturaService service) {
        this.service = service;
    }

    public record AsignarProfesorRequest(UUID professorId) {}

    @GetMapping
    @PreAuthorize("hasAuthority('subject:read')")
    public List<AsignaturaResponse> listar(@AuthenticationPrincipal UsuarioAutenticado actor,
                                           @RequestParam(required = false) UUID departmentId,
                                           @RequestParam(required = false) UUID programId,
                                           @RequestParam(required = false) UUID professorId,
                                           @RequestParam(required = false) String search,
                                           @RequestParam(defaultValue = "false") boolean includeInactive) {
        return service.listar(actor.id(),
                new AsignaturaService.Filtros(departmentId, programId, professorId, search, includeInactive));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permisos.puede(authentication, 'subject:read', #id)")
    public AsignaturaResponse obtener(@PathVariable UUID id) {
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('subject:create')")
    public AsignaturaResponse crear(@AuthenticationPrincipal UsuarioAutenticado actor,
                                    @Valid @RequestBody AsignaturaService.AsignaturaRequest request) {
        return service.crear(actor.id(), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permisos.puede(authentication, 'subject:update', #id)")
    public AsignaturaResponse actualizar(@AuthenticationPrincipal UsuarioAutenticado actor, @PathVariable UUID id,
                                         @Valid @RequestBody AsignaturaService.AsignaturaRequest request) {
        return service.actualizar(actor.id(), id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permisos.puede(authentication, 'subject:delete', #id)")
    public Map<String, String> desactivar(@AuthenticationPrincipal UsuarioAutenticado actor, @PathVariable UUID id) {
        service.desactivar(actor.id(), id);
        return Map.of("message", "Asignatura desactivada correctamente");
    }

    @PutMapping("/{id}/assign-professor")
    @PreAuthorize("@permisos.puede(authentication, 'subject:assign', #id)")
    public AsignaturaResponse asignarProfesor(@AuthenticationPrincipal UsuarioAutenticado actor, @PathVariable UUID id,
                                              @RequestBody AsignarProfesorRequest request) {
        return service.asignarProfesor(actor.id(), id, request.professorId());
    }
}
