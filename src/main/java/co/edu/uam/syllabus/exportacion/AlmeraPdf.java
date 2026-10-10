package co.edu.uam.syllabus.exportacion;

import co.edu.uam.syllabus.configuracion.Configuracion;
import co.edu.uam.syllabus.configuracion.ConfiguracionService;
import co.edu.uam.syllabus.configuracion.Evaluacion;
import co.edu.uam.syllabus.exportacion.DocumentoAlmera.Corte;
import co.edu.uam.syllabus.exportacion.DocumentoAlmera.FilaIdentificacion;
import co.edu.uam.syllabus.exportacion.DocumentoAlmera.Seccion;
import co.edu.uam.syllabus.exportacion.DocumentoAlmera.TipoEvaluacion;
import co.edu.uam.syllabus.syllabus.SyllabusResponse;
import co.edu.uam.syllabus.syllabus.SyllabusService;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * UC17: genera el PDF con el formato estándar Almera (GFR-FOR-005) y las medidas del documento
 * institucional: Carta, encabezado 3×3 con logo, Times New Roman 12 pt en el cuerpo y los títulos,
 * tabla de identificación, tabla de evaluación en Arial con CORTE y % TOTAL combinados, viñetas y
 * bibliografía. Times y Arial (Helvetica) son fuentes estándar de PDF: no se incrustan archivos.
 */
@Component
public class AlmeraPdf {

    /** Títulos institucionales por tipo de sección; las demás usan su propio título en mayúsculas. */
    private static final Map<String, String> TITULOS = Map.of(
            "identification", "IDENTIFICACIÓN",
            "justification", "JUSTIFICACIÓN",
            "performance", "DESEMPEÑO ESPERADO",
            "contents", "CONTENIDOS",
            "teaching_strategies", "ESTRATEGIAS DE ENSEÑANZA",
            SyllabusService.EVALUACION, "ESTRATEGIA DE EVALUACIÓN DEL APRENDIZAJE",
            "bibliography", "BIBLIOGRAFÍA");

    private static final List<String> ROTULOS_IDENTIFICACION = List.of(
            "Nombre asignatura", "Código", "Departamento (área)", "Programa (s) en los que se ofrece",
            "Modalidad de la asignatura", "Número de créditos", "Horas de trabajo presencial/sincrónico",
            "Horas de trabajo dirigido/tutoría sincrónica", "Horas de trabajo independiente/asincrónico",
            "Prerrequisitos", "Año- Periodo académico", "Docente (s)", "E-mail docente (s)");

    /** Secciones de la plantilla en blanco y cuántas líneas para escribir deja cada una. */
    private static final List<Map.Entry<String, Integer>> SECCIONES_EN_BLANCO = List.of(
            Map.entry("identification", 0), Map.entry("justification", 6), Map.entry("performance", 5),
            Map.entry("contents", 8), Map.entry("teaching_strategies", 5), Map.entry(SyllabusService.EVALUACION, 0),
            Map.entry("bibliography", 6));


    /** Tamaño del logo en el encabezado del documento institucional (52,94 × 25,18 mm). */
    private static final double LOGO_ANCHO_MAX = 52.94;
    private static final double LOGO_ALTO_MAX = 25.18;

    private final TemplateEngine plantillas;
    private final ConfiguracionService configuracion;

    public AlmeraPdf(TemplateEngine plantillas, ConfiguracionService configuracion) {
        this.plantillas = plantillas;
        this.configuracion = configuracion;
    }

    public byte[] syllabus(SyllabusResponse s) {
        Configuracion c = configuracion.obtener();
        DocumentoAlmera.Encabezado encabezado = encabezado(c,
                textoO(s.headerTitle(), c.syllabusHeader()),
                textoO(s.documentCode(), c.documentCode()),
                textoO(s.documentDate(), c.documentDate()));
        List<Seccion> secciones = new ArrayList<>();
        List<SyllabusResponse.Seccion> ordenadas = s.sections().stream()
                .sorted(Comparator.comparingInt(SyllabusResponse.Seccion::order)).toList();
        for (SyllabusResponse.Seccion x : ordenadas) {
            int numero = secciones.size() + 1;
            String titulo = TITULOS.getOrDefault(x.sectionType(), x.title().toUpperCase(Locale.ROOT));
            if ("identification".equals(x.sectionType())) {
                secciones.add(Seccion.identificacion(numero, titulo, identificacion(s)));
            } else if (x.evaluation() != null) {
                secciones.add(Seccion.evaluacion(numero, titulo, cortes(x.evaluation())));
            } else if ("bibliography".equals(x.sectionType())) {
                secciones.add(Seccion.bibliografia(numero, titulo, TextoConFormato.bibliografia(x.content())));
            } else {
                secciones.add(Seccion.texto(numero, titulo, TextoConFormato.bloques(x.content())));
            }
        }
        return render(new DocumentoAlmera(encabezado, secciones));
    }

