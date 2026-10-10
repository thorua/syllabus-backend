package co.edu.uam.syllabus.catalogos;

import co.edu.uam.syllabus.auditoria.AuditEvent;
import co.edu.uam.syllabus.usuarios.Usuario;
import co.edu.uam.syllabus.usuarios.UsuarioRepository;
import co.edu.uam.syllabus.usuarios.UsuarioResponse;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * UC9: vincula a un profesor (o coordinador) con varios departamentos y programas, de pregrado y
 * posgrado. Las afiliaciones definen su alcance (ScopeService) y a quién se puede asignar una asignatura.
 */
@Service
public class AfiliacionService {

    private final UsuarioRepository usuarios;
    private final DepartamentoRepository departamentos;
    private final ProgramaRepository programas;
    private final ApplicationEventPublisher eventos;

    public AfiliacionService(UsuarioRepository usuarios, DepartamentoRepository departamentos,
                             ProgramaRepository programas, ApplicationEventPublisher eventos) {
        this.usuarios = usuarios;
        this.departamentos = departamentos;
        this.programas = programas;
        this.eventos = eventos;
    }

    @Transactional
    public UsuarioResponse actualizar(UUID actor, UUID usuarioId, Set<UUID> deptoIds, Set<UUID> programaIds) {
        Usuario usuario = usuarios.findDetalleById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado."));
        List<Departamento> deptos = departamentos.findAllById(deptoIds);
        List<Programa> progs = programas.findAllById(programaIds);
        if (deptos.size() != deptoIds.size() || progs.size() != programaIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Algún departamento o programa no existe.");
        }
        usuario.getDepartamentos().clear();
        usuario.getDepartamentos().addAll(deptos);
        usuario.getProgramas().clear();
        usuario.getProgramas().addAll(progs);
        eventos.publishEvent(AuditEvent.de(actor, "AFFILIATIONS_UPDATE", "user", usuarioId).con(Map.of(
                "email", usuario.getEmail(),
                "departments", deptos.stream().map(Departamento::getCode).sorted().toList(),
                "programs", progs.stream().map(Programa::getCode).sorted().toList())));
        return UsuarioResponse.from(usuario);
    }
}
