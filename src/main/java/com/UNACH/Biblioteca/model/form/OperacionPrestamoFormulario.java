package com.UNACH.Biblioteca.model.form;

import java.time.LocalDate;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

public class OperacionPrestamoFormulario {
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaLimite;
    @Size(max = 1000, message = "La observación admite hasta 1,000 caracteres.")
    private String observacion;

    public LocalDate getFechaLimite() { return fechaLimite; }
    public void setFechaLimite(LocalDate fechaLimite) { this.fechaLimite = fechaLimite; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
}
