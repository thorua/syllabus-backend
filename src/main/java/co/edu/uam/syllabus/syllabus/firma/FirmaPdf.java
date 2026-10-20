package co.edu.uam.syllabus.syllabus.firma;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Signature;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.text.Normalizer;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import javax.imageio.ImageIO;
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
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.bouncycastle.asn1.pkcs.ContentInfo;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.pkcs.PKCS12PfxPdu;
import org.bouncycastle.pkcs.PKCS12SafeBag;
import org.bouncycastle.pkcs.PKCS12SafeBagFactory;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.PKCSException;
import org.bouncycastle.pkcs.jcajce.JcePKCS12MacCalculatorBuilderProvider;
import org.bouncycastle.pkcs.jcajce.JcePKCSPBEInputDecryptorProviderBuilder;
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
    private static final String CLAVE_INCORRECTA = "La contraseña del certificado no es correcta.";
    /** Proveedor de BouncyCastle solo para descifrar el archivo: admite los cifrados antiguos y los nuevos. */
    private static final Provider BC = new BouncyCastleProvider();
    private static final JcaPEMKeyConverter LLAVES = new JcaPEMKeyConverter();
    private static final JcaX509CertificateConverter CERTIFICADOS = new JcaX509CertificateConverter();

    /** Certificado abierto con su clave privada; los datos de texto son los que se muestran y guardan. */
    public record Certificado(PrivateKey clave, List<X509Certificate> cadena, String titular, String nombre,
                              String emisor, String serie, Instant vence) {}

    /**
     * Abre el archivo .p12/.pfx con su contraseña y comprueba que el certificado sirva para firmar.
     * Se lee con BouncyCastle y no con el KeyStore de Java porque Adobe Acrobat agrega a sus
     * identidades digitales un bloque propio que Java rechaza; aquí los bloques desconocidos se ignoran.
     */
    public Certificado abrir(byte[] archivo, char[] contrasena) {
        PKCS12PfxPdu pfx;
        try {
            pfx = new PKCS12PfxPdu(archivo);
        } catch (IOException | RuntimeException e) {
            throw invalido("El archivo no es un certificado de firma .p12 o .pfx.");
        }
        try {
            if (pfx.hasMac() && !pfx.isMacValid(
                    new JcePKCS12MacCalculatorBuilderProvider().setProvider(BC), contrasena)) {
                throw invalido(CLAVE_INCORRECTA);
            }
            InputDecryptorProvider descifrador = new JcePKCSPBEInputDecryptorProviderBuilder()
                    .setProvider(BC).build(contrasena);
            List<PrivateKey> claves = new ArrayList<>();
            List<X509Certificate> certificados = new ArrayList<>();
            for (ContentInfo contenido : pfx.getContentInfos()) {
                PKCS12SafeBagFactory bolsas = PKCSObjectIdentifiers.encryptedData.equals(contenido.getContentType())
                        ? new PKCS12SafeBagFactory(contenido, descifrador)
                        : new PKCS12SafeBagFactory(contenido);
                for (PKCS12SafeBag bolsa : bolsas.getSafeBags()) {
                    switch (bolsa.getBagValue()) {
                        case PKCS8EncryptedPrivateKeyInfo cifrada ->
                                claves.add(LLAVES.getPrivateKey(cifrada.decryptPrivateKeyInfo(descifrador)));
                        case PrivateKeyInfo clave -> claves.add(LLAVES.getPrivateKey(clave));
                        case X509CertificateHolder certificado -> certificados.add(CERTIFICADOS.getCertificate(certificado));
                        default -> { } // bloques propios de cada programa (p. ej. Adobe Acrobat)
                    }
                }
            }
            for (PrivateKey clave : claves) {
                for (X509Certificate certificado : certificados) {
                    if (sonPareja(clave, certificado)) {
                        validar(certificado);
                        List<X509Certificate> cadena = new ArrayList<>(List.of(certificado));
                        certificados.stream().filter(c -> c != certificado).forEach(cadena::add);
                        return certificado(clave, cadena);
                    }
                }
            }
            throw invalido("El archivo no contiene un certificado con clave privada para firmar.");
        } catch (PKCSException e) {
            throw invalido(CLAVE_INCORRECTA);
        } catch (IOException | GeneralSecurityException e) {
            throw invalido("No se pudo leer la clave del certificado: " + e.getMessage());
        }
    }

    /** La clave privada corresponde al certificado si lo que firma se verifica con su clave pública. */
    private static boolean sonPareja(PrivateKey clave, X509Certificate certificado) {
        try {
            String algoritmo = algoritmo(clave);
            byte[] prueba = "pareja".getBytes(StandardCharsets.UTF_8);
            Signature firma = Signature.getInstance(algoritmo);
            firma.initSign(clave);
            firma.update(prueba);
            byte[] firmado = firma.sign();
            Signature verificacion = Signature.getInstance(algoritmo);
            verificacion.initVerify(certificado.getPublicKey());
            verificacion.update(prueba);
            return verificacion.verify(firmado);
        } catch (GeneralSecurityException e) {
            return false;
        }
    }

    /**
     * Borra las manchas sueltas (puntos de la cuadrícula del papel, polvo del escáner): las zonas de
     * tinta conectadas que no llegan al 2 % de la más grande. Los trazos de la firma son continuos
     * y mucho más grandes, así que se conservan, y el recorte queda ajustado a la firma.
     */
    private static void quitarMotas(int[] pixeles, int ancho, int alto) {
        int[] zona = new int[pixeles.length];
        List<int[]> zonas = new ArrayList<>();
        int[] pila = new int[pixeles.length];
        for (int inicio = 0; inicio < pixeles.length; inicio++) {
            if (pixeles[inicio] == 0 || zona[inicio] != 0) {
                continue;
            }
            int numero = zonas.size() + 1;
            int tope = 0;
            int tamano = 0;
            pila[tope++] = inicio;
            zona[inicio] = numero;
            while (tope > 0) {
                int i = pila[--tope];
                tamano++;
                int x = i % ancho;
                int y = i / ancho;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int nx = x + dx;
                        int ny = y + dy;
                        if (nx < 0 || ny < 0 || nx >= ancho || ny >= alto) {
                            continue;
                        }
                        int vecino = ny * ancho + nx;
                        if (pixeles[vecino] != 0 && zona[vecino] == 0) {
                            zona[vecino] = numero;
                            pila[tope++] = vecino;
                        }
                    }
                }
            }
            zonas.add(new int[] {tamano});
        }
        int mayor = zonas.stream().mapToInt(z -> z[0]).max().orElse(0);
        int minimo = Math.max(8, mayor / 50);
        for (int i = 0; i < pixeles.length; i++) {
            if (zona[i] != 0 && zonas.get(zona[i] - 1)[0] < minimo) {
                pixeles[i] = 0;
            }
        }
    }

    private static boolean esPdf(byte[] archivo) {
        return archivo.length > 4 && new String(archivo, 0, 5, StandardCharsets.ISO_8859_1).equals("%PDF-");
    }

    private static BufferedImage imagen(byte[] archivo) {
        try {
            return ImageIO.read(new ByteArrayInputStream(archivo));
        } catch (IOException e) {
            return null;
        }
    }

    /** Dibuja la primera página del archivo de apariencias de Acrobat: la firma como la muestra Acrobat. */
    private static BufferedImage aparienciaDeAcrobat(byte[] archivo) {
        try (PDDocument apariencias = Loader.loadPDF(archivo)) {
            return apariencias.getNumberOfPages() == 0 ? null
                    : new PDFRenderer(apariencias).renderImageWithDPI(0, 150, ImageType.RGB);
        } catch (IOException e) {
            return null;
        }
    }

    private static String algoritmo(PrivateKey clave) {
        return "EC".equals(clave.getAlgorithm()) ? "SHA256withECDSA" : "SHA256withRSA";
    }

    /**
     * Limpia la imagen de la firma manuscrita: el fondo claro queda transparente, se recorta a lo
     * dibujado y se reduce a un tamaño moderado. Devuelve un PNG. Acepta una imagen (PNG o JPG) o el
     * archivo de apariencias de Adobe Acrobat ({@code appearances.acrodata}, que es un PDF con una
     * página por apariencia de firma): de él se toma la primera apariencia, tal como la dibuja Acrobat.
     */
    public byte[] rubrica(byte[] archivo) {
        BufferedImage original = esPdf(archivo) ? aparienciaDeAcrobat(archivo) : imagen(archivo);
        if (original == null) {
            throw invalido("La firma debe ser una imagen PNG o JPG, o el archivo appearances.acrodata de Acrobat.");
        }
        int ancho = original.getWidth();
        int alto = original.getHeight();
        int[] pixeles = new int[ancho * alto];
        for (int y = 0; y < alto; y++) {
            for (int x = 0; x < ancho; x++) {
                int argb = original.getRGB(x, y);
                int opacidad = argb >>> 24;
                int luz = Math.min((argb >> 16) & 0xFF, Math.min((argb >> 8) & 0xFF, argb & 0xFF));
                if (opacidad < 40 || luz > 215) {
                    continue;
                }
                // El borde del trazo se desvanece en vez de cortarse, para que no quede un halo blanco.
                int alfa = luz > 160 ? opacidad * (215 - luz) / 55 : opacidad;
                pixeles[y * ancho + x] = (alfa << 24) | (argb & 0xFFFFFF);
            }
        }
        quitarMotas(pixeles, ancho, alto);
        BufferedImage limpia = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_ARGB);
        limpia.setRGB(0, 0, ancho, alto, pixeles, 0, ancho);
        int minX = ancho;
        int minY = alto;
        int maxX = -1;
        int maxY = -1;
        for (int i = 0; i < pixeles.length; i++) {
            if (pixeles[i] != 0) {
                minX = Math.min(minX, i % ancho);
                minY = Math.min(minY, i / ancho);
                maxX = Math.max(maxX, i % ancho);
                maxY = Math.max(maxY, i / ancho);
            }
        }
        if (maxX < 0) {
            throw invalido("La imagen no tiene trazos: dibuje su firma o suba una imagen donde se vea.");
        }
        BufferedImage recorte = limpia.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
        double escala = Math.min(1, Math.min(900.0 / recorte.getWidth(), 300.0 / recorte.getHeight()));
        if (escala < 1) {
            int w = Math.max(1, (int) Math.round(recorte.getWidth() * escala));
            int h = Math.max(1, (int) Math.round(recorte.getHeight() * escala));
            BufferedImage reducida = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = reducida.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(recorte, 0, 0, w, h, null);
            g.dispose();
            recorte = reducida;
        }
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            ImageIO.write(recorte, "png", salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Firma el PDF. El sello visible se ubica sobre la casilla cuyo rótulo es {@code rotulo}; si el
     * documento no la tiene, la firma queda sin sello (sigue siendo válida en el panel de firmas).
     * Con {@code rubrica} (PNG de {@link #rubrica}) el sello muestra la firma manuscrita; sin ella,
     * el nombre en grande como la apariencia predeterminada de Acrobat.
     */
    public byte[] firmar(byte[] pdf, Certificado c, byte[] rubrica, String rotulo, String cargo, String motivo) {
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
                    opciones.setVisualSignature(sello(documento, casilla, c.nombre(), cargo, rubrica,
                            FECHA.format(ahora.toInstant())));
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
                return new Casilla(i, new PDRectangle(t.getXDirAdj(), base, 74 * MM, 30 * MM));
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

    /** Documento de una página con el campo de firma y su apariencia: la rúbrica o el nombre, y los datos. */
    private static InputStream sello(PDDocument origen, Casilla casilla, String nombre, String cargo, byte[] rubrica,
                                     String fecha) throws IOException {
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

            try (PDPageContentStream contenido = new PDPageContentStream(plantilla, flujo)) {
                if (rubrica != null) {
                    conRubrica(contenido, PDImageXObject.createFromByteArray(plantilla, rubrica, "rubrica"),
                            r.getWidth(), r.getHeight(), nombre, cargo, fecha);
                } else {
                    comoAcrobat(contenido, r.getWidth(), r.getHeight(), nombre, cargo, fecha);
                }
            }
            plantilla.save(salida);
            return new ByteArrayInputStream(salida.toByteArray());
        }
    }

    private static final PDType1Font NORMAL = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font NEGRITA = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    /** La firma manuscrita arriba, sobre una línea, y debajo el nombre, el cargo y la fecha. */
    private static void conRubrica(PDPageContentStream contenido, PDImageXObject imagen, float ancho, float alto,
                                   String nombre, String cargo, String fecha) throws IOException {
        float base = 9 * MM;
        float maxAncho = ancho - 4 * MM;
        float maxAlto = alto - base - 1 * MM;
        float escala = Math.min(maxAncho / imagen.getWidth(), maxAlto / imagen.getHeight());
        float w = imagen.getWidth() * escala;
        float h = imagen.getHeight() * escala;
        contenido.drawImage(imagen, (ancho - w) / 2, base + 0.5f * MM, w, h);

        contenido.setStrokingColor(0.55f);
        contenido.setLineWidth(0.4f);
        contenido.moveTo(2 * MM, base);
        contenido.lineTo(ancho - 2 * MM, base);
        contenido.stroke();

        texto(contenido, NEGRITA, 7.5f, nombre, 2 * MM, base - 2.9f * MM, maxAncho);
        texto(contenido, NORMAL, 6.5f, cargo, 2 * MM, base - 5.4f * MM, maxAncho);
        texto(contenido, NORMAL, 6.5f, "Firmado digitalmente el " + fecha + " (hora de Colombia)",
                2 * MM, base - 7.9f * MM, maxAncho);
    }

    /** Como la apariencia predeterminada de Acrobat: el nombre en grande a la izquierda y los datos a la derecha. */
    private static void comoAcrobat(PDPageContentStream contenido, float ancho, float alto, String nombre,
                                    String cargo, String fecha) throws IOException {
        float mitad = ancho * 0.45f;
        float tamano = 14;
        List<String> renglones = renglones(NEGRITA, tamano, nombre, mitad - 3 * MM);
        while ((renglones.size() > 3 || renglones.size() * tamano * 1.15f > alto - 2 * MM) && tamano > 7) {
            tamano -= 0.5f;
            renglones = renglones(NEGRITA, tamano, nombre, mitad - 3 * MM);
        }
        float y = (alto + renglones.size() * tamano * 1.15f) / 2 - tamano;
        for (String renglon : renglones) {
            texto(contenido, NEGRITA, tamano, renglon, 2 * MM, y, mitad - 3 * MM);
            y -= tamano * 1.15f;
        }

        float x = mitad + 1 * MM;
        float anchoDatos = ancho - x - 1.5f * MM;
        List<String> datos = new ArrayList<>(List.of("Firmado digitalmente por"));
        List<String> nombreDatos = renglones(NEGRITA, 7, nombre, anchoDatos);
        List<String> cargoDatos = renglones(NORMAL, 6.5f, cargo, anchoDatos);
        datos.addAll(nombreDatos);
        datos.addAll(cargoDatos);
        datos.add("Fecha: " + fecha);
        float paso = Math.min(2.8f * MM, (alto - 2 * MM) / datos.size());
        y = (alto + datos.size() * paso) / 2 - paso + 0.5f * MM;
        for (int i = 0; i < datos.size(); i++) {
            boolean esNombre = i >= 1 && i <= nombreDatos.size();
            texto(contenido, esNombre ? NEGRITA : NORMAL, esNombre ? 7 : 6.5f, datos.get(i), x, y, anchoDatos);
            y -= paso;
        }
    }

    /** Escribe una línea; si no cabe en {@code maxAncho}, reduce la letra hasta que quepa. */
    private static void texto(PDPageContentStream contenido, PDType1Font fuente, float tamano, String texto,
                              float x, float y, float maxAncho) throws IOException {
        String limpio = latin1(texto);
        float ancho = fuente.getStringWidth(limpio) / 1000 * tamano;
        contenido.beginText();
        contenido.setFont(fuente, ancho > maxAncho ? tamano * maxAncho / ancho : tamano);
        contenido.newLineAtOffset(x, y);
        contenido.showText(limpio);
        contenido.endText();
    }

    /** Parte el texto en renglones por palabras para que cada uno quepa en {@code ancho}. */
    private static List<String> renglones(PDType1Font fuente, float tamano, String texto, float ancho)
            throws IOException {
        List<String> renglones = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        for (String palabra : latin1(texto).split("\\s+")) {
            String prueba = actual.isEmpty() ? palabra : actual + " " + palabra;
            if (!actual.isEmpty() && fuente.getStringWidth(prueba) / 1000 * tamano > ancho) {
                renglones.add(actual.toString());
                actual = new StringBuilder(palabra);
            } else {
                actual = new StringBuilder(prueba);
            }
        }
        if (!actual.isEmpty()) {
            renglones.add(actual.toString());
        }
        return renglones;
    }

    private static byte[] cms(InputStream contenido, Certificado c) throws IOException {
        try {
            ContentSigner firmante = new JcaContentSignerBuilder(algoritmo(c.clave())).build(c.clave());
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
