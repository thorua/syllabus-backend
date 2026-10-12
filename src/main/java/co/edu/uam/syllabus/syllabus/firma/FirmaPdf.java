package co.edu.uam.syllabus.syllabus.firma;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.text.Normalizer;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.x500.X500Principal;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Firma digital de un PDF con un certificado PKCS#12 (.p12/.pfx), como lo haría Adobe Acrobat: firma
 * CMS separada (adbe.pkcs7.detached) agregada de forma incremental, de modo que una segunda firma no
 * invalida la primera. Cada firma tiene un sello visible en su casilla del documento.
 */
@Component
public class FirmaPdf {

    /** 1 mm en puntos PDF. */
    private static final float MM = 72f / 25.4f;
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("America/Bogota"));

    /** Certificado abierto con su clave privada; los datos de texto son los que se muestran y guardan. */
    public record Certificado(PrivateKey clave, List<X509Certificate> cadena, String titular, String nombre,
                              String emisor, String serie, Instant vence) {}

    /** Abre el archivo .p12/.pfx con su contraseña y comprueba que el certificado sirva para firmar. */
    public Certificado abrir(byte[] archivo, char[] contrasena) {
        try {
            KeyStore almacen = KeyStore.getInstance("PKCS12");
            almacen.load(new ByteArrayInputStream(archivo), contrasena);
            for (String alias : Collections.list(almacen.aliases())) {
                if (!almacen.isKeyEntry(alias)) {
                    continue;
                }
                PrivateKey clave = (PrivateKey) almacen.getKey(alias, contrasena);
                Certificate[] cadena = almacen.getCertificateChain(alias);
                if (clave == null || cadena == null || cadena.length == 0) {
                    continue;
                }
                List<X509Certificate> x509 = Arrays.stream(cadena).map(c -> (X509Certificate) c).toList();
                validar(x509.getFirst());
                return certificado(clave, x509);
            }
            throw invalido("El archivo no contiene un certificado con clave privada para firmar.");
        } catch (IOException e) {
            throw invalido("No se pudo abrir el certificado: revise la contraseña y que el archivo sea .p12 o .pfx.");
        } catch (GeneralSecurityException e) {
            throw invalido("No se pudo leer la clave del certificado: " + e.getMessage());
        }
    }

    /**
     * Firma el PDF. El sello visible se ubica sobre la casilla cuyo rótulo es {@code rotulo}; si el
     * documento no la tiene, la firma queda sin sello (sigue siendo válida en el panel de firmas).
     */
    public byte[] firmar(byte[] pdf, Certificado c, String rotulo, String cargo, String motivo) {
        try (PDDocument documento = Loader.loadPDF(pdf);
             ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            PDSignature firma = new PDSignature();
            firma.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            firma.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
            firma.setName(c.nombre());
            firma.setLocation("Manizales, Colombia");
            firma.setReason(motivo);
            Calendar ahora = Calendar.getInstance();
            firma.setSignDate(ahora);

            Casilla casilla = ubicar(documento, rotulo);
            try (SignatureOptions opciones = new SignatureOptions()) {
                opciones.setPreferredSignatureSize(SignatureOptions.DEFAULT_SIGNATURE_SIZE * 2);
                if (casilla != null) {
                    opciones.setPage(casilla.pagina());
                    opciones.setVisualSignature(sello(documento, casilla, List.of(
                            "Firmado digitalmente por:", c.nombre(), cargo,
                            "Fecha: " + FECHA.format(ahora.toInstant()) + " (hora de Colombia)")));
                }
                documento.addSignature(firma, contenido -> cms(contenido, c), opciones);
                documento.saveIncremental(salida);
            }
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo firmar el PDF.", e);
        }
    }

    /** Posición (en puntos, origen abajo a la izquierda) del espacio en blanco sobre un rótulo. */
    private record Casilla(int pagina, PDRectangle rectangulo) {}

    private static Casilla ubicar(PDDocument documento, String rotulo) throws IOException {
        for (int i = documento.getNumberOfPages() - 1; i >= 0; i--) {
            BuscadorDeTexto buscador = new BuscadorDeTexto(rotulo);
            buscador.setStartPage(i + 1);
            buscador.setEndPage(i + 1);
            buscador.getText(documento);
            TextPosition t = buscador.posicion();
            if (t != null) {
                float alto = documento.getPage(i).getMediaBox().getHeight();
                float base = alto - t.getYDirAdj() + t.getHeightDir() + 1.5f * MM;
                return new Casilla(i, new PDRectangle(t.getXDirAdj(), base, 74 * MM, 22 * MM));
            }
        }
        return null;
    }

    /**
     * Reúne el texto de la página con la posición de cada carácter (PDFBox lo entrega palabra por
     * palabra) para hallar dónde empieza el rótulo.
     */
    private static final class BuscadorDeTexto extends PDFTextStripper {

        private final String rotulo;
        private final StringBuilder texto = new StringBuilder();
        private final List<TextPosition> posiciones = new ArrayList<>();

        BuscadorDeTexto(String rotulo) {
            this.rotulo = rotulo;
        }

        @Override
        protected void writeString(String palabra, List<TextPosition> letras) {
            if (!texto.isEmpty()) {
                texto.append(' ');
                posiciones.add(null);
            }
            for (int i = 0; i < palabra.length(); i++) {
                texto.append(palabra.charAt(i));
                posiciones.add(i < letras.size() ? letras.get(i) : null);
            }
        }

        TextPosition posicion() {
            int i = texto.indexOf(rotulo);
            return i < 0 ? null : posiciones.get(i);
        }
    }

    /** Documento de una página con el campo de firma y su apariencia: el texto del sello. */
    private static InputStream sello(PDDocument origen, Casilla casilla, List<String> lineas) throws IOException {
        try (PDDocument plantilla = new PDDocument(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            PDPage pagina = new PDPage(origen.getPage(casilla.pagina()).getMediaBox());
            plantilla.addPage(pagina);
            PDAcroForm formulario = new PDAcroForm(plantilla);
            plantilla.getDocumentCatalog().setAcroForm(formulario);
            PDSignatureField campo = new PDSignatureField(formulario);
            PDAnnotationWidget widget = campo.getWidgets().getFirst();
            formulario.setSignaturesExist(true);
            formulario.setAppendOnly(true);
            formulario.getCOSObject().setDirect(true);
            formulario.getFields().add(campo);
            PDRectangle r = casilla.rectangulo();
            widget.setRectangle(r);

            PDFormXObject forma = new PDFormXObject(new PDStream(plantilla));
            forma.setResources(new PDResources());
            forma.setFormType(1);
            forma.setBBox(new PDRectangle(r.getWidth(), r.getHeight()));
            PDAppearanceDictionary apariencia = new PDAppearanceDictionary();
            apariencia.getCOSObject().setDirect(true);
            PDAppearanceStream flujo = new PDAppearanceStream(forma.getCOSObject());
            apariencia.setNormalAppearance(flujo);
            widget.setAppearance(apariencia);

            PDType1Font normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font negrita = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            try (PDPageContentStream contenido = new PDPageContentStream(plantilla, flujo)) {
                contenido.beginText();
                contenido.newLineAtOffset(2 * MM, r.getHeight() - 4 * MM);
                for (int i = 0; i < lineas.size(); i++) {
                    contenido.setFont(i == 1 ? negrita : normal, i == 1 ? 9 : 7.5f);
                    contenido.showText(latin1(lineas.get(i)));
                    contenido.newLineAtOffset(0, -3.8f * MM);
                }
                contenido.endText();
            }
            plantilla.save(salida);
            return new ByteArrayInputStream(salida.toByteArray());
        }
    }

    private static byte[] cms(InputStream contenido, Certificado c) throws IOException {
        try {
            String algoritmo = "EC".equals(c.clave().getAlgorithm()) ? "SHA256withECDSA" : "SHA256withRSA";
            ContentSigner firmante = new JcaContentSignerBuilder(algoritmo).build(c.clave());
            CMSSignedDataGenerator generador = new CMSSignedDataGenerator();
            generador.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(
                    new JcaDigestCalculatorProviderBuilder().build()).build(firmante, c.cadena().getFirst()));
            generador.addCertificates(new JcaCertStore(c.cadena()));
            return generador.generate(new CMSProcessableByteArray(contenido.readAllBytes()), false).getEncoded();
        } catch (GeneralSecurityException | OperatorCreationException | CMSException e) {
            throw new IOException("No se pudo generar la firma.", e);
        }
    }

    private static void validar(X509Certificate certificado) {
        try {
            certificado.checkValidity();
        } catch (CertificateExpiredException e) {
            throw invalido("El certificado venció el " + FECHA.format(certificado.getNotAfter().toInstant()) + ".");
        } catch (CertificateNotYetValidException e) {
            throw invalido("El certificado aún no es válido.");
        }
        boolean[] usos = certificado.getKeyUsage();
        if (usos != null && !usos[0] && !usos[1]) {
            throw invalido("El certificado no está autorizado para firma digital.");
        }
    }

    private static Certificado certificado(PrivateKey clave, List<X509Certificate> cadena) {
        X509Certificate c = cadena.getFirst();
        String titular = c.getSubjectX500Principal().getName(X500Principal.RFC2253);
        return new Certificado(clave, cadena, titular, nombreComun(titular, c),
                c.getIssuerX500Principal().getName(X500Principal.RFC2253),
                c.getSerialNumber().toString(16).toUpperCase(), c.getNotAfter().toInstant());
    }

    /** CN del titular (el nombre de la persona); si no tiene, el nombre completo. */
    private static String nombreComun(String titular, X509Certificate c) {
        try {
            return new LdapName(titular).getRdns().stream()
                    .filter(rdn -> "CN".equalsIgnoreCase(rdn.getType()))
                    .map(Rdn::getValue)
                    .map(Object::toString)
                    .findFirst()
                    .orElse(c.getSubjectX500Principal().getName());
        } catch (InvalidNameException e) {
            return c.getSubjectX500Principal().getName();
        }
    }

    /** Las fuentes estándar de PDF solo tienen caracteres latinos; los demás se aproximan. */
    private static String latin1(String texto) {
        StringBuilder r = new StringBuilder();
        for (char ch : texto.toCharArray()) {
            r.append(ch < 256 ? ch : Normalizer.normalize(String.valueOf(ch), Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "").replaceAll("[^\\x00-\\xFF]", "?"));
        }
        return r.toString();
    }

    private static ResponseStatusException invalido(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }
}
