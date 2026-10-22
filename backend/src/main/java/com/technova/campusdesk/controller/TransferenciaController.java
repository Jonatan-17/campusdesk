package com.technova.campusdesk.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.technova.campusdesk.dto.request.TransferenciaRequest;
import com.technova.campusdesk.dto.response.TicketResponse;
import com.technova.campusdesk.security.SecurityUtils;
import com.technova.campusdesk.service.TransferenciaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** [T3] Transferencia de tickets entre áreas (categorías). */
@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TransferenciaController {

	private final TransferenciaService transferenciaService;

	// PUT /api/tickets/{id}/transferir — body {"nuevaCategoria": "NETWORK", "justificacion": "..."} → 200
	@PutMapping("/{id}/transferir")
	public ResponseEntity<TicketResponse> transferir(@PathVariable Long id,
			@Valid @RequestBody TransferenciaRequest request) {
		return ResponseEntity.ok(transferenciaService.transferir(id, request, SecurityUtils.getCurrentUser()));
	}

}
