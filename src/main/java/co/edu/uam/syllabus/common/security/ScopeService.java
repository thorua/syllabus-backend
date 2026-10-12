package co.edu.uam.syllabus.common.security;

import co.edu.uam.syllabus.catalogos.Departamento;
import co.edu.uam.syllabus.catalogos.Programa;
import co.edu.uam.syllabus.usuarios.Rol;
import co.edu.uam.syllabus.usuarios.Usuario;
import co.edu.uam.syllabus.usuarios.UsuarioRepository;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Calcula el alcance de un usuario: Administrador y Registro Académico ven todo; Coord. de
 * Departamento, las asignaturas de sus departamentos y los programas que dependen de ellos;
 * Coord. de Programa y Estudiante, las asignaturas de los planes de estudio de sus programas;
 * el Profesor, las asignaturas que dicta.
 *
 * Para escribir, cada asignatura la gestiona quien la ofrece: el Coord. de Programa solo gestiona
 * las de su malla que pertenecen al departamento del programa (o, en posgrado, al programa), no
 * las que aportan otros departamentos, como Matemáticas; el profesor, solo las que dicta.
 */
@Service
public class ScopeService {

    /** Asignaturas de los planes de estudio de los programas y las de posgrado que les pertenecen. */
    private static final String DE_PROGRAMAS = """
            SELECT ps.subject_id FROM study_plan_subjects ps JOIN study_plans sp ON sp.id = ps.plan_id
            WHERE sp.program_id IN (:ids)
            UNION SELECT id FROM subjects WHERE program_id IN (:ids)""";

    /** De esas, las que ofrece el propio programa: su departamento o, en posgrado, el programa. */
    private static final String PROPIAS_DE_PROGRAMAS = """
            SELECT s.id FROM subjects s
            JOIN programs p ON p.id IN (:ids) AND (s.program_id = p.id OR s.department_id = p.department_id)
            WHERE s.program_id = p.id OR EXISTS (
                SELECT 1 FROM study_plan_subjects ps JOIN study_plans sp ON sp.id = ps.plan_id
                WHERE ps.subject_id = s.id AND sp.program_id = p.id)""";

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
            List<UUID> dictadas = jdbc.sql("SELECT id FROM subjects WHERE professor_id = ? AND is_active")
                    .param(usuarioId).query(UUID.class).list();
            asignaturas.addAll(dictadas);
            if (!escritura && !dictadas.isEmpty()) {
                // Para consultar, el profesor ve los programas y departamentos de lo que dicta.
                programas.addAll(ids("""
                        SELECT sp.program_id FROM study_plan_subjects ps JOIN study_plans sp ON sp.id = ps.plan_id
                        WHERE ps.subject_id IN (:ids)
                        UNION SELECT program_id FROM subjects WHERE id IN (:ids) AND program_id IS NOT NULL""",
                        dictadas));
                departamentos.addAll(ids(
                        "SELECT department_id FROM subjects WHERE id IN (:ids) AND department_id IS NOT NULL",
                        dictadas));
            }
        }
        if (roles.contains(Rol.COORDINADOR_PROGRAMA) || (!escritura && roles.contains(Rol.ESTUDIANTE))) {
            usuario.getProgramas().stream().map(Programa::getId).forEach(programas::add);
        }
        if (roles.contains(Rol.COORDINADOR_DEPARTAMENTO)) {
            usuario.getDepartamentos().stream().map(Departamento::getId).forEach(departamentos::add);
        }

        if (!departamentos.isEmpty() && roles.contains(Rol.COORDINADOR_DEPARTAMENTO)) {
            asignaturas.addAll(ids("SELECT id FROM subjects WHERE department_id IN (:ids)", departamentos));
            if (!escritura) {
                programas.addAll(ids("SELECT id FROM programs WHERE department_id IN (:ids)", departamentos));
            }
        }
        if (!programas.isEmpty()) {
            asignaturas.addAll(ids(escritura ? PROPIAS_DE_PROGRAMAS : DE_PROGRAMAS, programas));
        }
        if (!escritura && !roles.contains(Rol.COORDINADOR_DEPARTAMENTO) && !departamentos.isEmpty()) {
            // Profesor: también consulta las asignaturas de los departamentos de lo que dicta.
            asignaturas.addAll(ids("SELECT id FROM subjects WHERE department_id IN (:ids)", departamentos));
        }
        return new Alcance(false, Set.copyOf(programas), Set.copyOf(departamentos), Set.copyOf(asignaturas));
    }

    private Set<UUID> ids(String sql, Collection<UUID> ids) {
        return new HashSet<>(jdbc.sql(sql).param("ids", ids).query(UUID.class).list());
    }
}
