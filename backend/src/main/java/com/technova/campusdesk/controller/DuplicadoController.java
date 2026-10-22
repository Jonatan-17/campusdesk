package com.technova.campusdesk.controller;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.technova.campusdesk.dto.response.TicketResponse;
import com.technova.campusdesk.security.SecurityUtils;
import com.technova.campusdesk.service.DuplicadoService;

import lombok.RequiredArgsConstructor;

/** [P6] Tickets duplicados. */
@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class DuplicadoController {

	private final DuplicadoService duplicadoService;

	// PATCH /api/tickets/{id}/duplicado-de/{originalId} — sin body → 200 con el ticket CLOSED y duplicadoDeId
	@PatchMapping("/{id}/duplicado-de/{originalId}")
	public TicketResponse marcar(@PathVariable Long id, @PathVariable Long originalId) {
		return duplicadoService.marcarDuplicado(id, originalId, SecurityUtils.getCurrentUser());
	}

}
