package com.UNACH.Biblioteca.controller;

import com.UNACH.Biblioteca.model.*;
import com.UNACH.Biblioteca.model.form.UsuarioFormulario;
import com.UNACH.Biblioteca.service.UsuarioService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioRestController {
    private final UsuarioService usuarioService;

    public UsuarioRestController(UsuarioService usuarioService) { this.usuarioService = usuarioService; }

    @GetMapping
    public PaginaRespuesta<Usuario> listar(@RequestParam(defaultValue = "0") int pagina) {
        return PaginaRespuesta.de(usuarioService.listar(pagina));
    }

    @GetMapping("/{id}")
    public Usuario obtener(@PathVariable Long id) { return usuarioService.obtener(id); }

    @PostMapping
    public ResponseEntity<Usuario> crear(@Valid @RequestBody UsuarioFormulario datos) {
        Usuario usuario = usuarioService.crear(datos);
        return ResponseEntity.created(URI.create("/api/usuarios/" + usuario.getId())).body(usuario);
    }

    @PutMapping("/{id}")
    public Usuario actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioFormulario datos) {
        return usuarioService.actualizar(id, datos);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        usuarioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
