package com.UNACH.Biblioteca.service;

import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class SesionesUsuarioService {
    private final SessionRegistry sesiones;

    public SesionesUsuarioService(SessionRegistry sesiones) { this.sesiones = sesiones; }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void invalidar(CuentaModificada cambio) {
        for (Object principal : sesiones.getAllPrincipals()) {
            if (principal instanceof UserDetails usuario
                    && usuario.getUsername().equals(cambio.identificadorAnterior())) {
                sesiones.getAllSessions(principal, false).forEach(sesion -> sesion.expireNow());
            }
        }
    }
}
