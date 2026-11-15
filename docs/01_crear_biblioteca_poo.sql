-- Biblioteca POO - creación para una base nueva.
-- MariaDB 10.4+ / MySQL 8.0.16+. Ejecutar 01 y después 02.
CREATE DATABASE IF NOT EXISTS biblioteca_poo CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE biblioteca_poo;

CREATE TABLE roles (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 nombre VARCHAR(20) NOT NULL UNIQUE,
 CONSTRAINT ck_rol CHECK (nombre IN ('ADMIN','USUARIO'))
) ENGINE=InnoDB;
CREATE TABLE usuarios (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 nombre VARCHAR(100) NOT NULL,
 usuario VARCHAR(50) NOT NULL UNIQUE,
 password VARCHAR(255) NOT NULL,
 rol_id BIGINT NOT NULL,
 CONSTRAINT fk_usuario_rol FOREIGN KEY (rol_id) REFERENCES roles(id)
) ENGINE=InnoDB;
CREATE TABLE inventario_libros (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 autor TEXT,
 codigo_barras VARCHAR(50) NOT NULL UNIQUE,
 descripcion_original TEXT NOT NULL,
 titulo TEXT NOT NULL
) ENGINE=InnoDB;
CREATE TABLE lectores (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 correo_institucional VARCHAR(150) NOT NULL,
 fecha_registro DATETIME(6) NOT NULL,
 matricula VARCHAR(50) NOT NULL UNIQUE,
 nombre_completo VARCHAR(200) NOT NULL
) ENGINE=InnoDB;
CREATE TABLE prestamos (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 lector_id BIGINT NOT NULL,
 inventario_libro_id BIGINT NOT NULL,
 usuario_id BIGINT,
 fecha_solicitud DATETIME(6) NOT NULL,
 fecha_prestamo DATETIME(6),
 fecha_limite_devolucion DATE,
 fecha_devolucion DATETIME(6),
 estado VARCHAR(20) NOT NULL,
 ejemplar_activo_id BIGINT GENERATED ALWAYS AS
  (CASE WHEN estado IN ('SOLICITADO','PRESTADO') THEN inventario_libro_id ELSE NULL END) STORED,
 CONSTRAINT uq_prestamo_activo UNIQUE (ejemplar_activo_id),
 CONSTRAINT fk_prestamo_lector FOREIGN KEY (lector_id) REFERENCES lectores(id),
 CONSTRAINT fk_prestamo_libro FOREIGN KEY (inventario_libro_id) REFERENCES inventario_libros(id),
 CONSTRAINT fk_prestamo_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
 CONSTRAINT ck_prestamo_estado CHECK (estado IN ('SOLICITADO','PRESTADO','DEVUELTO','RECHAZADO')),
 CONSTRAINT ck_prestamo_fechas CHECK (
  (estado='SOLICITADO' AND usuario_id IS NULL AND fecha_prestamo IS NULL AND fecha_limite_devolucion IS NULL AND fecha_devolucion IS NULL)
  OR (estado='RECHAZADO' AND usuario_id IS NOT NULL AND fecha_prestamo IS NULL AND fecha_limite_devolucion IS NULL AND fecha_devolucion IS NULL)
  OR (estado='PRESTADO' AND usuario_id IS NOT NULL AND fecha_prestamo IS NOT NULL AND fecha_limite_devolucion IS NOT NULL AND fecha_devolucion IS NULL)
  OR (estado='DEVUELTO' AND usuario_id IS NOT NULL AND fecha_prestamo IS NOT NULL AND fecha_limite_devolucion IS NOT NULL AND fecha_devolucion IS NOT NULL))
) ENGINE=InnoDB;
CREATE TABLE historial_prestamos (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 prestamo_id BIGINT NOT NULL,
 usuario_id BIGINT,
 accion VARCHAR(30) NOT NULL,
 fecha DATETIME(6) NOT NULL,
 observacion VARCHAR(1000),
 CONSTRAINT fk_historial_prestamo FOREIGN KEY (prestamo_id) REFERENCES prestamos(id),
 CONSTRAINT fk_historial_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
 CONSTRAINT ck_historial_accion CHECK (accion IN ('SOLICITUD','CONFIRMACION','RECHAZO','DEVOLUCION')),
 CONSTRAINT ck_historial_autor CHECK (accion='SOLICITUD' OR usuario_id IS NOT NULL)
) ENGINE=InnoDB;
CREATE TABLE incidencias_prestamos (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 prestamo_id BIGINT NOT NULL,
 tipo VARCHAR(20) NOT NULL,
 descripcion VARCHAR(2000) NOT NULL,
 fecha_registro DATETIME(6) NOT NULL,
 administrador_registro_id BIGINT NOT NULL,
 estado VARCHAR(20) NOT NULL,
 fecha_resolucion DATETIME(6),
 administrador_resolucion_id BIGINT,
 solucion VARCHAR(2000),
 CONSTRAINT fk_incidencia_prestamo FOREIGN KEY (prestamo_id) REFERENCES prestamos(id),
 CONSTRAINT fk_incidencia_registro FOREIGN KEY (administrador_registro_id) REFERENCES usuarios(id),
 CONSTRAINT fk_incidencia_resolucion FOREIGN KEY (administrador_resolucion_id) REFERENCES usuarios(id),
 CONSTRAINT ck_incidencia_tipo CHECK (tipo IN ('DANO','PERDIDA','OTRO')),
 CONSTRAINT ck_incidencia_estado CHECK (estado IN ('PENDIENTE','RESUELTA')),
 CONSTRAINT ck_incidencia_resolucion CHECK (
  (estado='PENDIENTE' AND fecha_resolucion IS NULL AND administrador_resolucion_id IS NULL AND solucion IS NULL)
  OR (estado='RESUELTA' AND fecha_resolucion IS NOT NULL AND administrador_resolucion_id IS NOT NULL AND solucion IS NOT NULL))
) ENGINE=InnoDB;
CREATE INDEX ix_incidencia_prestamo ON incidencias_prestamos(prestamo_id,fecha_registro);
CREATE INDEX ix_prestamo_estado ON prestamos(estado);
