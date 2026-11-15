package com.UNACH.Biblioteca.controller;

import com.UNACH.Biblioteca.model.Usuario;
import com.UNACH.Biblioteca.model.form.UsuarioFormulario;
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
public class UsuarioController {
    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) { this.usuarioService = usuarioService; }

    @GetMapping("/admin/usuarios")
    public String listar(@RequestParam(defaultValue = "0") int pagina, Model model) {
        Page<Usuario> resultados = usuarioService.listar(pagina);
        if (resultados.getNumber() > 0 && resultados.isEmpty()) { resultados = usuarioService.listar(0); }
        model.addAttribute("resultados", resultados);
        return "usuarios/lista";
    }

    @GetMapping("/admin/usuarios/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        model.addAttribute("usuario", usuarioService.obtener(id));
        return "usuarios/detalle";
    }

    @GetMapping("/admin/usuarios/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("formulario", new UsuarioFormulario());
        return prepararFormulario(null, false, model);
    }

    @GetMapping("/usuarios/registro")
    public String registro(Model model) {
        model.addAttribute("formulario", new UsuarioFormulario());
        return prepararFormulario(null, true, model);
    }

    @GetMapping("/admin/usuarios/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("formulario", UsuarioFormulario.de(usuarioService.obtener(id)));
        return prepararFormulario(id, false, model);
    }

    @PostMapping("/usuarios/registro")
    public String registrar(@Valid @ModelAttribute("formulario") UsuarioFormulario datos, BindingResult errores,
            Model model, HttpServletResponse respuesta, RedirectAttributes avisos) {
        return guardar(null, true, datos, errores, model, respuesta, avisos);
    }

    @PostMapping("/admin/usuarios")
    public String crear(@Valid @ModelAttribute("formulario") UsuarioFormulario datos, BindingResult errores,
            Model model, HttpServletResponse respuesta, RedirectAttributes avisos) {
        return guardar(null, false, datos, errores, model, respuesta, avisos);
    }

    @PostMapping("/admin/usuarios/{id}")
    public String actualizar(@PathVariable Long id, @Valid @ModelAttribute("formulario") UsuarioFormulario datos,
            BindingResult errores, Model model, HttpServletResponse respuesta, RedirectAttributes avisos) {
        return guardar(id, false, datos, errores, model, respuesta, avisos);
    }

    private String guardar(Long id, boolean registro, UsuarioFormulario datos, BindingResult errores,
            Model model, HttpServletResponse respuesta, RedirectAttributes avisos) {
        if (errores.hasErrors()) {
            respuesta.setStatus(400);
            return prepararFormulario(id, registro, model);
        }
        try {
            if (id == null) { usuarioService.crear(datos); }
            else { usuarioService.actualizar(id, datos); }
        } catch (ReglaNegocioException error) {
            if (error.getEstado().value() == 404) { throw error; }
            errores.reject("usuario", error.getMessage());
            respuesta.setStatus(error.getEstado().value());
            return prepararFormulario(id, registro, model);
        }
        avisos.addFlashAttribute("mensajeExito", id == null
                ? "Usuario registrado. Ya puede iniciar sesión con sus credenciales."
                : "Usuario actualizado. Los cambios de identificador, contraseña o rol requieren iniciar sesión nuevamente.");
        return registro ? "redirect:/usuarios/registro" : "redirect:/admin/usuarios";
    }

    @GetMapping("/admin/usuarios/{id}/eliminar")
    public String confirmar(@PathVariable Long id, Model model) {
        Usuario usuario = usuarioService.obtener(id);
        model.addAttribute("nombreRegistro", usuario.getNombre() + " · " + usuario.getUsuario());
        model.addAttribute("accion", "/admin/usuarios/" + id + "/eliminar");
        model.addAttribute("volver", "/admin/usuarios");
        return "confirmar-eliminacion";
    }

    @PostMapping("/admin/usuarios/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes avisos) {
        usuarioService.eliminar(id);
        avisos.addFlashAttribute("mensajeExito", "Usuario eliminado correctamente.");
        return "redirect:/admin/usuarios";
    }

    private String prepararFormulario(Long id, boolean registro, Model model) {
        model.addAttribute("edicion", id != null);
        model.addAttribute("registroLimitado", registro);
        model.addAttribute("accion", registro ? "/usuarios/registro"
                : id == null ? "/admin/usuarios" : "/admin/usuarios/" + id);
        return "usuarios/formulario";
    }
}
