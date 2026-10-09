package co.edu.uam.syllabus.common.security;

import co.edu.uam.syllabus.usuarios.Usuario;
import co.edu.uam.syllabus.usuarios.UsuarioRepository;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, UsuarioRepository usuarios) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/login", "/api/auth/forgot-password", "/api/auth/reset-password").permitAll()
                        .requestMatchers("/api/docs/**", "/api/swagger-ui/**", "/actuator/health", "/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(rs -> rs.jwt(jwt -> jwt.jwtAuthenticationConverter(cargarUsuario(usuarios))))
                .build();
    }

    @Bean
    JwtDecoder jwtDecoder(JwtService jwtService) {
        return jwtService.decoder();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Cada petición recarga el usuario: así un borrado lógico o un cambio de roles/permisos
     * surte efecto inmediato sin esperar a que el token expire.
     */
    private static Converter<Jwt, AbstractAuthenticationToken> cargarUsuario(UsuarioRepository usuarios) {
        return jwt -> {
            Usuario usuario = usuarios.findDetalleById(UUID.fromString(jwt.getSubject()))
                    .filter(Usuario::isActive)
                    .orElseThrow(() -> new DisabledException("Usuario inactivo o inexistente"));
            var principal = new UsuarioAutenticado(usuario.getId(), usuario.getEmail(), usuario.permisosEfectivos());
            var authorities = principal.permisos().stream().map(SimpleGrantedAuthority::new).toList();
            return new UsernamePasswordAuthenticationToken(principal, jwt, authorities);
        };
    }
}
