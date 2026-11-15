package com.UNACH.Biblioteca;

import com.UNACH.Biblioteca.model.*;
import com.UNACH.Biblioteca.model.form.SolicitudPrestamoFormulario;
import com.UNACH.Biblioteca.model.form.OperacionPrestamoFormulario;
import com.UNACH.Biblioteca.repository.*;
import com.UNACH.Biblioteca.service.*;
import jakarta.persistence.EntityManager;
import java.time.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.mock.web.MockHttpSession;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WithMockUser(username = "operador", roles = "ADMIN")
class PrestamosTests extends PruebaAislada {
    @Autowired PrestamoService servicio;
    @Autowired LibroRepository libros;
    @Autowired LectorRepository lectores;
    @Autowired PrestamoRepository prestamos;
    @Autowired HistorialPrestamoRepository historial;
    @Autowired IncidenciaRepository incidencias;
    @Autowired UsuarioRepository usuarios;
    @Autowired EntityManager entidades;
    @Autowired Clock reloj;
    private long libroId;
    private long adminId;
    private static final String CODIGO = "PRUEBA-PRESTAMO-A";

    @BeforeEach
    void preparar() {
        libroId = insertarLibro("Ejemplar ficticio de préstamos", "Autora ficticia", CODIGO);
        adminId = insertarCuenta("operador", "ADMIN", CLAVE_ADMIN);
    }

    private long solicitar(String codigo) throws Exception {
        mvc.perform(post("/prestamos/solicitar").with(csrf()).with(anonymous())
                .param("matricula", "00001234").param("nombreCompleto", "Persona Lectora Ficticia")
                .param("correoInstitucional", "lectora@institucion.test").param("codigoBarras", codigo)
                .param("estado", "PRESTADO").param("usuario_id", Long.toString(adminId)))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/prestamos/solicitar"));
        return jdbc.queryForObject("SELECT MAX(id) FROM prestamos", Long.class);
    }

    private void confirmar(long id) throws Exception {
        mvc.perform(post("/admin/prestamos/" + id + "/confirmar").with(csrf())
                .param("fechaLimite", LocalDate.now(reloj).plusDays(3).toString()).param("observacion", "Entrega de prueba"))
                .andExpect(redirectedUrl("/admin/prestamos/" + id));
    }

