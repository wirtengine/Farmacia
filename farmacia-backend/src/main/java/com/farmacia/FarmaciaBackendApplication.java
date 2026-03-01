package com.farmacia;

import com.farmacia.model.Usuario;
import com.farmacia.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class FarmaciaBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(FarmaciaBackendApplication.class, args);
    }

    @Bean
    public CommandLineRunner initData(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // Solo inserta si la tabla está vacía
            if (usuarioRepository.count() == 0) {
                Usuario admin = new Usuario();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setNombre("Administrador");
                admin.setApellido("Sistema");
                admin.setRol("ADMIN");
                usuarioRepository.save(admin);

                Usuario vendedor = new Usuario();
                vendedor.setUsername("vendedor");
                vendedor.setPassword(passwordEncoder.encode("vendedor123"));
                vendedor.setNombre("Juan");
                vendedor.setApellido("Perez");
                vendedor.setRol("VENDEDOR");
                usuarioRepository.save(vendedor);

                System.out.println("Usuarios de prueba insertados correctamente.");
            } else {
                System.out.println("Ya existen usuarios en la base de datos. No se insertaron datos de prueba.");
            }
        };
    }
}