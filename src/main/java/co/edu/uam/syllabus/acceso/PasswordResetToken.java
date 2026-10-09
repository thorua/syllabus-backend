package co.edu.uam.syllabus.acceso;

import co.edu.uam.syllabus.usuarios.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

/** UC2: token de un solo uso; solo se guarda su hash SHA-256. */
@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private Usuario usuario;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PasswordResetToken() {}

    PasswordResetToken(Usuario usuario, String tokenHash, Instant expiresAt) {
        this.usuario = usuario;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    boolean vigente(Instant ahora) {
        return usedAt == null && ahora.isBefore(expiresAt);
    }

    void usar(Instant ahora) {
        this.usedAt = ahora;
    }

    Usuario getUsuario() {
        return usuario;
    }
}
