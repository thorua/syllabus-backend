package co.edu.uam.syllabus.configuracion;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Configuración institucional (UC18–UC20). La lectura está abierta a todo usuario autenticado. */
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
}
