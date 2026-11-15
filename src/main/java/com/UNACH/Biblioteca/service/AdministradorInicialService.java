package com.UNACH.Biblioteca.service;

import com.UNACH.Biblioteca.model.Rol;
import com.UNACH.Biblioteca.model.Usuario;
import com.UNACH.Biblioteca.model.form.UsuarioFormulario;
import com.UNACH.Biblioteca.repository.UsuarioRepository;
import jakarta.validation.Validator;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Solo lo invoca el procedimiento local, nunca un controlador HTTP.
@Service
public class AdministradorInicialService {
    public record Resumen(long usuarios, long administradoresCompatibles, long clavesIncompatibles) { }

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final Validator validator;
    private final ApplicationEventPublisher eventos;
    private final RolService roles;

    public AdministradorInicialService(UsuarioRepository usuarios, PasswordEncoder encoder,
            Validator validator, ApplicationEventPublisher eventos, RolService roles) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.validator = validator;
        this.eventos = eventos;
        this.roles = roles;
    }

    @Transactional(readOnly = true)
    public Resumen revisar() {
        var registros = usuarios.findAll();
        long compatibles = registros.stream()
                .filter(u -> u.getRol() == Rol.ADMIN && PoliticaClave.esBCrypt(u.getPassword())).count();
        long incompatibles = registros.stream().filter(u -> !PoliticaClave.esBCrypt(u.getPassword())).count();
        return new Resumen(registros.size(), compatibles, incompatibles);
    }

    @Transactional
    public void crear(String identificador, String nombre, String clave) {
        var administradores = usuarios.bloquearAdministradores();
        if (administradores.stream().anyMatch(u -> PoliticaClave.esBCrypt(u.getPassword()))) {
            throw new ReglaNegocioException(HttpStatus.CONFLICT,
                    "Ya existe un ADMIN con BCrypt compatible. Usa esa cuenta o el restablecimiento explícito.");
        }
        UsuarioFormulario datos = new UsuarioFormulario();
        datos.setUsuario(identificador == null ? "" : identificador.trim());
        datos.setNombre(nombre);
        datos.setRol(Rol.ADMIN);
        if (!validator.validate(datos).isEmpty()) {
            throw new ReglaNegocioException(HttpStatus.BAD_REQUEST, "Indica un identificador y nombre válidos para el administrador.");
        }
        PoliticaClave.validar(clave, true);
        if (usuarios.existsByUsuarioIgnoreCase(datos.getUsuario())) {
            throw new ReglaNegocioException(HttpStatus.CONFLICT,
                    "Ese identificador ya existe. No se sobrescribió ningún registro.");
        }
        Usuario administrador = new Usuario();
        administrador.setNombre(datos.getNombre().trim());
        administrador.setUsuario(datos.getUsuario());
        administrador.setRolSistema(roles.obtener(Rol.ADMIN));
        administrador.setPassword(encoder.encode(clave));
        usuarios.saveAndFlush(administrador);
    }

    @Transactional
    public void restablecer(String identificador, String nuevaClave) {
        usuarios.bloquearAdministradores();
        String nombre = identificador == null ? "" : identificador.trim();
        Usuario administrador = usuarios.findByUsuarioIgnoreCase(nombre).orElseThrow(() ->
                new ReglaNegocioException(HttpStatus.NOT_FOUND, "No existe el administrador indicado."));
        if (administrador.getRol() != Rol.ADMIN) {
            throw new ReglaNegocioException(HttpStatus.CONFLICT, "El procedimiento de recuperación solo admite cuentas ADMIN.");
        }
        PoliticaClave.validar(nuevaClave, true);
        administrador.setPassword(encoder.encode(nuevaClave));
        usuarios.saveAndFlush(administrador);
        eventos.publishEvent(new CuentaModificada(administrador.getUsuario()));
    }
}
