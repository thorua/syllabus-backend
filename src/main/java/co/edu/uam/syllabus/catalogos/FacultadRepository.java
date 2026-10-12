package co.edu.uam.syllabus.catalogos;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacultadRepository extends JpaRepository<Facultad, UUID> {

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
}
