package com.UNACH.Biblioteca.controller;

import com.UNACH.Biblioteca.model.*;
import com.UNACH.Biblioteca.service.PrestamoService;
import com.UNACH.Biblioteca.service.LibroService;
import java.time.LocalDate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;

@Controller
@RequestMapping("/admin/historial")
public class HistorialController {
    private final PrestamoService prestamos;
    private final LibroService libros;
    public HistorialController(PrestamoService prestamos, LibroService libros) { this.prestamos = prestamos; this.libros = libros; }

    @GetMapping("/lectores")
    public String lectores(@RequestParam(defaultValue = "") String texto, @RequestParam(defaultValue = "0") int pagina, Model model) {
        model.addAttribute("resultados", prestamos.buscarLectores(texto, pagina)); model.addAttribute("texto", texto.trim());
        return "historial/lectores";
    }
    @GetMapping("/lectores/{id}")
    public String lector(@PathVariable Long id, @RequestParam(required = false) EstadoPrestamo estado,
            @RequestParam(defaultValue = "false") boolean soloIncidencias, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int pagina, Model model) {
        model.addAttribute("lector", prestamos.obtenerLector(id)); model.addAttribute("consulta", prestamos.historialLector(id, estado, soloIncidencias, desde, hasta, pagina));
        model.addAttribute("soloIncidencias", soloIncidencias);
        return filtros(model, estado, desde, hasta, false, id, "lector");
    }
    @GetMapping("/libros")
    public String libros(@RequestParam(defaultValue = "") String texto, @RequestParam(defaultValue = "todos") String filtro,
            @RequestParam(defaultValue = "0") int pagina, Model model) {
        model.addAttribute("resultados", libros.buscarLibros(texto, filtro, pagina));
        model.addAttribute("texto", texto.trim()); model.addAttribute("filtro", filtro);
        return "historial/libros";
    }
    @GetMapping("/libros/{id}")
    public String libro(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean mismaObra, @RequestParam(required = false) EstadoPrestamo estado,
            @RequestParam(defaultValue = "false") boolean soloIncidencias, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int pagina, Model model) {
        model.addAttribute("libro", prestamos.historialLibro(id, mismaObra, estado, soloIncidencias, desde, hasta, pagina));
        model.addAttribute("soloIncidencias", soloIncidencias);
        model.addAttribute("seleccionado", prestamos.obtenerLibro(id));
        model.addAttribute("ejemplar", id); return filtros(model, estado, desde, hasta, mismaObra, id, "libro");
    }
    private String filtros(Model model, EstadoPrestamo estado, LocalDate desde, LocalDate hasta, boolean mismaObra, Long id, String tipo) {
        model.addAttribute("estado", estado); model.addAttribute("desde", desde); model.addAttribute("hasta", hasta); model.addAttribute("mismaObra", mismaObra);
        return "lector".equals(tipo) ? "historial/lector-detalle" : "historial/libro-detalle";
    }
}
