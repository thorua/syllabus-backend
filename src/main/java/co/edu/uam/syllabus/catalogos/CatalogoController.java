package co.edu.uam.syllabus.catalogos;

import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/programs")
    @PreAuthorize("hasAuthority('program:read')")
    public List<ProgramaResponse> programas(@RequestParam(required = false) UUID departmentId,
                                            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return service.programas(departmentId, includeInactive);
    }
}