    @Test
    void solicitudAnonimaNoEsEntregaYConservaLaMatriculaConCeros() throws Exception {
        long id = solicitar(CODIGO);
        assertEquals("SOLICITADO", jdbc.queryForObject("SELECT estado FROM prestamos WHERE id=?", String.class, id));
        assertNull(jdbc.queryForObject("SELECT usuario_id FROM prestamos WHERE id=?", Long.class, id));
        assertNull(jdbc.queryForObject("SELECT fecha_prestamo FROM prestamos WHERE id=?", LocalDateTime.class, id));
        assertNull(jdbc.queryForObject("SELECT fecha_limite_devolucion FROM prestamos WHERE id=?", LocalDate.class, id));
        assertNull(jdbc.queryForObject("SELECT fecha_devolucion FROM prestamos WHERE id=?", LocalDateTime.class, id));
        assertEquals("00001234", jdbc.queryForObject("SELECT matricula FROM lectores", String.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM historial_prestamos", Integer.class));
        assertEquals("SOLICITUD", jdbc.queryForObject("SELECT accion FROM historial_prestamos", String.class));
        assertNull(jdbc.queryForObject("SELECT usuario_id FROM historial_prestamos", Long.class));
        mvc.perform(get("/catalogo").with(anonymous())).andExpect(content().string(containsString("SOLICITADO")))
                .andExpect(content().string(not(containsString("lectora@institucion.test"))));
        mvc.perform(get("/api/libros").with(anonymous()))
                .andExpect(content().string(not(containsString("lectora@institucion.test"))));
    }

    @Test
    void solicitudEntregaDevolucionEHistorialFuncionanSinModificarElInventario() throws Exception {
        long id = solicitar(CODIGO);
        confirmar(id);
        assertEquals("PRESTADO", jdbc.queryForObject("SELECT estado FROM prestamos WHERE id=?", String.class, id));
        assertEquals(adminId, jdbc.queryForObject("SELECT usuario_id FROM prestamos WHERE id=?", Long.class, id));
        assertNotNull(jdbc.queryForObject("SELECT fecha_prestamo FROM prestamos WHERE id=?", LocalDateTime.class, id));
        assertEquals(LocalDate.now(reloj).plusDays(3), jdbc.queryForObject("SELECT fecha_limite_devolucion FROM prestamos WHERE id=?", LocalDate.class, id));
        mvc.perform(get("/catalogo")).andExpect(content().string(containsString("PRESTADO")));
        mvc.perform(post("/admin/prestamos/" + id + "/devolver").with(csrf()).param("observacion", "Devuelto de prueba"))
                .andExpect(redirectedUrl("/admin/prestamos/" + id));
        assertEquals("DEVUELTO", jdbc.queryForObject("SELECT estado FROM prestamos WHERE id=?", String.class, id));
        assertNotNull(jdbc.queryForObject("SELECT fecha_devolucion FROM prestamos WHERE id=?", LocalDateTime.class, id));
        assertEquals(3, jdbc.queryForObject("SELECT COUNT(*) FROM historial_prestamos WHERE prestamo_id=?", Integer.class, id));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM historial_prestamos WHERE usuario_id=?", Integer.class, adminId));
        mvc.perform(get("/admin/prestamos/" + id)).andExpect(status().isOk())
                .andExpect(content().string(containsString("CONFIRMACION")))
                .andExpect(content().string(containsString("DEVOLUCION")))
                .andExpect(content().string(containsString("Cerrar sesión")));
        assertEquals(CODIGO, jdbc.queryForObject("SELECT codigo_barras FROM inventario_libros WHERE id=?", String.class, libroId));
        assertEquals("Ejemplar ficticio de préstamos", jdbc.queryForObject("SELECT titulo FROM inventario_libros WHERE id=?", String.class, libroId));
        solicitar(CODIGO);
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM prestamos", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM lectores", Integer.class));
    }

    @Test
    void rechazoLiberaElEjemplarYConservaElHistorial() throws Exception {
        long id = solicitar(CODIGO);
        mvc.perform(post("/admin/prestamos/" + id + "/rechazar").with(csrf()).param("observacion", "Solicitud de prueba rechazada"))
                .andExpect(redirectedUrl("/admin/prestamos/" + id));
        assertEquals("RECHAZADO", jdbc.queryForObject("SELECT estado FROM prestamos WHERE id=?", String.class, id));
        assertNull(jdbc.queryForObject("SELECT fecha_prestamo FROM prestamos WHERE id=?", LocalDateTime.class, id));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM historial_prestamos", Integer.class));
        mvc.perform(get("/catalogo")).andExpect(content().string(containsString("Disponible para solicitud")));
        solicitar(CODIGO);
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM prestamos", Integer.class));
    }

