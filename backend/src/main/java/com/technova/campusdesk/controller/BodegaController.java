package com.technova.campusdesk.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.technova.campusdesk.dto.request.BodegaRequest;
import com.technova.campusdesk.dto.response.BodegaResponse;
import com.technova.campusdesk.service.BodegaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// Controlador REST para gestion de bodegas de almacenamiento
@RestController
@RequestMapping("/api/bodegas")
@RequiredArgsConstructor
public class BodegaController {

	private final BodegaService bodegaService;

	// GET /api/bodegas - Listar todas las bodegas
	@GetMapping
	public ResponseEntity<List<BodegaResponse>> listarBodegas() {
		return ResponseEntity.ok(bodegaService.listarTodas());
	}

	// GET /api/bodegas/{id} - Obtener detalle de una bodega por su ID
	@GetMapping("/{id}")
	public ResponseEntity<BodegaResponse> obtenerBodega(@PathVariable Long id) {
		return ResponseEntity.ok(bodegaService.buscarPorId(id));
	}

	// POST /api/bodegas - Registrar una nueva bodega
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ResponseEntity<BodegaResponse> crearBodega(@Valid @RequestBody BodegaRequest request) {
		BodegaResponse creada = bodegaService.crear(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(creada);
	}

}
