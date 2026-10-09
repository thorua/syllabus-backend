package co.edu.uam.syllabus.common;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Errores en formato RFC 7807 (ProblemDetail). Se añade la propiedad "error" con el mensaje,
 * que es la que lee el frontend.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail accesoDenegado() {
        return problema(HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta acción.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail conflicto() {
        return problema(HttpStatus.CONFLICT, "La operación entra en conflicto con datos existentes.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> campos.putIfAbsent(e.getField(), e.getDefaultMessage()));
        ProblemDetail body = problema(HttpStatus.BAD_REQUEST, "Datos inválidos.");
        body.setProperty("details", campos);
        return ResponseEntity.badRequest().body(body);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        ResponseEntity<Object> respuesta = super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (respuesta != null && respuesta.getBody() instanceof ProblemDetail detalle) {
            detalle.setProperty("error", detalle.getDetail() != null ? detalle.getDetail() : detalle.getTitle());
        }
        return respuesta;
    }

    private static ProblemDetail problema(HttpStatus status, String mensaje) {
        ProblemDetail detalle = ProblemDetail.forStatusAndDetail(status, mensaje);
        detalle.setProperty("error", mensaje);
        return detalle;
    }
}
