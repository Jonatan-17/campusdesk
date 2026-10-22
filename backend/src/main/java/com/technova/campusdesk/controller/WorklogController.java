package com.technova.campusdesk.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.technova.campusdesk.dto.request.WorklogRequest;
import com.technova.campusdesk.dto.response.WorklogResponse;
import com.technova.campusdesk.security.SecurityUtils;
import com.technova.campusdesk.service.WorklogService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** [T4] Bitácora de tiempo técnico. */
@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class WorklogController {

	private final WorklogService worklogService;

	// POST /api/tickets/{id}/worklogs — body {"minutosDedicados": 30, "descripcionActividad": "..."} → 201
	@PostMapping("/{id}/worklogs")
	public ResponseEntity<WorklogResponse> registrar(@PathVariable Long id, @Valid @RequestBody WorklogRequest request) {
		WorklogResponse creado = worklogService.registrar(id, request, SecurityUtils.getCurrentUser());
		return ResponseEntity.status(HttpStatus.CREATED).body(creado);
	}

	// GET /api/tickets/{id}/tiempo-total — {ticketId, totalMinutos, registros[]} → 200
	@GetMapping("/{id}/tiempo-total")
	public ResponseEntity<WorklogResponse.TiempoTotal> tiempoTotal(@PathVariable Long id) {
		return ResponseEntity.ok(worklogService.tiempoTotal(id, SecurityUtils.getCurrentUser()));
	}

}
