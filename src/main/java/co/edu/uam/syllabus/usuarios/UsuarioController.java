package co.edu.uam.syllabus.usuarios;

import co.edu.uam.syllabus.common.PageResponse;
import co.edu.uam.syllabus.common.security.UsuarioAutenticado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

@RestController
@RequestMapping("/api/users")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    public record PermisosRequest(@NotNull List<@Valid Item> permissionOverrides) {
        public record Item(@NotBlank String action, @NotBlank String resource, boolean allow) {}
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('user:read', 'subject:assign')")
    public PageResponse<UsuarioResponse> listar(
            @AuthenticationPrincipal UsuarioAutenticado actor,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) Rol role,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        return service.listar(actor, new UsuarioService.Filtros(search, departmentId, role, isActive), page, limit);
    }

    /** Catálogo de permisos con su descripción, para el editor de permisos (UC4). */
    @GetMapping("/permission-catalog")
    @PreAuthorize("hasAuthority('user:permissions')")
    public Map<String, String> catalogoPermisos() {
        return Permisos.CATALOGO;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('user:read')")
    public UsuarioResponse obtener(@PathVariable UUID id) {
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('user:create')")
    public UsuarioResponse crear(@AuthenticationPrincipal UsuarioAutenticado actor,
                                 @Valid @RequestBody UsuarioRequest request) {
        return service.crear(actor, request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('user:update')")
    public UsuarioResponse actualizar(@AuthenticationPrincipal UsuarioAutenticado actor, @PathVariable UUID id,
                                      @Valid @RequestBody UsuarioRequest request) {
        return service.actualizar(actor, id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('user:delete')")
    public Map<String, String> desactivar(@AuthenticationPrincipal UsuarioAutenticado actor, @PathVariable UUID id) {
        service.desactivar(actor, id);
        return Map.of("message", "Usuario desactivado correctamente");
    }

    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('user:permissions')")
    public UsuarioResponse permisos(@AuthenticationPrincipal UsuarioAutenticado actor, @PathVariable UUID id,
                                    @Valid @RequestBody PermisosRequest request) {
        var pedidos = request.permissionOverrides().stream()
                .map(o -> new UsuarioService.PermisoOverrideRequest(o.action(), o.resource(), o.allow()))
                .toList();
        return service.actualizarPermisos(actor, id, pedidos);
    }
}
