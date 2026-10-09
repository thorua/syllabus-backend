package co.edu.uam.syllabus.common.security;

import java.util.Set;
import java.util.UUID;

/** Principal de la petición: identidad y permisos efectivos del usuario. */
public record UsuarioAutenticado(UUID id, String email, Set<String> permisos) {}
