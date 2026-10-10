package co.edu.uam.syllabus.syllabus.versionado;

import co.edu.uam.syllabus.configuracion.Evaluacion;
import co.edu.uam.syllabus.syllabus.Syllabus;
import co.edu.uam.syllabus.usuarios.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** UC22: copia inmutable del syllabus en cada guardado (la base de datos también impide modificarla). */
@Entity
@Immutable
@Table(name = "syllabus_versions")
public class VersionSyllabus {

    /** Contenido del syllabus en el momento de la versión. */
    public record Snapshot(String title, Syllabus.Estado status, String headerTitle, String documentCode,
                           String documentDate, List<Seccion> sections) {

        public record Seccion(String sectionType, String title, String content, List<Evaluacion.Corte> evaluation,
                              int order) {}

        public static Snapshot de(Syllabus s) {
            return new Snapshot(s.getTitle(), s.getStatus(), s.getHeaderTitle(), s.getDocumentCode(),
                    s.getDocumentDate(), s.getSecciones().stream()
                            .map(x -> new Seccion(x.getSectionType(), x.getTitle(), x.getContent(), x.getEvaluation(),
                                    x.getOrden()))
                            .toList());
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "syllabus_id", nullable = false)
    private UUID syllabusId;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Snapshot snapshot;

    @Column(name = "change_description", nullable = false)
    private String changeDescription;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "modified_by")
    private Usuario modifiedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected VersionSyllabus() {}

    VersionSyllabus(Syllabus syllabus, int versionNumber, String changeDescription, Usuario modifiedBy) {
        this.syllabusId = syllabus.getId();
        this.versionNumber = versionNumber;
        this.snapshot = Snapshot.de(syllabus);
        this.changeDescription = changeDescription;
        this.modifiedBy = modifiedBy;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSyllabusId() {
        return syllabusId;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public Snapshot getSnapshot() {
        return snapshot;
    }

    public String getChangeDescription() {
        return changeDescription;
    }

    public Usuario getModifiedBy() {
        return modifiedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
