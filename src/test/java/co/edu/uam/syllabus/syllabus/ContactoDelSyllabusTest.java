package co.edu.uam.syllabus.syllabus;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.uam.syllabus.PruebaIntegracion;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

/**
 * El syllabus vigente debe tener a quién contactar: el correo de la coordinación del programa o,
 * en programas sin coordinación (posgrados), el docente asignado a la asignatura. La identificación
 * se arma con los datos de la asignatura, por eso puede quedar sin texto en un syllabus activo.
 */
class ContactoDelSyllabusTest extends PruebaIntegracion {

    private static final String PROFESOR = "40000000-0000-0000-0000-000000000001";

    private static final String SYLLABUS = """
            {"subjectId": "%s", "academicPeriod": "2026-02", "title": "Seminario de Investigación - 2026-02",
             "status": "%s",
             "sections": [
               {"sectionType": "identification", "title": "Identificación", "content": ""},
               {"sectionType": "justification", "title": "Justificación", "content": "Texto"},
               {"sectionType": "evaluation", "title": "Evaluación", "evaluation": [
                 {"corte": 1, "label": "I", "porcentaje": 30, "tipos": [{"tipo": "Quiz", "porcentaje": 100}]},
                 {"corte": 2, "label": "II", "porcentaje": 35, "tipos": [{"tipo": "Taller", "porcentaje": 100}]},
                 {"corte": 3, "label": "III", "porcentaje": 35, "tipos": [{"tipo": "Proyecto", "porcentaje": 100}]}]}]}
            """;

    @Test
    void enUnPosgradoSinCoordinacionHayQueAsignarDocenteAntesDeActivar() throws Exception {
        String asignatura = mvc.perform(como("admin", post("/api/subjects").content("""
                        {"name": "Seminario de Investigación", "code": "MIA901", "credits": 2, "semester": 2,
                         "programId": "%s"}""".formatted(PROGRAMA_MAESTRIA_IA))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(asignatura, "$.id");

        mvc.perform(como("admin", post("/api/syllabus").content(SYLLABUS.formatted(id, "ACTIVO"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El programa Maestría en Inteligencia Artificial no tiene correo "
                        + "de coordinación: asigne un docente a la asignatura antes de activar el syllabus."));

        mvc.perform(como("admin", put("/api/subjects/" + id + "/assign-professor")
                        .content("{\"professorId\": \"" + PROFESOR + "\"}")))
                .andExpect(status().isOk());
        mvc.perform(como("admin", post("/api/syllabus").content(SYLLABUS.formatted(id, "ACTIVO"))))
                .andExpect(status().isCreated());
    }
}
