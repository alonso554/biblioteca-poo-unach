package com.UNACH.Biblioteca.controller;

import com.UNACH.Biblioteca.model.*;
import com.UNACH.Biblioteca.model.form.LibroFormulario;
import com.UNACH.Biblioteca.service.LibroService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/libros")
public class LibroRestController {
    private final LibroService libroService;

    public LibroRestController(LibroService libroService) { this.libroService = libroService; }

    @GetMapping
    public PaginaRespuesta<Libro> listar(@RequestParam(defaultValue = "") String texto,
            @RequestParam(defaultValue = "todos") String filtro, @RequestParam(defaultValue = "0") int pagina) {
        return PaginaRespuesta.de(libroService.buscarLibros(texto, filtro, pagina));
    }

    @GetMapping("/{id}")
    public Libro obtener(@PathVariable Long id) { return libroService.obtener(id); }

    @PostMapping
    public ResponseEntity<Libro> crear(@Valid @RequestBody LibroFormulario datos) {
        Libro libro = libroService.crear(datos);
        return ResponseEntity.created(URI.create("/api/libros/" + libro.getId())).body(libro);
    }

    @PutMapping("/{id}")
    public Libro actualizar(@PathVariable Long id, @Valid @RequestBody LibroFormulario datos) {
        return libroService.actualizar(id, datos);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        libroService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
