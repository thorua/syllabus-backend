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

    private static final String REDES = ASIG_REDES_LAN.toString();

    @Test
    void losCodigosYNombresNoSeRepitenYNoSeDesactivaLoQueTieneDependencias() throws Exception {
        String facultad = facultad("FING");
        String departamento = "{\"name\": \"%s\", \"code\": \"%s\", \"facultyId\": \"%s\"}";
        mvc.perform(como("admin", post("/api/departments")
                        .content(departamento.formatted("departamento de ciencias computacionales", "NUEVO", facultad))))
                .andExpect(status().isConflict());
        mvc.perform(como("admin", post("/api/departments").content(departamento.formatted("Otro", "dcc", facultad))))
                .andExpect(status().isConflict());
        mvc.perform(como("admin", post("/api/departments").content("{\"name\": \"Sin facultad\", \"code\": \"SF\"}")))
                .andExpect(status().isBadRequest());
        mvc.perform(como("admin", delete("/api/departments/" + DEPTO_COMPUTACIONALES)))
                .andExpect(status().isConflict());
        mvc.perform(como("coordinador", post("/api/departments")
                        .content(departamento.formatted("X", "X", facultad))))
                .andExpect(status().isForbidden());
    }

    @Test
    void elPregradoExigeDepartamentoYElPosgradoPerteneceAUnaFacultad() throws Exception {
        mvc.perform(como("admin", post("/api/programs")
                        .content("{\"name\": \"Programa sin depto\", \"code\": \"PSD\", \"level\": \"PREGRADO\"}")))
                .andExpect(status().isBadRequest());
        mvc.perform(como("admin", post("/api/programs").content("""
                        {"name": "Especialización sin facultad", "code": "ESF", "level": "ESPECIALIZACION"}""")))
                .andExpect(status().isBadRequest());
        mvc.perform(como("admin", post("/api/programs").content("""
                        {"name": "Especialización de Prueba", "code": "EPR", "level": "ESPECIALIZACION",
                         "departmentId": "%s", "facultyId": "%s"}""".formatted(DEPTO_COMPUTACIONALES, facultad("FING")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.department").doesNotExist())
                .andExpect(jsonPath("$.faculty.code").value("FING"))
                .andExpect(jsonPath("$.coordinationEmail").doesNotExist());
    }

    @Test
    void elCorreoDeCoordinacionEsDelDepartamentoOElPosgradoYSeGuardaEnMinusculas() throws Exception {
        String posgrado = """
                {"name": "Maestría con Coordinación", "code": "%s", "level": "MAESTRIA", "facultyId": "%s",
                 "coordinationEmail": "%s"}""";
        mvc.perform(como("admin", post("/api/programs")
                        .content(posgrado.formatted("MCC1", facultad("FING"), "no-es-correo"))))
                .andExpect(status().isBadRequest());
        mvc.perform(como("admin", post("/api/programs")
                        .content(posgrado.formatted("MCC2", facultad("FING"), "Coordinacion.Prueba@Autonoma.edu.co"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.coordinationEmail").value("coordinacion.prueba@autonoma.edu.co"));
        // En pregrado el contacto es la coordinación del departamento; el programa no guarda correo.
        mvc.perform(como("admin", post("/api/programs").content("""
                        {"name": "Pregrado de Prueba", "code": "PPR", "level": "PREGRADO", "departmentId": "%s",
                         "coordinationEmail": "otro@autonoma.edu.co"}""".formatted(DEPTO_COMPUTACIONALES))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.coordinationEmail").doesNotExist())
                .andExpect(jsonPath("$.department.coordinationEmail")
                        .value("computacionales@autonoma.edu.co"));
    }

    @Test
    void soloSeAsignanProfesoresAfiliadosDentroDelAlcance() throws Exception {
        String asignar = "{\"professorId\": \"%s\"}";
        mvc.perform(como("coordinador", put("/api/subjects/" + REDES + "/assign-professor")
                        .content(asignar.formatted(ESTUDIANTE))))
                .andExpect(status().isBadRequest());
        // coordinador.programa no coordina Ing. de Sistemas, el programa de Redes LAN.
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
        mvc.perform(como("admin", delete("/api/subjects/" + ASIG_ING_SOFTWARE_II)))
                .andExpect(status().isConflict());
    }
}
