package com.UNACH.Biblioteca;

import com.UNACH.Biblioteca.controller.ErroresControllerAdvice;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.junit.jupiter.api.Assertions.*;

class ErroresControllerAdviceTests {
    @Test
    void relacionQueImpideEliminarDevuelveConflictoSinExponerDatosInternos() {
        var solicitud = new MockHttpServletRequest("DELETE", "/api/libros/1");
        var respuesta = (ResponseEntity<?>) new ErroresControllerAdvice().integridad(
                new DataIntegrityViolationException("Detalle interno que no debe salir"), solicitud);
        assertEquals(409, respuesta.getStatusCode().value());
        var error = (ErroresControllerAdvice.ErrorRespuesta) respuesta.getBody();
        assertTrue(error.mensaje().contains("registros relacionados"));
        assertFalse(error.mensaje().contains("Detalle interno"));
    }
}
