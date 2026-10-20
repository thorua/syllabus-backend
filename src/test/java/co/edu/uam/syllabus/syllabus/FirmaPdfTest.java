package co.edu.uam.syllabus.syllabus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.uam.syllabus.syllabus.firma.FirmaPdf;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.SafeBag;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OutputEncryptor;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS12PfxPduBuilder;
import org.bouncycastle.pkcs.PKCS12SafeBag;
import org.bouncycastle.pkcs.PKCS12SafeBagBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS12SafeBagBuilder;
import org.bouncycastle.pkcs.jcajce.JcePKCS12MacCalculatorBuilder;
import org.bouncycastle.pkcs.jcajce.JcePKCSPBEOutputEncryptorBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/** Lectura de certificados .p12/.pfx, incluidas las identidades digitales que crea Adobe Acrobat. */
class FirmaPdfTest {

    private static final char[] CLAVE = "Uam2026!".toCharArray();

    private final FirmaPdf firmaPdf = new FirmaPdf();

    @Test
    void abreLaIdentidadDigitalDeAcrobatQueJavaRechaza() throws Exception {
        byte[] pfx = comoAcrobat("Docente de Prueba");

        assertThatThrownBy(() -> KeyStore.getInstance("PKCS12").load(new ByteArrayInputStream(pfx), CLAVE))
                .isInstanceOf(IOException.class);

        FirmaPdf.Certificado certificado = firmaPdf.abrir(pfx, CLAVE);
        assertThat(certificado.nombre()).isEqualTo("Docente de Prueba");
        assertThat(certificado.cadena()).hasSize(1);
    }

    @Test
    void distingueLaContrasenaIncorrectaDeUnArchivoQueNoEsCertificado() throws Exception {
        byte[] pfx = comoAcrobat("Docente de Prueba");

        assertThatThrownBy(() -> firmaPdf.abrir(pfx, "otra".toCharArray()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("La contraseña del certificado no es correcta");
        assertThatThrownBy(() -> firmaPdf.abrir("no es un certificado".getBytes(), CLAVE))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("no es un certificado de firma .p12 o .pfx");
    }

    /**
     * Mismo formato que Acrobat DC guarda en AppData\Roaming\Adobe\Acrobat\DC\Security: AES-256 con
     * PBKDF2, MAC SHA-256 y, junto a la clave, un secretBag propio de Adobe (OID 1.2.840.113583.1.1.4).
     */
    private static byte[] comoAcrobat(String nombre) throws Exception {
        KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
        generador.initialize(2048);
        KeyPair claves = generador.generateKeyPair();
        X500Name titular = new X500Name("CN=" + nombre + ",O=Pruebas");
        Instant ahora = Instant.now();
        X509CertificateHolder certificado = new JcaX509v3CertificateBuilder(titular, BigInteger.ONE,
                Date.from(ahora.minus(Duration.ofDays(1))), Date.from(ahora.plus(Duration.ofDays(365))), titular,
                claves.getPublic()).build(new JcaContentSignerBuilder("SHA256withRSA").build(claves.getPrivate()));

        OutputEncryptor cifrado = new JcePKCSPBEOutputEncryptorBuilder(NISTObjectIdentifiers.id_aes256_CBC)
                .setProvider(new BouncyCastleProvider()).build(CLAVE);
        PKCS12SafeBag adobe = new PKCS12SafeBag(new SafeBag(PKCSObjectIdentifiers.secretBag, new DERSequence(
                new ASN1Encodable[] {new ASN1ObjectIdentifier("1.2.840.113583.1.1.4"), new ASN1Integer(0x7FFFFFFF)})));
        PKCS12SafeBag clave = new JcaPKCS12SafeBagBuilder(claves.getPrivate(), cifrado).build();
        PKCS12SafeBag cert = new PKCS12SafeBagBuilder(certificado).build();

        PKCS12PfxPduBuilder pfx = new PKCS12PfxPduBuilder();
        pfx.addData(adobe);
        pfx.addData(clave);
        pfx.addEncryptedData(cifrado, cert);
        return pfx.build(new JcePKCS12MacCalculatorBuilder(NISTObjectIdentifiers.id_sha256)
                .setProvider(new BouncyCastleProvider()), CLAVE).getEncoded();
    }
}
