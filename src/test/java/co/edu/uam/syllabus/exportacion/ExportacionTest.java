package co.edu.uam.syllabus.exportacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.uam.syllabus.PruebaIntegracion;
import com.jayway.jsonpath.JsonPath;
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
        byte[] pdf = mvc.perform(como("profesor", get("/api/export/syllabus/" + ING_SOFTWARE_II)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"syllabus-DCC014-2026-01.pdf\""))
                .andReturn().getResponse().getContentAsByteArray();

        try (PDDocument documento = Loader.loadPDF(pdf)) {
            // Página Carta (215,9 × 279,4 mm) y encabezado institucional en cada página.
            assertThat(documento.getPage(0).getMediaBox().getWidth()).isEqualTo(612f);
            assertThat(documento.getPage(0).getMediaBox().getHeight()).isEqualTo(792f);
            String texto = texto(documento);
            assertThat(texto)
                    .contains("SYLLABUS DE LA ASIGNATURA", "CÓDIGO: GFR-FOR-005", "VERSIÓN: 6")
                    .contains("IDENTIFICACIÓN", "Ingeniería de Software II", "computacionales@autonoma.edu.co")
                    .contains("ESTRATEGIA DE EVALUACIÓN DEL APRENDIZAJE", "TIPO DE EVALUACIÓN", "Scrum")
                    .contains("PRESSMAN", "Requerida")
                    // Horas y prerrequisitos no publicados: el documento los marca como pendientes.
                    .contains("Prerrequisitos Por definir");
        }
    }

    @Test
    void unaAsignaturaDeServicioMuestraSuDepartamentoYTodosLosProgramasQueLaCursan() throws Exception {
        String texto = textoPdf(mvc.perform(como("coordinador.programa",
                        get("/api/export/syllabus/" + MATEMATICAS_BASICAS)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray());
        assertThat(texto).contains("Departamento de Física y Matemáticas", "Ingeniería de Sistemas",
                "Ingeniería Electrónica", "Ingeniería Biomédica", "Ingeniería Mecánica", "Ingeniería Industrial",
                "fisicaymatematicas@autonoma.edu.co");
    }

    @Test
    void elContactoEsLaCoordinacionYSoloSinCoordinacionElDocente() throws Exception {
        // Ciencias Computacionales, que ofrece Ingeniería de Software II, tiene coordinación: «Docente (s)»
        // queda vacío y sale el correo de la coordinación del departamento.
        String pregrado = textoPdf(mvc.perform(como("profesor", get("/api/export/syllabus/" + ING_SOFTWARE_II)))
                .andReturn().getResponse().getContentAsByteArray());
        assertThat(pregrado).contains("Docente (s) E-mail docente (s) computacionales@autonoma.edu.co")
                .doesNotContain("profesor@autonoma.edu.co");

        // La Maestría en Ingeniería no tiene correo de coordinación: sale el docente asignado y su correo.
        String asignatura = mvc.perform(como("admin", post("/api/subjects").content("""
                        {"name": "Métodos Avanzados de Ingeniería", "code": "MING901", "credits": 3,
                         "programId": "%s"}""".formatted(PROGRAMA_MAESTRIA_INGENIERIA))))
                .andReturn().getResponse().getContentAsString();
        String asignaturaId = JsonPath.read(asignatura, "$.id");
        mvc.perform(como("admin", put("/api/subjects/" + asignaturaId + "/assign-professor")
                        .content("{\"professorId\": \"" + PROFESOR + "\"}")))
                .andExpect(status().isOk());
        String syllabus = mvc.perform(como("admin", post("/api/syllabus").content("""
                        {"subjectId": "%s", "academicPeriod": "2026-02", "title": "Métodos Avanzados - 2026-02",
                         "status": "ACTIVO",
                         "sections": [{"sectionType": "identification", "title": "Identificación", "content": ""},
                                      {"sectionType": "justification", "title": "Justificación", "content": "x"}]}
                        """.formatted(asignaturaId))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String posgrado = textoPdf(mvc.perform(como("profesor",
                        get("/api/export/syllabus/" + JsonPath.read(syllabus, "$.id"))))
                .andReturn().getResponse().getContentAsByteArray());
        assertThat(posgrado)
                .contains("Docente (s) Ricardo Salazar Mejía E-mail docente (s) profesor@autonoma.edu.co");
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
        // El coordinador de Ciencias Computacionales ve las mallas de sus programas, no el posgrado (MGDS).
        assertThat(archivos).contains("syllabus-DCC014-2026-01.pdf", "syllabus-DFM001-2026-01.pdf")
                .noneMatch(nombre -> nombre.contains("MGDS"));
    }

    @Test
    void soloExportaQuienTienePermiso() throws Exception {
        mvc.perform(como("estudiante", get("/api/export/syllabus/" + ING_SOFTWARE_II)))
                .andExpect(status().isForbidden());
        mvc.perform(como("coordinador.programa", post("/api/export/batch").content("{}")))
                .andExpect(status().isForbidden());
        mvc.perform(como("coordinador", post("/api/export/batch")
                        .content("{\"syllabusIds\": [\"" + BASES_DE_DATOS_BORRADOR + "\"]}")))
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
