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

/** Departamentos (UC5) y programas de pregrado y posgrado (UC6). */
@RestController
@RequestMapping("/api")
public class CatalogoController {

    private final CatalogoService service;

    public CatalogoController(CatalogoService service) {
        this.service = service;
    }

    @GetMapping("/departments")
    @PreAuthorize("hasAuthority('department:read')")
    public List<DepartamentoResponse> departamentos(@RequestParam(defaultValue = "false") boolean includeInactive) {
        return service.departamentos(includeInactive);
    }

    @GetMapping("/departments/{id}")
    @PreAuthorize("hasAuthority('department:read')")
    public DepartamentoResponse departamento(@PathVariable UUID id) {
        return service.departamento(id);
    }

    @PostMapping("/departments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('department:create')")
    public DepartamentoResponse crearDepartamento(@AuthenticationPrincipal UsuarioAutenticado actor,
                                                  @Valid @RequestBody CatalogoService.DepartamentoRequest request) {
        return service.crearDepartamento(actor.id(), request);
    }

    @PutMapping("/departments/{id}")
    @PreAuthorize("hasAuthority('department:update')")
    public DepartamentoResponse actualizarDepartamento(@AuthenticationPrincipal UsuarioAutenticado actor,
                                                       @PathVariable UUID id,
                                                       @Valid @RequestBody CatalogoService.DepartamentoRequest request) {
        return service.actualizarDepartamento(actor.id(), id, request);
    }

    @DeleteMapping("/departments/{id}")
    @PreAuthorize("hasAuthority('department:delete')")
    public Map<String, String> desactivarDepartamento(@AuthenticationPrincipal UsuarioAutenticado actor,
                                                      @PathVariable UUID id) {
        service.desactivarDepartamento(actor.id(), id);
        return Map.of("message", "Departamento desactivado correctamente");
    }

    @GetMapping("/programs")
    @PreAuthorize("hasAuthority('program:read')")
    public List<ProgramaResponse> programas(@RequestParam(required = false) UUID departmentId,
                                            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return service.programas(departmentId, includeInactive);
    }

    @GetMapping("/programs/{id}")
    @PreAuthorize("hasAuthority('program:read')")
    public ProgramaResponse programa(@PathVariable UUID id) {
        return service.programa(id);
    }

    @PostMapping("/programs")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('program:create')")
    public ProgramaResponse crearPrograma(@AuthenticationPrincipal UsuarioAutenticado actor,
                                          @Valid @RequestBody CatalogoService.ProgramaRequest request) {
        return service.crearPrograma(actor.id(), request);
    }

    @PutMapping("/programs/{id}")
    @PreAuthorize("hasAuthority('program:update')")
    public ProgramaResponse actualizarPrograma(@AuthenticationPrincipal UsuarioAutenticado actor, @PathVariable UUID id,
                                               @Valid @RequestBody CatalogoService.ProgramaRequest request) {
        return service.actualizarPrograma(actor.id(), id, request);
    }

    @DeleteMapping("/programs/{id}")
    @PreAuthorize("hasAuthority('program:delete')")
    public Map<String, String> desactivarPrograma(@AuthenticationPrincipal UsuarioAutenticado actor,
                                                  @PathVariable UUID id) {
        service.desactivarPrograma(actor.id(), id);
        return Map.of("message", "Programa desactivado correctamente");
    }
}
