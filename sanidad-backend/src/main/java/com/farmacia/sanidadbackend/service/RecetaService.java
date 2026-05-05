package com.farmacia.sanidadbackend.service;

import com.farmacia.sanidadbackend.dto.RecetaResponse;
import com.farmacia.sanidadbackend.model.*;
import com.farmacia.sanidadbackend.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecetaService {

    private final RecetaRepository recetaRepository;
    private final UsuarioRepository usuarioRepository;

    private static final String UPLOAD_DIR = "imagenes/recetas/";

    @Transactional
    public RecetaResponse uploadReceta(MultipartFile file, Long farmaceuticoId) throws IOException {

        Usuario farmaceutico = usuarioRepository.findById(farmaceuticoId)
                .orElseThrow(() -> new EntityNotFoundException("Farmacéutico no encontrado"));

        if (farmaceutico.getRol() != Rol.FARMACEUTICO && farmaceutico.getRol() != Rol.ADMIN) {
            throw new IllegalArgumentException("Solo un farmacéutico puede subir recetas");
        }

        File dir = new File(UPLOAD_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String nombreArchivo = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path ruta = Paths.get(UPLOAD_DIR + nombreArchivo);
        Files.write(ruta, file.getBytes());

        Receta receta = Receta.builder()
                .imagen(nombreArchivo)
                .fechaSubida(LocalDateTime.now())
                .farmaceutico(farmaceutico)
                .estado(EstadoReceta.PENDIENTE)
                .build();

        receta = recetaRepository.save(receta);

        return mapToResponse(receta);
    }

    @Transactional
    public RecetaResponse validarReceta(Long recetaId, boolean aprobar, Long farmaceuticoId) {

        Receta receta = recetaRepository.findById(recetaId)
                .orElseThrow(() -> new EntityNotFoundException("Receta no encontrada"));

        receta.setEstado(aprobar ? EstadoReceta.VALIDADA : EstadoReceta.RECHAZADA);

        receta = recetaRepository.save(receta);

        return mapToResponse(receta);
    }

    public RecetaResponse obtenerReceta(Long id) {

        Receta receta = recetaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Receta no encontrada"));

        return mapToResponse(receta);
    }

    public List<RecetaResponse> listarPendientes() {

        return recetaRepository
                .findByEstadoOrderByFechaSubidaDesc(EstadoReceta.PENDIENTE)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<RecetaResponse> listarPorFarmaceutico(Long farmaceuticoId) {

        return recetaRepository
                .findByFarmaceuticoIdOrderByFechaSubidaDesc(farmaceuticoId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<RecetaResponse> listarDisponibles() {

        return recetaRepository
                .findByEstadoAndVentaIsNull(EstadoReceta.VALIDADA)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // 🔥 NUEVO MÉTODO
    public List<RecetaResponse> listarTodas() {

        return recetaRepository
                .findAllByOrderByFechaSubidaDesc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void asociarVenta(Long recetaId, Venta venta) {

        Receta receta = recetaRepository.findById(recetaId)
                .orElseThrow(() -> new EntityNotFoundException("Receta no encontrada"));

        if (receta.getVenta() != null) {
            throw new IllegalStateException("La receta ya fue utilizada en otra venta");
        }

        receta.setVenta(venta);
        recetaRepository.save(receta);
    }

    private RecetaResponse mapToResponse(Receta receta) {
        RecetaResponse r = new RecetaResponse();

        r.setId(receta.getId());
        r.setImagenUrl("/imagenes/recetas/" + receta.getImagen());
        r.setFechaSubida(receta.getFechaSubida());
        r.setEstado(receta.getEstado().name());
        r.setFarmaceuticoId(receta.getFarmaceutico().getId());
        r.setFarmaceuticoUsername(receta.getFarmaceutico().getUsername());
        r.setVentaId(receta.getVenta() != null ? receta.getVenta().getId() : null);

        return r;
    }
}