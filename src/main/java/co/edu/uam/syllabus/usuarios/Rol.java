package co.edu.uam.syllabus.usuarios;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Roles y permisos según los casos de uso. Cada rol hereda los permisos de su actor general
 * (Profesor ⊂ Estudiante, Coord. Programa ⊂ Profesor, ...). Por decisión institucional,
 * Registro Académico tiene los mismos permisos que el Administrador.
 * El nivel solo ordena los roles para mostrar el principal.
 */
public enum Rol {
    ESTUDIANTE(10, null,
            "syllabus:read", "subject:read", "program:read", "department:read"),
    PROFESOR(20, ESTUDIANTE,
            "syllabus:create", "syllabus:update", "syllabus:export"),
    COORDINADOR_PROGRAMA(30, PROFESOR,
            "syllabus:delete", "syllabus:versions", "subject:assign"),
    COORDINADOR_DEPARTAMENTO(40, COORDINADOR_PROGRAMA,
            "subject:create", "subject:update", "subject:delete", "export:batch"),
    ADMINISTRADOR(50, COORDINADOR_DEPARTAMENTO,
            "user:read", "user:create", "user:update", "user:delete", "user:permissions", "user:affiliations",
            "department:create", "department:update", "department:delete",
            "program:create", "program:update", "program:delete",
            "config:update", "audit:read", "syllabus:header"),
    ADM_REGISTRO(45, ADMINISTRADOR);

    private final int nivel;
    private final Set<String> permisos;

    Rol(int nivel, Rol padre, String... propios) {
        Set<String> todos = new LinkedHashSet<>(padre == null ? Set.of() : padre.permisos);
        todos.addAll(List.of(propios));
        this.nivel = nivel;
        this.permisos = Collections.unmodifiableSet(todos);
    }

    public int nivel() {
        return nivel;
    }

    public Set<String> permisos() {
        return permisos;
    }
}
