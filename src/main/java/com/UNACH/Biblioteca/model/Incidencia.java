package com.UNACH.Biblioteca.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "incidencias_prestamos")
public class Incidencia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "prestamo_id", nullable = false)
    private Prestamo prestamo;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private TipoIncidencia tipo;
    @Column(nullable = false, length = 2000) private String descripcion;
    @Column(name = "fecha_registro", nullable = false) private LocalDateTime fechaRegistro;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "administrador_registro_id", nullable = false)
    private Usuario administradorRegistro;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private EstadoIncidencia estado;
    @Column(name = "fecha_resolucion") private LocalDateTime fechaResolucion;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "administrador_resolucion_id")
    private Usuario administradorResolucion;
    @Column(name = "solucion", length = 2000) private String solucion;
    public Long getId() { return id; }
    public Prestamo getPrestamo() { return prestamo; } public void setPrestamo(Prestamo v) { prestamo = v; }
    public TipoIncidencia getTipo() { return tipo; } public void setTipo(TipoIncidencia v) { tipo = v; }
    public String getDescripcion() { return descripcion; } public void setDescripcion(String v) { descripcion = v; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; } public void setFechaRegistro(LocalDateTime v) { fechaRegistro = v; }
    public Usuario getAdministradorRegistro() { return administradorRegistro; } public void setAdministradorRegistro(Usuario v) { administradorRegistro = v; }
    public EstadoIncidencia getEstado() { return estado; } public void setEstado(EstadoIncidencia v) { estado = v; }
    public LocalDateTime getFechaResolucion() { return fechaResolucion; } public void setFechaResolucion(LocalDateTime v) { fechaResolucion = v; }
    public Usuario getAdministradorResolucion() { return administradorResolucion; } public void setAdministradorResolucion(Usuario v) { administradorResolucion = v; }
    public String getSolucion() { return solucion; } public void setSolucion(String v) { solucion = v; }
    public boolean bloqueaDisponibilidad() { return estado == EstadoIncidencia.PENDIENTE && tipo != TipoIncidencia.OTRO; }
}
