package co.edu.uam.syllabus.acceso;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.uam.syllabus.PruebaIntegracion;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;

/** UC1 iniciar sesión y UC2 recuperar contraseña. */
class AccesoTest extends PruebaIntegracion {

    private static String credenciales(String usuario, String password) {
        return "{\"email\":\"%s@autonoma.edu.co\",\"password\":\"%s\"}".formatted(usuario, password);
    }

    @Test
    void credencialesInvalidasRespondenUnMensajeGenerico() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales("profesor", "incorrecta")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Credenciales inválidas."));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales("no.existe", "Uam2026!")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Credenciales inválidas."));
    }

    @Test
    void sinTokenOConTokenInvalidoNoHayAcceso() throws Exception {
        mvc.perform(get("/api/syllabus")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/syllabus").header(HttpHeaders.AUTHORIZATION, "Bearer token-falso"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meDevuelveRolesYPermisosEfectivos() throws Exception {
        mvc.perform(como("registro", get("/api/auth/me")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", hasItem("ADM_REGISTRO")))
                .andExpect(jsonPath("$.permissions", hasItem("config:update")))
                .andExpect(jsonPath("$.permissions", hasItem("audit:read")));
        mvc.perform(como("estudiante", get("/api/auth/me")))
                .andExpect(jsonPath("$.permissions", hasItem("syllabus:read")))
                .andExpect(jsonPath("$.permissions", not(hasItem("syllabus:create"))));
    }

    @Test
    void cambiarContrasenaExigeLaActual() throws Exception {
        mvc.perform(como("coordinador.programa", post("/api/auth/change-password")
                        .content("{\"currentPassword\":\"incorrecta\",\"newPassword\":\"OtraClave2026\"}")))
                .andExpect(status().isBadRequest());
        mvc.perform(como("coordinador.programa", post("/api/auth/change-password")
                        .content("{\"currentPassword\":\"Uam2026!\",\"newPassword\":\"corta\"}")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recuperarContrasenaConUnEnlaceDeUnSoloUso() throws Exception {
        clearInvocations(correo);
        // Un correo desconocido responde igual, sin revelar si la cuenta existe, y no envía nada.
        mvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"no.existe@autonoma.edu.co\"}"))
                .andExpect(status().isOk());
        verify(correo, never()).send(any(SimpleMailMessage.class));

        mvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"registro@autonoma.edu.co\"}"))
                .andExpect(status().isOk());
        ArgumentCaptor<SimpleMailMessage> enviado = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(correo).send(enviado.capture());
        assertThat(enviado.getValue().getTo()).containsExactly("registro@autonoma.edu.co");
        String enlace = Objects.requireNonNull(enviado.getValue().getText()).lines()
                .filter(l -> l.contains("/reset-password?token=")).findFirst().orElseThrow().strip();
        String token = enlace.substring(enlace.indexOf("token=") + 6);

        String restablecer = "{\"token\":\"" + token + "\",\"newPassword\":\"%s\"}";
        mvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content(restablecer.formatted("Uam2026!")))
                .andExpect(status().isOk());
        mvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content(restablecer.formatted("OtraClave2026")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales("registro", "Uam2026!")))
                .andExpect(status().isOk());
    }
}
