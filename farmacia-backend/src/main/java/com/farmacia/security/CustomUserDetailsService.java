package com.farmacia.security;

import com.farmacia.model.Usuario;
import com.farmacia.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("=== CustomUserDetailsService ===");
        System.out.println("Looking for user: " + username);
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        System.out.println("User found in DB: " + usuario.getUsername());
        System.out.println("Role from DB: " + usuario.getRol());
        System.out.println("Active status: " + usuario.getActivo());

        UserDetails user = User.builder()
                .username(usuario.getUsername())
                .password(usuario.getPassword())
                .roles(usuario.getRol())
                .build();

        System.out.println("Authorities generated: " + user.getAuthorities());
        return user;
    }
}