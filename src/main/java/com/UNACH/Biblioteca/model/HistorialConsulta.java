package com.UNACH.Biblioteca.model;

import org.springframework.data.domain.Page;
public record HistorialConsulta(Page<HistorialRegistro> registros, ResumenHistorial resumen) { }
