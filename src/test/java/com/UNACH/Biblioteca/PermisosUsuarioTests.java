package com.UNACH.Biblioteca;

import com.UNACH.Biblioteca.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WithMockUser(username = "alumno", roles = "USUARIO")
class PermisosUsuarioTests extends PruebaAislada {
    @Autowired UsuarioService usuarioService;

    @Test
    void usuarioPuedeRegistrarSinElegirRol() throws Exception {
        mvc.perform(post("/api/usuarios").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nombre":"Estudiante","usuario":"estudiante.prueba","clave":"%s"}
                    """.formatted(CLAVE_USUARIO)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.rol").value("USUARIO"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.clave").doesNotExist())
                .andExpect(jsonPath("$.contrasena").doesNotExist());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM usuarios", Integer.class));
        String hash = jdbc.queryForObject("SELECT password FROM usuarios", String.class);
        assertTrue(hash.startsWith("$2"));
        assertTrue(passwordEncoder.matches(CLAVE_USUARIO, hash));
    }

    @Test
    void usuarioNoPuedeAsignarAdminManipulandoJson() throws Exception {
        mvc.perform(post("/api/usuarios").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nombre":"Estudiante","usuario":"estudiante.prueba","rol":"ADMIN"}
                    """))
                .andExpect(status().isForbidden());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM usuarios", Integer.class));
    }

    @Test
    void usuarioNoPuedeListarConsultarEditarNiEliminarUsuarios() throws Exception {
        assertThrows(AccessDeniedException.class, () -> usuarioService.listar(0));
        long id = insertarUsuario();
        mvc.perform(get("/api/usuarios")).andExpect(status().isForbidden());
        mvc.perform(get("/api/usuarios/" + id)).andExpect(status().isForbidden());
        mvc.perform(put("/api/usuarios/" + id).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Cambio\",\"usuario\":\"cambio.prueba\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/usuarios/" + id).with(csrf())).andExpect(status().isForbidden());
        assertEquals("Persona de prueba", jdbc.queryForObject("SELECT nombre FROM usuarios WHERE id=?", String.class, id));
    }

    @Test
    void usuarioConsultaLibrosPeroNoPuedeModificarlos() throws Exception {
        long id = insertarLibro("Java de prueba", null, "TEST-U-1");
        mvc.perform(get("/api/libros")).andExpect(status().isOk());
        mvc.perform(get("/api/libros/" + id)).andExpect(status().isOk());
        mvc.perform(post("/api/libros").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/libros/" + id).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/libros/" + id).with(csrf())).andExpect(status().isForbidden());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM inventario_libros", Integer.class));
    }

    @Test
    void rutasHtmlAdministrativasTambienEstanProtegidas() throws Exception {
        for (String ruta : new String[]{"/admin/libros", "/admin/libros/nuevo", "/admin/libros/1",
                "/admin/libros/1/editar", "/admin/libros/1/eliminar", "/admin/usuarios",
                "/admin/usuarios/nuevo", "/admin/usuarios/1", "/admin/usuarios/1/editar", "/admin/usuarios/1/eliminar"}) {
            mvc.perform(get(ruta)).andExpect(status().isForbidden());
        }
        for (String ruta : new String[]{"/admin/libros", "/admin/libros/1", "/admin/libros/1/eliminar",
                "/admin/usuarios", "/admin/usuarios/1", "/admin/usuarios/1/eliminar"}) {
            mvc.perform(post(ruta).with(csrf())).andExpect(status().isForbidden());
        }
    }

    @Test
    void formularioLimitadoPermiteRegistroPeroRechazaEscalamiento() throws Exception {
        mvc.perform(get("/usuarios/registro")).andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(not(containsString("value=\"ADMIN\""))))
                .andExpect(content().string(containsString("Cerrar sesión")));
        mvc.perform(post("/usuarios/registro").with(csrf()).param("nombre", "Registro HTML")
                .param("usuario", "html.prueba").param("clave", CLAVE_USUARIO)).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/usuarios/registro"));
        mvc.perform(post("/usuarios/registro").with(csrf()).param("nombre", "Manipulado")
                .param("usuario", "manipulado.prueba").param("rol", "ADMIN"))
                .andExpect(status().isForbidden());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM usuarios", Integer.class));
    }

    @Test
    void registroRequiereCsrfYValidaCampos() throws Exception {
        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/usuarios").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\" \",\"usuario\":\"\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje", containsString("obligatorio")));
        mvc.perform(post("/usuarios/registro").with(csrf()).param("nombre", " ").param("usuario", ""))
                .andExpect(status().isBadRequest()).andExpect(content().string(containsString("obligatorio")));
    }
}
