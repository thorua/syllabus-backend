package co.edu.uam.syllabus.syllabus.firma;

import co.edu.uam.syllabus.auditoria.AuditEvent;
import co.edu.uam.syllabus.catalogos.Asignatura;
import co.edu.uam.syllabus.common.security.UsuarioAutenticado;
import co.edu.uam.syllabus.exportacion.AlmeraPdf;
import co.edu.uam.syllabus.exportacion.DocumentoAlmera.CasillaFirma;
import co.edu.uam.syllabus.syllabus.Syllabus;
import co.edu.uam.syllabus.syllabus.SyllabusRepository;
import co.edu.uam.syllabus.syllabus.SyllabusService;
import co.edu.uam.syllabus.syllabus.versionado.VersionadoService;
import co.edu.uam.syllabus.usuarios.Rol;
import co.edu.uam.syllabus.usuarios.Usuario;
import co.edu.uam.syllabus.usuarios.UsuarioRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * Aprobación del syllabus con firma digital. Sobre la versión vigente firma primero la coordinación
 * que ofrece la asignatura (el Coordinador del Departamento o, en posgrado, el Coordinador del
 * Programa) y después Registro Académico; con las dos firmas el syllabus queda verificado. Cada
 * firma se agrega al mismo PDF, que es el que se exporta una vez verificado. Editar el syllabus
 * crea una versión nueva sin firmas.
 */
@Service
public class FirmaService {

    private static final String ROTULO_COORDINACION = "Firma digital de la coordinación";
    private static final String ROTULO_REGISTRO = "Firma digital de Registro Académico";
    private static final String CARGO_REGISTRO = "Coordinación de Registro Académico";

    public record FirmaResponse(Verificacion.Paso step, String signerName, String certificateSubject,
                                String certificateIssuer, Instant signedAt) {}

    public record CertificadoResponse(String subject, String issuer, String serial, Instant notAfter,
                                      Instant uploadedAt) {}

    /**
     * Firmas de la versión vigente. nextStep y nextSigner dicen qué firma falta y de quién; canSign,
     * si el usuario actual puede hacerla; storedCertificate, si tiene un certificado registrado.
     */
    public record FirmasResponse(Verificacion.Estado state, int version, boolean signable,
                                 List<FirmaResponse> signatures, Verificacion.Paso nextStep, String nextSigner,
                                 boolean canSign, CertificadoResponse storedCertificate) {}

    private final SyllabusService syllabusService;
    private final SyllabusRepository syllabus;
    private final VersionadoService versionado;
    private final FirmaSyllabusRepository firmas;
    private final CertificadoFirmaRepository certificados;
    private final UsuarioRepository usuarios;
    private final AlmeraPdf almera;
    private final FirmaPdf firmaPdf;
    private final ApplicationEventPublisher eventos;

    public FirmaService(SyllabusService syllabusService, SyllabusRepository syllabus, VersionadoService versionado,
                        FirmaSyllabusRepository firmas, CertificadoFirmaRepository certificados,
                        UsuarioRepository usuarios, AlmeraPdf almera, FirmaPdf firmaPdf,
                        ApplicationEventPublisher eventos) {
        this.syllabusService = syllabusService;
        this.syllabus = syllabus;
        this.versionado = versionado;
        this.firmas = firmas;
        this.certificados = certificados;
        this.usuarios = usuarios;
        this.almera = almera;
        this.firmaPdf = firmaPdf;
        this.eventos = eventos;
    }

    @Transactional(readOnly = true)
    public FirmasResponse estado(UsuarioAutenticado actor, UUID id) {
        Syllabus s = syllabus.findDetalleById(id).orElseThrow(FirmaService::noEncontrado);
        int version = versionado.actual(id);
        List<FirmaSyllabus> hechas = firmas.findBySyllabusIdAndVersionNumberOrderBySignedAt(id, version);
        Verificacion.Paso siguiente = siguiente(hechas);
        boolean firmable = s.getStatus() == Syllabus.Estado.ACTIVO;
        return new FirmasResponse(
                Verificacion.estado(hechas.stream().map(f -> f.getStep().name()).toList()), version, firmable,
                hechas.stream().map(f -> new FirmaResponse(f.getStep(), f.getSignerName(), f.getCertificateSubject(),
                        f.getCertificateIssuer(), f.getSignedAt())).toList(),
                siguiente, siguiente == null ? null : cargo(siguiente, s.getAsignatura()),
                firmable && siguiente != null && puedeFirmar(actor, siguiente, s.getAsignatura()),
                certificadoGuardado(actor.id()).orElse(null));
    }

