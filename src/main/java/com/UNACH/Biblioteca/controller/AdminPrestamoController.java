package com.UNACH.Biblioteca.controller;

import com.UNACH.Biblioteca.model.*;
import com.UNACH.Biblioteca.model.form.OperacionPrestamoFormulario;
import com.UNACH.Biblioteca.model.form.IncidenciaFormulario;
import com.UNACH.Biblioteca.model.form.ResolucionIncidenciaFormulario;
import com.UNACH.Biblioteca.service.PrestamoService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/prestamos")
public class AdminPrestamoController {
    private final PrestamoService prestamos;
    public AdminPrestamoController(PrestamoService prestamos) { this.prestamos = prestamos; }

    @GetMapping
    public String listar(@RequestParam(defaultValue = "SOLICITADO") EstadoPrestamo estado,
            @RequestParam(defaultValue = "0") int pagina, Model model) {
        var resultados = prestamos.listar(estado, pagina);
        if (resultados.getNumber() > 0 && resultados.isEmpty()) { resultados = prestamos.listar(estado, 0); }
        model.addAttribute("resultados", resultados);
        model.addAttribute("estado", estado);
        model.addAttribute("conteos", prestamos.contarPorEstado());
        return "prestamos/lista";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        var operacion = new OperacionPrestamoFormulario();
        operacion.setFechaLimite(prestamos.fechaLimiteSugerida());
        model.addAttribute("operacion", operacion);
        return prepararDetalle(id, model);
    }

    @PostMapping("/{id}/confirmar")
    public String confirmar(@PathVariable Long id, @Valid @ModelAttribute("operacion") OperacionPrestamoFormulario datos,
            BindingResult errores, Model model, RedirectAttributes avisos, HttpServletResponse respuesta) {
        if (errores.hasErrors()) { respuesta.setStatus(400); return prepararDetalle(id, model); }
        prestamos.confirmar(id, datos);
        avisos.addFlashAttribute("mensajeExito", "Entrega confirmada. El ejemplar quedó PRESTADO.");
        return "redirect:/admin/prestamos/" + id;
    }

    @PostMapping("/{id}/rechazar")
    public String rechazar(@PathVariable Long id, @Valid @ModelAttribute("operacion") OperacionPrestamoFormulario datos,
            BindingResult errores, Model model, RedirectAttributes avisos, HttpServletResponse respuesta) {
        if (errores.hasErrors()) { respuesta.setStatus(400); return prepararDetalle(id, model); }
        prestamos.rechazar(id, datos);
        avisos.addFlashAttribute("mensajeExito", "Solicitud rechazada. El ejemplar puede volver a solicitarse.");
        return "redirect:/admin/prestamos/" + id;
    }

    @PostMapping("/{id}/devolver")
    public String devolver(@PathVariable Long id, @Valid @ModelAttribute("operacion") OperacionPrestamoFormulario datos,
            BindingResult errores, Model model, RedirectAttributes avisos, HttpServletResponse respuesta) {
        if (errores.hasErrors()) { respuesta.setStatus(400); return prepararDetalle(id, model); }
        prestamos.devolver(id, datos);
        avisos.addFlashAttribute("mensajeExito", "Devolución registrada. El ejemplar quedó DEVUELTO.");
        return "redirect:/admin/prestamos/" + id;
    }

    @PostMapping("/{id}/devolver-con-incidencia")
    public String devolverConIncidencia(@PathVariable Long id, @Valid @ModelAttribute("operacion") OperacionPrestamoFormulario datos,
            BindingResult erroresOperacion, @Valid @ModelAttribute("incidencia") IncidenciaFormulario incidencia,
            BindingResult erroresIncidencia, Model model, RedirectAttributes avisos, HttpServletResponse respuesta) {
        if (erroresOperacion.hasErrors() || erroresIncidencia.hasErrors()) { respuesta.setStatus(400); return prepararDetalle(id, model); }
        prestamos.devolverConIncidencia(id, datos, incidencia);
        avisos.addFlashAttribute("mensajeExito", "Devolución e incidencia registradas en una sola operación.");
        return "redirect:/admin/prestamos/" + id;
    }

    @PostMapping("/{id}/incidencias")
    public String registrarIncidencia(@PathVariable Long id, @Valid @ModelAttribute("incidencia") IncidenciaFormulario datos,
            BindingResult errores, Model model, RedirectAttributes avisos, HttpServletResponse respuesta) {
        if (errores.hasErrors()) { respuesta.setStatus(400); return prepararDetalle(id, model); }
        prestamos.registrarIncidencia(id, datos);
        avisos.addFlashAttribute("mensajeExito", "Incidencia registrada.");
        return "redirect:/admin/prestamos/" + id;
    }

    @PostMapping("/{prestamoId}/incidencias/{id}/resolver")
    public String resolverIncidencia(@PathVariable Long prestamoId, @PathVariable Long id,
            @Valid @ModelAttribute("resolucion") ResolucionIncidenciaFormulario datos, BindingResult errores,
            Model model, RedirectAttributes avisos, HttpServletResponse respuesta) {
        if (errores.hasErrors()) { respuesta.setStatus(400); return prepararDetalle(prestamoId, model); }
        prestamos.resolverIncidencia(prestamoId, id, datos);
        avisos.addFlashAttribute("mensajeExito", "Incidencia resuelta y conservada en el historial.");
        return "redirect:/admin/prestamos/" + prestamoId;
    }

    private String prepararDetalle(Long id, Model model) {
        model.addAttribute("prestamo", prestamos.obtener(id));
        model.addAttribute("historial", prestamos.consultarHistorial(id));
        model.addAttribute("incidencias", prestamos.consultarIncidencias(id));
        if (!model.containsAttribute("incidencia")) model.addAttribute("incidencia", new IncidenciaFormulario());
        if (!model.containsAttribute("resolucion")) model.addAttribute("resolucion", new ResolucionIncidenciaFormulario());
        model.addAttribute("hoy", prestamos.hoy());
        return "prestamos/detalle";
    }
}
