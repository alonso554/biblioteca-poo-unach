package com.UNACH.Biblioteca;

import com.UNACH.Biblioteca.model.*;
import com.UNACH.Biblioteca.model.form.*;
import com.UNACH.Biblioteca.service.PrestamoService;
import java.time.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import jakarta.persistence.EntityManager;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WithMockUser(username = "historial.admin", roles = "ADMIN")
class HistorialesIncidenciasTests extends PruebaAislada {
    @Autowired PrestamoService servicio;
    @Autowired EntityManager entidades;
    private long libroUno, libroDos, prestamoUno;

    @BeforeEach
    void preparar() {
        libroUno = insertarLibro("Obra compartida", "Autora", "HIST-1");
        libroDos = insertarLibro("Obra compartida", "Autora", "HIST-2");
        insertarCuenta("historial.admin", "ADMIN", CLAVE_ADMIN);
        prestamoUno = solicitar(libroUno, "HIST-L-1");
        confirmar(prestamoUno, LocalDate.now().plusDays(1));
        jdbc.update("UPDATE prestamos SET fecha_limite_devolucion=? WHERE id=?", servicio.hoy().minusDays(2), prestamoUno);
        entidades.clear();
    }

    @Test
    void separaEjemplaresYCalculaAtrasosSinFechaInventada() throws Exception {
        long otro = solicitar(libroDos, "HIST-L-2");
        var individual = servicio.historialLibro(libroUno, false, null, false, null, null, 0);
        var agrupado = servicio.historialLibro(libroUno, true, null, false, null, null, 0);
        assertEquals(1, individual.resumen().solicitudes());
        assertEquals(2, agrupado.resumen().solicitudes());
        assertEquals(1, individual.resumen().vencidos());
        assertTrue(individual.registros().getContent().getFirst().diasAtraso() >= 1);
        assertEquals(EstadoPrestamo.SOLICITADO, servicio.obtener(otro).getEstado());
        mvc.perform(get("/admin/historial/libros/" + libroUno)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("HIST-1")));
    }

    @Test
    void conservaIncidenciaYBloqueaDisponibilidadHastaResolverla() throws Exception {
        var incidencia = new IncidenciaFormulario(); incidencia.setTipo(TipoIncidencia.DANO); incidencia.setDescripcion("Portada rota");
        servicio.registrarIncidencia(prestamoUno, incidencia);
        var operacion = new OperacionPrestamoFormulario(); servicio.devolver(prestamoUno, operacion);
        assertEquals(EstadoEjemplar.NO_DISPONIBLE, servicio.estadosEjemplares(java.util.List.of(libroUno)).get(libroUno));
        long id = jdbc.queryForObject("SELECT id FROM incidencias_prestamos", Long.class);
        var solucion = new ResolucionIncidenciaFormulario(); solucion.setSolucion("Reparado y revisado");
        servicio.resolverIncidencia(prestamoUno, id, solucion);
        assertEquals(EstadoEjemplar.DISPONIBLE, servicio.estadosEjemplares(java.util.List.of(libroUno)).get(libroUno));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM incidencias_prestamos", Integer.class));
        mvc.perform(get("/admin/historial/lectores")).andExpect(status().isOk());
    }

    private long solicitar(long libro, String matricula) {
        var datos = new SolicitudPrestamoFormulario(); datos.setCodigoBarras(jdbc.queryForObject("SELECT codigo_barras FROM inventario_libros WHERE id=?", String.class, libro));
        datos.setMatricula(matricula); datos.setNombreCompleto("Lector " + matricula); datos.setCorreoInstitucional(matricula.toLowerCase() + "@test.edu");
        return servicio.solicitar(datos).getId();
    }
    private void confirmar(long id, LocalDate limite) { var datos = new OperacionPrestamoFormulario(); datos.setFechaLimite(limite); servicio.confirmar(id, datos); }
}
