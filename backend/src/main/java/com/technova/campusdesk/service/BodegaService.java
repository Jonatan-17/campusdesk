package com.technova.campusdesk.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.technova.campusdesk.dto.request.BodegaRequest;
import com.technova.campusdesk.dto.response.BodegaResponse;
import com.technova.campusdesk.entity.Bodega;
import com.technova.campusdesk.exception.ResourceNotFoundException;
import com.technova.campusdesk.repository.BodegaRepository;

import lombok.RequiredArgsConstructor;

// Servicio de logica de negocio para gestion de bodegas
@Service
@RequiredArgsConstructor
public class BodegaService {

	private final BodegaRepository bodegaRepository;

	// Listar todas las bodegas registradas
	@Transactional(readOnly = true)
	public List<BodegaResponse> listarTodas() {
		return bodegaRepository.findAll()
				.stream()
				.map(BodegaResponse::fromEntity)
				.toList();
	}

	// Buscar una bodega por su identificador primario
	@Transactional(readOnly = true)
	public BodegaResponse buscarPorId(Long id) {
		Bodega bodega = bodegaRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Bodega no encontrada con ID: " + id));
		return BodegaResponse.fromEntity(bodega);
	}

	// Registrar una nueva bodega en el sistema
	@Transactional
	public BodegaResponse crear(BodegaRequest request) {
		Bodega bodega = Bodega.builder()
				.nombre(request.nombre().trim())
				.ubicacion(request.ubicacion().trim())
				.build();

		Bodega guardada = bodegaRepository.save(bodega);
		return BodegaResponse.fromEntity(guardada);
	}

}
