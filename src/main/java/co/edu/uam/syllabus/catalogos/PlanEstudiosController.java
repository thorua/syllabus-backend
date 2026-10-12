package co.edu.uam.syllabus.catalogos;

import co.edu.uam.syllabus.common.security.UsuarioAutenticado;
import jakarta.validation.Valid;
import java.util.List;
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
import org.springframework.web.server.ResponseStatusException;

/** Planes de estudio de los programas y su malla curricular con el estado de los syllabus. */
@RestController
@RequestMapping("/api/study-plans")
public class PlanEstudiosController {

    private static final String PERIODO = "\\d{4}-0[1-3]";

    private final PlanEstudiosService service;

    public PlanEstudiosController(PlanEstudiosService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('program:read')")
    public List<PlanEstudiosService.PlanResponse> listar(@RequestParam UUID programId) {
        return service.listar(programId);
    }

    @GetMapping("/{id}/curriculum")
    @PreAuthorize("hasAuthority('program:read')")
    public PlanEstudiosService.MallaResponse malla(@PathVariable UUID id, @RequestParam String academicPeriod) {
        return service.malla(id, periodo(academicPeriod));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('plan:manage')")
    public PlanEstudiosService.PlanResponse crear(@AuthenticationPrincipal UsuarioAutenticado actor,
                                                  @Valid @RequestBody PlanEstudiosService.PlanRequest request) {
        return service.crear(actor.id(), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('plan:manage')")
    public PlanEstudiosService.PlanResponse actualizar(@AuthenticationPrincipal UsuarioAutenticado actor,
                                                       @PathVariable UUID id,
                                                       @Valid @RequestBody PlanEstudiosService.PlanRequest request) {
        return service.actualizar(actor.id(), id, request);
    }

    @PutMapping("/{id}/subjects/{subjectId}")
    @PreAuthorize("hasAuthority('plan:manage')")
    public PlanEstudiosService.MallaResponse ubicar(
            @AuthenticationPrincipal UsuarioAutenticado actor, @PathVariable UUID id, @PathVariable UUID subjectId,
            @RequestParam String academicPeriod,
            @Valid @RequestBody PlanEstudiosService.AsignaturaEnPlanRequest request) {
        return service.ubicarAsignatura(actor.id(), id, subjectId, request.semester(), request.area(),
                periodo(academicPeriod));
    }

    @DeleteMapping("/{id}/subjects/{subjectId}")
    @PreAuthorize("hasAuthority('plan:manage')")
    public PlanEstudiosService.MallaResponse quitar(@AuthenticationPrincipal UsuarioAutenticado actor,
                                                    @PathVariable UUID id, @PathVariable UUID subjectId,
                                                    @RequestParam String academicPeriod) {
        return service.quitarAsignatura(actor.id(), id, subjectId, periodo(academicPeriod));
    }

    private static String periodo(String valor) {
        if (!valor.matches(PERIODO)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Use el formato AAAA-01, AAAA-02 o AAAA-03.");
        }
        return valor;
    }
}
