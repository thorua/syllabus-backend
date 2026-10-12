package co.edu.uam.syllabus.syllabus.firma;

import co.edu.uam.syllabus.common.AppProperties;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Avisa por correo a quien le corresponde la firma pendiente de un syllabus vigente (guía
 * GFR-GUI-031: notificar a los coordinadores): a la coordinación del departamento que ofrece la
 * asignatura (en posgrado, la del programa) cuando se activa o cambia, y a Registro Académico
 * cuando ya la firmó la coordinación. Se envía después de confirmar la transacción; si el correo
 * falla, el syllabus igual queda en la bandeja de pendientes.
 */
@Component
class AvisoDeFirma {

    private static final Logger log = LoggerFactory.getLogger(AvisoDeFirma.class);

    private record Syllabus(String title, String code, String period, String departamento, String posgrado,
                            boolean firmadoPorCoordinacion) {}

    private record Destinatario(String email, String name) {}

    private final JdbcClient jdbc;
    private final JavaMailSender correo;
    private final AppProperties props;

    AvisoDeFirma(JdbcClient jdbc, JavaMailSender correo, AppProperties props) {
        this.jdbc = jdbc;
        this.correo = correo;
        this.props = props;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    void avisar(SyllabusPorFirmar evento) {
        Syllabus s = jdbc.sql("""
                SELECT y.title, s.code, y.academic_period, s.department_id::text AS departamento,
                       s.program_id::text AS posgrado,
                       EXISTS (SELECT 1 FROM syllabus_signatures f WHERE f.syllabus_id = y.id
                               AND f.step = 'DEPARTAMENTO' AND f.version_number =
                                   (SELECT max(version_number) FROM syllabus_versions WHERE syllabus_id = y.id))
                           AS firmado
                FROM syllabus y JOIN subjects s ON s.id = y.subject_id
                WHERE y.id = ? AND y.status = 'ACTIVO'""")
                .param(evento.syllabusId())
                .query((rs, i) -> new Syllabus(rs.getString("title"), rs.getString("code"),
                        rs.getString("academic_period"), rs.getString("departamento"), rs.getString("posgrado"),
                        rs.getBoolean("firmado")))
                .optional().orElse(null);
        if (s == null) {
            return;
        }
        String paso = s.firmadoPorCoordinacion() ? "Registro Académico" : "la coordinación";
        for (Destinatario d : destinatarios(s)) {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(props.mailFrom());
            mensaje.setTo(d.email());
            mensaje.setSubject("Syllabus pendiente de su firma: " + s.code() + " · " + s.period());
            mensaje.setText("Hola, " + d.name() + ":\n\n«" + s.title() + "» está vigente y espera la firma digital de "
                    + paso + ".\n\nRevíselo y fírmelo en " + props.url() + "/firmas\n\n"
                    + "Sistema de Gestión de Syllabus — Universidad Autónoma de Manizales");
            try {
                correo.send(mensaje);
            } catch (MailException e) {
                log.warn("No se pudo enviar el aviso de firma a {}", d.email(), e);
            }
        }
    }

    /** Registro Académico, o el coordinador del departamento (posgrado: del programa) que la ofrece. */
    private List<Destinatario> destinatarios(Syllabus s) {
        String base = "SELECT u.email, u.name FROM users u JOIN user_roles r ON r.user_id = u.id ";
        var consulta = s.firmadoPorCoordinacion()
                ? jdbc.sql(base + "WHERE u.is_active AND r.role = 'ADM_REGISTRO'")
                : s.departamento() != null
                        ? jdbc.sql(base + "JOIN user_departments a ON a.user_id = u.id WHERE u.is_active "
                                + "AND r.role = 'COORDINADOR_DEPARTAMENTO' AND a.department_id = ?::uuid")
                                .param(s.departamento())
                        : jdbc.sql(base + "JOIN user_programs a ON a.user_id = u.id WHERE u.is_active "
                                + "AND r.role = 'COORDINADOR_PROGRAMA' AND a.program_id = ?::uuid")
                                .param(s.posgrado());
        return consulta.query((rs, i) -> new Destinatario(rs.getString("email"), rs.getString("name"))).list();
    }
}
