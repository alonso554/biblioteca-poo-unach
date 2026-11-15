package com.UNACH.Biblioteca.controller;

import com.UNACH.Biblioteca.model.form.SolicitudPrestamoFormulario;
import com.UNACH.Biblioteca.service.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/prestamos")
public class PrestamoController {
    private final PrestamoService prestamos;
    public PrestamoController(PrestamoService prestamos) { this.prestamos = prestamos; }

    @GetMapping("/solicitar")
    public String formulario(@RequestParam(defaultValue = "") String codigo, Model model) {
        var datos = new SolicitudPrestamoFormulario();
        datos.setCodigoBarras(codigo);
        model.addAttribute("formulario", datos);
        return "prestamos/solicitud";
    }

    @PostMapping("/solicitar")
    public String solicitar(@Valid @ModelAttribute("formulario") SolicitudPrestamoFormulario datos,
            BindingResult errores, HttpServletResponse respuesta, RedirectAttributes avisos) {
        if (errores.hasErrors()) { respuesta.setStatus(400); return "prestamos/solicitud"; }
        try {
            var prestamo = prestamos.solicitar(datos);
            avisos.addFlashAttribute("mensajeExito", "Solicitud " + prestamo.getId()
                    + " registrada. Acude al personal para confirmar la entrega del ejemplar.");
            return "redirect:/prestamos/solicitar";
        } catch (ReglaNegocioException error) {
            respuesta.setStatus(error.getEstado().value());
            errores.reject("solicitud", error.getMessage());
            return "prestamos/solicitud";
        }
    }
}
