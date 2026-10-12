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
 * alcance (afiliaciones, planes de estudio y asignaturas dictadas) decide SOBRE QUÉ registros.
 */
class PermisosYAlcanceTest extends PruebaIntegracion {

    @Test
    void elEstudianteSoloVeSyllabusVigentesDeSuPrograma() throws Exception {
        // Ve los de la malla de Ing. de Sistemas, también los que ofrece Física y Matemáticas; no los del posgrado.
        mvc.perform(como("estudiante", get("/api/syllabus?limit=100")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", not(empty())))
                .andExpect(jsonPath("$.data[*].status", everyItem(is("ACTIVO"))))
                .andExpect(jsonPath("$.data[*].subject.name", hasItem("Matemáticas Básicas")))
                .andExpect(jsonPath("$.data[*].subject.name", not(hasItem("Gestión de Proyectos de Software"))));

        // Un borrador de su programa no existe para ella (UC10: solo la versión vigente).
        mvc.perform(como("estudiante", get("/api/syllabus/" + BASES_DE_DATOS_BORRADOR)))
                .andExpect(status().isNotFound());
        mvc.perform(como("estudiante", get("/api/syllabus/" + ING_SOFTWARE_II + "/versions")))
                .andExpect(status().isForbidden());
    }

    @Test
    void elCoordinadorDeProgramaSoloGestionaSusProgramas() throws Exception {
        // coordinador.programa coordina Ing. Electrónica y la Maestría en Software, no Ing. de Sistemas.
        mvc.perform(como("coordinador.programa", get("/api/syllabus/" + GESTION_PROYECTOS_MGDS + "/versions")))
                .andExpect(status().isOk());
        mvc.perform(como("coordinador.programa", get("/api/syllabus/" + ING_SOFTWARE_II + "/versions")))
                .andExpect(status().isForbidden());
        // Matemáticas Básicas está en la malla de Electrónica, pero la gestiona Física y Matemáticas.
        mvc.perform(como("coordinador.programa", get("/api/syllabus/" + MATEMATICAS_BASICAS)))
                .andExpect(status().isOk());
        mvc.perform(como("coordinador.programa", get("/api/syllabus/" + MATEMATICAS_BASICAS + "/versions")))
                .andExpect(status().isForbidden());
        mvc.perform(como("coordinador.matematicas", get("/api/syllabus/" + MATEMATICAS_BASICAS + "/versions")))
                .andExpect(status().isOk());
        mvc.perform(como("coordinador.programa", post("/api/syllabus").content("""
                        {"subjectId": "%s", "academicPeriod": "2027-01", "title": "Fuera de alcance",
                         "sections": [{"sectionType": "justification", "title": "Justificación", "content": "x"}]}
                        """.formatted(ASIG_ING_SOFTWARE_II))))
                .andExpect(status().isForbidden());
    }

    @Test
    void lasAsignaturasDeUnPosgradoLasGestionaSuCoordinacion() throws Exception {
        String asignatura = """
                {"name": "Asignatura de prueba", "code": "PRB%s", "credits": 3, "programId": "%s"}
                """;
        // Un coordinador de departamento no gestiona asignaturas de posgrado.
        mvc.perform(como("coordinador", post("/api/subjects")
                        .content(asignatura.formatted("901", PROGRAMA_MAESTRIA_SOFTWARE))))
                .andExpect(status().isForbidden());
        // La coordinadora de la maestría sí, y solo las de su programa.
        mvc.perform(como("coordinador.programa", post("/api/subjects")
                        .content(asignatura.formatted("902", PROGRAMA_MAESTRIA_SOFTWARE))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.department").doesNotExist())
                .andExpect(jsonPath("$.program.id").value(PROGRAMA_MAESTRIA_SOFTWARE.toString()));
        mvc.perform(como("coordinador.programa", post("/api/subjects")
                        .content(asignatura.formatted("903", PROGRAMA_MAESTRIA_INGENIERIA))))
                .andExpect(status().isForbidden());
        // Un coordinador de programa de pregrado no crea asignaturas: las ofrecen los departamentos.
        mvc.perform(como("coordinador.programa", post("/api/subjects").content("""
                        {"name": "Asignatura de prueba", "code": "PRB904", "credits": 3, "departmentId": "%s"}
                        """.formatted(DEPTO_COMPUTACIONALES))))
                .andExpect(status().isForbidden());
        mvc.perform(como("admin", post("/api/subjects")
                        .content(asignatura.formatted("905", PROGRAMA_MAESTRIA_INGENIERIA))))
                .andExpect(status().isCreated());
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
                            """.formatted(ASIG_ING_SOFTWARE_II))))
                    .andExpect(status().isForbidden());
        } finally {
            mvc.perform(como("admin", put("/api/users/" + ESTUDIANTE + "/permissions")
                    .content("{\"permissionOverrides\": []}")));
        }
    }
}
