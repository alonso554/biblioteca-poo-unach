package com.UNACH.Biblioteca.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "historial_prestamos")
public class HistorialPrestamo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "prestamo_id", nullable = false)
    private Prestamo prestamo;
    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario administrador;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AccionPrestamo accion;
    @Column(nullable = false)
    private LocalDateTime fecha;
    @Column(length = 1000)
    private String observacion;

    public HistorialPrestamo() { }
    public Long getId() { return id; }
    public Prestamo getPrestamo() { return prestamo; }
    public void setPrestamo(Prestamo prestamo) { this.prestamo = prestamo; }
    public Usuario getAdministrador() { return administrador; }
    public void setAdministrador(Usuario administrador) { this.administrador = administrador; }
    public AccionPrestamo getAccion() { return accion; }
    public void setAccion(AccionPrestamo accion) { this.accion = accion; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
}