    @Test
    void dosSolicitudesActivasDelMismoEjemplarSonRechazadas() throws Exception {
        solicitar(CODIGO);
        mvc.perform(post("/prestamos/solicitar").with(csrf()).with(anonymous()).param("matricula", "OTRA")
                .param("nombreCompleto", "Otra persona").param("correoInstitucional", "otra@institucion.test")
                .param("codigoBarras", CODIGO)).andExpect(status().isConflict())
                .andExpect(content().string(containsString("ya está solicitado o prestado")));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM prestamos", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM lectores", Integer.class));
    }

    @Test
    void ejemplaresConElMismoTituloYCodigosDistintosSeSolicitanPorSeparado() throws Exception {
        insertarLibro("Ejemplar ficticio de préstamos", "Autora ficticia", "PRUEBA-PRESTAMO-B");
        solicitar(CODIGO);
        solicitar("PRUEBA-PRESTAMO-B");
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM prestamos", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM lectores", Integer.class));
    }

    @Test
    void validaDatosCodigosExactosYNoCreaLectoresCuandoFalla() throws Exception {
        mvc.perform(post("/prestamos/solicitar").with(csrf()).with(anonymous()).param("matricula", " ").param("correoInstitucional", "invalido"))
                .andExpect(status().isBadRequest());
        for (String codigo : new String[]{"NO-EXISTE", CODIGO.toLowerCase()}) {
            mvc.perform(post("/prestamos/solicitar").with(csrf()).with(anonymous()).param("matricula", "00001234")
                    .param("nombreCompleto", "Persona Lectora Ficticia").param("correoInstitucional", "lectora@institucion.test")
                    .param("codigoBarras", codigo)).andExpect(status().isNotFound());
        }
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM lectores", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM prestamos", Integer.class));
    }

    @Test
    void noSeSobrescribenDatosDeUnLectorRegistrado() throws Exception {
        solicitar(CODIGO);
        insertarLibro("Otro ejemplar ficticio", null, "PRUEBA-PRESTAMO-B");
        mvc.perform(post("/prestamos/solicitar").with(csrf()).with(anonymous()).param("matricula", "00001234")
                .param("nombreCompleto", "Otra persona").param("correoInstitucional", "otra@institucion.test")
                .param("codigoBarras", "PRUEBA-PRESTAMO-B")).andExpect(status().isConflict());
        assertEquals("Persona Lectora Ficticia", jdbc.queryForObject("SELECT nombre_completo FROM lectores", String.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM prestamos", Integer.class));
    }

    @Test
    void exigeFechaExplicitaSinPlazoOficialYRechazaUnaFechaPasada() throws Exception {
        assertNull(servicio.fechaLimiteSugerida());
        long id = solicitar(CODIGO);
        mvc.perform(post("/admin/prestamos/" + id + "/confirmar").with(csrf())).andExpect(status().isBadRequest());
        mvc.perform(post("/admin/prestamos/" + id + "/confirmar").with(csrf())
                .param("fechaLimite", LocalDate.now(reloj).minusDays(1).toString())).andExpect(status().isBadRequest());
        assertEquals("SOLICITADO", jdbc.queryForObject("SELECT estado FROM prestamos WHERE id=?", String.class, id));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM historial_prestamos", Integer.class));
    }

    @Test
    void lasOperacionesNoSePuedenRepetirNiSaltarEstados() throws Exception {
        long id = solicitar(CODIGO);
        mvc.perform(post("/admin/prestamos/" + id + "/devolver").with(csrf())).andExpect(status().isConflict());
        confirmar(id);
        mvc.perform(post("/admin/prestamos/" + id + "/confirmar").with(csrf()).param("fechaLimite", LocalDate.now(reloj).plusDays(3).toString()))
                .andExpect(status().isConflict());
        mvc.perform(post("/admin/prestamos/" + id + "/rechazar").with(csrf())).andExpect(status().isConflict());
        mvc.perform(post("/admin/prestamos/" + id + "/devolver").with(csrf())).andExpect(status().is3xxRedirection());
        mvc.perform(post("/admin/prestamos/" + id + "/devolver").with(csrf())).andExpect(status().isConflict());
        assertEquals(3, jdbc.queryForObject("SELECT COUNT(*) FROM historial_prestamos", Integer.class));
    }

    @Test
    @WithMockUser(username = "terminal", roles = "USUARIO")
    void usuarioSoloPuedeSolicitarYNoGestionarNiConsultarDatosDePrestamos() throws Exception {
        comprobarServiciosAdministrativosProtegidos();
        long id = solicitar(CODIGO);
        mvc.perform(get("/panel")).andExpect(status().isForbidden());
        mvc.perform(get("/admin/prestamos")).andExpect(status().isForbidden());
        mvc.perform(get("/admin/prestamos/" + id)).andExpect(status().isForbidden());
        for (String accion : new String[]{"confirmar", "rechazar", "devolver"}) {
            mvc.perform(post("/admin/prestamos/" + id + "/" + accion).with(csrf())).andExpect(status().isForbidden());
        }
        assertEquals("SOLICITADO", jdbc.queryForObject("SELECT estado FROM prestamos WHERE id=?", String.class, id));
    }

    @Test
    void formularioPublicoEnviaSuTokenCsrfSinNecesitarLogin() throws Exception {
        var pagina = mvc.perform(get("/prestamos/solicitar").with(anonymous()).param("codigo", CODIGO))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"" + CODIGO + "\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(not(containsString("Cerrar sesión"))))
                .andExpect(content().string(not(containsString("href=\"/panel\"")))).andReturn();
        CsrfToken token = (CsrfToken) pagina.getRequest().getAttribute(CsrfToken.class.getName());
        assertNotNull(token);
        assertTrue(pagina.getResponse().getContentAsString().contains("value=\"" + token.getToken() + "\""));
        var sesion = (MockHttpSession) pagina.getRequest().getSession(false);
        assertNotNull(sesion);
        mvc.perform(post("/prestamos/solicitar").with(anonymous())).andExpect(status().isForbidden());
        mvc.perform(post("/prestamos/solicitar").with(anonymous()).with(csrf().useInvalidToken()))
                .andExpect(status().isForbidden());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM prestamos", Integer.class));
        // Usa el token renderizado por Thymeleaf, igual que el navegador; no se simula CSRF aquí.
        mvc.perform(post("/prestamos/solicitar").with(anonymous()).session(sesion)
                .param(token.getParameterName(), token.getToken())
                .param("matricula", "00001234").param("nombreCompleto", "Persona Lectora Ficticia")
                .param("correoInstitucional", "lectora@institucion.test").param("codigoBarras", CODIGO))
                .andExpect(redirectedUrl("/prestamos/solicitar"));
        long id = jdbc.queryForObject("SELECT MAX(id) FROM prestamos", Long.class);
        mvc.perform(post("/admin/prestamos/" + id + "/confirmar")).andExpect(status().isForbidden());
        mvc.perform(get("/admin/prestamos/" + id)).andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    @WithAnonymousUser
    void anonimoNoPuedeAbrirPanelListarDatosPrivadosNiGestionarSolicitudes() throws Exception {
        comprobarServiciosAdministrativosProtegidos();
        long id = solicitar(CODIGO);
        for (String ruta : new String[]{"/panel", "/admin/prestamos", "/admin/prestamos/" + id}) {
            mvc.perform(get(ruta)).andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login"))
                    .andExpect(content().string(not(containsString("lectora@institucion.test"))));
        }
        for (String accion : new String[]{"confirmar", "rechazar", "devolver"}) {
            mvc.perform(post("/admin/prestamos/" + id + "/" + accion).with(csrf()))
                    .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        }
        for (String ruta : new String[]{"/prestamos/" + id, "/lectores", "/lectores/1", "/api/lectores",
                "/api/prestamos", "/api/prestamos/" + id, "/api/historial_prestamos"}) {
            mvc.perform(get(ruta)).andExpect(status().is(ruta.startsWith("/api/") ? 401 : 302))
                    .andExpect(content().string(not(containsString("lectora@institucion.test"))));
        }
        assertEquals("SOLICITADO", jdbc.queryForObject("SELECT estado FROM prestamos WHERE id=?", String.class, id));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM historial_prestamos", Integer.class));
    }

    @Test
    @WithAnonymousUser
    void servicioAceptaSolicitudesAnonimasSinAsignarAdministrador() {
        var datos = new SolicitudPrestamoFormulario();
        datos.setMatricula("00001234"); datos.setNombreCompleto("Persona Lectora Ficticia");
        datos.setCorreoInstitucional("lectora@institucion.test"); datos.setCodigoBarras(CODIGO);
        var prestamo = servicio.solicitar(datos);
        assertEquals(EstadoPrestamo.SOLICITADO, prestamo.getEstado());
        assertNull(prestamo.getAdministrador());
        assertNull(prestamo.getFechaPrestamo());
    }

    private void comprobarServiciosAdministrativosProtegidos() {
        var datos = new OperacionPrestamoFormulario();
        assertThrows(AccessDeniedException.class, () -> servicio.listar(EstadoPrestamo.SOLICITADO, 0));
        assertThrows(AccessDeniedException.class, () -> servicio.obtener(1L));
        assertThrows(AccessDeniedException.class, () -> servicio.consultarHistorial(1L));
        assertThrows(AccessDeniedException.class, () -> servicio.contarPorEstado());
        assertThrows(AccessDeniedException.class, () -> servicio.confirmar(1L, datos));
        assertThrows(AccessDeniedException.class, () -> servicio.rechazar(1L, datos));
        assertThrows(AccessDeniedException.class, () -> servicio.devolver(1L, datos));
    }

    @Test
    void listaPorEstadosPaginacionYPanelRenderizanLasSolicitudes() throws Exception {
        long id = solicitar(CODIGO);
        for (EstadoPrestamo estado : EstadoPrestamo.values()) {
            mvc.perform(get("/admin/prestamos").param("estado", estado.name())).andExpect(status().isOk());
        }
        mvc.perform(get("/admin/prestamos").param("pagina", "99999")).andExpect(status().isOk())
                .andExpect(content().string(containsString("#" + id)));
        mvc.perform(get("/panel")).andExpect(content().string(containsString("Solicitudes pendientes")));
        mvc.perform(get("/admin/prestamos/999999")).andExpect(status().isNotFound());
        mvc.perform(post("/admin/prestamos/999999/devolver").with(csrf())).andExpect(status().isNotFound());
    }

    @Test
    void laBaseImpideUnSegundoActivoInclusoSiSeOmiteElServicio() throws Exception {
        long id = solicitar(CODIGO);
        long lectorId = jdbc.queryForObject("SELECT lector_id FROM prestamos WHERE id=?", Long.class, id);
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO prestamos(lector_id,inventario_libro_id,fecha_solicitud,estado) VALUES(?,?,CURRENT_TIMESTAMP,'SOLICITADO')", lectorId, libroId));
    }

    @Test
    void lasRelacionesImpidenBorrarLibrosLectoresPrestamosYAdministradoresAuditados() throws Exception {
        long id = solicitar(CODIGO);
        confirmar(id);
        long lectorId = jdbc.queryForObject("SELECT lector_id FROM prestamos WHERE id=?", Long.class, id);
        mvc.perform(delete("/api/libros/" + libroId).with(csrf())).andExpect(status().isConflict());
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("DELETE FROM lectores WHERE id=?", lectorId));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("DELETE FROM prestamos WHERE id=?", id));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("DELETE FROM usuarios WHERE id=?", adminId));
    }

    @Test
    void soloHaySeisEntidadesYSeisTablasRelacionadas() {
        assertEquals(7, entidades.getMetamodel().getEntities().size());
        assertEquals(7, jdbc.queryForObject("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='PUBLIC' AND TABLE_TYPE='BASE TABLE'", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM usuarios u LEFT JOIN roles r ON u.rol_id=r.id WHERE r.id IS NULL", Integer.class));
    }

    @Test
    void lasPoliticasDePlazoYCorreoSonConfigurables() {
        Clock fijo = Clock.fixed(Instant.parse("2026-10-18T18:00:00Z"), ZoneId.of("America/Mexico_City"));
        var configurado = new PrestamoService(libros, lectores, prestamos, historial, incidencias, usuarios, fijo, 7, "permitido.test");
        assertEquals(LocalDate.of(2026, 10, 25), configurado.fechaLimiteSugerida());
        var datos = new SolicitudPrestamoFormulario();
        datos.setMatricula("00001234"); datos.setNombreCompleto("Prueba");
        datos.setCodigoBarras(CODIGO); datos.setCorreoInstitucional("prueba@no-permitido.test");
        assertThrows(ReglaNegocioException.class, () -> configurado.solicitar(datos));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM prestamos", Integer.class));
    }
}
