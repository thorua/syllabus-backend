package co.edu.uam.syllabus.usuarios;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.uam.syllabus.PruebaIntegracion;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** UC3 gestionar usuarios, UC4 permisos individuales y UC9 afiliaciones. */
class UsuariosTest extends PruebaIntegracion {

    private static final String ADMIN = "40000000-0000-0000-0000-000000000003";

    private static String usuario(String email, String password) {
        return """
                {"name": "Usuario de Prueba", "email": "%s", "password": %s, "roles": ["PROFESOR"]}
                """.formatted(email, password == null ? "null" : "\"" + password + "\"");
    }

    @Test
    void registroCreaEditaYDesactivaUsuarios() throws Exception {
        String creado = mvc.perform(como("registro", post("/api/users")
                        .content(usuario("Nuevo.Docente@Autonoma.edu.co", "ClaveSegura2026"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("nuevo.docente@autonoma.edu.co"))
                .andExpect(jsonPath("$.roles", hasItem("PROFESOR")))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(creado, "$.id");

        String repetido = usuario("nuevo.docente@autonoma.edu.co", "OtraClave2026");
        mvc.perform(como("admin", post("/api/users").content(repetido)))
                .andExpect(status().isConflict());
        mvc.perform(como("admin", post("/api/users").content(usuario("sin.clave@autonoma.edu.co", null))))
                .andExpect(status().isBadRequest());

        // UC9: afiliar al profesor a un programa.
        mvc.perform(como("admin", put("/api/users/" + id + "/affiliations").content("""
                        {"departmentIds": [], "programIds": ["%s"]}""".formatted(PROGRAMA_SISTEMAS))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.programs[*].id", containsInAnyOrder(PROGRAMA_SISTEMAS.toString())));

        mvc.perform(como("admin", delete("/api/users/" + id))).andExpect(status().isOk());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nuevo.docente@autonoma.edu.co\",\"password\":\"ClaveSegura2026\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nadiePuedeDesactivarSuPropiaCuenta() throws Exception {
        mvc.perform(como("admin", delete("/api/users/" + ADMIN)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("No puede desactivar su propia cuenta."));
    }

    @Test
    void elCoordinadorDeProgramaSoloVeLosProfesoresQuePuedeAsignar() throws Exception {
        mvc.perform(como("coordinador.programa", get("/api/users?limit=100")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].roles", everyItem(hasItem("PROFESOR"))));
        mvc.perform(como("coordinador.programa", post("/api/users")
                        .content(usuario("intruso@autonoma.edu.co", "ClaveSegura2026"))))
                .andExpect(status().isForbidden());
        mvc.perform(como("estudiante", get("/api/users"))).andExpect(status().isForbidden());
    }

    @Test
    void elCatalogoDePermisosDescribeCadaPermiso() throws Exception {
        mvc.perform(como("admin", get("/api/users/permission-catalog")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['syllabus:header']").isString())
                .andExpect(jsonPath("$['export:batch']").isString());
        mvc.perform(como("admin", put("/api/users/" + ADMIN + "/permissions").content("""
                        {"permissionOverrides": [{"action": "volar", "resource": "syllabus", "allow": true}]}""")))
                .andExpect(status().isBadRequest());
    }
}
