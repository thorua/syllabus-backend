package co.edu.uam.syllabus.catalogos;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanEstudiosRepository extends JpaRepository<PlanEstudios, UUID> {

    @EntityGraph(attributePaths = {"programa", "programa.facultad", "programa.departamento",
        "programa.departamento.facultad"})
    List<PlanEstudios> findByProgramaIdOrderByName(UUID programaId);

    @EntityGraph(attributePaths = {"programa", "programa.facultad", "programa.departamento", "programa.departamento.facultad",
        "asignaturas", "asignaturas.asignatura", "asignaturas.asignatura.departamento", "asignaturas.asignatura.programa"})
    Optional<PlanEstudios> findDetalleById(UUID id);

    boolean existsByProgramaIdAndNameIgnoreCase(UUID programaId, String name);

    boolean existsByProgramaIdAndNameIgnoreCaseAndIdNot(UUID programaId, String name, UUID id);
}
