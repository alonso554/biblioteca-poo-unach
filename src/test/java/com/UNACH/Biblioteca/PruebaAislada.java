package com.UNACH.Biblioteca;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {"logging.level.root=WARN", "debug=false"})
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Transactional
public abstract class PruebaAislada {
    // Secretos efímeros únicamente para probar el login; nunca se escriben en archivos.
    protected static final String CLAVE_ADMIN = UUID.randomUUID().toString();
    protected static final String CLAVE_USUARIO = UUID.randomUUID().toString();

    @Autowired protected MockMvc mvc;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected PasswordEncoder passwordEncoder;

    @DynamicPropertySource
    static void baseAislada(DynamicPropertyRegistry propiedades) {
        // Estas propiedades tienen prioridad incluso sobre variables de entorno del equipo.
        propiedades.add("spring.datasource.url", () -> "jdbc:h2:mem:entrega_mvc;MODE=MariaDB;DB_CLOSE_DELAY=-1");
        propiedades.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        propiedades.add("spring.datasource.username", () -> "sa");
        propiedades.add("spring.datasource.password", () -> "");
        propiedades.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.H2Dialect");
        propiedades.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        propiedades.add("spring.sql.init.mode", () -> "always");
        propiedades.add("spring.sql.init.schema-locations", () -> "classpath:sql/esquema-pruebas.sql");
        propiedades.add("biblioteca.admin-inicial.habilitado", () -> "false");
        propiedades.add("biblioteca.prestamos.plazo-dias", () -> "0");
        propiedades.add("biblioteca.prestamos.dominios-correo", () -> "");
        propiedades.add("logging.level.root", () -> "WARN");
        propiedades.add("debug", () -> "false");
    }

    @BeforeEach
    void comprobarAislamiento() throws Exception {
        try (var conexion = jdbc.getDataSource().getConnection()) {
            assertTrue(conexion.getMetaData().getURL().startsWith("jdbc:h2:mem:"));
        }
    }

    protected long insertarLibro(String titulo, String autor, String codigo) {
        jdbc.update("INSERT INTO inventario_libros(titulo,autor,codigo_barras,descripcion_original) VALUES(?,?,?,?)",
                titulo, autor, codigo, "Descripción de un ejemplar ficticio de prueba");
        return jdbc.queryForObject("SELECT id FROM inventario_libros WHERE codigo_barras=?", Long.class, codigo);
    }

    protected long insertarUsuario() {
        jdbc.update("INSERT INTO usuarios(nombre,usuario,password,rol_id) VALUES(?,?,?,(SELECT id FROM roles WHERE nombre=?))",
                "Persona de prueba", "persona.prueba", passwordEncoder.encode(CLAVE_USUARIO), "USUARIO");
        return jdbc.queryForObject("SELECT id FROM usuarios WHERE usuario=?", Long.class, "persona.prueba");
    }

    protected long insertarCuenta(String identificador, String rol, String clave) {
        jdbc.update("INSERT INTO usuarios(nombre,usuario,password,rol_id) VALUES(?,?,?,(SELECT id FROM roles WHERE nombre=?))",
                "Cuenta ficticia de prueba", identificador, passwordEncoder.encode(clave), rol);
        return jdbc.queryForObject("SELECT id FROM usuarios WHERE usuario=?", Long.class, identificador);
    }
}
