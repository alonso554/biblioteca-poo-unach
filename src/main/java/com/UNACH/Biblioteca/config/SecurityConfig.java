package com.UNACH.Biblioteca.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public ServletListenerRegistrationBean<HttpSessionEventPublisher> eventosSesion() {
        return new ServletListenerRegistrationBean<>(new HttpSessionEventPublisher());
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public SecurityFilterChain seguridad(HttpSecurity http, SessionRegistry sesiones) throws Exception {

        RequestMatcher esApi = solicitud -> solicitud.getRequestURI()
                .startsWith(solicitud.getContextPath() + "/api/");
        var accesoAdmin = new SavedRequestAwareAuthenticationSuccessHandler();
        accesoAdmin.setDefaultTargetUrl("/panel");
        var accesoUsuario = new SavedRequestAwareAuthenticationSuccessHandler();
        accesoUsuario.setDefaultTargetUrl("/catalogo");

        http
            .authorizeHttpRequests(acceso -> acceso
                .requestMatchers(
                    "/",
                    "/catalogo",
                    "/login",
                    "/css/**",
                    "/img/**",
                    "/error"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/libros", "/api/libros/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/prestamos/solicitar").permitAll()
                .requestMatchers(HttpMethod.POST, "/prestamos/solicitar").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/usuarios").hasAnyRole("USUARIO", "ADMIN")
                .requestMatchers("/usuarios/registro").hasAnyRole("USUARIO", "ADMIN")
                .requestMatchers("/panel", "/admin/**", "/api/usuarios/**", "/api/libros/**").hasRole("ADMIN")
                .anyRequest().denyAll()
            )
            .formLogin(login -> login
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler((solicitud, respuesta, identidad) -> {
                    boolean esAdmin = identidad.getAuthorities().stream()
                            .anyMatch(rol -> rol.getAuthority().equals("ROLE_ADMIN"));
                    (esAdmin ? accesoAdmin : accesoUsuario).onAuthenticationSuccess(solicitud, respuesta, identidad);
                })
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(salida -> salida
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .sessionManagement(sesion -> sesion
                .maximumSessions(-1)
                .sessionRegistry(sesiones)
                .expiredSessionStrategy(evento -> {
                    if (esApi.matches(evento.getRequest())) {
                        evento.getResponse().setStatus(401);
                        evento.getResponse().setContentType("application/json;charset=UTF-8");
                        evento.getResponse().getWriter().write("{\"estado\":401,\"mensaje\":\"La cuenta cambió. Inicia sesión nuevamente.\"}");
                    } else {
                        evento.getResponse().sendRedirect(evento.getRequest().getContextPath() + "/login?actualizada");
                    }
                })
            )
            .exceptionHandling(errores -> errores
                .authenticationEntryPoint((solicitud, respuesta, error) -> {
                    if (esApi.matches(solicitud)) {
                        respuesta.setStatus(401);
                        respuesta.setContentType("application/json;charset=UTF-8");
                        respuesta.getWriter().write("{\"estado\":401,\"mensaje\":\"Inicia sesión para continuar.\"}");
                    } else {
                        new LoginUrlAuthenticationEntryPoint("/login").commence(solicitud, respuesta, error);
                    }
                })
                .accessDeniedHandler((solicitud, respuesta, error) -> {
                    if (esApi.matches(solicitud)) {
                        respuesta.setStatus(403);
                        respuesta.setContentType("application/json;charset=UTF-8");
                        respuesta.getWriter().write("{\"estado\":403,\"mensaje\":\"Acceso denegado. Verifica el rol y el token CSRF.\"}");
                    } else {
                        respuesta.sendError(403, "No tienes permiso para realizar esta operación.");
                    }
                })
            );

        return http.build();
    }
}
