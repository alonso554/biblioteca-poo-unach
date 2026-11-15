package com.UNACH.Biblioteca.config;

import com.UNACH.Biblioteca.service.AdministradorInicialService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class AdministradorInicialConfig {
    @Bean
    @ConditionalOnProperty(name = "biblioteca.admin-inicial.habilitado", havingValue = "true")
    public ApplicationRunner prepararAdministrador(AdministradorInicialService servicio, Environment entorno,
            @Value("${biblioteca.admin-inicial.accion:revisar}") String accion,
            @Value("${BIBLIOTECA_ADMIN_INICIAL_USUARIO:}") String identificador,
            @Value("${BIBLIOTECA_ADMIN_INICIAL_NOMBRE:}") String nombre,
            @Value("${BIBLIOTECA_ADMIN_INICIAL_PASSWORD:}") String clave) {
        return argumentos -> {
            if (!"none".equalsIgnoreCase(entorno.getProperty("spring.main.web-application-type"))) {
                throw new IllegalStateException("El procedimiento inicial solo se ejecuta sin servidor web, mediante scripts/administrador.ps1.");
            }
            switch (accion) {
                case "revisar" -> {
                    var resumen = servicio.revisar();
                    System.out.printf("Usuarios: %d; ADMIN con BCrypt compatible: %d; claves incompatibles: %d.%n",
                            resumen.usuarios(), resumen.administradoresCompatibles(), resumen.clavesIncompatibles());
                }
                case "crear" -> {
                    servicio.crear(identificador, nombre, clave);
                    System.out.println("Administrador creado. Ya puede iniciar sesión con las credenciales proporcionadas.");
                }
                case "restablecer" -> {
                    servicio.restablecer(identificador, clave);
                    System.out.println("Contraseña del administrador restablecida explícitamente.");
                }
                default -> throw new IllegalArgumentException("Acción inicial inválida. Usa revisar, crear o restablecer.");
            }
        };
    }
}
