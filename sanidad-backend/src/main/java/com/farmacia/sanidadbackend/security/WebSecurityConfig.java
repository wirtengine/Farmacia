package com.farmacia.sanidadbackend.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfig {

    private final AuthEntryPointJwt unauthorizedHandler;
    private final AuthTokenFilter authenticationJwtTokenFilter;

    public WebSecurityConfig(
            AuthEntryPointJwt unauthorizedHandler,
            AuthTokenFilter authenticationJwtTokenFilter) {
        this.unauthorizedHandler = unauthorizedHandler;
        this.authenticationJwtTokenFilter = authenticationJwtTokenFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",                                          // desarrollo local
                "https://farmacia-zhuq-j5g74snhg-wirtbillirt.vercel.app",        // URL anterior
                "https://farmacia-zhuq.vercel.app"                               // ← NUEVA URL de Vercel
        ));
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(exception -> {
                    exception.authenticationEntryPoint(unauthorizedHandler);
                    exception.accessDeniedHandler((request, response, accessDeniedException) -> {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Acceso denegado");
                    });
                })
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/imagenes/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()

                        // 🔓 Rutas públicas de solo lectura (sin token) – opcional
                        .requestMatchers(HttpMethod.GET, "/api/medicamentos/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/dashboard/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/ventas/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/clientes/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/proveedores/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/lotes/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/alertas/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/recomendaciones/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/perdidas/**").permitAll()
                        // Fin de rutas públicas

                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        authenticationJwtTokenFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}