package co.edu.uam.syllabus.auditoria;

import co.edu.uam.syllabus.common.PageResponse;
import co.edu.uam.syllabus.usuarios.UsuarioRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** UC21. Persiste los eventos de auditoría y los consulta con filtros. */
@Service
public class AuditoriaService {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");

    private final AuditLogRepository logs;
    private final UsuarioRepository usuarios;

    public AuditoriaService(AuditLogRepository logs, UsuarioRepository usuarios) {
        this.logs = logs;
        this.usuarios = usuarios;
    }

    /**
     * Se registra tras confirmar la transacción del cambio (un cambio revertido no deja rastro falso).
     * Si el evento se publica fuera de una transacción (p. ej. un login fallido) se registra de inmediato.
     */
    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(AuditEvent evento) {
        var autor = evento.userId() == null ? null : usuarios.getReferenceById(evento.userId());
        logs.save(new AuditLog(autor, evento, ipCliente()));
    }

    public record Filtros(UUID userId, String entity, String action, LocalDate dateFrom, LocalDate dateTo) {}

    public record AuditLogResponse(
            UUID id, UUID userId, Autor user, String action, String entity, String entityId,
            Map<String, Object> details, String ipAddress, Instant createdAt) {

        public record Autor(UUID id, String name, String email) {}

        static AuditLogResponse from(AuditLog a) {
            var u = a.getUsuario();
            return new AuditLogResponse(a.getId(), u == null ? null : u.getId(),
                    u == null ? null : new Autor(u.getId(), u.getName(), u.getEmail()),
                    a.getAction(), a.getEntity(), a.getEntityId(), a.getDetails(), a.getIpAddress(), a.getCreatedAt());
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> buscar(Filtros f, int page, int limit) {
        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (f.userId() != null) p.add(cb.equal(root.get("usuario").get("id"), f.userId()));
            if (StringUtils.hasText(f.entity())) p.add(cb.equal(root.get("entity"), f.entity()));
            if (StringUtils.hasText(f.action())) p.add(cb.equal(root.get("action"), f.action()));
            if (f.dateFrom() != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("createdAt"), f.dateFrom().atStartOfDay(ZONA).toInstant()));
            }
            if (f.dateTo() != null) {
                p.add(cb.lessThan(root.get("createdAt"), f.dateTo().plusDays(1).atStartOfDay(ZONA).toInstant()));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        var pagina = PageRequest.of(Math.max(page, 1) - 1, Math.clamp(limit, 1, 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.of(logs.findAll(spec, pagina), AuditLogResponse::from);
    }

    @Transactional(readOnly = true)
    public Map<String, List<String>> opciones() {
        return Map.of("entities", logs.entidades(), "actions", logs.acciones());
    }

    private static String ipCliente() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        String reenviada = request.getHeader("X-Forwarded-For");
        return StringUtils.hasText(reenviada) ? reenviada.split(",")[0].trim() : request.getRemoteAddr();
    }
}