    /** PDF que se va a firmar en el paso pendiente, para revisarlo antes de firmar. */
    @Transactional(readOnly = true)
    public byte[] vistaPrevia(UsuarioAutenticado actor, UUID id) {
        Syllabus s = syllabus.findDetalleById(id).orElseThrow(FirmaService::noEncontrado);
        int version = versionado.actual(id);
        List<FirmaSyllabus> hechas = firmas.findBySyllabusIdAndVersionNumberOrderBySignedAt(id, version);
        exigirTurno(actor, s, siguiente(hechas));
        return documentoPorFirmar(actor, s, hechas);
    }

    /**
     * Firma la versión indicada, que debe seguir siendo la vigente. El certificado es el archivo
     * enviado o, si no se envía, el registrado por el usuario; recordar lo deja registrado.
     */
    @Transactional
    public FirmasResponse firmar(UsuarioAutenticado actor, UUID id, int version, String contrasena,
                                 MultipartFile archivo, boolean recordar) {
        Syllabus s = syllabus.findParaEditarById(id).orElseThrow(FirmaService::noEncontrado);
        int actual = versionado.actual(id);
        if (version != actual) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El syllabus cambió (versión " + actual
                    + ") mientras lo revisaba. Recargue la página y revise la versión vigente antes de firmar.");
        }
        List<FirmaSyllabus> hechas = firmas.findBySyllabusIdAndVersionNumberOrderBySignedAt(id, actual);
        Verificacion.Paso paso = siguiente(hechas);
        exigirTurno(actor, s, paso);

