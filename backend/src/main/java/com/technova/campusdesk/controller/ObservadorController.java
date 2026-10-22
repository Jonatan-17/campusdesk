package com.technova.campusdesk.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.technova.campusdesk.dto.request.ObservadorRequest;
import com.technova.campusdesk.dto.response.TicketResponse;
import com.technova.campusdesk.security.SecurityUtils;
import com.technova.campusdesk.service.ObservadorService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** [P8] Observadores de un ticket. */
@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class ObservadorController {

	private final ObservadorService observadorService;

	// POST /api/tickets/{id}/observadores — body {"usuarioId": 5} → 200 con la lista actualizada
	@PostMapping("/{id}/observadores")
	public List<TicketResponse.UserSummary> agregar(@PathVariable Long id, @Valid @RequestBody ObservadorRequest request) {
		return observadorService.agregar(id, request.usuarioId(), SecurityUtils.getCurrentUser());
	}

	// GET /api/tickets/{id}/observadores → 200
	@GetMapping("/{id}/observadores")
	public List<TicketResponse.UserSummary> listar(@PathVariable Long id) {
		return observadorService.listar(id, SecurityUtils.getCurrentUser());
	}

	// DELETE /api/tickets/{id}/observadores/{usuarioId} → 204
	@DeleteMapping("/{id}/observadores/{usuarioId}")
	public ResponseEntity<Void> quitar(@PathVariable Long id, @PathVariable Long usuarioId) {
		observadorService.quitar(id, usuarioId, SecurityUtils.getCurrentUser());
		return ResponseEntity.noContent().build();
	}

}
