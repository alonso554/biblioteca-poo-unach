package com.UNACH.Biblioteca.service;

import com.UNACH.Biblioteca.model.Libro;
import com.UNACH.Biblioteca.repository.LibroRepository;
import com.UNACH.Biblioteca.repository.PrestamoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LibroServiceTests {

    @Mock
    private LibroRepository libroRepository;

    @Mock
    private PrestamoRepository prestamos;

    private LibroService libroService;

    @BeforeEach
    void prepararServicio() {
        libroService = new LibroService(libroRepository, prestamos);
    }

    @Test
    void buscarPorTituloRecortaTextoYConservaPaginacion() {
        Page<Libro> resultados = Page.empty();
        when(libroRepository.findByTituloContainingIgnoreCase(eq("Java"), any(Pageable.class)))
                .thenReturn(resultados);

        assertSame(resultados, libroService.buscarLibros("  Java  ", "titulo", 2));

        ArgumentCaptor<Pageable> paginacion = ArgumentCaptor.forClass(Pageable.class);
        verify(libroRepository).findByTituloContainingIgnoreCase(eq("Java"), paginacion.capture());
        assertEquals(2, paginacion.getValue().getPageNumber());
        assertEquals(20, paginacion.getValue().getPageSize());
        assertEquals(Sort.by(Sort.Order.asc("titulo"), Sort.Order.asc("id")),
                paginacion.getValue().getSort());
        verifyNoMoreInteractions(libroRepository);
    }

    @Test
    void buscarPorAutorUsaElFiltroSeleccionado() {
        Page<Libro> resultados = Page.empty();
        when(libroRepository.findByAutorContainingIgnoreCase(eq("García"), any(Pageable.class)))
                .thenReturn(resultados);

        assertSame(resultados, libroService.buscarLibros("García", "autor", 0));

        verify(libroRepository).findByAutorContainingIgnoreCase(eq("García"), any(Pageable.class));
        verifyNoMoreInteractions(libroRepository);
    }

    @Test
    void buscarPorCodigoUsaElFiltroSeleccionado() {
        Page<Libro> resultados = Page.empty();
        when(libroRepository.findByCodigoBarrasContainingIgnoreCase(eq("ABC123"), any(Pageable.class)))
                .thenReturn(resultados);

        assertSame(resultados, libroService.buscarLibros("ABC123", "codigo", 0));

        verify(libroRepository).findByCodigoBarrasContainingIgnoreCase(eq("ABC123"), any(Pageable.class));
        verifyNoMoreInteractions(libroRepository);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"todos", "desconocido"})
    void buscarEnTodosLosCamposCuandoNoHayFiltroEspecifico(String filtro) {
        Page<Libro> resultados = Page.empty();
        when(libroRepository
                .findByTituloContainingIgnoreCaseOrAutorContainingIgnoreCaseOrCodigoBarrasContainingIgnoreCase(
                        eq("Java"), eq("Java"), eq("Java"), any(Pageable.class)))
                .thenReturn(resultados);

        assertSame(resultados, libroService.buscarLibros("  Java  ", filtro, 0));

        verify(libroRepository)
                .findByTituloContainingIgnoreCaseOrAutorContainingIgnoreCaseOrCodigoBarrasContainingIgnoreCase(
                        eq("Java"), eq("Java"), eq("Java"), any(Pageable.class));
        verifyNoMoreInteractions(libroRepository);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void busquedaVaciaConsultaTodosLosEjemplares(String texto) {
        Page<Libro> resultados = Page.empty();
        when(libroRepository.findAll(any(Pageable.class))).thenReturn(resultados);

        assertSame(resultados, libroService.buscarLibros(texto, "autor", 0));

        verify(libroRepository).findAll(any(Pageable.class));
        verifyNoMoreInteractions(libroRepository);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, Integer.MAX_VALUE / 20 + 1, Integer.MAX_VALUE})
    void paginaInvalidaVuelveALaPrimera(int pagina) {
        when(libroRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

        libroService.buscarLibros("", "todos", pagina);

        ArgumentCaptor<Pageable> paginacion = ArgumentCaptor.forClass(Pageable.class);
        verify(libroRepository).findAll(paginacion.capture());
        assertEquals(0, paginacion.getValue().getPageNumber());
        assertEquals(0, paginacion.getValue().getOffset());
        verifyNoMoreInteractions(libroRepository);
    }

    @Test
    void conservaLaPaginaMasAltaConDesplazamientoValido() {
        int pagina = Integer.MAX_VALUE / 20;
        when(libroRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

        libroService.buscarLibros("", "todos", pagina);

        ArgumentCaptor<Pageable> paginacion = ArgumentCaptor.forClass(Pageable.class);
        verify(libroRepository).findAll(paginacion.capture());
        assertEquals(pagina, paginacion.getValue().getPageNumber());
        assertEquals((long) pagina * 20, paginacion.getValue().getOffset());
        verifyNoMoreInteractions(libroRepository);
    }
}
