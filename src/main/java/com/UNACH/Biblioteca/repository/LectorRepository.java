package com.UNACH.Biblioteca.repository;

import com.UNACH.Biblioteca.model.Lector;
import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LectorRepository extends JpaRepository<Lector, Long> {
    Optional<Lector> findByMatriculaIgnoreCase(String matricula);
    Page<Lector> findByMatriculaContainingIgnoreCaseOrNombreCompletoContainingIgnoreCase(String matricula, String nombre, Pageable pagina);
}
