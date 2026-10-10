package co.edu.uam.syllabus.catalogos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.uam.syllabus.PruebaIntegracion;
import org.junit.jupiter.api.Test;

/** UC5 departamentos, UC6 programas, UC7 asignaturas y UC8 asignar profesor. */
class CatalogosTest extends PruebaIntegracion {

    private static final String DEPTO_SISTEMAS = "10000000-0000-0000-0000-000000000001";
    private static final String REDES = "30000000-0000-0000-0000-000000000003";
    private static final String PROFESOR = "40000000-0000-0000-0000-000000000001";
    private static final String ESTUDIANTE = "40000000-0000-0000-0000-000000000005";

    @Test
    void losCodigosYNombresNoSeRepitenYNoSeDesactivaLoQueTieneDependencias() throws Exception {
        mvc.perform(como("admin", post("/api/departments")
                        .content("{\"name\": \"departamento de ingeniería de sistemas\", \"code\": \"NUEVO\"}")))
                .andExpect(status().isConflict());
        mvc.perform(como("admin", post("/api/departments").content("{\"name\": \"Otro\", \"code\": \"dis\"}")))
                .andExpect(status().isConflict());
        mvc.perform(como("admin", delete("/api/departments/" + DEPTO_SISTEMAS)))
                .andExpect(status().isConflict());
        mvc.perform(como("coordinador", post("/api/departments").content("{\"name\": \"X\", \"code\": \"X\"}")))
                .andExpect(status().isForbidden());
    }

    @Test
    void elPregradoExigeDepartamentoYElPosgradoNoTiene() throws Exception {
        mvc.perform(como("admin", post("/api/programs")
                        .content("{\"name\": \"Programa sin depto\", \"code\": \"PSD\", \"level\": \"PREGRADO\"}")))
                .andExpect(status().isBadRequest());
        mvc.perform(como("admin", post("/api/programs").content("""
                        {"name": "Especialización de Prueba", "code": "EPR", "level": "ESPECIALIZACION",
                         "departmentId": "%s"}""".formatted(DEPTO_SISTEMAS))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.department").doesNotExist())
                .andExpect(jsonPath("$.coordinationEmail").doesNotExist());
    }

    @Test
    void elCorreoDeCoordinacionSeValidaYSeGuardaEnMinusculas() throws Exception {
        String programa = """
                {"name": "Pregrado con Coordinación", "code": "%s", "level": "PREGRADO", "departmentId": "%s",
                 "coordinationEmail": "%s"}""";
        mvc.perform(como("admin", post("/api/programs")
                        .content(programa.formatted("PCC1", DEPTO_SISTEMAS, "no-es-correo"))))
                .andExpect(status().isBadRequest());
        mvc.perform(como("admin", post("/api/programs")
                        .content(programa.formatted("PCC2", DEPTO_SISTEMAS, "Coordinacion.Prueba@Autonoma.edu.co"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.coordinationEmail").value("coordinacion.prueba@autonoma.edu.co"));
    }

    @Test
    void soloSeAsignanProfesoresAfiliadosDentroDelAlcance() throws Exception {
        String asignar = "{\"professorId\": \"%s\"}";
        mvc.perform(como("coordinador", put("/api/subjects/" + REDES + "/assign-professor")
                        .content(asignar.formatted(ESTUDIANTE))))
                .andExpect(status().isBadRequest());
        // coordinador.programa no coordina Ing. de Sistemas, el programa de Redes.
        mvc.perform(como("coordinador.programa", put("/api/subjects/" + REDES + "/assign-professor")
                        .content(asignar.formatted(PROFESOR))))
                .andExpect(status().isForbidden());
        try {
            mvc.perform(como("coordinador", put("/api/subjects/" + REDES + "/assign-professor")
                            .content(asignar.formatted(PROFESOR))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.professor.id").value(PROFESOR));
        } finally {
            mvc.perform(como("coordinador", put("/api/subjects/" + REDES + "/assign-professor")
                    .content("{\"professorId\": null}")));
        }
    }

    @Test
    void noSeDesactivaUnaAsignaturaConSyllabusVigente() throws Exception {
        mvc.perform(como("admin", delete("/api/subjects/" + ASIG_BASE_DE_DATOS)))
                .andExpect(status().isConflict());
    }
}
