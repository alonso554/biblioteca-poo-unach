package com.UNACH.Biblioteca.controller;

import com.UNACH.Biblioteca.model.Libro;
import com.UNACH.Biblioteca.service.LibroService;
import com.UNACH.Biblioteca.service.PrestamoService;
import java.security.Principal;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LibroController {

    private final LibroService libroService;
    private final PrestamoService prestamos;

    public LibroController(LibroService libroService, PrestamoService prestamos) {
        this.libroService = libroService;
        this.prestamos = prestamos;
    }

    @GetMapping("/catalogo")
    public String mostrarCatalogo(
            @RequestParam(defaultValue = "") String texto,
            @RequestParam(defaultValue = "todos") String filtro,
            @RequestParam(defaultValue = "0") int pagina,
            Model model,
            Principal usuario) {

        texto = texto.trim();

        if (!"titulo".equals(filtro)
                && !"autor".equals(filtro)
                && !"codigo".equals(filtro)) {
            filtro = "todos";
        }

        Page<Libro> resultados = libroService.buscarLibros(
                texto, filtro, pagina);

        // Si se solicita una página inexistente, volvemos a la primera.
        if (resultados.getNumber() > 0 && resultados.isEmpty()) {
            resultados = libroService.buscarLibros(texto, filtro, 0);
        }

        model.addAttribute("resultados", resultados);
        model.addAttribute("estadosEjemplares", prestamos.estadosEjemplares(
                resultados.getContent().stream().map(Libro::getId).toList()));
        model.addAttribute("texto", texto);
        model.addAttribute("filtro", filtro);
        model.addAttribute("sesionIniciada", usuario != null);

        return "catalogo";
    }
}
