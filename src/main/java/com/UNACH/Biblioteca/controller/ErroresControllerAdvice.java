package com.UNACH.Biblioteca.controller;

import com.UNACH.Biblioteca.service.ReglaNegocioException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import java.util.stream.Collectors;

@ControllerAdvice
public class ErroresControllerAdvice {
    public record ErrorRespuesta(int estado, String mensaje) { }

    @ExceptionHandler(ReglaNegocioException.class)
    public Object negocio(ReglaNegocioException error, HttpServletRequest solicitud) {
        return responder(error.getEstado(), error.getMessage(), solicitud);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public Object integridad(DataIntegrityViolationException error, HttpServletRequest solicitud) {
        return responder(HttpStatus.CONFLICT,
                "No se pudo guardar o eliminar: hay un dato duplicado o registros relacionados que lo impiden.", solicitud);
    }

    @ExceptionHandler(DataAccessException.class)
    public Object conexion(DataAccessException error, HttpServletRequest solicitud) {
        return responder(HttpStatus.SERVICE_UNAVAILABLE,
                "No se pudo acceder a la base de datos. Revisa la conexión y la estructura de las tablas.", solicitud);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object campos(MethodArgumentNotValidException error, HttpServletRequest solicitud) {
        String mensaje = error.getBindingResult().getAllErrors().stream()
                .map(e -> e.getDefaultMessage()).collect(Collectors.joining(" "));
        return responder(HttpStatus.BAD_REQUEST, mensaje, solicitud);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Object validacion(ConstraintViolationException error, HttpServletRequest solicitud) {
        String mensaje = error.getConstraintViolations().stream()
                .map(e -> e.getMessage()).collect(Collectors.joining(" "));
        return responder(HttpStatus.BAD_REQUEST, mensaje, solicitud);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public Object formato(Exception error, HttpServletRequest solicitud) {
        return responder(HttpStatus.BAD_REQUEST,
                "Solicitud inválida. Revisa los campos, el rol y los identificadores numéricos.", solicitud);
    }

    private Object responder(HttpStatus estado, String mensaje, HttpServletRequest solicitud) {
        if (solicitud.getRequestURI().startsWith(solicitud.getContextPath() + "/api/")) {
            return ResponseEntity.status(estado).body(new ErrorRespuesta(estado.value(), mensaje));
        }
        ModelAndView vista = new ModelAndView("fallo");
        vista.setStatus(estado);
        vista.addObject("estado", estado.value());
        vista.addObject("mensaje", mensaje);
        return vista;
    }
}
