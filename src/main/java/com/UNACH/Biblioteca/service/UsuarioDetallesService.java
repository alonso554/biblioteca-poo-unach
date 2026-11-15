package com.UNACH.Biblioteca.service;

import com.UNACH.Biblioteca.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioDetallesService implements UserDetailsService {
    private final UsuarioRepository usuarioRepository;

    public UsuarioDetallesService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identificador) throws UsernameNotFoundException {
        String nombre = identificador == null ? "" : identificador.trim();
        var usuario = usuarioRepository.findByUsuarioIgnoreCase(nombre)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario o contraseña incorrectos."));
        if (!PoliticaClave.esBCrypt(usuario.getPassword()) || usuario.getRol() == null) {
            // No se intenta autenticar texto plano ni se convierte una clave antigua en silencio.
            throw new UsernameNotFoundException("Usuario o contraseña incorrectos.");
        }
        return User.withUsername(usuario.getUsuario())
                .password(usuario.getPassword())
                .roles(usuario.getRol().name())
                .build();
    }
}
