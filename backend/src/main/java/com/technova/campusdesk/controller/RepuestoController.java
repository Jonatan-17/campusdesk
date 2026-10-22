package com.technova.campusdesk.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.technova.campusdesk.dto.request.RepuestosRequest;
import com.technova.campusdesk.dto.response.RepuestoResponse;
import com.technova.campusdesk.security.SecurityUtils;
import com.technova.campusdesk.service.RepuestoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** [P1] Repuestos usados en un ticket (conecta el módulo de inventario con los tickets). */
@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class RepuestoController {

	private final RepuestoService repuestoService;

	// POST /api/tickets/{id}/repuestos — descuenta stock y registra el consumo → 201 (sin stock: 409, nada se guarda)
	@PostMapping("/{id}/repuestos")
	public ResponseEntity<List<RepuestoResponse>> registrar(@PathVariable Long id,
			@Valid @RequestBody RepuestosRequest request) {
		List<RepuestoResponse> creados = repuestoService.registrar(id, request, SecurityUtils.getCurrentUser());
		return ResponseEntity.status(HttpStatus.CREATED).body(creados);
	}

	// GET /api/tickets/{id}/repuestos — repuestos consumidos en el ticket → 200
	@GetMapping("/{id}/repuestos")
	public List<RepuestoResponse> listar(@PathVariable Long id) {
		return repuestoService.listar(id, SecurityUtils.getCurrentUser());
	}

}
