package com.UNACH.Biblioteca.service;

import com.UNACH.Biblioteca.model.*;
import com.UNACH.Biblioteca.model.form.*;
import com.UNACH.Biblioteca.repository.*;
import jakarta.validation.Valid;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true)
public class PrestamoService {
    private static final List<EstadoPrestamo> ACTIVOS = List.of(EstadoPrestamo.SOLICITADO, EstadoPrestamo.PRESTADO);
    private final LibroRepository libros;
    private final LectorRepository lectores;
    private final PrestamoRepository prestamos;
    private final HistorialPrestamoRepository historial;
    private final IncidenciaRepository incidencias;
    private final UsuarioRepository usuarios;
    private final Clock reloj;
    private final int plazoDias;
    private final Set<String> dominios;

    public PrestamoService(LibroRepository libros, LectorRepository lectores, PrestamoRepository prestamos,
            HistorialPrestamoRepository historial, IncidenciaRepository incidencias, UsuarioRepository usuarios, Clock reloj,
            @Value("${biblioteca.prestamos.plazo-dias:0}") int plazoDias,
            @Value("${biblioteca.prestamos.dominios-correo:}") String dominiosCorreo) {
        if (plazoDias < 0) { throw new IllegalArgumentException("El plazo de préstamo no puede ser negativo."); }
        this.libros = libros;
        this.lectores = lectores;
        this.prestamos = prestamos;
        this.historial = historial;
        this.incidencias = incidencias;
        this.usuarios = usuarios;
        this.reloj = reloj;
        this.plazoDias = plazoDias;
        this.dominios = new HashSet<>();
        for (String dominio : dominiosCorreo.split(",")) {
            if (!dominio.isBlank()) { this.dominios.add(dominio.trim().toLowerCase(Locale.ROOT)); }
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @PreAuthorize("permitAll()")
    public Prestamo solicitar(@Valid SolicitudPrestamoFormulario datos) {
        Libro encontrado = libros.findByCodigoBarras(datos.getCodigoBarras()).orElseThrow(() ->
                new ReglaNegocioException(HttpStatus.NOT_FOUND, "No existe un ejemplar con ese código de barras."));
        // Se bloquea el ejemplar, incluso cuando todavía no tiene préstamos.
        Libro libro = libros.bloquearPorId(encontrado.getId()).orElseThrow(() ->
                new ReglaNegocioException(HttpStatus.NOT_FOUND, "El ejemplar ya no existe."));
        if (incidencias.existeBloqueantePendientePorLibroId(libro.getId())) {
            throw new ReglaNegocioException(HttpStatus.CONFLICT, "El ejemplar tiene una incidencia pendiente y no está disponible.");
        }
        if (prestamos.existsByLibroIdAndEstadoIn(libro.getId(), ACTIVOS)) {
            throw new ReglaNegocioException(HttpStatus.CONFLICT, "El ejemplar ya está solicitado o prestado.");
        }
        String correo = datos.getCorreoInstitucional().trim().toLowerCase(Locale.ROOT);
        if (!dominios.isEmpty() && !dominios.contains(correo.substring(correo.lastIndexOf('@') + 1))) {
            throw new ReglaNegocioException(HttpStatus.BAD_REQUEST, "El correo no pertenece a un dominio institucional autorizado.");
        }
        String matricula = datos.getMatricula().trim().toUpperCase(Locale.ROOT);
        String nombre = datos.getNombreCompleto().trim();
        Lector lector = lectores.findByMatriculaIgnoreCase(matricula).orElse(null);
        if (lector == null) {
            lector = new Lector();
            lector.setMatricula(matricula);
            lector.setNombreCompleto(nombre);
            lector.setCorreoInstitucional(correo);
            lector.setFechaRegistro(LocalDateTime.now(reloj));
            lector = lectores.saveAndFlush(lector);
        } else if (!lector.getNombreCompleto().equalsIgnoreCase(nombre)
                || !lector.getCorreoInstitucional().equalsIgnoreCase(correo)) {
            throw new ReglaNegocioException(HttpStatus.CONFLICT,
                    "La matrícula ya está registrada con otros datos. Acude al personal para verificarlos.");
        }
        Prestamo prestamo = new Prestamo();
        prestamo.setLector(lector);
        prestamo.setLibro(libro);
        prestamo.setFechaSolicitud(LocalDateTime.now(reloj));
        prestamo.setEstado(EstadoPrestamo.SOLICITADO);
        prestamo = prestamos.saveAndFlush(prestamo);
        registrar(prestamo, AccionPrestamo.SOLICITUD, null, "Solicitud presencial pendiente de entrega.");
        return prestamo;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Page<Prestamo> listar(EstadoPrestamo estado, int pagina) {
        if (pagina < 0 || pagina > Integer.MAX_VALUE / 20) { pagina = 0; }
        return prestamos.findByEstado(estado, PageRequest.of(pagina, 20,
                Sort.by(Sort.Order.desc("fechaSolicitud"), Sort.Order.desc("id"))));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Prestamo obtener(Long id) {
        return prestamos.findById(id).orElseThrow(() ->
                new ReglaNegocioException(HttpStatus.NOT_FOUND, "El préstamo no existe."));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<HistorialPrestamo> consultarHistorial(Long id) {
        obtener(id);
        return historial.findByPrestamoIdOrderByFechaAscIdAsc(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Map<EstadoPrestamo, Long> contarPorEstado() {
        Map<EstadoPrestamo, Long> cantidades = new EnumMap<>(EstadoPrestamo.class);
        for (EstadoPrestamo estado : EstadoPrestamo.values()) { cantidades.put(estado, prestamos.countByEstado(estado)); }
        return cantidades;
    }

    public Map<Long, EstadoEjemplar> estadosEjemplares(Collection<Long> ids) {
        Map<Long, EstadoEjemplar> estados = new HashMap<>();
        for (Long id : ids) { estados.put(id, EstadoEjemplar.DISPONIBLE); }
        if (!ids.isEmpty()) {
            for (Prestamo prestamo : prestamos.findByLibroIdInAndEstadoIn(ids, ACTIVOS)) {
                estados.put(prestamo.getLibro().getId(), prestamo.getEstado() == EstadoPrestamo.SOLICITADO
                        ? EstadoEjemplar.SOLICITADO : EstadoEjemplar.PRESTADO);
            }
            for (Long id : incidencias.librosBloqueados(ids)) { estados.put(id, EstadoEjemplar.NO_DISPONIBLE); }
        }
        return estados;
    }

    public LocalDate fechaLimiteSugerida() {
        return plazoDias == 0 ? null : LocalDate.now(reloj).plusDays(plazoDias);
    }

    public LocalDate hoy() { return LocalDate.now(reloj); }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void confirmar(Long id, @Valid OperacionPrestamoFormulario datos) {
        Prestamo prestamo = bloquear(id);
        exigirEstado(prestamo, EstadoPrestamo.SOLICITADO);
        if (incidencias.existeBloqueantePendientePorLibroId(prestamo.getLibro().getId())) {
            throw new ReglaNegocioException(HttpStatus.CONFLICT, "No puede entregarse un ejemplar con una incidencia pendiente.");
        }
        LocalDate limite = datos.getFechaLimite() == null ? fechaLimiteSugerida() : datos.getFechaLimite();
        if (limite == null || limite.isBefore(LocalDate.now(reloj))) {
            throw new ReglaNegocioException(HttpStatus.BAD_REQUEST, "Indica una fecha límite de devolución que no sea anterior a hoy.");
        }
        Usuario admin = administradorActual();
        prestamo.setAdministrador(admin);
        prestamo.setEstado(EstadoPrestamo.PRESTADO);
        prestamo.setFechaPrestamo(LocalDateTime.now(reloj));
        prestamo.setFechaLimiteDevolucion(limite);
        prestamos.saveAndFlush(prestamo);
        registrar(prestamo, AccionPrestamo.CONFIRMACION, admin, datos.getObservacion());
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void rechazar(Long id, @Valid OperacionPrestamoFormulario datos) {
        Prestamo prestamo = bloquear(id);
        exigirEstado(prestamo, EstadoPrestamo.SOLICITADO);
        Usuario admin = administradorActual();
        prestamo.setAdministrador(admin);
        prestamo.setEstado(EstadoPrestamo.RECHAZADO);
        prestamos.saveAndFlush(prestamo);
        registrar(prestamo, AccionPrestamo.RECHAZO, admin, datos.getObservacion());
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void devolver(Long id, @Valid OperacionPrestamoFormulario datos) {
        devolverInterno(id, datos, null);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void devolverConIncidencia(Long id, @Valid OperacionPrestamoFormulario datos, @Valid IncidenciaFormulario incidencia) {
        devolverInterno(id, datos, incidencia);
    }

    private void devolverInterno(Long id, OperacionPrestamoFormulario datos, IncidenciaFormulario datosIncidencia) {
        Prestamo prestamo = bloquear(id);
        exigirEstado(prestamo, EstadoPrestamo.PRESTADO);
        Usuario admin = administradorActual();
        prestamo.setFechaDevolucion(LocalDateTime.now(reloj));
        prestamo.setEstado(EstadoPrestamo.DEVUELTO);
        prestamos.saveAndFlush(prestamo);
        registrar(prestamo, AccionPrestamo.DEVOLUCION, admin, datos.getObservacion());
        if (datosIncidencia != null) { crearIncidencia(prestamo, datosIncidencia, admin); }
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void registrarIncidencia(Long prestamoId, @Valid IncidenciaFormulario datos) {
        Prestamo prestamo = bloquear(prestamoId);
        if (prestamo.getFechaPrestamo() == null) {
            throw new ReglaNegocioException(HttpStatus.CONFLICT, "Solo puedes registrar incidencias en un ejemplar que fue entregado.");
        }
        crearIncidencia(prestamo, datos, administradorActual());
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void resolverIncidencia(Long prestamoId, Long id, @Valid ResolucionIncidenciaFormulario datos) {
        bloquear(prestamoId);
        Incidencia incidencia = incidencias.findById(id).orElseThrow(() ->
                new ReglaNegocioException(HttpStatus.NOT_FOUND, "La incidencia no existe."));
        if (!incidencia.getPrestamo().getId().equals(prestamoId)) {
            throw new ReglaNegocioException(HttpStatus.CONFLICT, "La incidencia no corresponde a este préstamo.");
        }
        if (incidencia.getEstado() != EstadoIncidencia.PENDIENTE) {
            throw new ReglaNegocioException(HttpStatus.CONFLICT, "La incidencia ya fue resuelta.");
        }
        incidencia.setEstado(EstadoIncidencia.RESUELTA);
        incidencia.setFechaResolucion(LocalDateTime.now(reloj));
        incidencia.setAdministradorResolucion(administradorActual());
        incidencia.setSolucion(datos.getSolucion().trim());
        incidencias.saveAndFlush(incidencia);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<Incidencia> consultarIncidencias(Long prestamoId) {
        obtener(prestamoId);
        return incidencias.findByPrestamoIdOrderByFechaRegistroAscIdAsc(prestamoId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Page<Lector> buscarLectores(String texto, int pagina) {
        String busqueda = texto == null ? "" : texto.trim();
        if (pagina < 0) pagina = 0;
        return lectores.findByMatriculaContainingIgnoreCaseOrNombreCompletoContainingIgnoreCase(busqueda, busqueda,
                PageRequest.of(pagina, 20, Sort.by("nombreCompleto").ascending()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Lector obtenerLector(Long id) { return lectores.findById(id).orElseThrow(() -> new ReglaNegocioException(HttpStatus.NOT_FOUND, "El lector no existe.")); }
    @PreAuthorize("hasRole('ADMIN')")
    public Libro obtenerLibro(Long id) { return libros.findById(id).orElseThrow(() -> new ReglaNegocioException(HttpStatus.NOT_FOUND, "El ejemplar no existe.")); }

    @PreAuthorize("hasRole('ADMIN')")
    public HistorialConsulta historialLector(Long lectorId, EstadoPrestamo estado, boolean soloIncidencias, LocalDate desde, LocalDate hasta, int pagina) {
        obtenerLector(lectorId);
        return construirHistorial(prestamos.historialLector(lectorId, estado, soloIncidencias, inicio(desde), fin(hasta), paginar(pagina)),
                prestamos.todosHistorialLector(lectorId, estado, soloIncidencias, inicio(desde), fin(hasta)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public HistorialConsulta historialLibro(Long libroId, boolean mismaObra, EstadoPrestamo estado, boolean soloIncidencias, LocalDate desde, LocalDate hasta, int pagina) {
        Libro seleccionado = libros.findById(libroId).orElseThrow(() -> new ReglaNegocioException(HttpStatus.NOT_FOUND, "El ejemplar no existe."));
        List<Long> ids = mismaObra ? libros.findMismaObra(seleccionado.getTitulo(), seleccionado.getAutor()).stream().map(Libro::getId).toList() : List.of(libroId);
        return construirHistorial(prestamos.historialLibros(ids, estado, soloIncidencias, inicio(desde), fin(hasta), paginar(pagina)),
                prestamos.todosHistorialLibros(ids, estado, soloIncidencias, inicio(desde), fin(hasta)));
    }

    private Pageable paginar(int pagina) { return PageRequest.of(Math.max(0, pagina), 20, Sort.by(Sort.Order.desc("fechaSolicitud"), Sort.Order.desc("id"))); }
    private LocalDateTime inicio(LocalDate fecha) { return fecha == null ? null : fecha.atStartOfDay(); }
    private LocalDateTime fin(LocalDate fecha) { return fecha == null ? null : fecha.plusDays(1).atStartOfDay(); }
    private HistorialConsulta construirHistorial(Page<Prestamo> pagina, List<Prestamo> todos) {
        List<Long> ids = todos.stream().map(Prestamo::getId).toList();
        Map<Long, List<Incidencia>> porPrestamo = (ids.isEmpty() ? List.<Incidencia>of() : incidencias.findByPrestamoIdInOrderByFechaRegistroAscIdAsc(ids)).stream()
                .collect(java.util.stream.Collectors.groupingBy(i -> i.getPrestamo().getId(), LinkedHashMap::new, java.util.stream.Collectors.toList()));
        Page<HistorialRegistro> filas = pagina.map(p -> new HistorialRegistro(p, porPrestamo.getOrDefault(p.getId(), List.of()), diasAtraso(p)));
        long entregados = todos.stream().filter(p -> p.getFechaPrestamo() != null).count();
        long activos = todos.stream().filter(p -> p.getEstado() == EstadoPrestamo.PRESTADO).count();
        long vencidos = todos.stream().filter(this::vencido).count();
        long tardias = todos.stream().filter(this::devolucionTardia).count();
        List<Incidencia> todas = porPrestamo.values().stream().flatMap(Collection::stream).toList();
        ResumenHistorial resumen = new ResumenHistorial(todos.size(), entregados, activos, vencidos, tardias,
                todos.stream().filter(p -> p.getFechaPrestamo() != null).map(p -> p.getLector().getId()).distinct().count(),
                todas.stream().filter(i -> i.getEstado() == EstadoIncidencia.PENDIENTE).count(),
                todas.stream().filter(i -> i.getEstado() == EstadoIncidencia.RESUELTA).count(),
                todas.stream().filter(i -> i.getTipo() != TipoIncidencia.OTRO).count());
        return new HistorialConsulta(filas, resumen);
    }
    public boolean vencido(Prestamo p) { return p.getEstado() == EstadoPrestamo.PRESTADO && p.getFechaLimiteDevolucion() != null && p.getFechaLimiteDevolucion().isBefore(hoy()); }
    public boolean devolucionTardia(Prestamo p) { return p.getEstado() == EstadoPrestamo.DEVUELTO && p.getFechaLimiteDevolucion() != null && p.getFechaDevolucion().toLocalDate().isAfter(p.getFechaLimiteDevolucion()); }
    public long diasAtraso(Prestamo p) { if (p.getFechaLimiteDevolucion() == null) return 0; LocalDate hasta = p.getFechaDevolucion() == null ? hoy() : p.getFechaDevolucion().toLocalDate(); return Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(p.getFechaLimiteDevolucion(), hasta)); }
    private void crearIncidencia(Prestamo prestamo, IncidenciaFormulario datos, Usuario admin) {
        Incidencia incidencia = new Incidencia(); incidencia.setPrestamo(prestamo); incidencia.setTipo(datos.getTipo()); incidencia.setDescripcion(datos.getDescripcion().trim());
        incidencia.setFechaRegistro(LocalDateTime.now(reloj)); incidencia.setAdministradorRegistro(admin); incidencia.setEstado(EstadoIncidencia.PENDIENTE); incidencias.saveAndFlush(incidencia);
    }

    private Prestamo bloquear(Long id) {
        Long libroId = prestamos.buscarLibroId(id).orElseThrow(() ->
                new ReglaNegocioException(HttpStatus.NOT_FOUND, "El préstamo no existe."));
        libros.bloquearPorId(libroId).orElseThrow(() ->
                new ReglaNegocioException(HttpStatus.NOT_FOUND, "El ejemplar no existe."));
        return prestamos.bloquearPorId(id).orElseThrow(() ->
                new ReglaNegocioException(HttpStatus.NOT_FOUND, "El préstamo no existe."));
    }

    private void exigirEstado(Prestamo prestamo, EstadoPrestamo esperado) {
        if (prestamo.getEstado() != esperado) {
            throw new ReglaNegocioException(HttpStatus.CONFLICT, "La operación no corresponde al estado actual del préstamo.");
        }
    }

    private Usuario administradorActual() {
        var identidad = SecurityContextHolder.getContext().getAuthentication();
        Usuario usuario = usuarios.findByUsuarioIgnoreCase(identidad.getName()).orElseThrow(() ->
                new AccessDeniedException("La cuenta administradora ya no existe."));
        if (usuario.getRol() != Rol.ADMIN) { throw new AccessDeniedException("Se requiere una cuenta ADMIN vigente."); }
        return usuario;
    }

    private void registrar(Prestamo prestamo, AccionPrestamo accion, Usuario admin, String observacion) {
        HistorialPrestamo evento = new HistorialPrestamo();
        evento.setPrestamo(prestamo);
        evento.setAdministrador(admin);
        evento.setAccion(accion);
        evento.setFecha(LocalDateTime.now(reloj));
        evento.setObservacion(observacion == null || observacion.isBlank() ? null : observacion.trim());
        historial.saveAndFlush(evento);
    }
}
