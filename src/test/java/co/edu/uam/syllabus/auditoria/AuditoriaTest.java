package co.edu.uam.syllabus.auditoria;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.uam.syllabus.PruebaIntegracion;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** UC21: registro de cambios con autor y fecha, consultable solo por Registro y Administración. */
class AuditoriaTest extends PruebaIntegracion {

    @Test
    void registraLosCambiosYLosIntentosFallidos() throws Exception {
        mvc.perform(como("admin", post("/api/departments")
                        .content("{\"name\": \"Departamento Auditado\", \"code\": \"AUD\"}")))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"profesor@autonoma.edu.co\",\"password\":\"mala\"}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(como("registro", get("/api/audit?entity=department&action=CREATE&limit=50")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].entity", everyItem(is("department"))))
                .andExpect(jsonPath("$.data[*].details.code", hasItem("AUD")))
                .andExpect(jsonPath("$.data[*].user.email", hasItem("admin@autonoma.edu.co")));
        mvc.perform(como("registro", get("/api/audit?action=LOGIN_FAILED&limit=50")))
                .andExpect(jsonPath("$.data[*].details.email", hasItem("profesor@autonoma.edu.co")));
        mvc.perform(como("admin", get("/api/audit/options")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entities", hasItem("department")));
    }

    @Test
    void soloRegistroYAdministracionConsultanLaAuditoria() throws Exception {
        mvc.perform(como("coordinador", get("/api/audit"))).andExpect(status().isForbidden());
        mvc.perform(como("profesor", get("/api/audit/options"))).andExpect(status().isForbidden());
    }
}
