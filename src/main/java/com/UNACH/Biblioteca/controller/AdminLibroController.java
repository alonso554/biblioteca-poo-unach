package com.UNACH.Biblioteca.controller;

import com.UNACH.Biblioteca.model.Libro;
import com.UNACH.Biblioteca.model.form.LibroFormulario;
import com.UNACH.Biblioteca.service.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/libros")
public class AdminLibroController {
    private final LibroService libroService;

    public AdminLibroController(LibroService libroService) { this.libroService = libroService; }

    @GetMapping
    public String listar(@RequestParam(defaultValue = "") String texto,
            @RequestParam(defaultValue = "todos") String filtro,
            @RequestParam(defaultValue = "0") int pagina, Model model) {
        Page<Libro> resultados = libroService.buscarLibros(texto, filtro, pagina);
        if (resultados.getNumber() > 0 && resultados.isEmpty()) {
            resultados = libroService.buscarLibros(texto, filtro, 0);
        }
        model.addAttribute("resultados", resultados);
        model.addAttribute("texto", texto.trim());
        model.addAttribute("filtro", filtro);
        return "libros/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("formulario", new LibroFormulario());
        return prepararFormulario(null, model);
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        model.addAttribute("libro", libroService.obtener(id));
        return "libros/detalle";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("formulario", LibroFormulario.de(libroService.obtener(id)));
        return prepararFormulario(id, model);
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("formulario") LibroFormulario datos, BindingResult errores,
            Model model, HttpServletResponse respuesta, RedirectAttributes avisos) {
        return guardar(null, datos, errores, model, respuesta, avisos);
    }

    @PostMapping("/{id}")
    public String actualizar(@PathVariable Long id, @Valid @ModelAttribute("formulario") LibroFormulario datos,
            BindingResult errores, Model model, HttpServletResponse respuesta, RedirectAttributes avisos) {
        return guardar(id, datos, errores, model, respuesta, avisos);
    }

    private String guardar(Long id, LibroFormulario datos, BindingResult errores, Model model,
            HttpServletResponse respuesta, RedirectAttributes avisos) {
        if (errores.hasErrors()) {
            respuesta.setStatus(400);
            return prepararFormulario(id, model);
        }
        try {
            if (id == null) { libroService.crear(datos); }
            else { libroService.actualizar(id, datos); }
        } catch (ReglaNegocioException error) {
            if (error.getEstado().value() == 404) { throw error; }
            errores.reject("libro", error.getMessage());
            respuesta.setStatus(error.getEstado().value());
            return prepararFormulario(id, model);
        }
        avisos.addFlashAttribute("mensajeExito", "Libro guardado correctamente.");
        return "redirect:/admin/libros";
    }

    @GetMapping("/{id}/eliminar")
    public String confirmar(@PathVariable Long id, Model model) {
        Libro libro = libroService.obtener(id);
        model.addAttribute("nombreRegistro", libro.getTitulo() + " · " + libro.getCodigoBarras());
        model.addAttribute("accion", "/admin/libros/" + id + "/eliminar");
        model.addAttribute("volver", "/admin/libros");
        return "confirmar-eliminacion";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes avisos) {
        libroService.eliminar(id);
        avisos.addFlashAttribute("mensajeExito", "Ejemplar eliminado correctamente.");
        return "redirect:/admin/libros";
    }

    private String prepararFormulario(Long id, Model model) {
        model.addAttribute("edicion", id != null);
        model.addAttribute("accion", id == null ? "/admin/libros" : "/admin/libros/" + id);
        return "libros/formulario";
    }
}
