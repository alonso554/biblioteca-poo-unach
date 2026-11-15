package com.UNACH.Biblioteca.repository;

import com.UNACH.Biblioteca.model.HistorialPrestamo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistorialPrestamoRepository extends JpaRepository<HistorialPrestamo, Long> {
    boolean existsByAdministradorId(Long usuarioId);
    List<HistorialPrestamo> findByPrestamoIdOrderByFechaAscIdAsc(Long prestamoId);
}
