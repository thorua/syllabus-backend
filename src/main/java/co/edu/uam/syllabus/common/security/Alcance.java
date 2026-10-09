package co.edu.uam.syllabus.common.security;

import java.util.Set;
import java.util.UUID;

/**
 * Registros sobre los que un usuario puede actuar: el rol decide QUÉ puede hacer y la
 * afiliación (programas, departamentos, asignaturas que dicta) decide SOBRE QUÉ.
 */
public record Alcance(boolean total, Set<UUID> programIds, Set<UUID> departmentIds, Set<UUID> subjectIds) {

    public static final Alcance TOTAL = new Alcance(true, Set.of(), Set.of(), Set.of());

    public boolean incluyeAsignatura(UUID subjectId, UUID programId, UUID departmentId) {
        return total
                || subjectIds.contains(subjectId)
                || (programId != null && programIds.contains(programId))
                || (departmentId != null && departmentIds.contains(departmentId));
    }

    public boolean incluyePrograma(UUID programId, UUID departmentId) {
        return total
                || programIds.contains(programId)
                || (departmentId != null && departmentIds.contains(departmentId));
    }

    public boolean incluyeDepartamento(UUID departmentId) {
        return total || departmentIds.contains(departmentId);
    }
}
