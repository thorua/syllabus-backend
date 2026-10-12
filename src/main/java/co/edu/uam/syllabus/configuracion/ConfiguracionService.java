package co.edu.uam.syllabus.configuracion;

import co.edu.uam.syllabus.auditoria.AuditEvent;
import co.edu.uam.syllabus.common.AppProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** Configuración institucional (UC18–UC20), guardada como pares clave/valor en institutional_config. */
@Service
public class ConfiguracionService {

    public static final String PLANTILLA_EVALUACION = "evaluationTemplate";

    private static final String MAXIMO_CARACTERES = "maxSectionLength";
    private static final String LOGO_URL = "universityLogoUrl";
    private static final String LOGO_POR_DEFECTO = "images/uam_header_logo.png";
    private static final TypeReference<List<Evaluacion.Corte>> CORTES = new TypeReference<>() {};

    /** Logo institucional listo para servir o incrustar en el PDF. */
    public record Logo(byte[] datos, MediaType tipo) {}

    private final JdbcClient jdbc;
    private final ObjectMapper json;
    private final Path uploads;
    private final ApplicationEventPublisher eventos;

    public ConfiguracionService(JdbcClient jdbc, ObjectMapper json, AppProperties props,
                                ApplicationEventPublisher eventos) {
        this.jdbc = jdbc;
        this.json = json;
        this.uploads = props.uploadsDir();
        this.eventos = eventos;
    }

    @Transactional(readOnly = true)
    public Configuracion obtener() {
        return Configuracion.de(valores(), plantillaEvaluacion());
    }

    @Transactional(readOnly = true)
    public Map<String, String> valores() {
        Map<String, String> valores = new LinkedHashMap<>();
        jdbc.sql("SELECT key, value FROM institutional_config ORDER BY key")
                .query((rs, i) -> valores.put(rs.getString("key"), rs.getString("value")))
                .list();
        return valores;
    }

    @Transactional(readOnly = true)
    public String valor(String clave) {
        return jdbc.sql("SELECT value FROM institutional_config WHERE key = ?").param(clave)
                .query(String.class).optional().orElse("");
    }

    /** Plantilla de evaluación vigente (UC20). */
    @Transactional(readOnly = true)
    public List<Evaluacion.Corte> plantillaEvaluacion() {
        try {
            return json.readValue(valor(PLANTILLA_EVALUACION), CORTES);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("La plantilla de evaluación guardada no es válida.", e);
        }
    }

    /** Máximo de caracteres por sección del syllabus (2000 si no está configurado). */
    @Transactional(readOnly = true)
    public int maximoCaracteresPorSeccion() {
        String valor = valor(MAXIMO_CARACTERES);
        return valor.isBlank() ? 2000 : Integer.parseInt(valor);
    }

    /**
     * UC18–UC20: guarda la configuración. El logo no se cambia aquí sino con {@link #guardarLogo}.
     * Los syllabus existentes adoptan la nueva plantilla de evaluación la próxima vez que se editan.
     */
    @Transactional
    public Configuracion actualizar(UUID actor, Configuracion c) {
        List<Evaluacion.Corte> plantilla = c.evaluationTemplate().stream()
                .map(x -> new Evaluacion.Corte(x.corte(), texto(x.label()), x.porcentaje(), x.tipos() == null ? null
                        : x.tipos().stream().map(t -> new Evaluacion.Tipo(texto(t.tipo()), t.porcentaje())).toList()))
                .toList();
        Evaluacion.validarPlantilla(plantilla);
        Map<String, String> nuevos = new LinkedHashMap<>();
        nuevos.put("universityName", c.universityName());
        nuevos.put("universityAddress", c.universityAddress());
        nuevos.put("universityPhone", c.universityPhone());
        nuevos.put("universityEmail", c.universityEmail());
        nuevos.put("facultyName", c.facultyName());
        nuevos.put("departmentName", c.departmentName());
        nuevos.put("syllabusHeader", c.syllabusHeader());
        nuevos.put("documentCode", c.documentCode());
        nuevos.put("documentVersion", c.documentVersion());
        nuevos.put("documentDate", c.documentDate());
        nuevos.put("syllabusSubtitle", c.syllabusSubtitle());
        nuevos.put("footerText", c.footerText());
        nuevos.put(MAXIMO_CARACTERES, String.valueOf(c.maxSectionLength()));
        nuevos.put(PLANTILLA_EVALUACION, aJson(plantilla));

        Map<String, String> actuales = valores();
        List<String> cambiados = nuevos.entrySet().stream()
                .filter(e -> !Objects.equals(actuales.get(e.getKey()), texto(e.getValue())))
                .map(Map.Entry::getKey)
                .toList();
        cambiados.forEach(clave -> guardar(clave, texto(nuevos.get(clave))));
        if (!cambiados.isEmpty()) {
            eventos.publishEvent(AuditEvent.de(actor, "UPDATE", "config", null)
                    .con(Map.of("name", "Configuración institucional", "changed", cambiados)));
        }
        return obtener();
    }

    /** UC18: reemplaza el logo institucional (PNG o JPEG). */
    @Transactional
    public Configuracion guardarLogo(UUID actor, MultipartFile archivo) {
        byte[] datos;
        try {
            datos = archivo.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        String extension = extension(datos);
        try {
            Files.createDirectories(uploads);
            for (String otra : List.of("png", "jpg")) {
                Files.deleteIfExists(uploads.resolve("logo." + otra));
            }
            Files.write(uploads.resolve("logo." + extension), datos);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar el logo.", e);
        }
        // El parámetro v evita que el navegador muestre el logo anterior desde su caché.
        guardar(LOGO_URL, "/api/settings/logo?v=" + System.currentTimeMillis());
        eventos.publishEvent(AuditEvent.de(actor, "UPDATE", "config", null)
                .con(Map.of("name", "Logo institucional", "changed", List.of(LOGO_URL))));
        return obtener();
    }

    /** Logo subido o, si no hay, el logo institucional por defecto. */
    public Logo logo() {
        try {
            for (String extension : List.of("png", "jpg")) {
                Path archivo = uploads.resolve("logo." + extension);
                if (Files.isRegularFile(archivo)) {
                    return new Logo(Files.readAllBytes(archivo),
                            extension.equals("png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG);
                }
            }
            try (InputStream in = new ClassPathResource(LOGO_POR_DEFECTO).getInputStream()) {
                return new Logo(in.readAllBytes(), MediaType.IMAGE_PNG);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el logo.", e);
        }
    }

    private void guardar(String clave, String valor) {
        jdbc.sql("UPDATE institutional_config SET value = ?, updated_at = now() WHERE key = ?")
                .params(valor, clave).update();
    }

    /** Se reconoce el formato por su firma, no por el nombre ni el tipo declarado del archivo. */
    private static String extension(byte[] d) {
        if (d.length > 8 && (d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G') {
            return "png";
        }
        if (d.length > 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El logo debe ser una imagen PNG o JPEG.");
    }

    private String aJson(List<Evaluacion.Corte> plantilla) {
        try {
            return json.writeValueAsString(plantilla);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }
}
