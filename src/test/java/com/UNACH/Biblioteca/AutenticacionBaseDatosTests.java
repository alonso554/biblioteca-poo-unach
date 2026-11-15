package com.UNACH.Biblioteca;

import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Los cambios deben confirmar sus transacciones para comprobar AFTER_COMMIT.
// La base sigue forzada a H2 por PruebaAislada; se limpia entre casos.
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AutenticacionBaseDatosTests extends PruebaAislada {
    @Autowired SessionRegistry sesiones;
    private long adminId;
    private long usuarioId;

    @BeforeEach
    void prepararCuentas() {
        limpiarCuentas();
        adminId = insertarCuenta("admin.db", "ADMIN", CLAVE_ADMIN);
        usuarioId = insertarCuenta("usuario.db", "USUARIO", CLAVE_USUARIO);
    }

    @AfterEach
    void limpiarCuentas() {
        for (Object principal : sesiones.getAllPrincipals()) {
            sesiones.getAllSessions(principal, true).forEach(s -> sesiones.removeSessionInformation(s.getSessionId()));
        }
        jdbc.update("DELETE FROM usuarios");
    }

    private MockHttpSession entrar(String identificador, String clave, String rol) throws Exception {
        var resultado = mvc.perform(formLogin().user(identificador).password(clave))
                .andExpect(authenticated().withRoles(rol))
                .andExpect(redirectedUrl("ADMIN".equals(rol) ? "/panel" : "/catalogo")).andReturn();
        return (MockHttpSession) resultado.getRequest().getSession(false);
    }

    private String datos(String identificador, String rol, String clave) {
        return """
            {"nombre":"Cuenta de prueba","usuario":"%s","rol":"%s","clave":"%s"}
            """.formatted(identificador, rol, clave);
    }

    @Test
    void loginAdminRegresaAlListadoAdministrativoSolicitado() throws Exception {
        var resultado = mvc.perform(get("/admin/prestamos").queryParam("estado", "SOLICITADO"))
                .andExpect(status().is3xxRedirection()).andReturn();
        var sesion = (MockHttpSession) resultado.getRequest().getSession(false);
        mvc.perform(post("/login").session(sesion).with(csrf())
                .param("username", "admin.db").param("password", CLAVE_ADMIN))
                .andExpect(authenticated().withRoles("ADMIN"))
                .andExpect(header().string("Location", containsString("/admin/prestamos?estado=SOLICITADO")));
        mvc.perform(get("/admin/prestamos").session(sesion)).andExpect(status().isOk());
    }

    @Test
    void loginConsultaLaBaseIgnoraMayusculasYNoRecodificaElHash() throws Exception {
        String antes = jdbc.queryForObject("SELECT password FROM usuarios WHERE id=?", String.class, usuarioId);
        var sesion = entrar("  USUARIO.DB  ", CLAVE_USUARIO, "USUARIO");
        mvc.perform(get("/catalogo").session(sesion)).andExpect(status().isOk());
        mvc.perform(get("/panel").session(sesion)).andExpect(status().isForbidden());
        assertTrue(antes.equals(jdbc.queryForObject("SELECT password FROM usuarios WHERE id=?", String.class, usuarioId)));
        mvc.perform(get("/api/usuarios").session(sesion)).andExpect(status().isForbidden());
    }

    @Test
    void claveIncorrectaEIdentificadorInexistenteDanElMismoErrorGenerico() throws Exception {
        String incorrecta = UUID.randomUUID().toString();
        for (String identificador : new String[]{"usuario.db", "inexistente.db"}) {
            mvc.perform(formLogin().user(identificador).password(incorrecta))
                    .andExpect(unauthenticated()).andExpect(redirectedUrl("/login?error"));
        }
        mvc.perform(get("/login?error")).andExpect(content().string(containsString("Usuario o contraseña incorrectos.")));
    }

    @Test
    void unaClaveEnTextoPlanoEsRechazadaSinConvertirla() throws Exception {
        jdbc.update("UPDATE usuarios SET password=? WHERE id=?", CLAVE_USUARIO, usuarioId);
        mvc.perform(formLogin().user("usuario.db").password(CLAVE_USUARIO))
                .andExpect(unauthenticated()).andExpect(redirectedUrl("/login?error"));
        assertTrue(CLAVE_USUARIO.equals(jdbc.queryForObject("SELECT password FROM usuarios WHERE id=?", String.class, usuarioId)));
    }

    @Test
    void registroDesdeSesionUsuarioPermiteIniciarSesionConLaNuevaCuenta() throws Exception {
        var creador = entrar("usuario.db", CLAVE_USUARIO, "USUARIO");
        String claveNueva = UUID.randomUUID().toString();
        mvc.perform(post("/api/usuarios").session(creador).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(datos("nuevo.db", "USUARIO", claveNueva)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.clave").doesNotExist());
        var nuevaSesion = entrar("nuevo.db", claveNueva, "USUARIO");
        mvc.perform(get("/catalogo").session(nuevaSesion)).andExpect(status().isOk());
        mvc.perform(get("/api/usuarios").session(nuevaSesion)).andExpect(status().isForbidden());
    }

    @Test
    void cambioDeClaveRevocaTodasLasSesionesYRechazaLaAnterior() throws Exception {
        var admin = entrar("admin.db", CLAVE_ADMIN, "ADMIN");
        var primera = entrar("usuario.db", CLAVE_USUARIO, "USUARIO");
        var segunda = entrar("usuario.db", CLAVE_USUARIO, "USUARIO");
        String nuevaClave = UUID.randomUUID().toString();
        mvc.perform(put("/api/usuarios/" + usuarioId).session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(datos("usuario.db", "USUARIO", nuevaClave)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.password").doesNotExist());
        mvc.perform(get("/api/libros").session(primera)).andExpect(status().isUnauthorized());
        mvc.perform(get("/panel").session(segunda)).andExpect(redirectedUrl("/login?actualizada"));
        assertTrue(primera.isInvalid());
        assertTrue(segunda.isInvalid());
        mvc.perform(formLogin().user("usuario.db").password(CLAVE_USUARIO)).andExpect(unauthenticated());
        entrar("usuario.db", nuevaClave, "USUARIO");
        mvc.perform(get("/admin/usuarios").session(admin)).andExpect(status().isOk());
    }

    @Test
    void cambioDeRolRevocaLaSesionYElNuevoLoginUsaElRolGuardado() throws Exception {
        var admin = entrar("admin.db", CLAVE_ADMIN, "ADMIN");
        var anterior = entrar("usuario.db", CLAVE_USUARIO, "USUARIO");
        mvc.perform(put("/api/usuarios/" + usuarioId).session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(datos("usuario.db", "ADMIN", ""))).andExpect(status().isOk());
        mvc.perform(get("/api/libros").session(anterior)).andExpect(status().isUnauthorized());
        var actual = entrar("usuario.db", CLAVE_USUARIO, "ADMIN");
        mvc.perform(get("/api/usuarios").session(actual)).andExpect(status().isOk());
    }

    @Test
    void eliminarCuentaRevocaLaSesionYNoPermiteOtroLogin() throws Exception {
        var admin = entrar("admin.db", CLAVE_ADMIN, "ADMIN");
        var anterior = entrar("usuario.db", CLAVE_USUARIO, "USUARIO");
        mvc.perform(delete("/api/usuarios/" + usuarioId).session(admin).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/panel").session(anterior)).andExpect(redirectedUrl("/login?actualizada"));
        assertTrue(anterior.isInvalid());
        mvc.perform(formLogin().user("usuario.db").password(CLAVE_USUARIO)).andExpect(unauthenticated());
    }

    @Test
    void renombrarCuentaRevocaLaSesionSinCambiarLaClave() throws Exception {
        var admin = entrar("admin.db", CLAVE_ADMIN, "ADMIN");
        var anterior = entrar("usuario.db", CLAVE_USUARIO, "USUARIO");
        mvc.perform(put("/api/usuarios/" + usuarioId).session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(datos("renombrado.db", "USUARIO", ""))).andExpect(status().isOk());
        mvc.perform(get("/api/libros").session(anterior)).andExpect(status().isUnauthorized());
        mvc.perform(formLogin().user("usuario.db").password(CLAVE_USUARIO)).andExpect(unauthenticated());
        entrar("renombrado.db", CLAVE_USUARIO, "USUARIO");
    }

    @Test
    void cambiarSoloNombreORechazarUnDuplicadoNoRevocaLaSesion() throws Exception {
        var admin = entrar("admin.db", CLAVE_ADMIN, "ADMIN");
        var usuario = entrar("usuario.db", CLAVE_USUARIO, "USUARIO");
        mvc.perform(put("/api/usuarios/" + usuarioId).session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(datos("usuario.db", "USUARIO", ""))).andExpect(status().isOk());
        mvc.perform(get("/catalogo").session(usuario)).andExpect(status().isOk());
        mvc.perform(put("/api/usuarios/" + usuarioId).session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(datos("admin.db", "USUARIO", ""))).andExpect(status().isConflict());
        mvc.perform(get("/catalogo").session(usuario)).andExpect(status().isOk());
    }

    @Test
    void ultimoAdminNoPuedeEliminarseNiPerderElRol() throws Exception {
        var admin = entrar("admin.db", CLAVE_ADMIN, "ADMIN");
        mvc.perform(delete("/api/usuarios/" + adminId).session(admin).with(csrf())).andExpect(status().isConflict());
        mvc.perform(put("/api/usuarios/" + adminId).session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(datos("admin.db", "USUARIO", ""))).andExpect(status().isConflict());
        mvc.perform(get("/admin/usuarios").session(admin)).andExpect(status().isOk());
    }

    @Test
    void adminConClaveIncompatibleNoSustituyeAlUltimoAdminConAcceso() throws Exception {
        insertarCuenta("admin.sinacceso", "ADMIN", CLAVE_ADMIN);
        jdbc.update("UPDATE usuarios SET password=? WHERE usuario=?", UUID.randomUUID().toString(), "admin.sinacceso");
        var admin = entrar("admin.db", CLAVE_ADMIN, "ADMIN");
        mvc.perform(delete("/api/usuarios/" + adminId).session(admin).with(csrf())).andExpect(status().isConflict());
    }

    @Test
    void adminPuedeCambiarSuClavePeroDebeAutenticarseOtraVez() throws Exception {
        var admin = entrar("admin.db", CLAVE_ADMIN, "ADMIN");
        String nuevaClave = UUID.randomUUID().toString();
        mvc.perform(put("/api/usuarios/" + adminId).session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(datos("admin.db", "ADMIN", nuevaClave))).andExpect(status().isOk());
        mvc.perform(get("/admin/usuarios").session(admin)).andExpect(redirectedUrl("/login?actualizada"));
        entrar("admin.db", nuevaClave, "ADMIN");
    }

    @Test
    void adminPuedeDejarSuRolSiExisteOtroAdministrador() throws Exception {
        insertarCuenta("admin.respaldo", "ADMIN", CLAVE_ADMIN);
        var admin = entrar("admin.db", CLAVE_ADMIN, "ADMIN");
        mvc.perform(put("/api/usuarios/" + adminId).session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(datos("admin.db", "USUARIO", ""))).andExpect(status().isOk());
        mvc.perform(get("/api/usuarios").session(admin)).andExpect(status().isUnauthorized());
        var usuario = entrar("admin.db", CLAVE_ADMIN, "USUARIO");
        mvc.perform(get("/api/usuarios").session(usuario)).andExpect(status().isForbidden());
    }
}
