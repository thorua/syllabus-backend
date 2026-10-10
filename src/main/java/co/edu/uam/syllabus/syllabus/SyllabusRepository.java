package co.edu.uam.syllabus.syllabus;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface SyllabusRepository extends JpaRepository<Syllabus, UUID>, JpaSpecificationExecutor<Syllabus> {

    @Override
    @EntityGraph(attributePaths = {"asignatura", "asignatura.programa", "asignatura.programa.departamento",
        "asignatura.departamento", "asignatura.profesor", "createdBy"})
    Page<Syllabus> findAll(Specification<Syllabus> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"asignatura", "asignatura.programa", "asignatura.programa.departamento",
        "asignatura.departamento", "asignatura.profesor", "createdBy"})
    List<Syllabus> findAll(Specification<Syllabus> spec, Sort sort);

    @EntityGraph(attributePaths = {"asignatura", "asignatura.programa", "asignatura.programa.departamento",
        "asignatura.departamento", "asignatura.profesor", "createdBy", "secciones"})
    Optional<Syllabus> findDetalleById(UUID id);

    /** Bloquea la fila para que dos ediciones simultáneas no generen el mismo número de versión. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Syllabus s where s.id = :id")
    Optional<Syllabus> findParaEditarById(UUID id);

    boolean existsByAsignaturaIdAndAcademicPeriod(UUID asignaturaId, String academicPeriod);

    @Query("select distinct s.academicPeriod from Syllabus s order by s.academicPeriod desc")
    List<String> periodos();
}
