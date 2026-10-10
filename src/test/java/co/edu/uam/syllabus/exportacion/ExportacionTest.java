package co.edu.uam.syllabus.exportacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.uam.syllabus.PruebaIntegracion;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

/** UC15 exportar individual, UC16 por lote y UC17 formato Almera. */
class ExportacionTest extends PruebaIntegracion {

    @Test
    void elPdfTieneElFormatoAlmeraYLosDatosDelSyllabus() throws Exception {
        byte[] pdf = mvc.perform(como("profesor", get("/api/export/syllabus/" + BASE_DE_DATOS)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"syllabus-SIS201-2026-01.pdf\""))
                .andReturn().getResponse().getContentAsByteArray();

        try (PDDocument documento = Loader.loadPDF(pdf)) {
            // Página Carta (215,9 × 279,4 mm) y encabezado institucional en cada página.
            assertThat(documento.getPage(0).getMediaBox().getWidth()).isEqualTo(612f);
            assertThat(documento.getPage(0).getMediaBox().getHeight()).isEqualTo(792f);
            String texto = texto(documento);
            assertThat(texto)
                    .contains("SYLLABUS DE LA ASIGNATURA", "CÓDIGO: GFR-FOR-005", "VERSIÓN: 6")
                    .contains("IDENTIFICACIÓN", "Base de Datos I", "coordinacion.sistemas@autonoma.edu.co")
                    .contains("ESTRATEGIA DE EVALUACIÓN DEL APRENDIZAJE", "TIPO DE EVALUACIÓN", "Avance proyecto")
                    .contains("Fundamentals of Database Systems", "Requerida");
        }
    }

    @Test
    void elContactoEsLaCoordinacionYSoloSinCoordinacionElDocente() throws Exception {
        // Ing. de Sistemas tiene coordinación: «Docente (s)» queda vacío y sale el correo de la coordinación.
        String pregrado = textoPdf(mvc.perform(como("profesor", get("/api/export/syllabus/" + BASE_DE_DATOS)))
                .andReturn().getResponse().getContentAsByteArray());
        assertThat(pregrado).contains("Docente (s) E-mail docente (s) coordinacion.sistemas@autonoma.edu.co")
                .doesNotContain("profesor@autonoma.edu.co");

        // La maestría no tiene coordinación: sale el docente asignado y su correo.
        String posgrado = textoPdf(mvc.perform(como("profesor", get("/api/export/syllabus/" + POSGRADO)))
                .andReturn().getResponse().getContentAsByteArray());
        assertThat(posgrado)
                .contains("Docente (s) Carlos Andrés Martínez E-mail docente (s) profesor@autonoma.edu.co");
    }

    @Test
    void elLoteEsUnZipConLosSyllabusVigentesDelAlcance() throws Exception {
        byte[] zip = mvc.perform(como("coordinador", post("/api/export/batch")
                        .content("{\"academicPeriod\": \"2026-01\"}")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/zip"))
                .andReturn().getResponse().getContentAsByteArray();

        List<String> archivos = new ArrayList<>();
        try (ZipInputStream entrada = new ZipInputStream(new ByteArrayInputStream(zip))) {
            for (ZipEntry e = entrada.getNextEntry(); e != null; e = entrada.getNextEntry()) {
                archivos.add(e.getName());
                assertThat(new String(entrada.readAllBytes(), 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
            }
        }
        // El coordinador de departamento solo ve Ing. de Sistemas: no incluye el posgrado (MIA501).
        assertThat(archivos).contains("syllabus-SIS201-2026-01.pdf", "syllabus-SIS301-2026-01.pdf")
                .noneMatch(nombre -> nombre.contains("MIA501"));
    }

    @Test
    void soloExportaQuienTienePermiso() throws Exception {
        mvc.perform(como("estudiante", get("/api/export/syllabus/" + BASE_DE_DATOS)))
                .andExpect(status().isForbidden());
        mvc.perform(como("coordinador.programa", post("/api/export/batch").content("{}")))
                .andExpect(status().isForbidden());
        mvc.perform(como("coordinador", post("/api/export/batch")
                        .content("{\"syllabusIds\": [\"" + ALGORITMOS_BORRADOR + "\"]}")))
                .andExpect(status().isForbidden());
    }

    @Test
    void laPlantillaEnBlancoSeGenera() throws Exception {
        byte[] pdf = mvc.perform(como("profesor", get("/api/export/template")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            assertThat(texto(documento)).contains("Nombre asignatura", "TIPO DE EVALUACIÓN");
        }
    }

    private static final String POSGRADO = "50000000-0000-0000-0000-000000000005";

    private static String textoPdf(byte[] pdf) throws IOException {
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            return texto(documento);
        }
    }

    /** Texto del PDF con los espacios normalizados: las celdas angostas parten "% TOTAL" en dos líneas. */
    private static String texto(PDDocument documento) throws IOException {
        return new PDFTextStripper().getText(documento).replaceAll("\\s+", " ");
    }
}
