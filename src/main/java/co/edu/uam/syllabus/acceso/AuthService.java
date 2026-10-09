package co.edu.uam.syllabus.acceso;

import co.edu.uam.syllabus.auditoria.AuditEvent;
import co.edu.uam.syllabus.common.AppProperties;
import co.edu.uam.syllabus.common.security.JwtService;
import co.edu.uam.syllabus.usuarios.Usuario;
import co.edu.uam.syllabus.usuarios.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ENTIDAD = "session";

    private final UsuarioRepository usuarios;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JavaMailSender mailSender;
    private final AppProperties props;
    private final ApplicationEventPublisher eventos;

    public AuthService(UsuarioRepository usuarios, PasswordResetTokenRepository tokens, PasswordEncoder passwordEncoder,
                       JwtService jwtService, JavaMailSender mailSender, AppProperties props,
                       ApplicationEventPublisher eventos) {
        this.usuarios = usuarios;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mailSender = mailSender;
        this.props = props;
        this.eventos = eventos;
    }

    public record Sesion(String token, Usuario usuario) {}

    /** UC1. Sin transacción propia: así el intento fallido también queda en la auditoría. */
    public Sesion login(String email, String password) {
        Optional<Usuario> encontrado = usuarios.findDetalleByEmailIgnoreCase(email.trim());
        Optional<Usuario> valido = encontrado
                .filter(Usuario::isActive)
                .filter(u -> passwordEncoder.matches(password, u.getPasswordHash()));
        if (valido.isEmpty()) {
            eventos.publishEvent(AuditEvent.de(null, "LOGIN_FAILED", ENTIDAD,
                    encontrado.map(Usuario::getId).orElse(null)).con(Map.of("email", email.trim().toLowerCase())));
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas.");
        }
        Usuario usuario = valido.get();
        eventos.publishEvent(AuditEvent.de(usuario.getId(), "LOGIN", ENTIDAD, usuario.getId()));
        return new Sesion(jwtService.emitir(usuario.getId(), usuario.getEmail()), usuario);
    }

    public void logout(UUID usuarioId) {
        eventos.publishEvent(AuditEvent.de(usuarioId, "LOGOUT", ENTIDAD, usuarioId));
    }

    @Transactional(readOnly = true)
    public Usuario perfil(UUID usuarioId) {
        return usuarios.findDetalleById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado."));
    }

    @Transactional
    public void cambiarPassword(UUID usuarioId, String actual, String nueva) {
        Usuario usuario = perfil(usuarioId);
        if (!passwordEncoder.matches(actual, usuario.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña actual no es correcta.");
        }
        usuario.setPasswordHash(passwordEncoder.encode(nueva));
        eventos.publishEvent(AuditEvent.de(usuarioId, "PASSWORD_CHANGE", ENTIDAD, usuarioId));
    }

    /**
     * UC2. Responde igual exista o no el correo, para no revelar qué cuentas existen.
     * El token viaja solo en el enlace del correo; en base de datos queda su hash.
     */
    @Transactional
    public void solicitarRecuperacion(String email) {
        usuarios.findDetalleByEmailIgnoreCase(email.trim()).filter(Usuario::isActive).ifPresent(usuario -> {
            Instant ahora = Instant.now();
            tokens.invalidarPendientes(usuario.getId(), ahora);
            String token = generarToken();
            tokens.save(new PasswordResetToken(usuario, hash(token), ahora.plus(props.passwordResetExpiresIn())));
            enviarCorreo(usuario, props.url() + "/reset-password?token=" + token);
            eventos.publishEvent(AuditEvent.de(usuario.getId(), "PASSWORD_RESET_REQUEST", ENTIDAD, usuario.getId()));
        });
    }

    @Transactional
    public void restablecerPassword(String token, String nueva) {
        Instant ahora = Instant.now();
        PasswordResetToken registro = tokens.findByTokenHash(hash(token))
                .filter(t -> t.vigente(ahora))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El enlace de recuperación no es válido o ha expirado."));
        Usuario usuario = registro.getUsuario();
        usuario.setPasswordHash(passwordEncoder.encode(nueva));
        registro.usar(ahora);
        tokens.invalidarPendientes(usuario.getId(), ahora);
        eventos.publishEvent(AuditEvent.de(usuario.getId(), "PASSWORD_RESET", ENTIDAD, usuario.getId()));
    }

    private void enviarCorreo(Usuario usuario, String enlace) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(props.mailFrom());
        mensaje.setTo(usuario.getEmail());
        mensaje.setSubject("Recuperación de contraseña - Sistema de Gestión de Syllabus UAM");
        mensaje.setText("""
                Hola %s,

                Recibimos una solicitud para restablecer su contraseña. Use el siguiente enlace
                (válido por %d minutos y de un solo uso):

                %s

                Si usted no hizo esta solicitud, ignore este correo.
                """.formatted(usuario.getName(), props.passwordResetExpiresIn().toMinutes(), enlace));
        try {
            mailSender.send(mensaje);
        } catch (MailException e) {
            log.error("No se pudo enviar el correo de recuperación a {}", usuario.getEmail(), e);
        }
    }

    private static String generarToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
