package co.edu.uam.syllabus.catalogos;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public interface AsignaturaRepository extends JpaRepository<Asignatura, UUID>, JpaSpecificationExecutor<Asignatura> {

    @Override
    @EntityGraph(attributePaths = {"programa", "programa.facultad", "programa.departamento", "programa.departamento.facultad",
        "departamento", "departamento.facultad", "profesor"})
    @NonNull
    List<Asignatura> findAll(@Nullable Specification<Asignatura> spec, @NonNull Sort sort);

    @EntityGraph(attributePaths = {"programa", "programa.facultad", "programa.departamento", "programa.departamento.facultad",
        "departamento", "departamento.facultad", "profesor"})
    Optional<Asignatura> findDetalleById(UUID id);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);

    boolean existsByProgramaId(UUID programaId);

    /** Si el programa tiene asignaturas activas, propias (posgrado) o en sus planes de estudio. */
    @Query(value = """
            SELECT EXISTS (SELECT 1 FROM subjects s WHERE s.is_active AND (s.program_id = :programaId OR s.id IN (
                SELECT ps.subject_id FROM study_plan_subjects ps JOIN study_plans sp ON sp.id = ps.plan_id
                WHERE sp.program_id = :programaId)))""", nativeQuery = true)
    boolean programaTieneAsignaturasActivas(UUID programaId);

    boolean existsByDepartamentoIdAndActiveTrue(UUID departamentoId);

    /** Filas [subject_id, total] con el número de syllabus por asignatura. */
    @Query(value = "SELECT subject_id, count(*) FROM syllabus GROUP BY subject_id", nativeQuery = true)
    List<Object[]> contarSyllabusPorAsignatura();

    @Query(value = "SELECT count(*) FROM syllabus WHERE subject_id = :id", nativeQuery = true)
    long contarSyllabus(UUID id);

    @Query(value = "SELECT count(*) > 0 FROM syllabus WHERE subject_id = :id AND status = 'ACTIVO'", nativeQuery = true)
    boolean tieneSyllabusActivos(UUID id);

    /** Filas [department_id, total] con las asignaturas activas por departamento. */
    @Query("select a.departamento.id, count(a) from Asignatura a where a.active and a.departamento is not null"
            + " group by a.departamento.id")
    List<Object[]> contarPorDepartamento();

    /** Filas [program_id, total] con las asignaturas activas por programa (propias o en sus planes). */
    @Query(value = """
            SELECT x.program_id, count(DISTINCT x.subject_id) FROM (
                SELECT sp.program_id, ps.subject_id FROM study_plan_subjects ps JOIN study_plans sp ON sp.id = ps.plan_id
                UNION SELECT program_id, id FROM subjects WHERE program_id IS NOT NULL) x
            JOIN subjects s ON s.id = x.subject_id AND s.is_active
            GROUP BY x.program_id""", nativeQuery = true)
    List<Object[]> contarPorPrograma();
}
