package co.edu.uam.syllabus.usuarios;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Set;

/**
 * Datos básicos y roles de un usuario (UC3). La contraseña solo es obligatoria al crear;
 * al editar, si llega, se restablece. isActive nulo se interpreta como "sin cambio".
 */
public record UsuarioRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Email @Size(max = 150) String email,
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres") String password,
        @NotEmpty(message = "Seleccione al menos un rol") Set<Rol> roles,
        Usuario.Genero gender,
        @Size(max = 30) String phone,
        @Size(max = 30) String studentCode,
        @Size(max = 30) String employeeCode,
        Boolean isActive) {}
