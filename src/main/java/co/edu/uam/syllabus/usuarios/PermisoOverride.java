package co.edu.uam.syllabus.usuarios;

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

/** UC4: permiso otorgado (allow=true) o restringido (allow=false) a un usuario concreto. */
@Entity
@Table(name = "user_permission_overrides")
public class PermisoOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private Usuario usuario;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String resource;

    @Column(nullable = false)
    private boolean allow;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public String permiso() {
        return resource + ":" + action;
    }

    public String getAction() {
        return action;
    }

    public String getResource() {
        return resource;
    }

    public boolean isAllow() {
        return allow;
    }
}
