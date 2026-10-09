package co.edu.uam.syllabus.catalogos;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartamentoRepository extends JpaRepository<Departamento, UUID> {}
