package co.edu.uam.syllabus.catalogos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.uam.syllabus.PruebaIntegracion;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Facultad → departamento → programa, y planes de estudio: una asignatura pertenece al departamento
 * que la ofrece y entra en la malla de varios programas (p. ej. Matemáticas, que no tiene programa).
 */
class EstructuraAcademicaTest extends PruebaIntegracion {

    private static final String MIA501 = "30000000-0000-0000-0000-000000000007";
    private static final String FISICA = "30000000-0000-0000-0000-000000000015";

    @Test
    void lasFacultadesAgrupanDepartamentosYNoSeDesactivanConDependencias() throws Exception {
        mvc.perform(como("estudiante", get("/api/faculties")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code", hasItem("FING")));
        String creada = mvc.perform(como("admin", post("/api/faculties")
                        .content("{\"name\": \"Facultad de Prueba\", \"code\": \"fpr\"}")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("FPR"))
                .andReturn().getResponse().getContentAsString();
        mvc.perform(como("admin", delete("/api/faculties/" + JsonPath.read(creada, "$.id"))))
                .andExpect(status().isOk());
        mvc.perform(como("admin", delete("/api/faculties/" + facultad("FING"))))
                .andExpect(status().isConflict());
        mvc.perform(como("coordinador", post("/api/faculties").content("{\"name\": \"X\", \"code\": \"X\"}")))
                .andExpect(status().isForbidden());
        mvc.perform(como("admin", get("/api/departments?facultyId=" + facultad("FING"))))
                .andExpect(jsonPath("$[*].code", hasItem("DMAT")))
                .andExpect(jsonPath("$[*].code", not(hasItem("DEDU"))));
    }

    @Test
    void unaAsignaturaDeMatematicasEstaEnLaMallaDeVariosProgramas() throws Exception {
        mvc.perform(como("admin", get("/api/subjects/" + ASIG_MATEMATICAS_I)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.department.code").value("DMAT"))
                .andExpect(jsonPath("$.program").doesNotExist())
                .andExpect(jsonPath("$.programs[*].code", hasItem("IS")))
                .andExpect(jsonPath("$.programs[*].code", hasItem("IEL")))
                .andExpect(jsonPath("$.programs[*].code", hasItem("IBM")));
        // Matemáticas no tiene programas propios, pero sus asignaturas aparecen al filtrar por programa.
        mvc.perform(como("admin", get("/api/programs?departmentId=" + DEPTO_MATEMATICAS)))
                .andExpect(jsonPath("$").isEmpty());
        mvc.perform(como("admin", get("/api/subjects?programId=" + PROGRAMA_ELECTRONICA)))
                .andExpect(jsonPath("$[*].code", hasItem("MAT100")))
                .andExpect(jsonPath("$[*].code", not(hasItem("SIS201"))));
    }

    @Test
    void laMallaMuestraElEstadoDelSyllabusDeCadaAsignaturaEnElPeriodo() throws Exception {
        mvc.perform(como("estudiante", get("/api/study-plans?programId=" + PROGRAMA_SISTEMAS)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", contains("Plan 2020", "Plan 2026")));
        String malla = mvc.perform(como("estudiante",
                        get("/api/study-plans/" + PLAN_SISTEMAS_2026 + "/curriculum?academicPeriod=2026-01")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.semesters[*].number", contains(1, 2, 3, 4, 6)))
                .andReturn().getResponse().getContentAsString();
        assertEstado(malla, "MAT100", "PENDIENTE_FIRMA");
        assertEstado(malla, "EDU100", "BORRADOR");
        assertEstado(malla, "ING100", "SIN_SYLLABUS");
        assertEstado(malla, "SIS201", "PENDIENTE_FIRMA");
        mvc.perform(como("estudiante",
                        get("/api/study-plans/" + PLAN_SISTEMAS_2026 + "/curriculum?academicPeriod=2026")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void soloLaAdministracionArmaLosPlanesYUnPosgradoNoPrestaSusAsignaturas() throws Exception {
        String plan = mvc.perform(como("admin", post("/api/study-plans")
                        .content("{\"programId\": \"%s\", \"name\": \"Plan de prueba\"}".formatted(PROGRAMA_ELECTRONICA))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(plan, "$.id");
        String ubicar = "/api/study-plans/" + id + "/subjects/%s?academicPeriod=2026-01";

        mvc.perform(como("admin", put(ubicar.formatted(FISICA)).content("{\"semester\": 2}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.semesters[0].number").value(2));
        mvc.perform(como("admin", put(ubicar.formatted(FISICA)).content("{\"semester\": 3}")))
                .andExpect(jsonPath("$.semesters[*].number", contains(3)));
        mvc.perform(como("admin", put(ubicar.formatted(MIA501)).content("{\"semester\": 1}")))
                .andExpect(status().isBadRequest());
        mvc.perform(como("coordinador", put(ubicar.formatted(FISICA)).content("{\"semester\": 1}")))
                .andExpect(status().isForbidden());
        mvc.perform(como("admin", delete(ubicar.formatted(FISICA))))
                .andExpect(jsonPath("$.semesters").isEmpty());
        mvc.perform(como("admin", post("/api/study-plans")
                        .content("{\"programId\": \"%s\", \"name\": \"plan de PRUEBA\"}".formatted(PROGRAMA_ELECTRONICA))))
                .andExpect(status().isConflict());
        mvc.perform(como("admin", get("/api/study-plans?programId=" + PROGRAMA_ELECTRONICA)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Plan 2023", "Plan de prueba")));
    }

    private static void assertEstado(String malla, String codigo, String estado) {
        List<String> estados = JsonPath.read(malla, "$.semesters[*].subjects[?(@.code == '" + codigo + "')].state");
        assertThat(estados).as(codigo).containsExactly(estado);
    }
}
