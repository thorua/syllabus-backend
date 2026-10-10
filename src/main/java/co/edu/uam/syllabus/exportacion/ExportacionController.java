package co.edu.uam.syllabus.exportacion;

import co.edu.uam.syllabus.common.security.UsuarioAutenticado;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exportación en PDF formato Almera: individual (UC15), por lote (UC16) y plantilla en blanco. */
@RestController
@RequestMapping("/api/export")
public class ExportacionController {

    private static final MediaType ZIP = MediaType.parseMediaType("application/zip");

    private final ExportacionService service;

    public ExportacionController(ExportacionService service) {
        this.service = service;
    }

    /** Exportar es una lectura: se puede exportar cualquier syllabus que el usuario puede consultar. */
    @GetMapping("/syllabus/{id}")
    @PreAuthorize("hasAuthority('syllabus:export') and @permisos.puede(authentication, 'syllabus:read', #id)")
    public ResponseEntity<byte[]> individual(@AuthenticationPrincipal UsuarioAutenticado actor, @PathVariable UUID id) {
        return descarga(service.individual(actor, id), MediaType.APPLICATION_PDF);
    }

    @PostMapping("/batch")
    @PreAuthorize("hasAuthority('export:batch')")
    public ResponseEntity<byte[]> lote(@AuthenticationPrincipal UsuarioAutenticado actor,
                                       @RequestBody ExportacionService.LoteRequest request) {
        return descarga(service.lote(actor, request), ZIP);
    }

    @GetMapping("/template")
    @PreAuthorize("hasAuthority('syllabus:export')")
    public ResponseEntity<byte[]> plantilla() {
        return descarga(service.plantilla(), MediaType.APPLICATION_PDF);
    }

    private static ResponseEntity<byte[]> descarga(ExportacionService.Archivo archivo, MediaType tipo) {
        return ResponseEntity.ok()
                .contentType(tipo)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(archivo.nombre()).build().toString())
                .body(archivo.contenido());
    }
}
