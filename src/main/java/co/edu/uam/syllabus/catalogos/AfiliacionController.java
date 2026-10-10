package co.edu.uam.syllabus.catalogos;

import co.edu.uam.syllabus.common.security.UsuarioAutenticado;
import co.edu.uam.syllabus.usuarios.UsuarioResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** UC9 afiliaciones profesor–departamento/programa (módulo catálogos, según el diagrama de componentes). */
@RestController
public class AfiliacionController {

    private final AfiliacionService service;

    public AfiliacionController(AfiliacionService service) {
        this.service = service;
    }

    public record AfiliacionesRequest(@NotNull Set<UUID> departmentIds, @NotNull Set<UUID> programIds) {}

    @PutMapping("/api/users/{id}/affiliations")
    @PreAuthorize("hasAuthority('user:affiliations')")
    public UsuarioResponse actualizar(@AuthenticationPrincipal UsuarioAutenticado actor, @PathVariable UUID id,
                                      @Valid @RequestBody AfiliacionesRequest request) {
        return service.actualizar(actor.id(), id, request.departmentIds(), request.programIds());
    }
}
