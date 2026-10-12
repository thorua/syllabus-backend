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
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
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

    /** Syllabus activos con la versión vigente y los pasos ya firmados en ella. */
    private static final String PENDIENTES = """
            SELECT y.id, y.title, y.academic_period, y.updated_at, s.code, s.name, s.department_id, s.program_id,
                   coalesce(d.name, p.name) AS responsable, v.version,
                   coalesce(bool_or(f.step = 'DEPARTAMENTO'), FALSE) AS departamento,
                   coalesce(bool_or(f.step = 'REGISTRO'), FALSE) AS registro
            FROM syllabus y
            JOIN subjects s ON s.id = y.subject_id
            LEFT JOIN departments d ON d.id = s.department_id
            LEFT JOIN programs p ON p.id = s.program_id
            JOIN LATERAL (SELECT max(version_number) AS version FROM syllabus_versions
                          WHERE syllabus_id = y.id) v ON TRUE
            LEFT JOIN syllabus_signatures f ON f.syllabus_id = y.id AND f.version_number = v.version
            WHERE y.status = 'ACTIVO'
            GROUP BY y.id, s.id, d.name, p.name, v.version
            ORDER BY y.updated_at""";

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

    /** Syllabus vigente que espera la firma del usuario en la versión indicada. */
    public record PendienteResponse(UUID id, String title, String academicPeriod, String subjectCode,
                                    String subjectName, String offeredBy, int version, Verificacion.Paso step,
                                    Instant updatedAt) {}

    public record ItemLote(UUID id, int version) {}

    /** Resultado de cada syllabus de una firma por lote. */
    public record ResultadoLote(UUID id, boolean signed, String message, Verificacion.Estado state) {}

    private final SyllabusService syllabusService;
    private final SyllabusRepository syllabus;
    private final VersionadoService versionado;
    private final FirmaSyllabusRepository firmas;
    private final CertificadoFirmaRepository certificados;
    private final UsuarioRepository usuarios;
    private final AlmeraPdf almera;
    private final FirmaPdf firmaPdf;
    private final JdbcClient jdbc;
    private final TransactionTemplate transaccion;
    private final ApplicationEventPublisher eventos;

    public FirmaService(SyllabusService syllabusService, SyllabusRepository syllabus, VersionadoService versionado,
                        FirmaSyllabusRepository firmas, CertificadoFirmaRepository certificados,
                        UsuarioRepository usuarios, AlmeraPdf almera, FirmaPdf firmaPdf, JdbcClient jdbc,
                        PlatformTransactionManager transacciones, ApplicationEventPublisher eventos) {
        this.syllabusService = syllabusService;
        this.syllabus = syllabus;
        this.versionado = versionado;
        this.firmas = firmas;
        this.certificados = certificados;
        this.usuarios = usuarios;
        this.almera = almera;
        this.firmaPdf = firmaPdf;
        this.jdbc = jdbc;
        this.transaccion = new TransactionTemplate(transacciones);
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
        // Primero a quién le corresponde la firma; después, el certificado.
        Syllabus s = syllabus.findDetalleById(id).orElseThrow(FirmaService::noEncontrado);
        exigirTurno(actor, s, siguiente(firmas.findBySyllabusIdAndVersionNumberOrderBySignedAt(id,
                versionado.actual(id))));
        byte[] p12 = archivo == null || archivo.isEmpty() ? certificadoRegistrado(actor.id()) : bytes(archivo);
        FirmaPdf.Certificado certificado = firmaPdf.abrir(p12, contrasena.toCharArray());
        if (recordar && archivo != null && !archivo.isEmpty()) {
            registrar(actor.id(), p12, certificado);
        }
        firmarCon(actor, id, version, certificado);
        return estado(actor, id);
    }

    /**
     * Firma varios syllabus con el mismo certificado; cada uno en su propia transacción, de modo que
     * si uno falla (p. ej. cambió mientras se revisaba) los demás quedan firmados.
     */
    public List<ResultadoLote> firmarLote(UsuarioAutenticado actor, List<ItemLote> items, String contrasena,
                                         MultipartFile archivo) {
        byte[] p12 = archivo == null || archivo.isEmpty() ? certificadoRegistrado(actor.id()) : bytes(archivo);
        FirmaPdf.Certificado certificado = firmaPdf.abrir(p12, contrasena.toCharArray());
        return items.stream().map(item -> {
            try {
                Verificacion.Estado estado = transaccion.execute(tx -> {
                    firmarCon(actor, item.id(), item.version(), certificado);
                    return Verificacion.estado(firmas.pasosFirmados(item.id(), item.version()));
                });
                return new ResultadoLote(item.id(), true, "Firmado", estado);
            } catch (ResponseStatusException e) {
                return new ResultadoLote(item.id(), false, e.getReason(), null);
            }
        }).toList();
    }

    /** Syllabus vigentes cuya firma pendiente le corresponde al usuario, los más antiguos primero. */
    @Transactional(readOnly = true)
    public List<PendienteResponse> pendientes(UsuarioAutenticado actor) {
        if (!actor.permisos().contains("syllabus:sign")) {
            return List.of();
        }
        Usuario u = usuarios.findDetalleById(actor.id()).orElseThrow();
        return jdbc.sql(PENDIENTES).query((rs, i) -> {
            if (rs.getBoolean("registro")) {
                return null;
            }
            Verificacion.Paso paso = rs.getBoolean("departamento")
                    ? Verificacion.Paso.REGISTRO : Verificacion.Paso.DEPARTAMENTO;
            if (!leCorresponde(u, paso, rs.getObject("department_id", UUID.class),
                    rs.getObject("program_id", UUID.class))) {
                return null;
            }
            return new PendienteResponse(rs.getObject("id", UUID.class), rs.getString("title"),
                    rs.getString("academic_period"), rs.getString("code"), rs.getString("name"),
                    rs.getString("responsable"), rs.getInt("version"), paso,
                    rs.getTimestamp("updated_at").toInstant());
        }).list().stream().filter(Objects::nonNull).toList();
    }

    private void firmarCon(UsuarioAutenticado actor, UUID id, int version, FirmaPdf.Certificado certificado) {
        Syllabus s = syllabus.findParaEditarById(id).orElseThrow(FirmaService::noEncontrado);
        int actual = versionado.actual(id);
        if (version != actual) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El syllabus cambió (versión " + actual
                    + ") mientras lo revisaba. Recargue la página y revise la versión vigente antes de firmar.");
        }
        List<FirmaSyllabus> hechas = firmas.findBySyllabusIdAndVersionNumberOrderBySignedAt(id, actual);
        Verificacion.Paso paso = siguiente(hechas);
        exigirTurno(actor, s, paso);

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
        if (paso == Verificacion.Paso.DEPARTAMENTO) {
            eventos.publishEvent(new SyllabusPorFirmar(id));
        }
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
        return actor.permisos().contains("syllabus:sign") && leCorresponde(
                usuarios.findDetalleById(actor.id()).orElseThrow(), paso,
                a.getDepartamento() == null ? null : a.getDepartamento().getId(),
                a.getPrograma() == null ? null : a.getPrograma().getId());
    }

    private static boolean leCorresponde(Usuario u, Verificacion.Paso paso, UUID departamento, UUID posgrado) {
        if (paso == Verificacion.Paso.REGISTRO) {
            return u.getRoles().contains(Rol.ADM_REGISTRO);
        }
        if (departamento != null) {
            return u.getRoles().contains(Rol.COORDINADOR_DEPARTAMENTO)
                    && u.getDepartamentos().stream().anyMatch(d -> d.getId().equals(departamento));
        }
        return u.getRoles().contains(Rol.COORDINADOR_PROGRAMA)
                && u.getProgramas().stream().anyMatch(p -> p.getId().equals(posgrado));
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
