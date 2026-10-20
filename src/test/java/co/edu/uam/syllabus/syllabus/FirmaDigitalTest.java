package co.edu.uam.syllabus.syllabus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.uam.syllabus.PruebaIntegracion;
import com.jayway.jsonpath.JsonPath;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.apache.pdfbox.text.PDFTextStripper;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

/**
 * Aprobación con firma digital: firma primero la coordinación del departamento que ofrece la
 * asignatura y después Registro Académico; el PDF exportado lleva las dos firmas y sigue siendo
 * válido. Editar el syllabus deja la versión nueva sin firmas. Los firmantes demo tienen un
 * certificado de prueba registrado con la contraseña Uam2026!.
 */
class FirmaDigitalTest extends PruebaIntegracion {

    private static final String CLAVE = "Uam2026!";

    private static final String SYLLABUS = """
            {"subjectId": "%s", "academicPeriod": "%s", "title": "Matemáticas Básicas - %s", "status": "ACTIVO",
             "sections": [
               {"sectionType": "identification", "title": "Identificación", "content": ""},
               {"sectionType": "justification", "title": "Justificación", "content": "%s"},
               {"sectionType": "evaluation", "title": "Evaluación", "evaluation": [
                 {"corte": 1, "label": "I", "porcentaje": 30, "tipos": [{"tipo": "Quiz", "porcentaje": 100}]},
                 {"corte": 2, "label": "II", "porcentaje": 35, "tipos": [{"tipo": "Taller", "porcentaje": 100}]},
                 {"corte": 3, "label": "III", "porcentaje": 35, "tipos": [{"tipo": "Examen", "porcentaje": 100}]}]}]%s}
            """;

