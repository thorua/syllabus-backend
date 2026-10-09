package co.edu.uam.syllabus.usuarios;

import co.edu.uam.syllabus.common.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties props;

    public UsuarioService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder, AppProperties props) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.props = props;
    }

    /** Crea el administrador inicial desde ADMIN_EMAIL / ADMIN_PASSWORD si aún no existe. */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void crearAdministradorInicial() {
        AppProperties.Admin admin = props.admin();
        if (!StringUtils.hasText(admin.email()) || !StringUtils.hasText(admin.password())
                || usuarios.existsByEmailIgnoreCase(admin.email())) {
            return;
        }
        Usuario usuario = new Usuario();
        usuario.setEmail(admin.email().toLowerCase());
        usuario.setName("Administrador");
        usuario.setPasswordHash(passwordEncoder.encode(admin.password()));
        usuario.getRoles().add(Rol.ADMINISTRADOR);
        usuarios.save(usuario);
        log.info("Administrador inicial creado: {}", usuario.getEmail());
    }
}
