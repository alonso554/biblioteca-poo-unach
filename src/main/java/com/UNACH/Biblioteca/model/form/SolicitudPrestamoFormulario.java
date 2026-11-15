package com.UNACH.Biblioteca.model.form;

import jakarta.validation.constraints.*;

public class SolicitudPrestamoFormulario {
    @NotBlank(message = "La matrícula es obligatoria.")
    @Size(max = 50, message = "La matrícula admite hasta 50 caracteres.")
    private String matricula;
    @NotBlank(message = "El nombre completo es obligatorio.")
    @Size(max = 200, message = "El nombre admite hasta 200 caracteres.")
    private String nombreCompleto;
    @NotBlank(message = "El correo institucional es obligatorio.")
    @Email(message = "Escribe un correo institucional válido.")
    @Size(max = 150, message = "El correo admite hasta 150 caracteres.")
    private String correoInstitucional;
    @NotBlank(message = "El código de barras es obligatorio.")
    @Size(max = 50, message = "El código admite hasta 50 caracteres.")
    private String codigoBarras;

    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }
    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }
    public String getCorreoInstitucional() { return correoInstitucional; }
    public void setCorreoInstitucional(String correoInstitucional) { this.correoInstitucional = correoInstitucional; }
    public String getCodigoBarras() { return codigoBarras; }
    public void setCodigoBarras(String codigoBarras) { this.codigoBarras = codigoBarras; }
}
