package com.andretti101.escolaweb.config;

import com.andretti101.escolaweb.service.impl.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final HandlerExceptionResolver exceptionResolver;

    public SecurityConfig(
            CustomUserDetailsService userDetailsService,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.exceptionResolver = exceptionResolver;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/auth/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml",
                                "/login.html",
                                "/forgot-password.html",
                                "/reset-password.html",
                                "/entrar",
                                "/recuperar-senha",
                                "/redefinir-senha",
                                "/assets/**",
                                "/fonts/**",
                                "/js/**",
                                "/libs/**",
                                "/favicon.ico",
                                "/ws-chat/**",
                                "/"
                        ).permitAll()
                        .requestMatchers("/direcao", "/direcao/**", "/principal", "/principal/**").hasRole("PRINCIPAL")
                        .requestMatchers("/secretaria", "/secretaria/**", "/secretary", "/secretary/**").hasAnyRole("SECRETARY", "PRINCIPAL")
                        .requestMatchers("/professor", "/professor/**", "/teacher", "/teacher/**").hasAnyRole("TEACHER", "SECRETARY", "PRINCIPAL")
                        .requestMatchers("/aluno", "/aluno/**", "/student", "/student/**").hasAnyRole("STUDENT", "SECRETARY", "PRINCIPAL")
                        .requestMatchers("/components/**", "/ai-chat.html", "/assistente-ia", "/dashboard.html").authenticated()
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) -> {
                            boolean isHtmlOrWebRoute = request.getRequestURI().endsWith(".html")
                                    || (request.getHeader("Accept") != null && request.getHeader("Accept").contains("text/html"))
                                    || request.getRequestURI().startsWith("/direcao")
                                    || request.getRequestURI().startsWith("/secretaria")
                                    || request.getRequestURI().startsWith("/professor")
                                    || request.getRequestURI().startsWith("/aluno")
                                    || request.getRequestURI().startsWith("/principal")
                                    || request.getRequestURI().startsWith("/secretary")
                                    || request.getRequestURI().startsWith("/teacher")
                                    || request.getRequestURI().startsWith("/student")
                                    || "/assistente-ia".equals(request.getRequestURI());

                            if ("GET".equalsIgnoreCase(request.getMethod()) && isHtmlOrWebRoute) {
                                if (request.getRequestURI().endsWith(".html")
                                        || request.getRequestURI().startsWith("/principal")
                                        || request.getRequestURI().startsWith("/secretary")
                                        || request.getRequestURI().startsWith("/teacher")
                                        || request.getRequestURI().startsWith("/student")) {
                                    response.sendRedirect("/login.html");
                                } else {
                                    response.sendRedirect("/entrar");
                                }
                            } else {
                                exceptionResolver.resolveException(request, response, null, e);
                            }
                        })
                        .accessDeniedHandler((request, response, e) ->
                                exceptionResolver.resolveException(request, response, null, e))
                );

        return http.build();
    }

    private DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
