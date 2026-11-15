package co.edu.uam.syllabus.common;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.uam.syllabus.PruebaIntegracion;
import org.junit.jupiter.api.Test;

/** La interfaz Angular se sirve desde el mismo contenedor y origen que la API. */
class InterfazWebTest extends PruebaIntegracion {

    @Test
    void lasRutasDeLaInterfazDevuelvenIndexSinSesion() throws Exception {
        // La raíz la atiende la página de bienvenida de Spring Boot, que reenvía a index.html.
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(forwardedUrl("index.html"));
        for (String ruta : new String[] {"/syllabus/" + ING_SOFTWARE_II + "/edit", "/login"}) {
            mvc.perform(get(ruta))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("<app-root>")))
                    .andExpect(header().string("Cache-Control", "no-cache"));
        }
    }

    @Test
    void laApiSigueProtegidaYNoDevuelveLaInterfaz() throws Exception {
        mvc.perform(get("/api/syllabus")).andExpect(status().isUnauthorized());
        mvc.perform(como("admin", get("/api/no-existe")))
                .andExpect(status().isNotFound())
                .andExpect(forwardedUrl(null));
    }
}
