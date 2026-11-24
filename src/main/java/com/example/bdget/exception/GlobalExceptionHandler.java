package com.example.bdget.exception;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Intercepta las excepciones de los controllers y devuelve JSON estructurado.
 *
 * Extiende ResponseEntityExceptionHandler para que los errores propios de Spring MVC
 * (ruta inexistente = 404, método no permitido = 405, JSON mal formado = 400...) conserven
 * su código HTTP correcto. Sin esto, el handler genérico los convertiría en 500 y
 * contaminarían la métrica 5XX que vigila la alarma de CloudWatch.
 */
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // Errores de validación (@Valid falla en el body del request) → 400
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            fieldErrors.put(error.getField(), error.getDefaultMessage())
        );

        Map<String, Object> response = body(HttpStatus.BAD_REQUEST);
        response.put("errors", fieldErrors);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // Base de datos caída o inaccesible: 503 (y no 400) para que el service mesh /
    // CloudWatch lo cuenten como error de servidor (5XX) y disparen las alarmas.
    @ExceptionHandler({DataAccessException.class, TransactionException.class})
    public ResponseEntity<Object> handleDatabaseUnavailable(Exception ex) {
        Map<String, Object> response = body(HttpStatus.SERVICE_UNAVAILABLE);
        response.put("message", "Base de datos no disponible");
        return new ResponseEntity<>(response, HttpStatus.SERVICE_UNAVAILABLE);
    }

    // Errores de lógica de negocio (RuntimeException) → 400
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Object> handleRuntimeException(RuntimeException ex) {
        Map<String, Object> response = body(HttpStatus.BAD_REQUEST);
        response.put("message", ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // Cualquier otro error inesperado → 500.
    // No se devuelve ex.getMessage(): filtrar detalles internos es una vulnerabilidad
    // (exposición de información) que SonarCloud y los escáneres de seguridad reportan.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneralException(Exception ex) {
        Map<String, Object> response = body(HttpStatus.INTERNAL_SERVER_ERROR);
        response.put("message", "Ocurrió un error inesperado");
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private Map<String, Object> body(HttpStatus status) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", status.value());
        return response;
    }
}
