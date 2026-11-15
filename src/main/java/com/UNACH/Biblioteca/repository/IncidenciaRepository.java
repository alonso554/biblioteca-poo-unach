package com.UNACH.Biblioteca.repository;

import com.UNACH.Biblioteca.model.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface IncidenciaRepository extends JpaRepository<Incidencia, Long> {
    @EntityGraph(attributePaths = {"administradorRegistro", "administradorResolucion"})
    List<Incidencia> findByPrestamoIdInOrderByFechaRegistroAscIdAsc(Collection<Long> prestamoIds);
    @EntityGraph(attributePaths = {"administradorRegistro", "administradorResolucion"})
    List<Incidencia> findByPrestamoIdOrderByFechaRegistroAscIdAsc(Long prestamoId);
    @Query("select count(i) > 0 from Incidencia i where i.prestamo.libro.id = :libroId and i.estado = 'PENDIENTE' and i.tipo in ('DANO','PERDIDA')")
    boolean existeBloqueantePendientePorLibroId(Long libroId);
    @Query("select i.prestamo.libro.id from Incidencia i where i.prestamo.libro.id in :ids and i.estado = 'PENDIENTE' and i.tipo in ('DANO','PERDIDA')")
    List<Long> librosBloqueados(Collection<Long> ids);
}
