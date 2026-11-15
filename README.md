# Biblioteca de Tecnologías Digitales Aplicadas

Proyecto final de Programación Orientada a Objetos de la Universidad Autónoma de Chiapas.

**Integrantes**
- Alonso de Jesús de León Sánchez.
- Antonio de Jesús Ruiz Gómez.

**Docente** Dr. Gutiérrez Alfaro Luis.
**Grupo** Segundo semestre, M.
**Entrega** 15 de noviembre de 2026.

## Requisitos
- JDK 21 para compilar y ejecutar desde el código fuente.
- MariaDB 10.4 o posterior, disponible mediante XAMPP.
- Conexión a internet durante la primera compilación para descargar Maven y las dependencias.

## Preparar la base de datos
1. Iniciar MySQL desde XAMPP y abrir http://localhost/phpmyadmin.
2. Desde la pantalla principal de phpMyAdmin, importar el archivo docs/01_crear_biblioteca_poo.sql. El script crea la base biblioteca_poo y sus siete tablas.
3. Seleccionar biblioteca_poo e importar docs/02_datos_demostracion.sql. Se agregan las cuentas de prueba y los 5,020 registros bibliográficos.
4. Importar cada script una vez en una instalación nueva.

## Ejecutar desde el repositorio
Abrir una terminal en la carpeta que contiene pom.xml.

En PowerShell de Windows:
    .\mvnw.cmd spring-boot:run

En Linux o macOS:
    chmod +x mvnw
    ./mvnw spring-boot:run

Con Maven instalado:
    mvn spring-boot:run

Abrir http://localhost:8080/login. Mantener la terminal abierta durante el uso del sistema. Ctrl+C detiene el servidor.

## Cuentas de demostración

| Rol | Usuario | Contraseña |
| --- | --- | --- |
| ADMIN | admin.demo | Faro!84Nube26 |
| USUARIO | usuario.demo | Libro!57Sol26 |

Las contraseñas se almacenan como hashes BCrypt. Estas cuentas permiten comprobar los dos perfiles en la instalación de demostración.

## Configuración
La conexión predeterminada utiliza localhost, puerto 3306, base biblioteca_poo, usuario root y contraseña vacía.

Las variables BIBLIOTECA_DB_URL, BIBLIOTECA_DB_USER y BIBLIOTECA_DB_PASSWORD permiten modificar estos valores. Por ejemplo, en PowerShell:

    $env:BIBLIOTECA_DB_URL="jdbc:mariadb://localhost:3306/biblioteca_poo"
    $env:BIBLIOTECA_DB_USER="root"
    $env:BIBLIOTECA_DB_PASSWORD=""
    .\mvnw.cmd spring-boot:run

Si el catálogo muestra un error y la terminal indica Unknown database 'biblioteca_poo', completar los dos pasos de importación de la base y volver a cargar la página.

## Pruebas y empaquetado
Desde la carpeta de pom.xml:

Windows:
    .\mvnw.cmd test
    .\mvnw.cmd package

Con Maven instalado:
    mvn test
    mvn package

Las pruebas utilizan H2 en memoria. La comprobación documentada ejecutó 81 pruebas sin fallos ni errores. El JAR se genera en target/Biblioteca-0.0.1-SNAPSHOT.jar y se inicia con:

    java -jar target/Biblioteca-0.0.1-SNAPSHOT.jar

## Recorrido de demostración
1. Abrir el catálogo y buscar por título, autor o código.
2. Iniciar sesión como ADMIN.
3. Crear un libro ficticio con un código de barras nuevo y editar su título.
4. Solicitar el ejemplar con matrícula, nombre y correo de demostración.
5. Confirmar la entrega desde administración e indicar una fecha límite futura.
6. Registrar la devolución y consultar los eventos del historial.
7. Cerrar sesión e ingresar como USUARIO para verificar sus permisos.

## Arquitectura y estructura
- src/main/java/com/UNACH/Biblioteca/model contiene las entidades y enumeraciones.
- controller atiende las solicitudes MVC y REST.
- service aplica reglas y transacciones.
- repository contiene las interfaces JPA.
- config configura la seguridad y los parámetros.
- src/main/resources/templates contiene las vistas Thymeleaf.
- src/main/resources/static contiene Bootstrap, CSS e imágenes.
- src/test contiene las pruebas automatizadas.
- docs contiene el documento técnico, los scripts SQL, el DBML y los diagramas editables.

El sistema utiliza Java 21, Spring Boot 4.1.1, Spring Security, JPA/Hibernate, Thymeleaf y Bootstrap 5.3.3. Las siete tablas son roles, usuarios, inventario_libros, lectores, prestamos, historial_prestamos e incidencias_prestamos.
