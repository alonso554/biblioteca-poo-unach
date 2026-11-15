-- Solo se carga en H2 en memoria desde PruebaAislada. Nunca en MariaDB.
CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(20) NOT NULL UNIQUE,
    CONSTRAINT uq_roles_id_nombre UNIQUE (id, nombre),
    CHECK (nombre IN ('ADMIN', 'USUARIO'))
);
INSERT INTO roles(nombre) VALUES ('ADMIN'), ('USUARIO');
CREATE TABLE inventario_libros (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    descripcion_original TEXT NOT NULL,
    autor TEXT,
    titulo TEXT NOT NULL,
    codigo_barras VARCHAR(50) NOT NULL UNIQUE
);
CREATE TABLE usuarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    usuario VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    rol_id BIGINT NOT NULL,
    CONSTRAINT fk_usuarios_roles FOREIGN KEY (rol_id) REFERENCES roles(id)
);
CREATE TABLE lectores (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    matricula VARCHAR(50) NOT NULL UNIQUE,
    nombre_completo VARCHAR(200) NOT NULL,
    correo_institucional VARCHAR(150) NOT NULL,
    fecha_registro TIMESTAMP NOT NULL
);
CREATE TABLE prestamos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lector_id BIGINT NOT NULL REFERENCES lectores(id),
    inventario_libro_id BIGINT NOT NULL REFERENCES inventario_libros(id),
    usuario_id BIGINT REFERENCES usuarios(id),
    fecha_solicitud TIMESTAMP NOT NULL,
    fecha_prestamo TIMESTAMP,
    fecha_limite_devolucion DATE,
    fecha_devolucion TIMESTAMP,
    estado VARCHAR(20) NOT NULL CHECK (estado IN ('SOLICITADO','PRESTADO','DEVUELTO','RECHAZADO')),
    ejemplar_activo_id BIGINT GENERATED ALWAYS AS
        (CASE WHEN estado IN ('SOLICITADO','PRESTADO') THEN inventario_libro_id ELSE NULL END),
    CONSTRAINT uq_prestamos_ejemplar_activo UNIQUE (ejemplar_activo_id),
    CHECK (
        (estado = 'SOLICITADO' AND usuario_id IS NULL AND fecha_prestamo IS NULL AND fecha_limite_devolucion IS NULL AND fecha_devolucion IS NULL)
        OR (estado = 'RECHAZADO' AND usuario_id IS NOT NULL AND fecha_prestamo IS NULL AND fecha_limite_devolucion IS NULL AND fecha_devolucion IS NULL)
        OR (estado = 'PRESTADO' AND usuario_id IS NOT NULL AND fecha_prestamo IS NOT NULL AND fecha_limite_devolucion IS NOT NULL AND fecha_devolucion IS NULL)
        OR (estado = 'DEVUELTO' AND usuario_id IS NOT NULL AND fecha_prestamo IS NOT NULL AND fecha_limite_devolucion IS NOT NULL AND fecha_devolucion IS NOT NULL)
    )
);
CREATE TABLE historial_prestamos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    prestamo_id BIGINT NOT NULL REFERENCES prestamos(id),
    usuario_id BIGINT REFERENCES usuarios(id),
    accion VARCHAR(30) NOT NULL CHECK (accion IN ('SOLICITUD','CONFIRMACION','RECHAZO','DEVOLUCION')),
    fecha TIMESTAMP NOT NULL,
    observacion VARCHAR(1000),
    CHECK (accion = 'SOLICITUD' OR usuario_id IS NOT NULL)
);
CREATE TABLE incidencias_prestamos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    prestamo_id BIGINT NOT NULL REFERENCES prestamos(id),
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('DANO','PERDIDA','OTRO')),
    descripcion VARCHAR(2000) NOT NULL,
    fecha_registro TIMESTAMP NOT NULL,
    administrador_registro_id BIGINT NOT NULL REFERENCES usuarios(id),
    estado VARCHAR(20) NOT NULL CHECK (estado IN ('PENDIENTE','RESUELTA')),
    fecha_resolucion TIMESTAMP,
    administrador_resolucion_id BIGINT REFERENCES usuarios(id),
    solucion VARCHAR(2000),
    CHECK ((estado='PENDIENTE' AND fecha_resolucion IS NULL AND administrador_resolucion_id IS NULL AND solucion IS NULL)
        OR (estado='RESUELTA' AND fecha_resolucion IS NOT NULL AND administrador_resolucion_id IS NOT NULL AND solucion IS NOT NULL))
);
