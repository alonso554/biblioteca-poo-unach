package com.UNACH.Biblioteca.model.form;

import com.UNACH.Biblioteca.model.Rol;
import com.UNACH.Biblioteca.model.Usuario;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UsuarioFormulario {
    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 100, message = "El nombre admite hasta 100 caracteres.")
    private String nombre;

    @NotBlank(message = "El identificador de usuario es obligatorio.")
    @Size(max = 50, message = "El identificador admite hasta 50 caracteres.")
    private String usuario;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String clave;

    @NotNull(message = "El rol es obligatorio.")
    private Rol rol = Rol.USUARIO;

    public static UsuarioFormulario de(Usuario usuario) {
        UsuarioFormulario datos = new UsuarioFormulario();
        datos.setNombre(usuario.getNombre());
        datos.setUsuario(usuario.getUsuario());
        datos.setRol(usuario.getRol());
        return datos;
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }
    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }
}
