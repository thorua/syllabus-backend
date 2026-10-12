package co.edu.uam.syllabus.catalogos;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DepartamentoRepository extends JpaRepository<Departamento, UUID> {

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    boolean existsByFacultadIdAndActiveTrue(UUID facultadId);

    /** Filas [faculty_id, total] con los departamentos activos por facultad. */
    @Query("select d.facultad.id, count(d) from Departamento d where d.active group by d.facultad.id")
    List<Object[]> contarPorFacultad();
}
