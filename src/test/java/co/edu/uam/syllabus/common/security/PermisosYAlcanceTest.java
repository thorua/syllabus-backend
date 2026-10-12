package co.edu.uam.syllabus.common.security;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.uam.syllabus.PruebaIntegracion;
import org.junit.jupiter.api.Test;

/**
 * PermisoEvaluator y ScopeService: el permiso efectivo decide QUÉ puede hacer cada rol y el
 * alcance (afiliaciones y asignaturas dictadas) decide SOBRE QUÉ registros.
 */
class PermisosYAlcanceTest extends PruebaIntegracion {

    private static final String ESTUDIANTE = "40000000-0000-0000-0000-000000000005";

    @Test
    void elEstudianteSoloVeSyllabusVigentesDeSuPrograma() throws Exception {
        // Ve los de la malla de Ing. de Sistemas, también los que ofrece Matemáticas; no los de posgrado.
        mvc.perform(como("estudiante", get("/api/syllabus?limit=100")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", not(empty())))
                .andExpect(jsonPath("$.data[*].status", everyItem(is("ACTIVO"))))
                .andExpect(jsonPath("$.data[*].subject.code", hasItem("MAT100")))
                .andExpect(jsonPath("$.data[*].subject.code", not(hasItem("MIA501"))));

        // Un borrador de su programa no existe para él (UC10: solo la versión vigente).
        mvc.perform(como("estudiante", get("/api/syllabus/" + ALGORITMOS_BORRADOR)))
                .andExpect(status().isNotFound());
        mvc.perform(como("estudiante", get("/api/syllabus/" + BASE_DE_DATOS + "/versions")))
                .andExpect(status().isForbidden());
    }

    @Test
    void elCoordinadorDeProgramaSoloGestionaSusProgramas() throws Exception {
        // coordinador.programa coordina Ing. Electrónica y posgrados, no Ing. de Sistemas.
        mvc.perform(como("coordinador.programa", get("/api/syllabus/" + APRENDIZAJE_AUTOMATICO + "/versions")))
                .andExpect(status().isOk());
        mvc.perform(como("coordinador.programa", get("/api/syllabus/" + BASE_DE_DATOS + "/versions")))
                .andExpect(status().isForbidden());
        // Matemáticas I está en la malla de Electrónica, pero la gestiona el Departamento de Matemáticas.
        mvc.perform(como("coordinador.programa", get("/api/syllabus/" + MATEMATICAS_I)))
                .andExpect(status().isOk());
        mvc.perform(como("coordinador.programa", get("/api/syllabus/" + MATEMATICAS_I + "/versions")))
                .andExpect(status().isForbidden());
        mvc.perform(como("coordinador.matematicas", get("/api/syllabus/" + MATEMATICAS_I + "/versions")))
                .andExpect(status().isOk());
        mvc.perform(como("coordinador.programa", post("/api/syllabus").content("""
                        {"subjectId": "%s", "academicPeriod": "2027-01", "title": "Fuera de alcance",
                         "sections": [{"sectionType": "justification", "title": "Justificación", "content": "x"}]}
                        """.formatted(ASIG_BASE_DE_DATOS))))
                .andExpect(status().isForbidden());
    }

    @Test
    void lasAsignaturasDePosgradoSoloLasGestionaAlcanceTotal() throws Exception {
        String asignatura = """
                {"name": "Asignatura de prueba", "code": "PRB%s", "credits": 3, "programId": "%s"}
                """;
        mvc.perform(como("coordinador", post("/api/subjects")
                        .content(asignatura.formatted("901", PROGRAMA_MAESTRIA_IA))))
                .andExpect(status().isForbidden());
        mvc.perform(como("admin", post("/api/subjects")
                        .content(asignatura.formatted("902", PROGRAMA_MAESTRIA_IA))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.department").doesNotExist())
                .andExpect(jsonPath("$.program.id").value(PROGRAMA_MAESTRIA_IA.toString()));
    }

    @Test
    void losPermisosIndividualesSeSumanYRestanSinAmpliarElAlcance() throws Exception {
        try {
            // Otorgar syllabus:create no le da asignaturas a cargo: el alcance sigue mandando.
            mvc.perform(como("admin", put("/api/users/" + ESTUDIANTE + "/permissions").content("""
                            {"permissionOverrides": [
                              {"action": "create", "resource": "syllabus", "allow": true},
                              {"action": "read", "resource": "syllabus", "allow": false}]}
                            """)))
                    .andExpect(status().isOk());
            mvc.perform(como("estudiante", get("/api/auth/me")))
                    .andExpect(jsonPath("$.permissions", hasItem("syllabus:create")))
                    .andExpect(jsonPath("$.permissions", not(hasItem("syllabus:read"))));
            mvc.perform(como("estudiante", get("/api/syllabus")))
                    .andExpect(status().isForbidden());
            mvc.perform(como("estudiante", post("/api/syllabus").content("""
                            {"subjectId": "%s", "academicPeriod": "2027-02", "title": "Sin alcance",
                             "sections": [{"sectionType": "justification", "title": "Justificación", "content": "x"}]}
                            """.formatted(ASIG_BASE_DE_DATOS))))
                    .andExpect(status().isForbidden());
        } finally {
            mvc.perform(como("admin", put("/api/users/" + ESTUDIANTE + "/permissions")
                    .content("{\"permissionOverrides\": []}")));
        }
    }
}
