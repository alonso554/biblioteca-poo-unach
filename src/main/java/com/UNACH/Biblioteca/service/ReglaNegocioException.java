package com.UNACH.Biblioteca.service;

import org.springframework.http.HttpStatus;

public class ReglaNegocioException extends RuntimeException {
    private final HttpStatus estado;

    public ReglaNegocioException(HttpStatus estado, String mensaje) {
        super(mensaje);
        this.estado = estado;
    }

    public HttpStatus getEstado() { return estado; }
}
