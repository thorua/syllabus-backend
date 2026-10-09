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
}
