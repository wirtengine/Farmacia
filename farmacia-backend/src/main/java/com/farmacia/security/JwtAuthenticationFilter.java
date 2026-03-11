package com.farmacia.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        System.out.println("=== JwtAuthenticationFilter ===");
        System.out.println("Request URI: " + request.getRequestURI());
        System.out.println("Method: " + request.getMethod());

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("No Bearer token found, continuing filter chain");
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        username = jwtService.extractUsername(jwt);
        System.out.println("Extracted username: " + username);

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            System.out.println("Loading user details for: " + username);
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
            System.out.println("User details loaded, authorities: " + userDetails.getAuthorities());

            if (jwtService.validateToken(jwt, userDetails)) {
                System.out.println("Token validated successfully");
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                System.out.println("Authentication set in SecurityContext");
            } else {
                System.out.println("Token validation failed");
            }
        } else {
            System.out.println("Username is null or authentication already exists");
        }

        filterChain.doFilter(request, response);
    }
}