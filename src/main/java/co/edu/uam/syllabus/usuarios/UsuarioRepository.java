package co.edu.uam.syllabus.usuarios;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID>, JpaSpecificationExecutor<Usuario> {

    @EntityGraph(attributePaths = {"roles", "departamentos", "departamentos.facultad", "programas", "programas.facultad",
        "programas.departamento", "programas.departamento.facultad", "overrides"})
    Optional<Usuario> findDetalleById(UUID id);

    @EntityGraph(attributePaths = {"roles", "departamentos", "departamentos.facultad", "programas", "programas.facultad",
        "programas.departamento", "programas.departamento.facultad", "overrides"})
    Optional<Usuario> findDetalleByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);
}
