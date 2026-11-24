package com.example.bdget.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @SuppressWarnings("unchecked")
    private Map<String, Object> bodyOf(ResponseEntity<Object> response) {
        assertNotNull(response.getBody());
        return (Map<String, Object>) response.getBody();
    }

    @Test
    void databaseUnavailable_returns503() {
        ResponseEntity<Object> response =
                handler.handleDatabaseUnavailable(new DataAccessResourceFailureException("MySQL caído"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals(503, bodyOf(response).get("status"));
        assertEquals("Base de datos no disponible", bodyOf(response).get("message"));
    }

    @Test
    void runtimeException_returns400WithMessage() {
        ResponseEntity<Object> response =
                handler.handleRuntimeException(new IllegalArgumentException("dato inválido"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("dato inválido", bodyOf(response).get("message"));
    }

    @Test
    void unexpectedException_returns500WithoutLeakingDetails() {
        ResponseEntity<Object> response =
                handler.handleGeneralException(new Exception("detalle interno secreto"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Ocurrió un error inesperado", bodyOf(response).get("message"));
        assertFalse(bodyOf(response).containsKey("details"));
        assertFalse(bodyOf(response).toString().contains("secreto"));
    }
}
