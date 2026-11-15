package com.UNACH.Biblioteca;

import com.UNACH.Biblioteca.model.form.SolicitudPrestamoFormulario;
import com.UNACH.Biblioteca.service.PrestamoService;
import com.UNACH.Biblioteca.service.ReglaNegocioException;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PrestamosConcurrentesTests extends PruebaAislada {
    private static final String CODIGO = "SOLO-H2-CONCURRENCIA";
    @Autowired PrestamoService prestamos;

    @BeforeEach
    void preparar() {
        insertarLibro("Ejemplar ficticio concurrente", null, CODIGO);
    }

    @AfterEach
    void limpiarSoloLosDatosDeEsteCaso() throws Exception {
        comprobarAislamiento();
        jdbc.update("DELETE FROM incidencias_prestamos WHERE prestamo_id IN (SELECT p.id FROM prestamos p JOIN inventario_libros l ON l.id=p.inventario_libro_id WHERE l.codigo_barras=?)", CODIGO);
        jdbc.update("DELETE FROM historial_prestamos WHERE prestamo_id IN (SELECT p.id FROM prestamos p JOIN inventario_libros l ON l.id=p.inventario_libro_id WHERE l.codigo_barras=?)", CODIGO);
        jdbc.update("DELETE FROM prestamos WHERE inventario_libro_id IN (SELECT id FROM inventario_libros WHERE codigo_barras=?)", CODIGO);
        jdbc.update("DELETE FROM lectores WHERE matricula IN ('CONCURRENTE-1','CONCURRENTE-2')");
        jdbc.update("DELETE FROM inventario_libros WHERE codigo_barras=?", CODIGO);
    }

    @Test
    void dosTransaccionesSimultaneasProducenUnaSolicitudYUnConflicto() throws Exception {
        CountDownLatch preparados = new CountDownLatch(2);
        CountDownLatch inicio = new CountDownLatch(1);
        try (var ejecutor = Executors.newFixedThreadPool(2)) {
            var uno = ejecutor.submit(() -> solicitar(1, preparados, inicio));
            var dos = ejecutor.submit(() -> solicitar(2, preparados, inicio));
            assertTrue(preparados.await(5, TimeUnit.SECONDS));
            inicio.countDown();
            var resultados = List.of(uno.get(10, TimeUnit.SECONDS), dos.get(10, TimeUnit.SECONDS));
            assertEquals(1, resultados.stream().filter("CREADO"::equals).count());
            assertEquals(1, resultados.stream().filter("CONFLICTO"::equals).count());
        } finally { inicio.countDown(); }
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM prestamos WHERE estado='SOLICITADO'", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM historial_prestamos", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM lectores", Integer.class));
    }

    private String solicitar(int numero, CountDownLatch preparados, CountDownLatch inicio) throws Exception {
        try {
            SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                    "prueba", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
            var datos = new SolicitudPrestamoFormulario();
            datos.setMatricula("CONCURRENTE-" + numero);
            datos.setNombreCompleto("Persona ficticia " + numero);
            datos.setCorreoInstitucional("ficticia" + numero + "@institucion.test");
            datos.setCodigoBarras(CODIGO);
            preparados.countDown();
            if (!inicio.await(5, TimeUnit.SECONDS)) { throw new IllegalStateException("No se inició la prueba concurrente."); }
            try { prestamos.solicitar(datos); return "CREADO"; }
            catch (ReglaNegocioException error) {
                assertEquals(HttpStatus.CONFLICT, error.getEstado());
                return "CONFLICTO";
            }
        } finally { SecurityContextHolder.clearContext(); }
    }
}
