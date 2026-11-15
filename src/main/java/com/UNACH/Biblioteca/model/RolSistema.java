package com.UNACH.Biblioteca.model;

import jakarta.persistence.*;

@Entity
@Table(name = "roles")
public class RolSistema {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 20)
    private Rol nombre;

    public RolSistema() { }
    public Long getId() { return id; }
    public Rol getNombre() { return nombre; }
}
