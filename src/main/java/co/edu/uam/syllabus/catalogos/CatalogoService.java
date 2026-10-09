package co.edu.uam.syllabus.catalogos;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Catálogos académicos: departamentos (UC5) y programas (UC6). */
@Service
public class CatalogoService {

    private static final Sort POR_NOMBRE = Sort.by("name");

    private final DepartamentoRepository departamentos;
    private final ProgramaRepository programas;

    public CatalogoService(DepartamentoRepository departamentos, ProgramaRepository programas) {
        this.departamentos = departamentos;
        this.programas = programas;
    }

    @Transactional(readOnly = true)
    public List<DepartamentoResponse> departamentos(boolean incluirInactivos) {
        return departamentos.findAll(POR_NOMBRE).stream()
                .filter(d -> incluirInactivos || d.isActive())
                .map(DepartamentoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProgramaResponse> programas(UUID departamentoId, boolean incluirInactivos) {
        return programas.findAll(POR_NOMBRE).stream()
                .filter(p -> incluirInactivos || p.isActive())
                .filter(p -> departamentoId == null
                        || (p.getDepartamento() != null && p.getDepartamento().getId().equals(departamentoId)))
                .map(ProgramaResponse::from)
                .toList();
    }
}
