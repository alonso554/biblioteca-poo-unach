package com.UNACH.Biblioteca.service;

import com.UNACH.Biblioteca.model.Libro;
import com.UNACH.Biblioteca.repository.LibroRepository;
import com.UNACH.Biblioteca.repository.PrestamoRepository;
import com.UNACH.Biblioteca.model.form.LibroFormulario;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true)
public class LibroService {

    private static final int TAMANO_PAGINA = 20;

    private final LibroRepository libroRepository;
    private final PrestamoRepository prestamos;

    public LibroService(LibroRepository libroRepository, PrestamoRepository prestamos) {
        this.libroRepository = libroRepository;
        this.prestamos = prestamos;
    }

    public Libro obtener(Long id) {
        return libroRepository.findById(id).orElseThrow(() ->
                new ReglaNegocioException(HttpStatus.NOT_FOUND, "El libro no existe."));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Libro crear(@Valid LibroFormulario datos) {
        String codigo = datos.getCodigoBarras().trim();
        if (libroRepository.existsByCodigoBarras(codigo)) {
            throw new ReglaNegocioException(HttpStatus.CONFLICT, "Ya existe un ejemplar con ese código de barras.");
        }
        Libro libro = new Libro();
        libro.setCodigoBarras(codigo);
        copiar(datos, libro);
        return libroRepository.saveAndFlush(libro);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Libro actualizar(Long id, @Valid LibroFormulario datos) {
        Libro libro = obtener(id);
        if (!libro.getCodigoBarras().equals(datos.getCodigoBarras())) {
            throw new ReglaNegocioException(HttpStatus.BAD_REQUEST,
                    "El código de barras identifica al ejemplar y no se puede cambiar.");
        }
        copiar(datos, libro);
        return libroRepository.saveAndFlush(libro);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void eliminar(Long id) {
        Libro libro = obtener(id);
        if (prestamos.existsByLibroId(id)) {
            throw new ReglaNegocioException(HttpStatus.CONFLICT,
                    "No puedes eliminar un ejemplar que tiene solicitudes o préstamos registrados.");
        }
        libroRepository.delete(libro);
        libroRepository.flush();
    }

    private void copiar(LibroFormulario datos, Libro libro) {
        libro.setTitulo(datos.getTitulo().trim());
        libro.setAutor(datos.getAutor() == null || datos.getAutor().isBlank() ? null : datos.getAutor().trim());
        libro.setDescripcionOriginal(datos.getDescripcionOriginal().trim());
    }

    public Page<Libro> buscarLibros(
            String texto, String filtro, int pagina) {

        if (texto == null) {
            texto = "";
        }

        texto = texto.trim();

        if (pagina < 0 || pagina > Integer.MAX_VALUE / TAMANO_PAGINA) {
            pagina = 0;
        }

        Pageable paginacion = PageRequest.of(
                pagina,
                TAMANO_PAGINA,
                Sort.by("titulo").ascending()
                        .and(Sort.by("id").ascending())
        );

        if (texto.isEmpty()) {
            return libroRepository.findAll(paginacion);
        }

        if ("titulo".equals(filtro)) {
            return libroRepository.findByTituloContainingIgnoreCase(
                    texto, paginacion);
        }

        if ("autor".equals(filtro)) {
            return libroRepository.findByAutorContainingIgnoreCase(
                    texto, paginacion);
        }

        if ("codigo".equals(filtro)) {
            return libroRepository.findByCodigoBarrasContainingIgnoreCase(
                    texto, paginacion);
        }

        return libroRepository
                .findByTituloContainingIgnoreCaseOrAutorContainingIgnoreCaseOrCodigoBarrasContainingIgnoreCase(
                        texto,
                        texto,
                        texto,
                        paginacion);
    }
}
