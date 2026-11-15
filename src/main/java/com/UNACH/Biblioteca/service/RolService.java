package com.UNACH.Biblioteca.service;

import com.UNACH.Biblioteca.model.*;
import com.UNACH.Biblioteca.repository.RolRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RolService {
    private final RolRepository roles;
    public RolService(RolRepository roles) { this.roles = roles; }

    @Transactional(readOnly = true)
    public RolSistema obtener(Rol nombre) {
        return roles.findByNombre(nombre).orElseThrow(() -> new ReglaNegocioException(
                HttpStatus.SERVICE_UNAVAILABLE, "Falta el catálogo de roles. Aplica la migración de préstamos."));
    }
}
