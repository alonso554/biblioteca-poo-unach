package com.UNACH.Biblioteca.model;

import java.util.List;
import org.springframework.data.domain.Page;

public record PaginaRespuesta<T>(List<T> contenido, int pagina, int tamano,
                                  long totalElementos, int totalPaginas) {
    public static <T> PaginaRespuesta<T> de(Page<T> pagina) {
        return new PaginaRespuesta<>(pagina.getContent(), pagina.getNumber(),
                pagina.getSize(), pagina.getTotalElements(), pagina.getTotalPages());
    }
}
