package co.edu.uam.syllabus.configuracion;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.uam.syllabus.PruebaIntegracion;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

/** UC18 plantilla institucional, UC19 versión del formato y UC20 estrategia de evaluación. */
class ConfiguracionTest extends PruebaIntegracion {

    @Test
    void todosLeenLaConfiguracionYSoloRegistroOAdministracionLaCambian() throws Exception {
        String actual = mvc.perform(como("estudiante", get("/api/settings")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentVersion").value("6"))
                .andExpect(jsonPath("$.evaluationTemplate[*].label", contains("I", "II", "III")))
                .andExpect(jsonPath("$.evaluationTemplate[*].porcentaje", contains(30, 35, 35)))
                .andReturn().getResponse().getContentAsString();

        mvc.perform(como("profesor", put("/api/settings").content(actual))).andExpect(status().isForbidden());

        String otroPie = actual.replace("Documento generado por el Sistema de Gestión de Syllabus", "Pie de prueba");
        try {
            mvc.perform(como("registro", put("/api/settings").content(otroPie)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.footerText").value("Pie de prueba"));
        } finally {
            mvc.perform(como("registro", put("/api/settings").content(actual))).andExpect(status().isOk());
        }
    }

    @Test
    void laPlantillaAdmiteComoMaximoTresCortesQueSumen100() throws Exception {
        String actual = mvc.perform(como("admin", get("/api/settings")))
                .andReturn().getResponse().getContentAsString();
        String cuatroCortes = actual.replace("\"evaluationTemplate\":[", """
                "evaluationTemplate":[
                {"corte":0,"label":"EXTRA","porcentaje":10,"tipos":[{"tipo":"Quiz","porcentaje":100}]},""");
        mvc.perform(como("admin", put("/api/settings").content(cuatroCortes)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La plantilla debe tener entre 1 y 3 cortes."));
    }

    @Test
    void elLogoDebeSerUnaImagenYSeSirveSinSesion() throws Exception {
        MockMultipartFile texto = new MockMultipartFile("file", "logo.png", "image/png", "no soy imagen".getBytes());
        mvc.perform(multipart("/api/settings/logo").file(texto)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("admin")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El logo debe ser una imagen PNG o JPEG."));
        mvc.perform(get("/api/settings/logo"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));
    }
}
