package com.UNACH.Biblioteca.model;

public record ResumenHistorial(long solicitudes, long entregados, long activos, long vencidos,
        long devolucionesTardias, long lectoresDistintos, long incidenciasPendientes,
        long incidenciasResueltas, long danosOPerdidas) { }
