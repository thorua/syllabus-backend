package co.edu.uam.syllabus.auditoria;

import java.util.Map;
import java.util.UUID;

/**
 * Evento que publican los servicios al cambiar datos; {@link AuditoriaService} lo persiste.
 * userId es el autor (nulo si no hay usuario identificado, p. ej. un login fallido).
 */
public record AuditEvent(UUID userId, String action, String entity, Object entityId, Map<String, Object> details) {

    public static AuditEvent de(UUID userId, String action, String entity, Object entityId) {
        return new AuditEvent(userId, action, entity, entityId, Map.of());
    }

    public AuditEvent con(Map<String, Object> detalles) {
        return new AuditEvent(userId, action, entity, entityId, detalles);
    }
}
