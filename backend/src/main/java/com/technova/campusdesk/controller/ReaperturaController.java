package com.technova.campusdesk.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.technova.campusdesk.dto.request.ReaperturaRequest;
import com.technova.campusdesk.dto.response.TicketResponse;
import com.technova.campusdesk.security.SecurityUtils;
import com.technova.campusdesk.service.ReaperturaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** [T2] Reapertura de tickets resueltos. */
@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class ReaperturaController {

	private final ReaperturaService reaperturaService;

	// POST /api/tickets/{id}/reabrir — body {"motivo": "..."} → 200 con el ticket en IN_PROGRESS
	@PostMapping("/{id}/reabrir")
	public ResponseEntity<TicketResponse> reabrir(@PathVariable Long id, @Valid @RequestBody ReaperturaRequest request) {
		return ResponseEntity.ok(reaperturaService.reabrir(id, request.motivo(), SecurityUtils.getCurrentUser()));
	}

	/*
	 * GET /api/tickets/reabiertos — solo ADMIN.
	 * @PreAuthorize funciona porque SecurityConfig tiene @EnableMethodSecurity. Si el rol no coincide, Spring lanza
	 * AccessDeniedException ANTES de entrar al método y GlobalExceptionHandler responde 403.
	 * Alternativa equivalente: .requestMatchers(HttpMethod.GET, "/api/tickets/reabiertos").hasRole("ADMIN") en SecurityConfig.
	 */
	@GetMapping("/reabiertos")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<List<TicketResponse>> reabiertos() {
		return ResponseEntity.ok(reaperturaService.listarReabiertos());
	}

}
