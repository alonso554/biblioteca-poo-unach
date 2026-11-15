package com.UNACH.Biblioteca.service;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;

public final class PoliticaClave {
    private static final Pattern BCRYPT = Pattern.compile(
            "\\A\\$2[aby]\\$(0[4-9]|[12][0-9]|3[01])\\$[./A-Za-z0-9]{53}\\z");

    private PoliticaClave() { }

    public static boolean esBCrypt(String hash) {
        return hash != null && BCRYPT.matcher(hash).matches();
    }

    public static void validar(String clave, boolean obligatoria) {
        if (clave == null || clave.isBlank()) {
            if (obligatoria) {
                throw new ReglaNegocioException(HttpStatus.BAD_REQUEST, "La contraseña es obligatoria al registrar un usuario.");
            }
            return;
        }
        if (clave.length() < 8 || clave.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ReglaNegocioException(HttpStatus.BAD_REQUEST,
                    "La contraseña debe tener al menos 8 caracteres y ocupar hasta 72 bytes en UTF-8.");
        }
    }
}
