package com.technova.campusdesk.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.technova.campusdesk.dto.request.ValoracionRequest;
import com.technova.campusdesk.dto.response.ValoracionResponse;
import com.technova.campusdesk.security.SecurityUtils;
import com.technova.campusdesk.service.ValoracionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** [P2] Valoración de tickets cerrados. El reporte por técnico está en ReportController. */
@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class ValoracionController {

	private final ValoracionService valoracionService;

	// POST /api/tickets/{id}/valoracion — body {"puntuacion": 5, "comentario": "..."} → 201
	@PostMapping("/{id}/valoracion")
	public ResponseEntity<ValoracionResponse> valorar(@PathVariable Long id,
			@Valid @RequestBody ValoracionRequest request) {
		ValoracionResponse creada = valoracionService.valorar(id, request, SecurityUtils.getCurrentUser());
		return ResponseEntity.status(HttpStatus.CREATED).body(creada);
	}

	// GET /api/tickets/{id}/valoracion → 200, o 404 si no hay
	@GetMapping("/{id}/valoracion")
	public ValoracionResponse obtener(@PathVariable Long id) {
		return valoracionService.obtener(id, SecurityUtils.getCurrentUser());
	}

}
