package com.UNACH.Biblioteca.model.form;

import com.UNACH.Biblioteca.model.TipoIncidencia;
import jakarta.validation.constraints.*;

public class IncidenciaFormulario {
    @NotNull(message = "Selecciona el tipo de incidencia.") private TipoIncidencia tipo;
    @NotBlank(message = "Describe la incidencia.") @Size(max = 2000, message = "La descripción admite hasta 2,000 caracteres.") private String descripcion;
    public TipoIncidencia getTipo() { return tipo; } public void setTipo(TipoIncidencia v) { tipo = v; }
    public String getDescripcion() { return descripcion; } public void setDescripcion(String v) { descripcion = v; }
}