        byte[] p12 = archivo == null || archivo.isEmpty() ? certificadoRegistrado(actor.id()) : bytes(archivo);
        FirmaPdf.Certificado certificado = firmaPdf.abrir(p12, contrasena.toCharArray());
        if (recordar && archivo != null && !archivo.isEmpty()) {
            registrar(actor.id(), p12, certificado);
        }
        String cargo = cargo(paso, s.getAsignatura());
        byte[] firmado = firmaPdf.firmar(documentoPorFirmar(actor, s, hechas), certificado,
                paso == Verificacion.Paso.DEPARTAMENTO ? ROTULO_COORDINACION : ROTULO_REGISTRO, cargo,
                "Aprobación del syllabus " + s.getAsignatura().getCode() + " " + s.getAcademicPeriod()
                        + " (versión " + actual + ")");
        Usuario firmante = usuarios.findById(actor.id()).orElseThrow();
        firmas.save(new FirmaSyllabus(id, actual, paso, firmante, certificado, firmado));
        eventos.publishEvent(AuditEvent.de(actor.id(), "SIGN", "syllabus", id).con(Map.of(
                "title", s.getTitle(), "versionNumber", actual, "step", paso.name(),
                "certificate", certificado.titular())));
        firmas.flush();
        return estado(actor, id);
    }

    /** PDF firmado por coordinación y Registro de la versión vigente, si el syllabus está verificado. */
    @Transactional(readOnly = true)
    public Optional<byte[]> documentoVerificado(UUID id) {
        int version = versionado.actual(id);
        return firmas.findBySyllabusIdAndVersionNumberOrderBySignedAt(id, version).stream()
                .filter(f -> f.getStep() == Verificacion.Paso.REGISTRO)
                .map(FirmaSyllabus::getDocument)
                .findFirst();
    }

    // ---------- Certificado registrado ----------

    @Transactional(readOnly = true)
    public Optional<CertificadoResponse> certificadoGuardado(UUID usuario) {
        return certificados.findById(usuario).map(c -> new CertificadoResponse(c.getCertificateSubject(),
                c.getCertificateIssuer(), c.getCertificateSerial(), c.getNotAfter(), c.getUploadedAt()));
    }

    /** Registra (o reemplaza) el certificado del usuario; la contraseña solo se usa para comprobarlo. */
    @Transactional
    public CertificadoResponse guardarCertificado(UUID usuario, MultipartFile archivo, String contrasena) {
        byte[] p12 = bytes(archivo);
        registrar(usuario, p12, firmaPdf.abrir(p12, contrasena.toCharArray()));
        return certificadoGuardado(usuario).orElseThrow();
    }

    @Transactional
    public void eliminarCertificado(UUID usuario) {
        if (certificados.existsById(usuario)) {
            certificados.deleteById(usuario);
            eventos.publishEvent(AuditEvent.de(usuario, "DELETE", "certificate", usuario)
                    .con(Map.of("name", "Certificado de firma digital")));
        }
    }

    private void registrar(UUID usuario, byte[] p12, FirmaPdf.Certificado c) {
        CertificadoFirma registro = certificados.findById(usuario).orElseGet(() -> new CertificadoFirma(usuario));
        registro.reemplazar(p12, c);
        certificados.save(registro);
        eventos.publishEvent(AuditEvent.de(usuario, "UPDATE", "certificate", usuario).con(Map.of(
                "name", "Certificado de firma digital", "certificate", c.titular(), "notAfter", c.vence().toString())));
    }

    private byte[] certificadoRegistrado(UUID usuario) {
        return certificados.findById(usuario).map(CertificadoFirma::getFile)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Adjunte su certificado de firma (.p12 o .pfx): no tiene uno registrado."));
    }

    // ---------- Reglas ----------

    private byte[] documentoPorFirmar(UsuarioAutenticado actor, Syllabus s, List<FirmaSyllabus> hechas) {
        if (!hechas.isEmpty()) {
            return hechas.getLast().getDocument();
        }
        return almera.syllabus(syllabusService.obtener(actor, s.getId()), List.of(
                new CasillaFirma(ROTULO_COORDINACION, cargo(Verificacion.Paso.DEPARTAMENTO, s.getAsignatura())),
                new CasillaFirma(ROTULO_REGISTRO, CARGO_REGISTRO)));
    }

    private void exigirTurno(UsuarioAutenticado actor, Syllabus s, Verificacion.Paso paso) {
        if (s.getStatus() != Syllabus.Estado.ACTIVO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Solo se firma un syllabus activo (vigente). Actívelo antes de enviarlo a firma.");
        }
        if (paso == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El syllabus ya está verificado.");
        }
        if (!puedeFirmar(actor, paso, s.getAsignatura())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "La firma pendiente le corresponde a: " + cargo(paso, s.getAsignatura()) + ".");
        }
    }

    /**
     * Primer paso: Coordinador del Departamento que ofrece la asignatura, o Coordinador del Programa
     * si es de posgrado. Segundo paso: Registro Académico. El Administrador no firma.
     */
    private boolean puedeFirmar(UsuarioAutenticado actor, Verificacion.Paso paso, Asignatura a) {
        if (!actor.permisos().contains("syllabus:sign")) {
            return false;
        }
        Usuario u = usuarios.findDetalleById(actor.id()).orElseThrow();
        if (paso == Verificacion.Paso.REGISTRO) {
            return u.getRoles().contains(Rol.ADM_REGISTRO);
        }
        if (a.getDepartamento() != null) {
            return u.getRoles().contains(Rol.COORDINADOR_DEPARTAMENTO)
                    && u.getDepartamentos().stream().anyMatch(d -> d.getId().equals(a.getDepartamento().getId()));
        }
        return u.getRoles().contains(Rol.COORDINADOR_PROGRAMA)
                && u.getProgramas().stream().anyMatch(p -> p.getId().equals(a.getPrograma().getId()));
    }

    private static Verificacion.Paso siguiente(List<FirmaSyllabus> hechas) {
        if (hechas.stream().anyMatch(f -> f.getStep() == Verificacion.Paso.REGISTRO)) {
            return null;
        }
        return hechas.isEmpty() ? Verificacion.Paso.DEPARTAMENTO : Verificacion.Paso.REGISTRO;
    }

    private static String cargo(Verificacion.Paso paso, Asignatura a) {
        if (paso == Verificacion.Paso.REGISTRO) {
            return CARGO_REGISTRO;
        }
        return a.getDepartamento() != null
                ? "Coordinación del " + a.getDepartamento().getName()
                : "Coordinación de " + a.getPrograma().getName();
    }

    private static byte[] bytes(MultipartFile archivo) {
        try {
            return archivo.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static ResponseStatusException noEncontrado() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Syllabus no encontrado.");
    }
}
