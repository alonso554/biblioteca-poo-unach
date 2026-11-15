package com.UNACH.Biblioteca.model;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "prestamos")
public class Prestamo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "lector_id", nullable = false)
    private Lector lector;
    @ManyToOne(optional = false)
    @JoinColumn(name = "inventario_libro_id", nullable = false)
    private Libro libro;
    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario administrador;
    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime fechaSolicitud;
    @Column(name = "fecha_prestamo")
    private LocalDateTime fechaPrestamo;
    @Column(name = "fecha_limite_devolucion")
    private LocalDate fechaLimiteDevolucion;
    @Column(name = "fecha_devolucion")
    private LocalDateTime fechaDevolucion;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPrestamo estado;

    public Prestamo() { }
    public Long getId() { return id; }
    public Lector getLector() { return lector; }
    public void setLector(Lector lector) { this.lector = lector; }
    public Libro getLibro() { return libro; }
    public void setLibro(Libro libro) { this.libro = libro; }
    public Usuario getAdministrador() { return administrador; }
    public void setAdministrador(Usuario administrador) { this.administrador = administrador; }
    public LocalDateTime getFechaSolicitud() { return fechaSolicitud; }
    public void setFechaSolicitud(LocalDateTime fechaSolicitud) { this.fechaSolicitud = fechaSolicitud; }
    public LocalDateTime getFechaPrestamo() { return fechaPrestamo; }
    public void setFechaPrestamo(LocalDateTime fechaPrestamo) { this.fechaPrestamo = fechaPrestamo; }
    public LocalDate getFechaLimiteDevolucion() { return fechaLimiteDevolucion; }
    public void setFechaLimiteDevolucion(LocalDate fechaLimiteDevolucion) { this.fechaLimiteDevolucion = fechaLimiteDevolucion; }
    public LocalDateTime getFechaDevolucion() { return fechaDevolucion; }
    public void setFechaDevolucion(LocalDateTime fechaDevolucion) { this.fechaDevolucion = fechaDevolucion; }
    public EstadoPrestamo getEstado() { return estado; }
    public void setEstado(EstadoPrestamo estado) { this.estado = estado; }
}