    /** Plantilla en blanco con el formato vigente, para diligenciar a mano. */
    public byte[] plantillaEnBlanco() {
        Configuracion c = configuracion.obtener();
        DocumentoAlmera.Encabezado encabezado = encabezado(c, c.syllabusHeader(), c.documentCode(), c.documentDate());
        List<Seccion> secciones = new ArrayList<>();
        for (var entrada : SECCIONES_EN_BLANCO) {
            int numero = secciones.size() + 1;
            String titulo = TITULOS.get(entrada.getKey());
            if ("identification".equals(entrada.getKey())) {
                secciones.add(Seccion.identificacion(numero, titulo, ROTULOS_IDENTIFICACION.stream()
                        .map(r -> new FilaIdentificacion(r, "", false)).toList()));
            } else if (SyllabusService.EVALUACION.equals(entrada.getKey())) {
                secciones.add(Seccion.evaluacion(numero, titulo, cortes(c.evaluationTemplate())));
            } else {
                secciones.add(Seccion.lineas(numero, titulo, entrada.getValue()));
            }
        }
        return render(new DocumentoAlmera(encabezado, secciones));
    }

    private byte[] render(DocumentoAlmera documento) {
        Context contexto = new Context(Locale.forLanguageTag("es-CO"), Map.of("doc", documento));
        String html = plantillas.process("syllabus-almera", contexto);
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(salida);
            builder.run();
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el PDF.", e);
        }
    }

    private DocumentoAlmera.Encabezado encabezado(Configuracion c, String titulo, String codigo, String fecha) {
        ConfiguracionService.Logo logo = configuracion.logo();
        double ancho = LOGO_ANCHO_MAX;
        double alto = LOGO_ALTO_MAX;
        try {
            BufferedImage imagen = ImageIO.read(new ByteArrayInputStream(logo.datos()));
            double proporcion = (double) imagen.getWidth() / imagen.getHeight();
            ancho = Math.min(LOGO_ANCHO_MAX, LOGO_ALTO_MAX * proporcion);
            alto = ancho / proporcion;
        } catch (IOException | RuntimeException e) {
            // Si no se puede medir, se usa el tamaño del logo institucional.
        }
        String dataUri = "data:" + logo.tipo() + ";base64," + Base64.getEncoder().encodeToString(logo.datos());
        return new DocumentoAlmera.Encabezado(titulo.toUpperCase(Locale.ROOT), codigo, c.documentVersion(), fecha,
                dataUri, ancho, alto);
    }

    /**
     * Tabla de identificación. Si el programa tiene coordinación, «Docente (s)» queda vacío y el correo
     * es el de la coordinación; si no (p. ej. posgrados), se muestran el docente asignado y su correo.
     */
    private static List<FilaIdentificacion> identificacion(SyllabusResponse s) {
        SyllabusResponse.Materia m = s.subject();
        String coordinacion = m.program().coordinationEmail();
        String docente = coordinacion != null ? "" : m.professor() == null ? "Por asignar" : m.professor().name();
        String correo = coordinacion != null ? coordinacion : m.professor() == null ? "" : m.professor().email();
        Function<Integer, String> horas = h -> h == null ? "—" : String.valueOf(h);
        List<String> valores = List.of(
                m.name(), m.code(),
                m.department() == null ? "No aplica (posgrado)" : m.department().name(),
                m.program().name(),
                textoO(m.modality(), "Presencial"),
                String.valueOf(m.credits()),
                horas.apply(m.hoursPresencial()), horas.apply(m.hoursDirigido()), horas.apply(m.hoursIndependiente()),
                textoO(m.prerequisites(), "Ninguno"),
                s.academicPeriod(), docente, correo);
        List<FilaIdentificacion> filas = new ArrayList<>();
        for (int i = 0; i < ROTULOS_IDENTIFICACION.size(); i++) {
            boolean esCorreo = i == ROTULOS_IDENTIFICACION.size() - 1;
            filas.add(new FilaIdentificacion(ROTULOS_IDENTIFICACION.get(i), valores.get(i), esCorreo));
        }
        return filas;
    }

    private static List<Corte> cortes(List<Evaluacion.Corte> evaluacion) {
        return evaluacion.stream()
                .map(c -> new Corte(c.label(), c.porcentaje() + "%", c.tipos().stream()
                        .map(t -> new TipoEvaluacion(t.tipo(), t.porcentaje() + "%")).toList()))
                .toList();
    }

    private static String textoO(String valor, String porDefecto) {
        return StringUtils.hasText(valor) ? valor : porDefecto;
    }
}
