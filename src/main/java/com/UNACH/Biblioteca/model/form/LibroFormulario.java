package com.UNACH.Biblioteca.model.form;

import com.UNACH.Biblioteca.model.Libro;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Solo se reciben los campos editables; el ID siempre procede de la ruta.
public class LibroFormulario {
    @NotBlank(message = "El título es obligatorio.")
    @Size(max = 16000, message = "El título es demasiado largo.")
    private String titulo;

    @Size(max = 16000, message = "El autor es demasiado largo.")
    private String autor;

    @NotBlank(message = "La descripción del inventario es obligatoria.")
    @Size(max = 16000, message = "La descripción es demasiado larga.")
    private String descripcionOriginal;

    @NotBlank(message = "El código de barras es obligatorio.")
    @Size(max = 50, message = "El código de barras admite hasta 50 caracteres.")
    private String codigoBarras;

    public static LibroFormulario de(Libro libro) {
        LibroFormulario datos = new LibroFormulario();
        datos.setTitulo(libro.getTitulo());
        datos.setAutor(libro.getAutor());
        datos.setDescripcionOriginal(libro.getDescripcionOriginal());
        datos.setCodigoBarras(libro.getCodigoBarras());
        return datos;
    }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getAutor() { return autor; }
    public void setAutor(String autor) { this.autor = autor; }
    public String getDescripcionOriginal() { return descripcionOriginal; }
    public void setDescripcionOriginal(String descripcionOriginal) { this.descripcionOriginal = descripcionOriginal; }
    public String getCodigoBarras() { return codigoBarras; }
    public void setCodigoBarras(String codigoBarras) { this.codigoBarras = codigoBarras; }
}
