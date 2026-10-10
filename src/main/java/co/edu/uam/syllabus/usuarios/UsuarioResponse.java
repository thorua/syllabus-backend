package co.edu.uam.syllabus.usuarios;

import co.edu.uam.syllabus.catalogos.DepartamentoResponse;
import co.edu.uam.syllabus.catalogos.ProgramaResponse;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Forma JSON del usuario que consume el frontend. "role" es el rol principal (solo para mostrar);
 * "permissions" es la lista de permisos efectivos que usa can().
 */
public record UsuarioResponse(
        UUID id,
        String email,
        String name,
        Rol role,
        List<Rol> roles,
        Usuario.Genero gender,
        String phone,
        boolean isActive,
        List<DepartamentoResponse> departments,
        List<ProgramaResponse> programs,
        List<OverrideRef> permissionOverrides,
        Set<String> permissions,
        String studentCode,
        String employeeCode,
        Instant createdAt,
        Instant updatedAt) {

    public record OverrideRef(String action, String resource, boolean allow) {}

    public static UsuarioResponse from(Usuario u) {
        return new UsuarioResponse(
                u.getId(),
                u.getEmail(),
                u.getName(),
                u.rolPrincipal(),
                u.getRoles().stream().sorted(Comparator.comparingInt(Rol::nivel)).toList(),
                u.getGender(),
                u.getPhone(),
                u.isActive(),
                u.getDepartamentos().stream().map(DepartamentoResponse::from)
                        .sorted(Comparator.comparing(DepartamentoResponse::name)).toList(),
                u.getProgramas().stream().map(ProgramaResponse::from)
                        .sorted(Comparator.comparing(ProgramaResponse::name)).toList(),
                u.getOverrides().stream()
                        .map(o -> new OverrideRef(o.getAction(), o.getResource(), o.isAllow())).toList(),
                u.permisosEfectivos(),
                u.getStudentCode(),
                u.getEmployeeCode(),
                u.getCreatedAt(),
                u.getUpdatedAt());
    }
}
