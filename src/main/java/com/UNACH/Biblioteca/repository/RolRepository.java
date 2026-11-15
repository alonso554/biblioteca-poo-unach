package com.UNACH.Biblioteca.repository;

import com.UNACH.Biblioteca.model.*;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolRepository extends JpaRepository<RolSistema, Long> {
    Optional<RolSistema> findByNombre(Rol nombre);
}
