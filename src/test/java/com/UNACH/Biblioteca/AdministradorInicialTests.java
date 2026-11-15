package com.UNACH.Biblioteca;

import com.UNACH.Biblioteca.service.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import static org.junit.jupiter.api.Assertions.*;

class AdministradorInicialTests extends PruebaAislada {
    @Autowired AdministradorInicialService inicial;
    @Autowired UserDetailsService detalles;
    @Autowired ApplicationContext contexto;

    @Test
    void deshabilitadoPorDefectoNoCreaCuentasEnElArranque() {
        assertFalse(contexto.containsBean("prepararAdministrador"));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM usuarios", Integer.class));
    }

    @Test
    void creacionExplicitaGuardaUnAdminBCryptQuePuedeAutenticarse() {
        inicial.crear("primer.admin", "Administrador de prueba", CLAVE_ADMIN);
        var usuario = detalles.loadUserByUsername("PRIMER.ADMIN");
        assertTrue(passwordEncoder.matches(CLAVE_ADMIN, usuario.getPassword()));
        assertTrue(usuario.getAuthorities().stream().anyMatch(r -> r.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void segundoIntentoNoModificaElAdministradorExistente() {
        inicial.crear("primer.admin", "Administrador de prueba", CLAVE_ADMIN);
        String hash = jdbc.queryForObject("SELECT password FROM usuarios", String.class);
        assertThrows(ReglaNegocioException.class,
                () -> inicial.crear("otro.admin", "Otra cuenta", UUID.randomUUID().toString()));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM usuarios", Integer.class));
        assertTrue(hash.equals(jdbc.queryForObject("SELECT password FROM usuarios", String.class)));
    }

    @Test
    void crearNoSobrescribeUnIdentificadorExistenteAunqueNoSeaAdmin() {
        insertarUsuario();
        String hash = jdbc.queryForObject("SELECT password FROM usuarios", String.class);
        assertThrows(ReglaNegocioException.class,
                () -> inicial.crear("PERSONA.PRUEBA", "Otra cuenta", CLAVE_ADMIN));
        assertTrue(hash.equals(jdbc.queryForObject("SELECT password FROM usuarios", String.class)));
    }

    @Test
    void restablecimientoExplicitoRecuperaAdminIncompatibleSinCambiarRolNiIdentificador() {
        long id = insertarCuenta("admin.antiguo", "ADMIN", CLAVE_ADMIN);
        jdbc.update("UPDATE usuarios SET password=? WHERE id=?", UUID.randomUUID().toString(), id);
        assertThrows(UsernameNotFoundException.class, () -> detalles.loadUserByUsername("admin.antiguo"));
        assertEquals(1, inicial.revisar().clavesIncompatibles());
        inicial.restablecer("ADMIN.ANTIGUO", CLAVE_ADMIN);
        var usuario = detalles.loadUserByUsername("admin.antiguo");
        assertTrue(passwordEncoder.matches(CLAVE_ADMIN, usuario.getPassword()));
        assertEquals(1, inicial.revisar().administradoresCompatibles());
    }

    @Test
    void restablecimientoNoPromueveUsuarioANiCreaAdministradorInexistente() {
        insertarUsuario();
        assertThrows(ReglaNegocioException.class, () -> inicial.restablecer("persona.prueba", CLAVE_ADMIN));
        assertThrows(ReglaNegocioException.class, () -> inicial.restablecer("inexistente", CLAVE_ADMIN));
        assertEquals("USUARIO", jdbc.queryForObject("SELECT r.nombre FROM usuarios u JOIN roles r ON r.id=u.rol_id", String.class));
    }

    @Test
    void elProcedimientoValidaLosDatosAntesDeGuardar() {
        assertThrows(ReglaNegocioException.class, () -> inicial.crear("", "", CLAVE_ADMIN));
        assertThrows(ReglaNegocioException.class, () -> inicial.crear("primer.admin", "Prueba", null));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM usuarios", Integer.class));
    }
}
