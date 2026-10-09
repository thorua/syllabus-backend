package co.edu.uam.syllabus.auditoria;

import co.edu.uam.syllabus.common.PageResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit")
@PreAuthorize("hasAuthority('audit:read')")
public class AuditoriaController {

    private final AuditoriaService auditoria;

    public AuditoriaController(AuditoriaService auditoria) {
        this.auditoria = auditoria;
    }

    @GetMapping
    public PageResponse<AuditoriaService.AuditLogResponse> listar(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String entity,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        return auditoria.buscar(new AuditoriaService.Filtros(userId, entity, action, dateFrom, dateTo), page, limit);
    }

    /** Valores existentes de entidad y acción, para los filtros de la pantalla. */
    @GetMapping("/options")
    public Map<String, List<String>> opciones() {
        return auditoria.opciones();
    }
}
