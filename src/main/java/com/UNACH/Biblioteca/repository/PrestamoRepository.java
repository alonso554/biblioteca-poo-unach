package com.UNACH.Biblioteca.repository;

import com.UNACH.Biblioteca.model.*;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {
    boolean existsByLibroId(Long libroId);
    boolean existsByAdministradorId(Long usuarioId);
    boolean existsByLibroIdAndEstadoIn(Long libroId, Collection<EstadoPrestamo> estados);
    List<Prestamo> findByLibroIdInAndEstadoIn(Collection<Long> libros, Collection<EstadoPrestamo> estados);
    Page<Prestamo> findByEstado(EstadoPrestamo estado, Pageable pagina);
    long countByEstado(EstadoPrestamo estado);

    @EntityGraph(attributePaths = {"lector", "libro", "administrador"})
    @Query(value = "select p from Prestamo p where p.lector.id = :lectorId " +
            "and (:estado is null or p.estado = :estado) and (:soloIncidencias = false or exists (select i from Incidencia i where i.prestamo = p)) " +
            "and (:desde is null or p.fechaSolicitud >= :desde) and (:hasta is null or p.fechaSolicitud < :hasta) order by p.fechaSolicitud desc, p.id desc",
            countQuery = "select count(p) from Prestamo p where p.lector.id = :lectorId and (:estado is null or p.estado = :estado) and (:soloIncidencias = false or exists (select i from Incidencia i where i.prestamo = p)) and (:desde is null or p.fechaSolicitud >= :desde) and (:hasta is null or p.fechaSolicitud < :hasta)")
    Page<Prestamo> historialLector(Long lectorId, EstadoPrestamo estado, boolean soloIncidencias, java.time.LocalDateTime desde, java.time.LocalDateTime hasta, Pageable pagina);

    @EntityGraph(attributePaths = {"lector", "libro", "administrador"})
    @Query(value = "select p from Prestamo p where p.libro.id in :libros " +
            "and (:estado is null or p.estado = :estado) and (:soloIncidencias = false or exists (select i from Incidencia i where i.prestamo = p)) and (:desde is null or p.fechaSolicitud >= :desde) and (:hasta is null or p.fechaSolicitud < :hasta) order by p.fechaSolicitud desc, p.id desc",
            countQuery = "select count(p) from Prestamo p where p.libro.id in :libros and (:estado is null or p.estado = :estado) and (:soloIncidencias = false or exists (select i from Incidencia i where i.prestamo = p)) and (:desde is null or p.fechaSolicitud >= :desde) and (:hasta is null or p.fechaSolicitud < :hasta)")
    Page<Prestamo> historialLibros(Collection<Long> libros, EstadoPrestamo estado, boolean soloIncidencias, java.time.LocalDateTime desde, java.time.LocalDateTime hasta, Pageable pagina);

    @EntityGraph(attributePaths = {"lector", "libro", "administrador"})
    @Query("select p from Prestamo p where p.lector.id = :lectorId and (:estado is null or p.estado = :estado) and (:soloIncidencias = false or exists (select i from Incidencia i where i.prestamo = p)) and (:desde is null or p.fechaSolicitud >= :desde) and (:hasta is null or p.fechaSolicitud < :hasta)")
    List<Prestamo> todosHistorialLector(Long lectorId, EstadoPrestamo estado, boolean soloIncidencias, java.time.LocalDateTime desde, java.time.LocalDateTime hasta);

    @EntityGraph(attributePaths = {"lector", "libro", "administrador"})
    @Query("select p from Prestamo p where p.libro.id in :libros and (:estado is null or p.estado = :estado) and (:soloIncidencias = false or exists (select i from Incidencia i where i.prestamo = p)) and (:desde is null or p.fechaSolicitud >= :desde) and (:hasta is null or p.fechaSolicitud < :hasta)")
    List<Prestamo> todosHistorialLibros(Collection<Long> libros, EstadoPrestamo estado, boolean soloIncidencias, java.time.LocalDateTime desde, java.time.LocalDateTime hasta);

    @Query("select p.libro.id from Prestamo p where p.id = :id")
    Optional<Long> buscarLibroId(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Prestamo p where p.id = :id")
    Optional<Prestamo> bloquearPorId(Long id);
}
