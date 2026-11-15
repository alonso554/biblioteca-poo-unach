package com.UNACH.Biblioteca.controller;

import java.security.Principal;
import com.UNACH.Biblioteca.service.PrestamoService;
import com.UNACH.Biblioteca.model.EstadoPrestamo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    private final PrestamoService prestamos;
    public LoginController(PrestamoService prestamos) { this.prestamos = prestamos; }

    @GetMapping("/")
    public String inicio() {
        return "redirect:/catalogo";
    }

    @GetMapping("/login")
    public String mostrarLogin() {
        return "login";
    }

    @GetMapping("/panel")
    @PreAuthorize("hasRole('ADMIN')")
    public String mostrarPanel(Principal usuario, Model model) {
        model.addAttribute("nombreUsuario", usuario.getName());
        var cantidades = prestamos.contarPorEstado();
        model.addAttribute("solicitudesPendientes", cantidades.get(EstadoPrestamo.SOLICITADO));
        model.addAttribute("prestamosActivos", cantidades.get(EstadoPrestamo.PRESTADO));
        model.addAttribute("prestamosDevueltos", cantidades.get(EstadoPrestamo.DEVUELTO));
        return "panel";
    }
}
