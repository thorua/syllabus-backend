package co.edu.uam.syllabus.syllabus.versionado;

import co.edu.uam.syllabus.syllabus.Syllabus;
import co.edu.uam.syllabus.usuarios.Usuario;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * UC22 registrar versión: cada creación, edición con cambios o desactivación de un syllabus guarda
 * una copia inmutable (snapshot JSONB) con autor, fecha y descripción del cambio. La base de datos
 * impide modificarlas (trigger syllabus_versions_sin_cambios). También sirve el historial (UC14).
 */
@Service
public class VersionadoService {

    private final VersionSyllabusRepository versiones;

    public VersionadoService(VersionSyllabusRepository versiones) {
        this.versiones = versiones;
    }

    /** Número de la versión vigente; 0 si el syllabus aún no tiene versiones. */
    @Transactional(readOnly = true)
    public int actual(UUID syllabusId) {
        return versiones.findFirstBySyllabusIdOrderByVersionNumberDesc(syllabusId)
                .map(VersionSyllabus::getVersionNumber).orElse(0);
    }

    /**
     * Registra la siguiente versión con el estado actual del syllabus. Quien llama debe tener la fila
     * del syllabus bloqueada para que dos guardados simultáneos no obtengan el mismo número.
     */
    @Transactional
    public int registrar(Syllabus syllabus, String descripcion, Usuario autor) {
        int numero = actual(syllabus.getId()) + 1;
        versiones.save(new VersionSyllabus(syllabus, numero, descripcion, autor));
        return numero;
    }

    @Transactional(readOnly = true)
    public List<VersionResponse> historial(UUID syllabusId) {
        return versiones.findBySyllabusIdOrderByVersionNumberDesc(syllabusId).stream()
                .map(VersionResponse::resumen).toList();
    }

    @Transactional(readOnly = true)
    public VersionResponse obtener(UUID syllabusId, UUID versionId) {
        return versiones.findByIdAndSyllabusId(versionId, syllabusId).map(VersionResponse::detalle)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Versión no encontrada."));
    }
}
