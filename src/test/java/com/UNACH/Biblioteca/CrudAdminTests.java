package com.UNACH.Biblioteca;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WithMockUser(username = "administrador", roles = "ADMIN")
class CrudAdminTests extends PruebaAislada {
    private static final String LIBRO = """
        {"titulo":"Libro ficticio","autor":null,"descripcionOriginal":"Inventario de prueba","codigoBarras":"TEST-ADMIN-1"}
        """;
    private static final String USUARIO = """
        {"nombre":"Persona ficticia","usuario":"ficticia.prueba","rol":"USUARIO","clave":"%s"}
        """.formatted(CLAVE_USUARIO);

    @Test
    void adminRealizaCrudDeUnLibroFicticio() throws Exception {
        String ruta = mvc.perform(post("/api/libros").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(LIBRO))
                .andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");
        mvc.perform(get(ruta)).andExpect(status().isOk()).andExpect(jsonPath("$.titulo").value("Libro ficticio"));
        mvc.perform(put(ruta).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(LIBRO.replace("Libro ficticio", "Libro actualizado")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.titulo").value("Libro actualizado"));
        mvc.perform(get("/api/libros")).andExpect(jsonPath("$.totalElementos").value(1));
        mvc.perform(delete(ruta).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get(ruta)).andExpect(status().isNotFound()).andExpect(jsonPath("$.estado").value(404));
    }

    @Test
    void adminRealizaCrudDeUnUsuarioFicticio() throws Exception {
        insertarCuenta("otro.administrador", "ADMIN", CLAVE_ADMIN);
        String ruta = mvc.perform(post("/api/usuarios").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(USUARIO))
                .andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");
        mvc.perform(get(ruta)).andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Persona ficticia"));
        mvc.perform(put(ruta).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(USUARIO.replace("Persona ficticia", "Persona actualizada").replace("USUARIO", "ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.rol").value("ADMIN"));
        mvc.perform(get("/api/usuarios")).andExpect(jsonPath("$.totalElementos").value(2));
        mvc.perform(delete(ruta).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get(ruta)).andExpect(status().isNotFound());
    }

    @Test
    void rechazaCodigosDuplicadosSinConfundirTitulosRepetidos() throws Exception {
        insertarLibro("Libro ficticio", null, "TEST-ADMIN-1");
        mvc.perform(post("/api/libros").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(LIBRO.replace("TEST-ADMIN-1", "TEST-ADMIN-2"))).andExpect(status().isCreated());
        mvc.perform(post("/api/libros").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(LIBRO))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje", containsString("código de barras")));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM inventario_libros", Integer.class));
    }

    @Test
    void noPermiteCambiarElCodigoExistente() throws Exception {
        long id = insertarLibro("Libro ficticio", null, "TEST-ADMIN-1");
        mvc.perform(put("/api/libros/" + id).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(LIBRO.replace("TEST-ADMIN-1", "CODIGO-CAMBIADO")))
                .andExpect(status().isBadRequest());
        assertEquals("TEST-ADMIN-1", jdbc.queryForObject("SELECT codigo_barras FROM inventario_libros WHERE id=?", String.class, id));
    }

    @Test
    void rechazaUsuarioDuplicadoSinDistinguirMayusculas() throws Exception {
        insertarUsuario();
        mvc.perform(post("/api/usuarios").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(USUARIO.replace("ficticia.prueba", "PERSONA.PRUEBA")))
                .andExpect(status().isConflict());
    }

    @Test
    void validaCamposRolesEIdentificadores() throws Exception {
        mvc.perform(post("/api/libros").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/libros").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(LIBRO.replace("TEST-ADMIN-1", "X".repeat(51)))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/usuarios").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(USUARIO.replace("USUARIO", "SUPERADMIN"))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/libros/no-es-id")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/usuarios/999999")).andExpect(status().isNotFound());
        mvc.perform(put("/api/libros/999999").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(LIBRO))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/usuarios/999999").with(csrf())).andExpect(status().isNotFound());
    }

    @Test
    void adminTambienNecesitaCsrfParaEscribir() throws Exception {
        mvc.perform(post("/api/libros").contentType(MediaType.APPLICATION_JSON).content(LIBRO))
                .andExpect(status().isForbidden());
        mvc.perform(post("/admin/usuarios").param("nombre", "Prueba").param("usuario", "prueba"))
                .andExpect(status().isForbidden());
    }

    @Test
    void editarSinClaveConservaHashYNoLoExpone() throws Exception {
        long id = insertarUsuario();
        String hashAnterior = jdbc.queryForObject("SELECT password FROM usuarios WHERE id=?", String.class, id);
        mvc.perform(put("/api/usuarios/" + id).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Nombre nuevo\",\"usuario\":\"persona.prueba\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.clave").doesNotExist());
        assertTrue(hashAnterior.equals(jdbc.queryForObject("SELECT password FROM usuarios WHERE id=?", String.class, id)));
        mvc.perform(get("/api/usuarios")).andExpect(jsonPath("$.contenido[0].password").doesNotExist());
        mvc.perform(get("/admin/usuarios/" + id + "/editar")).andExpect(status().isOk())
                .andExpect(content().string(not(containsString("$2a$"))));
    }

    @Test
    void clavesFaltantesOLargasSonRechazadas() throws Exception {
        mvc.perform(post("/api/usuarios").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Prueba\",\"usuario\":\"prueba\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/usuarios").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(USUARIO.replace(CLAVE_USUARIO, "á".repeat(37))))
                .andExpect(status().isBadRequest());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM usuarios", Integer.class));
    }

    @Test
    void codigosConDistintaCapitalizacionSonEjemplaresDiferentes() throws Exception {
        insertarLibro("Libro ficticio", null, "TEST-ADMIN-1");
        mvc.perform(post("/api/libros").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(LIBRO.replace("TEST-ADMIN-1", "test-admin-1")))
                .andExpect(status().isCreated());
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM inventario_libros", Integer.class));
    }

    @Test
    void pantallasAdminRenderizanDatosYConfirmacionesSinEliminarPorGet() throws Exception {
        long libroId = insertarLibro("Libro HTML", null, "TEST-HTML-1");
        long usuarioId = insertarUsuario();
        for (String ruta : new String[]{"/panel", "/admin/libros", "/admin/libros/nuevo", "/admin/libros/" + libroId,
                "/admin/libros/" + libroId + "/editar", "/admin/usuarios", "/admin/usuarios/nuevo",
                "/admin/usuarios/" + usuarioId, "/admin/usuarios/" + usuarioId + "/editar"}) {
            mvc.perform(get(ruta)).andExpect(status().isOk()).andExpect(content().string(containsString("Cerrar sesión")));
        }
        mvc.perform(get("/admin/libros/" + libroId + "/eliminar")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Confirmar eliminación")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
        mvc.perform(get("/admin/usuarios/" + usuarioId + "/eliminar")).andExpect(status().isOk());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM inventario_libros", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM usuarios", Integer.class));
    }

    @Test
    void formulariosHtmlCompartenCrudYValidaciones() throws Exception {
        insertarCuenta("otro.administrador", "ADMIN", CLAVE_ADMIN);
        mvc.perform(post("/admin/libros").with(csrf()).param("titulo", "Libro HTML")
                .param("codigoBarras", "TEST-HTML-2").param("descripcionOriginal", "Prueba"))
                .andExpect(redirectedUrl("/admin/libros"));
        long id = jdbc.queryForObject("SELECT id FROM inventario_libros WHERE codigo_barras='TEST-HTML-2'", Long.class);
        mvc.perform(post("/admin/libros/" + id).with(csrf()).param("titulo", "Libro editado")
                .param("codigoBarras", "TEST-HTML-2").param("descripcionOriginal", "Prueba editada"))
                .andExpect(redirectedUrl("/admin/libros"));
        assertEquals("Libro editado", jdbc.queryForObject("SELECT titulo FROM inventario_libros WHERE id=?", String.class, id));
        mvc.perform(post("/admin/libros/" + id + "/eliminar").with(csrf())).andExpect(redirectedUrl("/admin/libros"));
        mvc.perform(post("/admin/usuarios").with(csrf()).param("nombre", "Usuario HTML")
                .param("usuario", "html.prueba").param("clave", CLAVE_USUARIO)).andExpect(redirectedUrl("/admin/usuarios"));
        long usuarioId = jdbc.queryForObject("SELECT id FROM usuarios WHERE usuario='html.prueba'", Long.class);
        mvc.perform(post("/admin/usuarios/" + usuarioId).with(csrf()).param("nombre", "Usuario editado")
                .param("usuario", "html.prueba").param("rol", "ADMIN"))
                .andExpect(redirectedUrl("/admin/usuarios"));
        mvc.perform(post("/admin/usuarios/" + usuarioId + "/eliminar").with(csrf()))
                .andExpect(redirectedUrl("/admin/usuarios"));
    }
}
