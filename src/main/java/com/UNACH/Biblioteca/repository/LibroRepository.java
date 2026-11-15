package com.UNACH.Biblioteca.repository;

import com.UNACH.Biblioteca.model.Libro;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.List;

public interface LibroRepository extends JpaRepository<Libro, Long> {

    Optional<Libro> findByCodigoBarras(String codigoBarras);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Libro l where l.id = :id")
    Optional<Libro> bloquearPorId(Long id);

    boolean existsByCodigoBarras(String codigoBarras);

    Page<Libro> findByTituloContainingIgnoreCase(
            String titulo, Pageable pageable);

    Page<Libro> findByAutorContainingIgnoreCase(
            String autor, Pageable pageable);

    Page<Libro> findByCodigoBarrasContainingIgnoreCase(
            String codigoBarras, Pageable pageable);

    Page<Libro> findByTituloContainingIgnoreCaseOrAutorContainingIgnoreCaseOrCodigoBarrasContainingIgnoreCase(
            String titulo,
            String autor,
            String codigoBarras,
            Pageable pageable);

    @Query("select l from Libro l where lower(l.titulo) = lower(:titulo) and ((:autor is null and l.autor is null) or lower(l.autor) = lower(:autor)) order by l.codigoBarras")
    List<Libro> findMismaObra(String titulo, String autor);
}
