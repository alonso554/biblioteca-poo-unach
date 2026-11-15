package com.UNACH.Biblioteca.model;

import java.util.List;

/** Fila de consulta: no es una tabla; agrupa un préstamo con sus incidencias. */
public record HistorialRegistro(Prestamo prestamo, List<Incidencia> incidencias, long diasAtraso) { }
