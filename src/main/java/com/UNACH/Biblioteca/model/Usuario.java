package com.UNACH.Biblioteca.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

// Cuenta de acceso almacenada en la tabla existente de usuarios.
@Entity
@Table(name = "usuarios")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, unique = true, length = 50)
    private String usuario;

    @JsonIgnore
    @Column(nullable = false, length = 255)
    private String password;

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "rol_id", nullable = false)
    private RolSistema rolSistema;

    public Usuario() { }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    @JsonIgnore
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Rol getRol() { return rolSistema == null ? null : rolSistema.getNombre(); }
    public void setRolSistema(RolSistema rolSistema) {
        this.rolSistema = rolSistema;
    }
}
