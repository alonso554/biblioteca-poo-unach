package com.UNACH.Biblioteca.model.form;

import jakarta.validation.constraints.*;
public class ResolucionIncidenciaFormulario {
    @NotBlank(message = "Describe cómo se resolvió la incidencia.") @Size(max = 2000, message = "La solución admite hasta 2,000 caracteres.") private String solucion;
    public String getSolucion() { return solucion; } public void setSolucion(String v) { solucion = v; }
}
