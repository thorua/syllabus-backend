package co.edu.uam.syllabus.configuracion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Configuración institucional (UC18–UC20), guardada como pares clave/valor en institutional_config. */
@Service
public class ConfiguracionService {

    public static final String PLANTILLA_EVALUACION = "evaluationTemplate";

    private static final TypeReference<List<Evaluacion.Corte>> CORTES = new TypeReference<>() {};

    private final JdbcClient jdbc;
    private final ObjectMapper json;

    public ConfiguracionService(JdbcClient jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
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
}
