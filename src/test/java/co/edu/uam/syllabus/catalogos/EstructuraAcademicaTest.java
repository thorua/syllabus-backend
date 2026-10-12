package co.edu.uam.syllabus.catalogos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
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
 * que la ofrece y entra en la malla de varios programas (p. ej. Física y Matemáticas, que no tiene
 * programa), con el área de formación y el orden del plan publicado.
 */
class EstructuraAcademicaTest extends PruebaIntegracion {

    private static final String FISICA_II = "30000000-0000-0000-0000-000000000064";

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
                .andExpect(jsonPath("$[*].code", hasItem("DFM")))
                .andExpect(jsonPath("$[*].code", not(hasItem("DHU"))));
    }

    @Test
    void matematicasBasicasEstaEnLaMallaDeTodasLasIngenierias() throws Exception {
        mvc.perform(como("admin", get("/api/subjects/" + ASIG_MATEMATICAS_BASICAS)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.department.code").value("DFM"))
                .andExpect(jsonPath("$.program").doesNotExist())
                .andExpect(jsonPath("$.programs[*].code", containsInAnyOrder("IS", "IE", "IB", "IM", "II")))
                .andExpect(jsonPath("$.programs[*].area", everyItem(is("BASICA"))));
        // Física y Matemáticas no tiene programas propios, pero sus asignaturas aparecen al filtrar por programa.
        mvc.perform(como("admin", get("/api/programs?departmentId=" + DEPTO_FISICA_MATEMATICAS)))
                .andExpect(jsonPath("$").isEmpty());
        mvc.perform(como("admin", get("/api/subjects?programId=" + PROGRAMA_ELECTRONICA)))
                .andExpect(jsonPath("$[*].code", hasItem("DFM001")))
                .andExpect(jsonPath("$[*].code", not(hasItem("DCC014"))));
    }

    @Test
    void laMallaSigueElPlanPublicadoYMuestraElEstadoDeCadaSyllabus() throws Exception {
        mvc.perform(como("estudiante", get("/api/study-plans?programId=" + PROGRAMA_SISTEMAS)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", contains("Plan 2025")));
        String malla = mvc.perform(como("estudiante",
                        get("/api/study-plans/" + PLAN_SISTEMAS + "/curriculum?academicPeriod=2026-01")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.semesters[*].number", contains(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)))
                // Primer semestre en el orden del plan publicado: área básica primero.
                .andExpect(jsonPath("$.semesters[0].subjects[0].code").value("DFM001"))
                .andExpect(jsonPath("$.semesters[0].subjects[0].area").value("BASICA"))
                .andExpect(jsonPath("$.semesters[0].credits").value(16))
                .andReturn().getResponse().getContentAsString();
        assertEstado(malla, "DFM001", "PENDIENTE_FIRMA");
        assertEstado(malla, "DCC008", "BORRADOR");
        assertEstado(malla, "DCC001", "SIN_SYLLABUS");
        // Los cursos libres no tienen syllabus propio: no cuentan como faltantes.
        assertEstado(malla, "DHU005", "ESPACIO");
        mvc.perform(como("estudiante",
                        get("/api/study-plans/" + PLAN_SISTEMAS + "/curriculum?academicPeriod=2026")))
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
        String basica = "{\"semester\": %d, \"area\": \"BASICA\"}";

        mvc.perform(como("admin", put(ubicar.formatted(FISICA_II)).content(basica.formatted(2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.semesters[0].number").value(2));
        mvc.perform(como("admin", put(ubicar.formatted(FISICA_II)).content(basica.formatted(3))))
                .andExpect(jsonPath("$.semesters[*].number", contains(3)));
        mvc.perform(como("admin", put(ubicar.formatted(FISICA_II)).content("{\"semester\": 3}")))
                .andExpect(status().isBadRequest());
        mvc.perform(como("admin", put(ubicar.formatted(ASIG_IA_MGDS)).content(basica.formatted(1))))
                .andExpect(status().isBadRequest());
        mvc.perform(como("coordinador", put(ubicar.formatted(FISICA_II)).content(basica.formatted(1))))
                .andExpect(status().isForbidden());
        mvc.perform(como("admin", delete(ubicar.formatted(FISICA_II))))
                .andExpect(jsonPath("$.semesters").isEmpty());
        mvc.perform(como("admin", post("/api/study-plans")
                        .content("{\"programId\": \"%s\", \"name\": \"plan de PRUEBA\"}".formatted(PROGRAMA_ELECTRONICA))))
                .andExpect(status().isConflict());
        mvc.perform(como("admin", get("/api/study-plans?programId=" + PROGRAMA_ELECTRONICA)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Plan 2025", "Plan de prueba")));
    }

    private static void assertEstado(String malla, String codigo, String estado) {
        List<String> estados = JsonPath.read(malla, "$.semesters[*].subjects[?(@.code == '" + codigo + "')].state");
        assertThat(estados).as(codigo).containsExactly(estado);
    }
}
