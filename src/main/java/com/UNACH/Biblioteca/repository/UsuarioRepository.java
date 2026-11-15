package com.UNACH.Biblioteca.repository;

import com.UNACH.Biblioteca.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsuarioIgnoreCase(String usuario);
    boolean existsByUsuarioIgnoreCase(String usuario);
    boolean existsByUsuarioIgnoreCaseAndIdNot(String usuario, Long id);

    // Serializa las operaciones que pueden dejar al sistema sin administradores.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.rolSistema.nombre = com.UNACH.Biblioteca.model.Rol.ADMIN order by u.id")
    List<Usuario> bloquearAdministradores();
}
