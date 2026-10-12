package co.edu.uam.syllabus.exportacion;

import co.edu.uam.syllabus.auditoria.AuditEvent;
import co.edu.uam.syllabus.common.security.UsuarioAutenticado;
import co.edu.uam.syllabus.syllabus.SyllabusResponse;
import co.edu.uam.syllabus.syllabus.SyllabusService;
import co.edu.uam.syllabus.syllabus.firma.FirmaService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * UC15 exportar un syllabus en PDF y UC16 exportar por lote (ZIP de PDF), con el formato Almera (UC17).
 * Un syllabus verificado se exporta con el PDF que firmaron la coordinación y Registro Académico.
 */
@Service
public class ExportacionService {

    private static final String ENTIDAD = "syllabus";
    private static final int MAX_LOTE = 200;

    /** Archivo generado listo para descargar. */
    public record Archivo(String nombre, byte[] contenido) {}

    public record LoteRequest(List<UUID> syllabusIds, UUID departmentId, UUID programId, String academicPeriod) {}

    private final SyllabusService syllabus;
    private final FirmaService firmas;
    private final AlmeraPdf pdf;
    private final ApplicationEventPublisher eventos;

    public ExportacionService(SyllabusService syllabus, FirmaService firmas, AlmeraPdf pdf,
                              ApplicationEventPublisher eventos) {
        this.syllabus = syllabus;
        this.firmas = firmas;
        this.pdf = pdf;
        this.eventos = eventos;
    }

    public Archivo individual(UsuarioAutenticado actor, UUID id) {
        SyllabusResponse s = syllabus.obtener(actor, id);
        eventos.publishEvent(AuditEvent.de(actor.id(), "EXPORT", ENTIDAD, id)
                .con(Map.of("title", s.title(), "format", "PDF")));
        return new Archivo(nombre(s) + ".pdf", documento(s));
    }

    /** UC16: syllabus vigentes elegidos o filtrados por departamento, programa o período. */
    public Archivo lote(UsuarioAutenticado actor, LoteRequest req) {
        List<SyllabusResponse> lista = syllabus.vigentesParaExportar(actor, new SyllabusService.Filtros(
                null, req.departmentId(), req.programId(), null, req.academicPeriod(), null), req.syllabusIds());
        if (lista.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No hay syllabus vigentes para exportar con esos filtros.");
        }
        if (lista.size() > MAX_LOTE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El lote supera " + MAX_LOTE + " syllabus; acote los filtros.");
        }
        Set<String> usados = new HashSet<>();
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(salida)) {
            for (SyllabusResponse s : lista) {
                String base = nombre(s);
                String archivo = base + ".pdf";
                for (int n = 2; !usados.add(archivo); n++) {
                    archivo = base + "-" + n + ".pdf";
                }
                zip.putNextEntry(new ZipEntry(archivo));
                zip.write(documento(s));
                zip.closeEntry();
            }
            zip.finish();
            eventos.publishEvent(AuditEvent.de(actor.id(), "EXPORT", ENTIDAD, null).con(Map.of(
                    "name", "Lote de " + lista.size() + " syllabus",
                    "format", "ZIP",
                    "syllabusIds", lista.stream().map(s -> s.id().toString()).toList())));
            return new Archivo("syllabus-" + lista.size() + ".zip", salida.toByteArray());
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el archivo ZIP.", e);
        }
    }

    private byte[] documento(SyllabusResponse s) {
        return firmas.documentoVerificado(s.id()).orElseGet(() -> pdf.syllabus(s));
    }

    public Archivo plantilla() {
        return new Archivo("plantilla-syllabus-en-blanco.pdf", pdf.plantillaEnBlanco());
    }

    /** syllabus-SIS201-2026-01, sin tildes ni caracteres problemáticos para los sistemas de archivos. */
    private static String nombre(SyllabusResponse s) {
        String base = "syllabus-" + s.subject().code() + "-" + s.academicPeriod();
        return Normalizer.normalize(base, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
