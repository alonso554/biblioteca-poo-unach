package com.UNACH.Biblioteca.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "inventario_libros")
public class Libro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "descripcion_original",
            nullable = false, columnDefinition = "TEXT")
    private String descripcionOriginal;

    @Column(columnDefinition = "TEXT")
    private String autor;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String titulo;

    @Column(name = "codigo_barras",
            nullable = false, unique = true, length = 50)
    private String codigoBarras;

    public Libro() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescripcionOriginal() {
        return descripcionOriginal;
    }

    public void setDescripcionOriginal(String descripcionOriginal) {
        this.descripcionOriginal = descripcionOriginal;
    }

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getCodigoBarras() {
        return codigoBarras;
    }

    public void setCodigoBarras(String codigoBarras) {
        this.codigoBarras = codigoBarras;
    }
}