package co.edu.uam.syllabus.syllabus.firma;

import co.edu.uam.syllabus.usuarios.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Firma digital de una versión del syllabus, con el PDF firmado hasta ese paso. Es inmutable. */
@Entity
@Table(name = "syllabus_signatures")
public class FirmaSyllabus {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "syllabus_id", nullable = false, updatable = false)
    private UUID syllabusId;

    @Column(name = "version_number", nullable = false, updatable = false)
    private int versionNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private Verificacion.Paso step;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "signer_id", updatable = false)
    private Usuario signer;

    @Column(name = "signer_name", nullable = false, updatable = false)
    private String signerName;

    @Column(name = "certificate_subject", nullable = false, updatable = false)
    private String certificateSubject;

    @Column(name = "certificate_issuer", nullable = false, updatable = false)
    private String certificateIssuer;

    @Column(name = "certificate_serial", nullable = false, updatable = false)
    private String certificateSerial;

    @Column(nullable = false, updatable = false)
    private byte[] document;

    @Column(name = "signed_at", nullable = false, updatable = false)
    private Instant signedAt;

    protected FirmaSyllabus() {}

    FirmaSyllabus(UUID syllabusId, int versionNumber, Verificacion.Paso step, Usuario signer,
                  FirmaPdf.Certificado certificado, byte[] document) {
        this.syllabusId = syllabusId;
        this.versionNumber = versionNumber;
        this.step = step;
        this.signer = signer;
        this.signerName = signer.getName();
        this.certificateSubject = certificado.titular();
        this.certificateIssuer = certificado.emisor();
        this.certificateSerial = certificado.serie();
        this.document = document;
        this.signedAt = Instant.now();
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public Verificacion.Paso getStep() {
        return step;
    }

    public String getSignerName() {
        return signerName;
    }

    public String getCertificateSubject() {
        return certificateSubject;
    }

    public String getCertificateIssuer() {
        return certificateIssuer;
    }

    public String getCertificateSerial() {
        return certificateSerial;
    }

    public byte[] getDocument() {
        return document;
    }

    public Instant getSignedAt() {
        return signedAt;
    }
}
