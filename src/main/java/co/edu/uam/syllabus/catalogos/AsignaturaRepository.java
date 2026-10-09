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

public interface AsignaturaRepository extends JpaRepository<Asignatura, UUID>, JpaSpecificationExecutor<Asignatura> {

    @Override
    @EntityGraph(attributePaths = {"programa", "programa.departamento", "departamento", "profesor"})
    List<Asignatura> findAll(Specification<Asignatura> spec, Sort sort);

    @EntityGraph(attributePaths = {"programa", "programa.departamento", "departamento", "profesor"})
    Optional<Asignatura> findDetalleById(UUID id);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);

    boolean existsByProgramaId(UUID programaId);

    boolean existsByProgramaIdAndActiveTrue(UUID programaId);

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

    /** Filas [program_id, total] con las asignaturas activas por programa. */
    @Query("select a.programa.id, count(a) from Asignatura a where a.active group by a.programa.id")
    List<Object[]> contarPorPrograma();
}
