package com.technova.campusdesk.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.technova.campusdesk.dto.response.TicketResponse;
import com.technova.campusdesk.security.SecurityUtils;
import com.technova.campusdesk.service.TagService;

import lombok.RequiredArgsConstructor;

/**
 * [T1] Etiquetas. Comparte el prefijo /api/tickets con TicketController: Spring permite varios controllers
 * con el mismo @RequestMapping mientras las rutas completas no choquen.
 * "/por-tag" no choca con "/{id}": Spring prefiere la ruta literal antes que la que tiene variables.
 */
@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TagController {

	private final TagService tagService;

	// POST /api/tickets/{id}/tags — body: ["vpn", "Remoto"] → 200 con el ticket y sus etiquetas
	@PostMapping("/{id}/tags")
	public ResponseEntity<TicketResponse> agregarTags(@PathVariable Long id, @RequestBody List<String> nombres) {
		return ResponseEntity.ok(tagService.agregarTags(id, nombres, SecurityUtils.getCurrentUser()));
	}

	// GET /api/tickets/por-tag?nombre=vpn — tickets con esa etiqueta, según lo que el rol puede ver → 200
	@GetMapping("/por-tag")
	public ResponseEntity<List<TicketResponse>> porTag(@RequestParam String nombre) {
		return ResponseEntity.ok(tagService.buscarPorTag(nombre, SecurityUtils.getCurrentUser()));
	}

}
