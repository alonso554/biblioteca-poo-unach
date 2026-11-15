package com.UNACH.Biblioteca;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BibliotecaApplication {

	public static void main(String[] args) {
		var contexto = SpringApplication.run(BibliotecaApplication.class, args);
		if (contexto.getEnvironment().getProperty("biblioteca.admin-inicial.habilitado", Boolean.class, false)) {
			// El procedimiento local termina después de una sola operación.
			SpringApplication.exit(contexto);
		}
	}

}
