package co.edu.uam.syllabus.syllabus.firma;

import co.edu.uam.syllabus.common.security.UsuarioAutenticado;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Aprobación del syllabus con firma digital y certificado de firma registrado por el usuario.
 * Quién firma cada paso lo decide {@link FirmaService} según el cargo y la afiliación.
 */
@RestController
public class FirmaController {

    private final FirmaService service;
    private final ObjectMapper json;

    public FirmaController(FirmaService service, ObjectMapper json) {
        this.service = service;
        this.json = json;
    }

    @GetMapping("/api/syllabus/{id}/signatures")
    @PreAuthorize("@permisos.puede(authentication, 'syllabus:read', #id)")
    public FirmaService.FirmasResponse estado(@AuthenticationPrincipal UsuarioAutenticado actor, @PathVariable UUID id) {
        return service.estado(actor, id);
    }

    /** El PDF se muestra en el navegador (inline) para revisarlo antes de firmar. */
    @GetMapping("/api/syllabus/{id}/signatures/preview")
    @PreAuthorize("hasAuthority('syllabus:sign')")
    public ResponseEntity<byte[]> vistaPrevia(@AuthenticationPrincipal UsuarioAutenticado actor,
                                              @PathVariable UUID id) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(service.vistaPrevia(actor, id));
    }

    @PostMapping(path = "/api/syllabus/{id}/signatures", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('syllabus:sign')")
    public FirmaService.FirmasResponse firmar(@AuthenticationPrincipal UsuarioAutenticado actor,
                                              @PathVariable UUID id,
                                              @RequestParam int version,
                                              @RequestParam String password,
                                              @RequestParam(required = false) MultipartFile certificate,
                                              @RequestParam(defaultValue = "false") boolean remember) {
        return service.firmar(actor, id, version, password, certificate, remember);
    }

    /** Bandeja de syllabus vigentes cuya firma pendiente le corresponde al usuario. */
    @GetMapping("/api/signatures/pending")
    @PreAuthorize("hasAuthority('syllabus:sign')")
    public List<FirmaService.PendienteResponse> pendientes(@AuthenticationPrincipal UsuarioAutenticado actor) {
        return service.pendientes(actor);
    }

    /** Firma por lote: items es la lista JSON [{"id", "version"}] de los syllabus revisados. */
    @PostMapping(path = "/api/signatures/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('syllabus:sign')")
    public List<FirmaService.ResultadoLote> firmarLote(@AuthenticationPrincipal UsuarioAutenticado actor,
                                                      @RequestParam String items,
                                                      @RequestParam String password,
                                                      @RequestParam(required = false) MultipartFile certificate)
            throws JsonProcessingException {
        List<FirmaService.ItemLote> lista = json.readValue(items, new TypeReference<>() {});
        return service.firmarLote(actor, lista, password, certificate);
    }

    @GetMapping("/api/auth/me/certificate")
    @PreAuthorize("hasAuthority('syllabus:sign')")
    public ResponseEntity<FirmaService.CertificadoResponse> certificado(
            @AuthenticationPrincipal UsuarioAutenticado actor) {
        return service.certificadoGuardado(actor.id()).map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PutMapping(path = "/api/auth/me/certificate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('syllabus:sign')")
    public FirmaService.CertificadoResponse guardarCertificado(@AuthenticationPrincipal UsuarioAutenticado actor,
                                                               @RequestParam MultipartFile certificate,
                                                               @RequestParam String password) {
        return service.guardarCertificado(actor.id(), certificate, password);
    }

    @DeleteMapping("/api/auth/me/certificate")
    @PreAuthorize("hasAuthority('syllabus:sign')")
    public Map<String, String> eliminarCertificado(@AuthenticationPrincipal UsuarioAutenticado actor) {
        service.eliminarCertificado(actor.id());
        return Map.of("message", "Certificado eliminado");
    }

    @GetMapping(path = "/api/auth/me/signature-image", produces = MediaType.IMAGE_PNG_VALUE)
    @PreAuthorize("hasAuthority('syllabus:sign')")
    public ResponseEntity<byte[]> imagenDeFirma(@AuthenticationPrincipal UsuarioAutenticado actor) {
        return service.imagenDeFirma(actor.id()).map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PutMapping(path = "/api/auth/me/signature-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('syllabus:sign')")
    public Map<String, String> guardarImagenDeFirma(@AuthenticationPrincipal UsuarioAutenticado actor,
                                                    @RequestParam MultipartFile image) {
        service.guardarImagenDeFirma(actor.id(), image);
        return Map.of("message", "Imagen de la firma registrada");
    }

    @DeleteMapping("/api/auth/me/signature-image")
    @PreAuthorize("hasAuthority('syllabus:sign')")
    public Map<String, String> eliminarImagenDeFirma(@AuthenticationPrincipal UsuarioAutenticado actor) {
        service.eliminarImagenDeFirma(actor.id());
        return Map.of("message", "Imagen de la firma eliminada");
    }
}