    @Test
    void laCoordinacionDelDepartamentoYRegistroFirmanEnOrdenYElPdfQuedaVerificado() throws Exception {
        String id = crear("2027-01");
        String firmas = "/api/syllabus/" + id + "/signatures";

        mvc.perform(como("registro", get(firmas)))
                .andExpect(jsonPath("$.state").value("SIN_FIRMAS"))
                .andExpect(jsonPath("$.nextStep").value("DEPARTAMENTO"))
                .andExpect(jsonPath("$.nextSigner").value("Coordinación del Departamento de Física y Matemáticas"))
                .andExpect(jsonPath("$.canSign").value(false));
        mvc.perform(firmar("registro", id, 1, CLAVE)).andExpect(status().isForbidden());
        mvc.perform(firmar("coordinador", id, 1, CLAVE)).andExpect(status().isForbidden());
        mvc.perform(como("coordinador.matematicas", get(firmas)))
                .andExpect(jsonPath("$.canSign").value(true))
                .andExpect(jsonPath("$.storedCertificate.subject").value(
                        "CN=Sandra Milena Arango,OU=Certificado de prueba (demo),O=Universidad Autonoma de Manizales,C=CO"));

        byte[] vistaPrevia = mvc.perform(como("coordinador.matematicas", get(firmas + "/preview")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(texto(vistaPrevia)).contains("Firma digital de la coordinación",
                "Coordinación del Departamento de Física y Matemáticas", "Firma digital de Registro Académico");

        mvc.perform(firmar("coordinador.matematicas", id, 1, "clave-equivocada"))
                .andExpect(status().isBadRequest());
        mvc.perform(firmar("coordinador.matematicas", id, 1, CLAVE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("PENDIENTE_REGISTRO"))
                .andExpect(jsonPath("$.signatures[0].signerName").value("Sandra Milena Arango"))
                .andExpect(jsonPath("$.nextStep").value("REGISTRO"));
        // Hasta que Registro firme, se exporta el PDF sin firmas.
        assertThat(firmasValidas(exportar(id))).isZero();

        mvc.perform(firmar("registro", id, 1, CLAVE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("VERIFICADO"))
                .andExpect(jsonPath("$.nextStep").doesNotExist());
        assertThat(firmasValidas(exportar(id))).isEqualTo(2);
        mvc.perform(firmar("registro", id, 1, CLAVE)).andExpect(status().isConflict());
        mvc.perform(como("admin", get("/api/syllabus/" + id)))
                .andExpect(jsonPath("$.verification").value("VERIFICADO"));
        String malla = mvc.perform(como("admin",
                        get("/api/study-plans/" + PLAN_SISTEMAS + "/curriculum?academicPeriod=2027-01")))
                .andReturn().getResponse().getContentAsString();
        List<String> estados = JsonPath.read(malla, "$.semesters[*].subjects[?(@.code == 'DFM001')].state");
        assertThat(estados).containsExactly("VERIFICADO");

        // Una edición crea una versión nueva, que vuelve a necesitar las firmas.
        mvc.perform(como("coordinador.matematicas", put("/api/syllabus/" + id).content("""
                        {"title": "Matemáticas Básicas - 2027-01", "status": "ACTIVO", "changeDescription": "Ajuste",
                         "baseVersion": 1,
                         "sections": %s}""".formatted(secciones("Justificación ajustada")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verification").value("SIN_FIRMAS"));
        assertThat(firmasValidas(exportar(id))).isZero();
        mvc.perform(firmar("coordinador.matematicas", id, 1, CLAVE)).andExpect(status().isConflict());
    }

    @Test
    void cadaFirmanteVeSusPendientesFirmaPorLoteYRecibeAviso() throws Exception {
        String uno = crear("2027-03");
        String dos = crear("2028-02");
        // Al quedar vigentes, se avisa a la coordinación del departamento que ofrece la asignatura.
        verify(correo, atLeastOnce()).send(argThat((SimpleMailMessage m) -> aviso(m, "coordinador.matematicas@autonoma.edu.co", "DFM001 · 2028-02")));

        mvc.perform(como("coordinador.matematicas", get("/api/signatures/pending")))
                .andExpect(jsonPath("$[*].id", hasItems(uno, dos)))
                .andExpect(jsonPath("$[*].step", everyItem(is("DEPARTAMENTO"))));
        mvc.perform(como("registro", get("/api/signatures/pending")))
                .andExpect(jsonPath("$[*].id", not(hasItems(uno))));

        mvc.perform(lote("coordinador.matematicas", "[{\"id\": \"%s\", \"version\": 1}, {\"id\": \"%s\", \"version\": 1}]"
                        .formatted(uno, dos)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].signed", everyItem(is(true))))
                .andExpect(jsonPath("$[*].state", everyItem(is("PENDIENTE_REGISTRO"))));
        verify(correo, atLeastOnce()).send(argThat((SimpleMailMessage m) -> aviso(m, "registro@autonoma.edu.co", "DFM001 · 2027-03")));

        // Registro firma por lote; una versión que ya no es la vigente no se firma, las demás sí.
        mvc.perform(lote("registro", "[{\"id\": \"%s\", \"version\": 1}, {\"id\": \"%s\", \"version\": 9}]"
                        .formatted(uno, dos)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].signed").value(true))
                .andExpect(jsonPath("$[0].state").value("VERIFICADO"))
                .andExpect(jsonPath("$[1].signed").value(false));
        mvc.perform(como("registro", get("/api/signatures/pending")))
                .andExpect(jsonPath("$[*].id", hasItem(dos)))
                .andExpect(jsonPath("$[*].id", not(hasItem(uno))));
        mvc.perform(como("estudiante", get("/api/signatures/pending"))).andExpect(status().isForbidden());
    }

    @Test
    void soloSeFirmaUnSyllabusActivoYElAdministradorNoFirma() throws Exception {
        mvc.perform(como("coordinador", get("/api/syllabus/" + BASES_DE_DATOS_BORRADOR + "/signatures/preview")))
                .andExpect(status().isBadRequest());
        String id = crear("2027-02");
        mvc.perform(como("admin", get("/api/syllabus/" + id + "/signatures")))
                .andExpect(jsonPath("$.canSign").value(false));
        mvc.perform(firmar("admin", id, 1, CLAVE)).andExpect(status().isForbidden());
        mvc.perform(como("estudiante", get("/api/syllabus/" + id + "/signatures/preview")))
                .andExpect(status().isForbidden());
    }

    @Test
    void elFirmanteRegistraSuCertificadoUnaVezYSeRechazanLosVencidos() throws Exception {
        byte[] vigente = certificado("Carlos Andrés Martínez", Instant.now().minus(Duration.ofDays(1)),
                Instant.now().plus(Duration.ofDays(365)));
        byte[] vencido = certificado("Certificado Vencido", Instant.now().minus(Duration.ofDays(30)),
                Instant.now().minus(Duration.ofDays(1)));
        try {
            mvc.perform(conCertificado("profesor", vencido, CLAVE))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value(startsWith("El certificado venció")));
            mvc.perform(conCertificado("profesor", vigente, "otra"))
                    .andExpect(status().isBadRequest());
            mvc.perform(conCertificado("profesor", vigente, CLAVE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.subject").value("O=Pruebas,CN=Carlos Andrés Martínez"));
            mvc.perform(como("profesor", get("/api/auth/me/certificate")))
                    .andExpect(jsonPath("$.issuer").value("O=Pruebas,CN=Carlos Andrés Martínez"));
            mvc.perform(como("estudiante", get("/api/auth/me/certificate"))).andExpect(status().isForbidden());
        } finally {
            mvc.perform(como("profesor", delete("/api/auth/me/certificate"))).andExpect(status().isOk());
        }
        mvc.perform(como("profesor", get("/api/auth/me/certificate"))).andExpect(status().isNoContent());
    }

    @Test
    void laImagenDeLaFirmaRegistradaSeDibujaEnElSelloYElPdfSigueValido() throws Exception {
        String id = crear("2028-03");
        try {
            mvc.perform(como("registro", get("/api/auth/me/signature-image"))).andExpect(status().isNoContent());
            mvc.perform(conImagen("registro", jpg(new Color(250, 250, 248))))
                    .andExpect(status().isOk());
            byte[] guardada = mvc.perform(como("registro", get("/api/auth/me/signature-image")))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsByteArray();
            BufferedImage png = ImageIO.read(new ByteArrayInputStream(guardada));
            assertThat(png.getWidth()).as("recortada a lo dibujado").isLessThan(400);
            assertThat(png.getRGB(0, 0) >>> 24).as("el fondo blanco queda transparente").isZero();
            mvc.perform(conImagen("registro", "no es una imagen".getBytes())).andExpect(status().isBadRequest());
            mvc.perform(como("estudiante", get("/api/auth/me/signature-image"))).andExpect(status().isForbidden());

            // La coordinación no tiene imagen (sello con el nombre); Registro sí (sello con la rúbrica).
            mvc.perform(firmar("coordinador.matematicas", id, 1, CLAVE)).andExpect(status().isOk());
            mvc.perform(firmar("registro", id, 1, CLAVE)).andExpect(status().isOk());
            byte[] pdf = exportar(id);
            assertThat(firmasValidas(pdf)).isEqualTo(2);
            assertThat(imagenesEnSellos(pdf)).isEqualTo(1);
        } finally {
            mvc.perform(como("registro", delete("/api/auth/me/signature-image"))).andExpect(status().isOk());
        }
        mvc.perform(como("registro", get("/api/auth/me/signature-image"))).andExpect(status().isNoContent());
    }

    private String crear(String periodo) throws Exception {
        String respuesta = mvc.perform(como("coordinador.matematicas", post("/api/syllabus")
                        .content(SYLLABUS.formatted(ASIG_MATEMATICAS_BASICAS, periodo, periodo, "Justificación", ""))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.id");
    }

    private static String secciones(String justificacion) {
        String json = SYLLABUS.formatted("x", "x", "x", justificacion, "");
        return json.substring(json.indexOf("["), json.lastIndexOf("]") + 1);
    }

    private MockMultipartHttpServletRequestBuilder firmar(String usuario, String id, int version, String clave)
            throws Exception {
        MockMultipartHttpServletRequestBuilder peticion = multipart("/api/syllabus/" + id + "/signatures");
        peticion.param("version", String.valueOf(version)).param("password", clave)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(usuario));
        return peticion;
    }

    /** Correo de aviso de firma dirigido a la persona indicada y sobre el syllabus indicado. */
    private static boolean aviso(SimpleMailMessage m, String para, String asunto) {
        String[] destinatarios = m.getTo();
        String titulo = m.getSubject();
        return destinatarios != null && List.of(destinatarios).contains(para) && titulo != null
                && titulo.contains(asunto);
    }

    private MockMultipartHttpServletRequestBuilder lote(String usuario, String items) throws Exception {
        MockMultipartHttpServletRequestBuilder peticion = multipart("/api/signatures/batch");
        peticion.param("items", items).param("password", CLAVE)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(usuario));
        return peticion;
    }

    private MockMultipartHttpServletRequestBuilder conCertificado(String usuario, byte[] p12, String clave)
            throws Exception {
        MockMultipartHttpServletRequestBuilder peticion = multipart(HttpMethod.PUT, "/api/auth/me/certificate")
                .file(new MockMultipartFile("certificate", "firma.p12", "application/x-pkcs12", p12));
        peticion.param("password", clave).header(HttpHeaders.AUTHORIZATION, "Bearer " + token(usuario));
        return peticion;
    }

    private MockMultipartHttpServletRequestBuilder conImagen(String usuario, byte[] imagen) throws Exception {
        MockMultipartHttpServletRequestBuilder peticion = multipart(HttpMethod.PUT, "/api/auth/me/signature-image")
                .file(new MockMultipartFile("image", "firma.jpg", "image/jpeg", imagen));
        peticion.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(usuario));
        return peticion;
    }

    /** Foto de una firma: un trazo oscuro sobre papel casi blanco, en JPG (sin transparencia). */
    private static byte[] jpg(Color papel) throws Exception {
        BufferedImage foto = new BufferedImage(800, 400, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = foto.createGraphics();
        g.setColor(papel);
        g.fillRect(0, 0, 800, 400);
        g.setColor(new Color(20, 30, 90));
        g.setStroke(new BasicStroke(6));
        g.drawLine(250, 220, 330, 150);
        g.drawLine(330, 150, 420, 240);
        g.drawLine(420, 240, 520, 170);
        g.dispose();
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(foto, "jpg", salida);
        return salida.toByteArray();
    }

    /** Imágenes dentro de las apariencias de los campos de firma (los sellos visibles). */
    private static int imagenesEnSellos(byte[] pdf) throws Exception {
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            int total = 0;
            for (PDField campo : documento.getDocumentCatalog().getAcroForm().getFieldTree()) {
                if (campo instanceof PDSignatureField firma) {
                    for (PDAnnotationWidget widget : firma.getWidgets()) {
                        total += imagenes(widget.getAppearance().getNormalAppearance().getAppearanceStream()
                                .getResources());
                    }
                }
            }
            return total;
        }
    }

    private static int imagenes(PDResources recursos) throws Exception {
        int total = 0;
        if (recursos == null) {
            return 0;
        }
        for (COSName nombre : recursos.getXObjectNames()) {
            PDXObject objeto = recursos.getXObject(nombre);
            if (objeto instanceof PDImageXObject) {
                total++;
            } else if (objeto instanceof PDFormXObject forma) {
                total += imagenes(forma.getResources());
            }
        }
        return total;
    }

    private byte[] exportar(String id) throws Exception {
        return mvc.perform(como("coordinador.matematicas", get("/api/export/syllabus/" + id)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
    }

    /** Verifica cada firma CMS contra los bytes que cubre y devuelve cuántas hay. */
    private static int firmasValidas(byte[] pdf) throws Exception {
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            List<PDSignature> firmas = documento.getSignatureDictionaries();
            for (PDSignature firma : firmas) {
                CMSSignedData cms = new CMSSignedData(new CMSProcessableByteArray(firma.getSignedContent(pdf)),
                        firma.getContents(pdf));
                SignerInformation firmante = cms.getSignerInfos().getSigners().iterator().next();
                @SuppressWarnings("unchecked")
                X509CertificateHolder certificado = (X509CertificateHolder) cms.getCertificates()
                        .getMatches(firmante.getSID()).iterator().next();
                assertThat(firmante.verify(new JcaSimpleSignerInfoVerifierBuilder().build(certificado))).isTrue();
            }
            if (!firmas.isEmpty()) {
                int[] rango = firmas.getLast().getByteRange();
                assertThat(rango[2] + rango[3]).as("la última firma cubre todo el archivo").isEqualTo(pdf.length);
            }
            return firmas.size();
        }
    }

    private static String texto(byte[] pdf) throws Exception {
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(documento).replaceAll("\\s+", " ");
        }
    }

    /** Certificado autofirmado de prueba (.p12) con la contraseña Uam2026!. */
    private static byte[] certificado(String nombre, Instant desde, Instant hasta) throws Exception {
        KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
        generador.initialize(2048);
        KeyPair claves = generador.generateKeyPair();
        X500Name titular = new X500Name("CN=" + nombre + ",O=Pruebas");
        X509Certificate x509 = new JcaX509CertificateConverter().getCertificate(new JcaX509v3CertificateBuilder(
                titular, BigInteger.valueOf(System.nanoTime()), Date.from(desde), Date.from(hasta), titular,
                claves.getPublic()).build(new JcaContentSignerBuilder("SHA256withRSA").build(claves.getPrivate())));
        KeyStore almacen = KeyStore.getInstance("PKCS12");
        almacen.load(null, null);
        almacen.setKeyEntry("firma", claves.getPrivate(), CLAVE.toCharArray(), new Certificate[] {x509});
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        almacen.store(salida, CLAVE.toCharArray());
        return salida.toByteArray();
    }
}
