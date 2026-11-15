package com.UNACH.Biblioteca;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CatalogoYLoginTests extends PruebaAislada {
    @Test
    void catalogoLoginYRecursosSonPublicos() throws Exception {
        mvc.perform(get("/")).andExpect(redirectedUrl("/catalogo"));
        mvc.perform(get("/catalogo")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Biblioteca de Tecnologías Digitales Aplicadas")))
                .andExpect(content().string(not(containsString("Cerrar sesión"))));
        mvc.perform(get("/login")).andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"_csrf\"")));
        mvc.perform(get("/css/catalogo.css")).andExpect(status().isOk());
        mvc.perform(get("/css/gestion.css")).andExpect(status().isOk());
        mvc.perform(get("/img/logo-unach.png")).andExpect(status().isOk()).andExpect(content().contentType("image/png"));
        mvc.perform(get("/api/usuarios")).andExpect(status().isUnauthorized());
        mvc.perform(get("/panel")).andExpect(status().is3xxRedirection());
    }

    @Test
    void busquedaYPaginacionConservanLosEjemplaresYFiltros() throws Exception {
        for (int i = 0; i < 23; i++) { insertarLibro("Java para aprender", "Autora de prueba", "TEST-PAG-" + i); }
        insertarLibro("Otro título", null, "TEST-OTRO");
        mvc.perform(get("/api/libros").param("texto", "Java").param("filtro", "titulo"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(23))
                .andExpect(jsonPath("$.tamano").value(20)).andExpect(jsonPath("$.contenido.length()").value(20));
        mvc.perform(get("/api/libros").param("texto", "Java").param("filtro", "titulo").param("pagina", "1"))
                .andExpect(jsonPath("$.contenido.length()").value(3)).andExpect(jsonPath("$.pagina").value(1));
        mvc.perform(get("/api/libros").param("texto", "Autora").param("filtro", "autor"))
                .andExpect(jsonPath("$.totalElementos").value(23));
        mvc.perform(get("/api/libros").param("texto", "TEST-OTRO").param("filtro", "codigo"))
                .andExpect(jsonPath("$.totalElementos").value(1));
        mvc.perform(get("/api/libros").param("texto", "Nada coincide"))
                .andExpect(jsonPath("$.totalElementos").value(0));
        mvc.perform(get("/catalogo").param("texto", "Java").param("filtro", "titulo"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("texto=Java&amp;filtro=titulo&amp;pagina=1")));
        mvc.perform(get("/catalogo").param("pagina", "2147483647")).andExpect(status().isOk());
        mvc.perform(get("/catalogo").param("pagina", "999999")).andExpect(status().isOk())
                .andExpect(content().string(containsString("TEST-PAG-")));
    }

    @Test
    void idsInexistentesPublicosDevuelven404YAutorNuloSePresentaConClaridad() throws Exception {
        insertarLibro("Sin autor", null, "TEST-NULO");
        mvc.perform(get("/api/libros/999999")).andExpect(status().isNotFound());
        mvc.perform(get("/catalogo")).andExpect(content().string(containsString("Autor no registrado")));
        mvc.perform(get("/api/libros").param("pagina", "-1")).andExpect(jsonPath("$.pagina").value(0));
    }

    @Test
    void loginAdminYSesionCierranConPostYCsrf() throws Exception {
        insertarCuenta("admin", "ADMIN", CLAVE_ADMIN);
        var resultado = mvc.perform(formLogin().user("admin").password(CLAVE_ADMIN))
                .andExpect(authenticated().withRoles("ADMIN")).andExpect(redirectedUrl("/panel")).andReturn();
        MockHttpSession sesion = (MockHttpSession) resultado.getRequest().getSession(false);
        mvc.perform(get("/catalogo").session(sesion)).andExpect(content().string(containsString("Cerrar sesión")));
        mvc.perform(get("/panel").session(sesion)).andExpect(content().string(containsString("Administrar libros")));
        mvc.perform(get("/login").session(sesion)).andExpect(content().string(containsString("Cerrar sesión")));
        mvc.perform(post("/logout").session(sesion)).andExpect(status().isForbidden());
        mvc.perform(post("/logout").session(sesion).with(csrf()))
                .andExpect(unauthenticated()).andExpect(redirectedUrl("/login?logout"));
        mvc.perform(get("/admin/libros")).andExpect(status().is3xxRedirection());
    }

    @Test
    void loginUsuarioConservaSusPermisosLimitados() throws Exception {
        insertarCuenta("usuario", "USUARIO", CLAVE_USUARIO);
        var resultado = mvc.perform(formLogin().user("usuario").password(CLAVE_USUARIO))
                .andExpect(authenticated().withRoles("USUARIO")).andExpect(redirectedUrl("/catalogo")).andReturn();
        MockHttpSession sesion = (MockHttpSession) resultado.getRequest().getSession(false);
        mvc.perform(get("/panel").session(sesion)).andExpect(status().isForbidden());
        mvc.perform(get("/catalogo").session(sesion)).andExpect(status().isOk())
                .andExpect(content().string(not(containsString("href=\"/panel\""))));
        mvc.perform(get("/usuarios/registro").session(sesion)).andExpect(status().isOk());
        mvc.perform(get("/admin/usuarios").session(sesion)).andExpect(status().isForbidden());
        mvc.perform(get("/api/usuarios").session(sesion).header("X-Role", "ADMIN").param("rol", "ADMIN"))
                .andExpect(status().isForbidden());
    }
}
