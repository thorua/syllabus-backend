package co.edu.uam.syllabus.usuarios;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Catálogo de permisos "recurso:acción" con su descripción (usado por UC4 y por los roles). */
public final class Permisos {

    public static final Map<String, String> CATALOGO = catalogo();

    private Permisos() {}

    private static Map<String, String> catalogo() {
        Map<String, String> c = new LinkedHashMap<>();
        c.put("syllabus:read", "Consultar syllabus");
        c.put("syllabus:create", "Crear syllabus");
        c.put("syllabus:update", "Editar syllabus");
        c.put("syllabus:delete", "Eliminar syllabus (borrado lógico)");
        c.put("syllabus:versions", "Consultar historial de versiones");
        c.put("syllabus:export", "Exportar syllabus en PDF");
        c.put("syllabus:header", "Editar encabezado del syllabus");
        c.put("syllabus:sign", "Firmar digitalmente syllabus (según su cargo)");
        c.put("export:batch", "Exportar syllabus por lote");
        c.put("subject:read", "Consultar asignaturas");
        c.put("subject:create", "Crear asignaturas");
        c.put("subject:update", "Editar asignaturas");
        c.put("subject:delete", "Desactivar asignaturas");
        c.put("subject:assign", "Asignar asignatura a profesor");
        c.put("program:read", "Consultar programas");
        c.put("program:create", "Crear programas");
        c.put("program:update", "Editar programas");
        c.put("program:delete", "Desactivar programas");
        c.put("plan:manage", "Gestionar planes de estudio");
        c.put("faculty:read", "Consultar facultades");
        c.put("faculty:create", "Crear facultades");
        c.put("faculty:update", "Editar facultades");
        c.put("faculty:delete", "Desactivar facultades");
        c.put("department:read", "Consultar departamentos");
        c.put("department:create", "Crear departamentos");
        c.put("department:update", "Editar departamentos");
        c.put("department:delete", "Desactivar departamentos");
        c.put("user:read", "Consultar usuarios");
        c.put("user:create", "Crear usuarios");
        c.put("user:update", "Editar usuarios y roles");
        c.put("user:delete", "Desactivar usuarios");
        c.put("user:permissions", "Gestionar permisos por usuario");
        c.put("user:affiliations", "Gestionar afiliaciones");
        c.put("config:update", "Configurar plantilla y estrategia de evaluación");
        c.put("audit:read", "Ver auditoría de cambios");
        return Collections.unmodifiableMap(c);
    }
}
