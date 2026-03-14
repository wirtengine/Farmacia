package com.farmacia.sanidadbackend.security;

import com.farmacia.sanidadbackend.service.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AuthTokenFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);

    private final JwtUtils jwtUtils;
    private final UserDetailsServiceImpl userDetailsService;

    public AuthTokenFilter(JwtUtils jwtUtils, UserDetailsServiceImpl userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Log de entrada: método y URI
        logger.info("==== PROCESANDO {} {} ====", request.getMethod(), request.getRequestURI());

        try {
            String jwt = parseJwt(request);
            logger.info("Token extraído: {}", jwt != null ? "PRESENTE" : "NO PRESENTE");

            if (jwt != null) {
                boolean isValid = jwtUtils.validateJwtToken(jwt);
                logger.info("Token válido según JwtUtils: {}", isValid);

                if (isValid) {
                    String username = jwtUtils.getUserNameFromJwtToken(jwt);
                    logger.info("Usuario extraído del token: {}", username);

                    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        logger.info("Cargando detalles del usuario: {}", username);
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                        logger.info("Usuario cargado: {} | Autoridades: {}", username, userDetails.getAuthorities());

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );

                        authentication.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request)
                        );

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        logger.info("✅ Autenticación establecida en el contexto para el usuario: {}", username);
                    } else {
                        logger.warn("⚠️ Username nulo o ya hay autenticación en el contexto");
                    }
                } else {
                    logger.warn("⚠️ Token inválido según JwtUtils");
                }
            } else {
                logger.warn("⚠️ No se encontró token en la petición");
            }

        } catch (Exception e) {
            logger.error("❌ Excepción en AuthTokenFilter", e); // Imprime traza completa
        }

        // Continuar con la cadena de filtros
        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        logger.info("Header Authorization recibido: {}", headerAuth); // Log del header completo

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}