package com.UNACH.Biblioteca.config;

import java.time.*;
import org.springframework.context.annotation.*;

@Configuration
public class PrestamosConfig {
    @Bean
    public Clock relojBiblioteca() { return Clock.system(ZoneId.of("America/Mexico_City")); }
}
