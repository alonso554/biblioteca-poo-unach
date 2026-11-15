package com.UNACH.Biblioteca.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class SesionVistaAdvice {
    @ModelAttribute("sesionIniciada")
    public boolean sesionIniciada(Principal usuario) { return usuario != null; }

    @ModelAttribute("esAdmin")
    public boolean esAdmin(HttpServletRequest solicitud) { return solicitud.isUserInRole("ADMIN"); }
}
