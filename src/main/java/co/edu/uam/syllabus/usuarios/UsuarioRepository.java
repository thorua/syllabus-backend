package co.edu.uam.syllabus.usuarios;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    @EntityGraph(attributePaths = {"roles", "departamentos", "programas", "programas.departamento", "overrides"})
    Optional<Usuario> findDetalleById(UUID id);

    @EntityGraph(attributePaths = {"roles", "departamentos", "programas", "programas.departamento", "overrides"})
    Optional<Usuario> findDetalleByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
