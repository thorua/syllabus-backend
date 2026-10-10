package co.edu.uam.syllabus.configuracion;

import co.edu.uam.syllabus.common.security.UsuarioAutenticado;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Configuración institucional (UC18–UC20). La lectura está abierta a todo usuario autenticado y el
 * logo es público; modificarla requiere config:update (Administrador y Registro Académico).
 */
@RestController
@RequestMapping("/api/settings")
public class ConfiguracionController {

    private final ConfiguracionService service;

    public ConfiguracionController(ConfiguracionService service) {
        this.service = service;
    }

    @GetMapping
    public Configuracion obtener() {
        return service.obtener();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('config:update')")
    public Configuracion actualizar(@AuthenticationPrincipal UsuarioAutenticado actor,
                                    @Valid @RequestBody Configuracion configuracion) {
        return service.actualizar(actor.id(), configuracion);
    }

    @PostMapping("/logo")
    @PreAuthorize("hasAuthority('config:update')")
    public Configuracion subirLogo(@AuthenticationPrincipal UsuarioAutenticado actor,
                                   @RequestParam("file") MultipartFile archivo) {
        return service.guardarLogo(actor.id(), archivo);
    }

    @GetMapping("/logo")
    public ResponseEntity<byte[]> logo() {
        ConfiguracionService.Logo logo = service.logo();
        return ResponseEntity.ok()
                .contentType(logo.tipo())
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)))
                .body(logo.datos());
    }
}
