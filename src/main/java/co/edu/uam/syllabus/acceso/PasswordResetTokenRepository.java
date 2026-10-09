package co.edu.uam.syllabus.acceso;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /** Invalida los tokens pendientes del usuario (al emitir uno nuevo o al restablecer). */
    @Modifying
    @Query("update PasswordResetToken t set t.usedAt = :ahora where t.usuario.id = :usuarioId and t.usedAt is null")
    void invalidarPendientes(UUID usuarioId, Instant ahora);
}
