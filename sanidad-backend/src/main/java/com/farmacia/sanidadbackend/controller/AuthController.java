package com.farmacia.sanidadbackend.controller;

import com.farmacia.sanidadbackend.dto.LoginRequest;
import com.farmacia.sanidadbackend.dto.LoginResponse;
import com.farmacia.sanidadbackend.model.Rol;           // 👈 import necesario
import com.farmacia.sanidadbackend.model.Usuario;
import com.farmacia.sanidadbackend.repository.UsuarioRepository;
import com.farmacia.sanidadbackend.security.JwtUtils;
import com.farmacia.sanidadbackend.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    // Para el registro temporal (luego se eliminarán)
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ============================================================
    // 🔐 ENDPOINT DE LOGIN (ya existente)
    // ============================================================
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String rol = userDetails.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");

        return ResponseEntity.ok(new LoginResponse(jwt, userDetails.getId(), userDetails.getUsername(), rol));
    }

    // ============================================================
    // 🆕 ENDPOINT DE REGISTRO (TEMPORAL – solo para crear admin)
    // ============================================================
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        // Encripta la contraseña automáticamente
        String encryptedPassword = passwordEncoder.encode(request.getPassword());

        // Crea el usuario
        Usuario usuario = new Usuario();
        usuario.setUsername(request.getUsername());
        usuario.setPassword(encryptedPassword);
        usuario.setRol(Rol.valueOf(request.getRol()));  // 👈 Usamos Rol directamente

        usuarioRepository.save(usuario);

        return ResponseEntity.ok("Usuario creado correctamente");
    }

    // ============================================================
    // 📦 DTO interno para el registro (temporal)
    // ============================================================
    static class RegisterRequest {
        private String username;
        private String password;
        private String rol;

        // Getters y setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getRol() { return rol; }
        public void setRol(String rol) { this.rol = rol; }
    }
}