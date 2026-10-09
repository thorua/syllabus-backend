package co.edu.uam.syllabus.common.security;

import co.edu.uam.syllabus.catalogos.Departamento;
import co.edu.uam.syllabus.catalogos.Programa;
import co.edu.uam.syllabus.usuarios.Rol;
import co.edu.uam.syllabus.usuarios.Usuario;
import co.edu.uam.syllabus.usuarios.UsuarioRepository;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Calcula el alcance de un usuario (réplica de scoping.service.ts del proyecto anterior):
 * Administrador y Registro Académico ven todo; Coord. de Departamento sus departamentos;
 * Coord. de Programa y Estudiante sus programas; Profesor las asignaturas que dicta.
 *
 * Mejora frente al anterior: para escribir, el profesor queda limitado a SUS asignaturas
 * (antes heredaba el programa y el departamento completos de cada asignatura dictada).
 */
@Service
public class ScopeService {

    private final UsuarioRepository usuarios;
    private final JdbcClient jdbc;

    public ScopeService(UsuarioRepository usuarios, JdbcClient jdbc) {
        this.usuarios = usuarios;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public Alcance de(UUID usuarioId, boolean escritura) {
        Usuario usuario = usuarios.findDetalleById(usuarioId).orElseThrow();
        Set<Rol> roles = usuario.getRoles();
        if (roles.contains(Rol.ADMINISTRADOR) || roles.contains(Rol.ADM_REGISTRO)) {
            return Alcance.TOTAL;
        }

        Set<UUID> programas = new HashSet<>();
        Set<UUID> departamentos = new HashSet<>();
        Set<UUID> asignaturas = new HashSet<>();

        if (roles.contains(Rol.PROFESOR)) {
            jdbc.sql("SELECT id, program_id, department_id FROM subjects WHERE professor_id = ? AND is_active")
                    .param(usuarioId)
                    .query((rs, i) -> {
                        asignaturas.add(rs.getObject("id", UUID.class));
                        if (!escritura) {
                            programas.add(rs.getObject("program_id", UUID.class));
                            UUID depto = rs.getObject("department_id", UUID.class);
                            if (depto != null) departamentos.add(depto);
                        }
                        return null;
                    })
                    .list();
        }
        if (roles.contains(Rol.COORDINADOR_PROGRAMA) || (!escritura && roles.contains(Rol.ESTUDIANTE))) {
            usuario.getProgramas().stream().map(Programa::getId).forEach(programas::add);
        }
        if (roles.contains(Rol.COORDINADOR_DEPARTAMENTO)) {
            usuario.getDepartamentos().stream().map(Departamento::getId).forEach(departamentos::add);
        }
        return new Alcance(false, Set.copyOf(programas), Set.copyOf(departamentos), Set.copyOf(asignaturas));
    }
}
