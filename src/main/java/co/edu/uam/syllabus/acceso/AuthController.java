package co.edu.uam.syllabus.acceso;

import co.edu.uam.syllabus.common.security.UsuarioAutenticado;
import co.edu.uam.syllabus.usuarios.UsuarioResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    public record LoginResponse(String message, String token, UsuarioResponse user) {}

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres") String newPassword) {}

    public record ForgotPasswordRequest(@NotBlank @Email String email) {}

    public record ResetPasswordRequest(
            @NotBlank String token,
            @NotBlank @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres") String newPassword) {}

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        AuthService.Sesion sesion = authService.login(request.email(), request.password());
        return new LoginResponse("Inicio de sesión exitoso", sesion.token(), UsuarioResponse.from(sesion.usuario()));
    }

    /** JWT sin estado: el cliente descarta el token. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UsuarioAutenticado actual) {
        authService.logout(actual.id());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UsuarioResponse me(@AuthenticationPrincipal UsuarioAutenticado actual) {
        return UsuarioResponse.from(authService.perfil(actual.id()));
    }

    @PostMapping("/change-password")
    public Map<String, String> changePassword(@AuthenticationPrincipal UsuarioAutenticado actual,
                                              @Valid @RequestBody ChangePasswordRequest request) {
        authService.cambiarPassword(actual.id(), request.currentPassword(), request.newPassword());
        return Map.of("message", "Contraseña actualizada correctamente.");
    }

    @PostMapping("/forgot-password")
    public Map<String, String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.solicitarRecuperacion(request.email());
        return Map.of("message", "Si el correo está registrado, recibirá un enlace para restablecer su contraseña.");
    }

    @PostMapping("/reset-password")
    public Map<String, String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.restablecerPassword(request.token(), request.newPassword());
        return Map.of("message", "Contraseña restablecida. Ya puede iniciar sesión.");
    }
}
