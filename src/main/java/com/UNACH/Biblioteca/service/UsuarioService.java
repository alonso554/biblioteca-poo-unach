package com.UNACH.Biblioteca.service;

import com.UNACH.Biblioteca.model.Rol;
import com.UNACH.Biblioteca.model.Usuario;
import com.UNACH.Biblioteca.model.form.UsuarioFormulario;
import com.UNACH.Biblioteca.repository.UsuarioRepository;
import com.UNACH.Biblioteca.repository.PrestamoRepository;
import com.UNACH.Biblioteca.repository.HistorialPrestamoRepository;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true)
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventos;
    private final RolService roles;
    private final PrestamoRepository prestamos;
    private final HistorialPrestamoRepository historial;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
            ApplicationEventPublisher eventos, RolService roles, PrestamoRepository prestamos,
            HistorialPrestamoRepository historial) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventos = eventos;
        this.roles = roles;
        this.prestamos = prestamos;
        this.historial = historial;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Page<Usuario> listar(int pagina) {
        if (pagina < 0 || pagina > Integer.MAX_VALUE / 20) { pagina = 0; }
        return usuarioRepository.findAll(PageRequest.of(pagina, 20, Sort.by("nombre", "id")));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Usuario obtener(Long id) {
        return usuarioRepository.findById(id).orElseThrow(() ->
                new ReglaNegocioException(HttpStatus.NOT_FOUND, "El usuario no existe."));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('USUARIO', 'ADMIN')")
    public Usuario crear(@Valid UsuarioFormulario datos) {
        // La identidad procede de Spring Security, nunca del formulario ni de una cabecera.
        boolean esAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream().anyMatch(rol -> rol.getAuthority().equals("ROLE_ADMIN"));
        if (!esAdmin && datos.getRol() != Rol.USUARIO) {
            throw new AccessDeniedException("Solo ADMIN puede asignar el rol ADMIN.");
        }
        PoliticaClave.validar(datos.getClave(), true);
        String identificador = datos.getUsuario().trim();
        if (usuarioRepository.existsByUsuarioIgnoreCase(identificador)) {
            throw new ReglaNegocioException(HttpStatus.CONFLICT, "Ya existe ese identificador de usuario.");
        }
        Usuario usuario = new Usuario();
        copiar(datos, identificador, usuario);
        usuario.setPassword(passwordEncoder.encode(datos.getClave()));
        return usuarioRepository.saveAndFlush(usuario);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Usuario actualizar(Long id, @Valid UsuarioFormulario datos) {
        List<Usuario> administradores = usuarioRepository.bloquearAdministradores();
        Usuario usuario = obtener(id);
        protegerAdministrador(usuario, datos.getRol(), administradores);
        PoliticaClave.validar(datos.getClave(), false);
        String identificador = datos.getUsuario().trim();
        if (usuarioRepository.existsByUsuarioIgnoreCaseAndIdNot(identificador, id)) {
            throw new ReglaNegocioException(HttpStatus.CONFLICT, "Ya existe ese identificador de usuario.");
        }
        String identificadorAnterior = usuario.getUsuario();
        boolean cambiaAcceso = !identificadorAnterior.equals(identificador) || usuario.getRol() != datos.getRol()
                || (datos.getClave() != null && !datos.getClave().isBlank());
        copiar(datos, identificador, usuario);
        if (datos.getClave() != null && !datos.getClave().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(datos.getClave()));
        }
        Usuario guardado = usuarioRepository.saveAndFlush(usuario);
        if (cambiaAcceso) { eventos.publishEvent(new CuentaModificada(identificadorAnterior)); }
        return guardado;
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void eliminar(Long id) {
        List<Usuario> administradores = usuarioRepository.bloquearAdministradores();
        Usuario usuario = obtener(id);
        protegerAdministrador(usuario, null, administradores);
        if (prestamos.existsByAdministradorId(id) || historial.existsByAdministradorId(id)) {
            throw new ReglaNegocioException(HttpStatus.CONFLICT,
                    "No puedes eliminar una cuenta que figura en préstamos o en su historial.");
        }
        usuarioRepository.delete(usuario);
        usuarioRepository.flush();
        eventos.publishEvent(new CuentaModificada(usuario.getUsuario()));
    }

    private void copiar(UsuarioFormulario datos, String identificador, Usuario usuario) {
        usuario.setNombre(datos.getNombre().trim());
        usuario.setUsuario(identificador);
        usuario.setRolSistema(roles.obtener(datos.getRol()));
    }

    private void protegerAdministrador(Usuario usuario, Rol nuevoRol, List<Usuario> administradores) {
        if (usuario.getRol() == Rol.ADMIN && nuevoRol != Rol.ADMIN) {
            long compatibles = administradores.stream().filter(a -> PoliticaClave.esBCrypt(a.getPassword())).count();
            if (administradores.size() <= 1
                    || (PoliticaClave.esBCrypt(usuario.getPassword()) && compatibles <= 1)) {
                throw new ReglaNegocioException(HttpStatus.CONFLICT,
                        "No puedes eliminar ni quitar el rol al último administrador con acceso al sistema.");
            }
        }
    }
}
