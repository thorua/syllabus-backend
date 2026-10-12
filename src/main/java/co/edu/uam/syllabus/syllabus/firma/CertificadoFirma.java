package co.edu.uam.syllabus.syllabus.firma;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Certificado de firma (PKCS#12) que el usuario dejó registrado para no buscar el archivo en cada
 * firma. El archivo sigue protegido por su contraseña, que nunca se guarda.
 */
@Entity
@Table(name = "user_certificates")
public class CertificadoFirma {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false)
    private byte[] file;

    @Column(name = "certificate_subject", nullable = false)
    private String certificateSubject;

    @Column(name = "certificate_issuer", nullable = false)
    private String certificateIssuer;

    @Column(name = "certificate_serial", nullable = false)
    private String certificateSerial;

    @Column(name = "not_after", nullable = false)
    private Instant notAfter;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    protected CertificadoFirma() {}

    CertificadoFirma(UUID userId) {
        this.userId = userId;
    }

    void reemplazar(byte[] archivo, FirmaPdf.Certificado c) {
        this.file = archivo;
        this.certificateSubject = c.titular();
        this.certificateIssuer = c.emisor();
        this.certificateSerial = c.serie();
        this.notAfter = c.vence();
        this.uploadedAt = Instant.now();
    }

    public byte[] getFile() {
        return file;
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

    public Instant getNotAfter() {
        return notAfter;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }
}
