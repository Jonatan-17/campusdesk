package com.technova.campusdesk.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.technova.campusdesk.dto.response.SlaStatusResponse;
import com.technova.campusdesk.dto.response.TicketResponse;
import com.technova.campusdesk.security.SecurityUtils;
import com.technova.campusdesk.service.SlaService;

import lombok.RequiredArgsConstructor;

/** [P7] Tickets con el plazo de atención (SLA) vencido. */
@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class SlaController {

	private final SlaService slaService;

	// GET /api/tickets/vencidos — solo ADMIN → 200
	@GetMapping("/vencidos")
	@PreAuthorize("hasRole('ADMIN')")
	public List<TicketResponse> vencidos() {
		return slaService.vencidos(SecurityUtils.getCurrentUser());
	}

	// GET /api/tickets/{id}/sla-status — dueño, técnico asignado o ADMIN → 200
	@GetMapping("/{id}/sla-status")
	public SlaStatusResponse estado(@PathVariable Long id) {
		return slaService.estado(id, SecurityUtils.getCurrentUser());
	}

}
