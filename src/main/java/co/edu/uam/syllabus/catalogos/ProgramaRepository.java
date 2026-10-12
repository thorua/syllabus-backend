package co.edu.uam.syllabus.catalogos;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProgramaRepository extends JpaRepository<Programa, UUID> {

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);

    boolean existsByDepartamentoIdAndActiveTrue(UUID departamentoId);

    /** Filas [department_id, total] con los programas activos por departamento. */
    @Query("select p.departamento.id, count(p) from Programa p where p.active and p.departamento is not null"
            + " group by p.departamento.id")
    List<Object[]> contarPorDepartamento();

    boolean existsByFacultadIdAndActiveTrue(UUID facultadId);

    /** Filas [faculty_id, total] con los programas activos por facultad (la propia o la de su departamento). */
    @Query("select coalesce(d.facultad.id, p.facultad.id), count(p) from Programa p left join p.departamento d"
            + " where p.active group by coalesce(d.facultad.id, p.facultad.id)")
    List<Object[]> contarPorFacultad();
}
