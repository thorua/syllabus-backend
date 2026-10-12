package co.edu.uam.syllabus;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base de las pruebas de integración: la aplicación completa contra un PostgreSQL 16 real
 * (Testcontainers) con las migraciones y los datos demo. Un solo contenedor para todas las clases.
 * Las pruebas que escriben datos usan registros propios para no depender del orden de ejecución, y
 * las que cambian datos compartidos los restauran. El correo se simula para leer lo que se enviaría.
 */
@SpringBootTest(properties = {
    "spring.flyway.locations=classpath:db/migration,classpath:db/demo",
    "app.jwt.secret=secreto-de-pruebas-con-mas-de-treinta-y-dos-caracteres",
    "app.uploads-dir=target/uploads-pruebas"
})
@AutoConfigureMockMvc
public abstract class PruebaIntegracion {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    // Datos demo (db/demo/V3__datos_demo.sql y V11__demo_estructura_academica.sql)
    protected static final UUID BASE_DE_DATOS = UUID.fromString("50000000-0000-0000-0000-000000000001");
    protected static final UUID ING_SOFTWARE = UUID.fromString("50000000-0000-0000-0000-000000000002");
    protected static final UUID ALGORITMOS_BORRADOR = UUID.fromString("50000000-0000-0000-0000-000000000004");
    protected static final UUID APRENDIZAJE_AUTOMATICO = UUID.fromString("50000000-0000-0000-0000-000000000005");
    protected static final UUID MATEMATICAS_I = UUID.fromString("50000000-0000-0000-0000-000000000006");
    protected static final UUID ASIG_BASE_DE_DATOS = UUID.fromString("30000000-0000-0000-0000-000000000001");
    protected static final UUID ASIG_MATEMATICAS_I = UUID.fromString("30000000-0000-0000-0000-000000000009");
    protected static final UUID DEPTO_COMPUTACIONALES = UUID.fromString("10000000-0000-0000-0000-000000000001");
    protected static final UUID DEPTO_MATEMATICAS = UUID.fromString("10000000-0000-0000-0000-000000000002");
    protected static final UUID PROGRAMA_SISTEMAS = UUID.fromString("20000000-0000-0000-0000-000000000001");
    protected static final UUID PROGRAMA_ELECTRONICA = UUID.fromString("20000000-0000-0000-0000-000000000002");
    protected static final UUID PROGRAMA_MAESTRIA_IA = UUID.fromString("20000000-0000-0000-0000-000000000004");
    protected static final UUID PLAN_SISTEMAS_2026 = UUID.fromString("60000000-0000-0000-0000-000000000003");

    @Autowired
    protected MockMvc mvc;

    @MockitoBean
    protected JavaMailSender correo;

    /** Inicia sesión como un usuario demo (p. ej. "profesor") y devuelve su JWT. */
    protected String token(String usuario) throws Exception {
        String respuesta = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s@autonoma.edu.co\",\"password\":\"Uam2026!\"}".formatted(usuario)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.token");
    }

    /** Agrega el token de un usuario demo a la petición. */
    protected MockHttpServletRequestBuilder como(String usuario, MockHttpServletRequestBuilder peticion)
            throws Exception {
        return peticion.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(usuario))
                .contentType(MediaType.APPLICATION_JSON);
    }

    /** Id de una facultad por su código (las facultades las crea V9__facultades_y_planes_de_estudio.sql). */
    protected String facultad(String codigo) throws Exception {
        String json = mvc.perform(como("admin", get("/api/faculties"))).andReturn().getResponse().getContentAsString();
        List<String> ids = JsonPath.read(json, "$[?(@.code == '" + codigo + "')].id");
        return ids.getFirst();
    }
}
