package co.edu.uam.syllabus.common.security;

import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Único punto de decisión de permisos: permiso efectivo (rol ∪ otorgados − restringidos)
 * acotado por el alcance del usuario. Uso:
 * {@code @PreAuthorize("@permisos.puede(authentication, 'syllabus:update', #id)")}
 */
@Component("permisos")
public class PermisoEvaluator {

    private final ScopeService scopeService;
    private final JdbcClient jdbc;

    public PermisoEvaluator(ScopeService scopeService, JdbcClient jdbc) {
        this.scopeService = scopeService;
        this.jdbc = jdbc;
    }

    public boolean puede(Authentication auth, String permiso, UUID id) {
        if (!(auth.getPrincipal() instanceof UsuarioAutenticado usuario) || !usuario.permisos().contains(permiso)) {
            return false;
        }
        if (id == null) {
            return true;
        }
        Alcance alcance = scopeService.de(usuario.id(), !permiso.endsWith(":read"));
        if (alcance.total()) {
            return true;
        }
        String recurso = permiso.substring(0, permiso.indexOf(':'));
        // Si el registro no existe se permite el paso para que el servicio responda 404.
        return switch (recurso) {
            case "syllabus" -> asignatura("""
                    SELECT s.id, s.program_id, s.department_id
                    FROM syllabus y JOIN subjects s ON s.id = y.subject_id WHERE y.id = ?""", id, alcance);
            case "subject" -> asignatura(
                    "SELECT id, program_id, department_id FROM subjects WHERE id = ?", id, alcance);
            case "program" -> jdbc.sql("SELECT department_id FROM programs WHERE id = ?").param(id)
                    .query((rs, i) -> Optional.ofNullable(rs.getObject("department_id", UUID.class)))
                    .optional()
                    .map(depto -> alcance.incluyePrograma(id, depto.orElse(null)))
                    .orElse(true);
            case "department" -> alcance.incluyeDepartamento(id);
            default -> true;
        };
    }

    private boolean asignatura(String sql, UUID id, Alcance alcance) {
        return jdbc.sql(sql).param(id)
                .query((rs, i) -> alcance.incluyeAsignatura(
                        rs.getObject("id", UUID.class),
                        rs.getObject("program_id", UUID.class),
                        rs.getObject("department_id", UUID.class)))
                .optional()
                .orElse(true);
    }
}
