package co.edu.uam.syllabus.syllabus;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.uam.syllabus.PruebaIntegracion;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

/** UC12 editar y UC22 registrar versión: cada guardado con cambios deja un snapshot inmutable. */
class VersionadoTest extends PruebaIntegracion {

    @Autowired
    private JdbcClient jdbc;

    private static String edicion(String justificacion, int baseVersion) {
        return """
                {"title": "Syllabus Base de Datos I - 2026-03", "status": "BORRADOR",
                 "changeDescription": "Ajuste de justificación", "baseVersion": %d,
                 "sections": [{"sectionType": "justification", "title": "Justificación", "content": "%s"}]}
                """.formatted(baseVersion, justificacion);
    }

    @Test
    void cadaCambioCreaUnaVersionYLasVersionesSonInmutables() throws Exception {
        String creado = mvc.perform(como("profesor", post("/api/syllabus").content("""
                        {"subjectId": "%s", "academicPeriod": "2026-03", "title": "Syllabus Base de Datos I - 2026-03",
                         "sections": [
                           {"sectionType": "justification", "title": "Justificación", "content": "Primera"}]}
                        """.formatted(ASIG_BASE_DE_DATOS))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("BORRADOR"))
                .andExpect(jsonPath("$.currentVersion").value(1))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(creado, "$.id");

        mvc.perform(como("profesor", put("/api/syllabus/" + id).content(edicion("Segunda", 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentVersion").value(2));

        // Guardar lo mismo no genera versión; editar sobre una versión vieja se rechaza (409).
        mvc.perform(como("profesor", put("/api/syllabus/" + id).content(edicion("Segunda", 2))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("No hay cambios para guardar."));
        mvc.perform(como("profesor", put("/api/syllabus/" + id).content(edicion("Tercera", 1))))
                .andExpect(status().isConflict());

        // UC13: desactivar también queda en el historial.
        mvc.perform(como("coordinador", delete("/api/syllabus/" + id))).andExpect(status().isOk());

        String versiones = mvc.perform(como("coordinador", get("/api/syllabus/" + id + "/versions")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].versionNumber", contains(3, 2, 1)))
                .andExpect(jsonPath("$[0].changeDescription").value("Syllabus desactivado"))
                .andReturn().getResponse().getContentAsString();
        String version2 = JsonPath.read(versiones, "$[1].id");
        mvc.perform(como("coordinador", get("/api/syllabus/" + id + "/versions/" + version2)))
                .andExpect(jsonPath("$.snapshot.status").value("BORRADOR"))
                .andExpect(jsonPath("$.snapshot.sections[0].content").value("Segunda"));

        assertThatThrownBy(() -> jdbc.sql("UPDATE syllabus_versions SET change_description = 'x' WHERE id = ?::uuid")
                .param(version2).update())
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("inmutables");
    }

    @Test
    void unSyllabusActivoExigeContenidoYEvaluacionCompleta() throws Exception {
        mvc.perform(como("profesor", post("/api/syllabus").content("""
                        {"subjectId": "%s", "academicPeriod": "2027-03", "title": "Incompleto", "status": "ACTIVO",
                         "sections": [
                           {"sectionType": "justification", "title": "Justificación", "content": "Texto"},
                           {"sectionType": "evaluation", "title": "Evaluación", "evaluation": [
                             {"corte": 1, "label": "I", "porcentaje": 30,
                              "tipos": [{"tipo": "Quiz", "porcentaje": 50}]},
                             {"corte": 2, "label": "II", "porcentaje": 35,
                              "tipos": [{"tipo": "Parcial", "porcentaje": 100}]},
                             {"corte": 3, "label": "III", "porcentaje": 35,
                              "tipos": [{"tipo": "Proyecto", "porcentaje": 100}]}]}]}
                        """.formatted(ASIG_BASE_DE_DATOS))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        "I: los porcentajes parciales suman 50 % y deben sumar 100 %."));
    }

    @Test
    void renovarSinAjustesCopiaElSyllabusYLoDejaEnElHistorial() throws Exception {
        String renovado = mvc.perform(como("profesor", post("/api/syllabus/" + BASE_DE_DATOS + "/renew")
                        .content("{\"academicPeriod\": \"2028-01\"}")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.academicPeriod").value("2028-01"))
                .andExpect(jsonPath("$.title").value("Syllabus Base de Datos I - 2028-01"))
                .andExpect(jsonPath("$.status").value("ACTIVO"))
                .andExpect(jsonPath("$.currentVersion").value(1))
                .andReturn().getResponse().getContentAsString();
        mvc.perform(como("profesor", get("/api/syllabus/" + JsonPath.read(renovado, "$.id") + "/versions")))
                .andExpect(jsonPath("$[0].changeDescription").value("Renovado sin ajustes respecto al período 2026-01"));
        mvc.perform(como("profesor", post("/api/syllabus/" + BASE_DE_DATOS + "/renew")
                        .content("{\"academicPeriod\": \"2028-01\"}")))
                .andExpect(status().isConflict());
        mvc.perform(como("estudiante", post("/api/syllabus/" + BASE_DE_DATOS + "/renew")
                        .content("{\"academicPeriod\": \"2028-02\"}")))
                .andExpect(status().isForbidden());
    }
}
