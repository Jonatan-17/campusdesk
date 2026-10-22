package com.technova.campusdesk.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.technova.campusdesk.dto.request.QuickResponseRequest;
import com.technova.campusdesk.dto.response.CommentResponse;
import com.technova.campusdesk.dto.response.QuickResponseResponse;
import com.technova.campusdesk.security.SecurityUtils;
import com.technova.campusdesk.service.QuickResponseService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [T5] Plantillas de solución rápida. Sin @RequestMapping de clase porque las rutas tienen dos prefijos
 * distintos (/api/tecnicos/plantillas y /api/tickets/...): cada método lleva su ruta completa.
 * @PreAuthorize en la CLASE se aplica a todos los métodos: solo TECHNICIAN (otro rol → 403).
 */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('TECHNICIAN')")
public class QuickResponseController {

	private final QuickResponseService quickResponseService;

	// POST /api/tecnicos/plantillas — crea una plantilla del técnico autenticado → 201 (título repetido: 409)
	@PostMapping("/api/tecnicos/plantillas")
	public ResponseEntity<QuickResponseResponse> crear(@Valid @RequestBody QuickResponseRequest request) {
		QuickResponseResponse creada = quickResponseService.crear(request, SecurityUtils.getCurrentUser());
		return ResponseEntity.status(HttpStatus.CREATED).body(creada);
	}

	// GET /api/tecnicos/plantillas — solo las propias → 200
	@GetMapping("/api/tecnicos/plantillas")
	public List<QuickResponseResponse> listar() {
		return quickResponseService.listarPropias(SecurityUtils.getCurrentUser());
	}

	// GET /api/tecnicos/plantillas/{id} — 200, o 404 si no existe o es de otro técnico
	@GetMapping("/api/tecnicos/plantillas/{id}")
	public QuickResponseResponse obtener(@PathVariable Long id) {
		return quickResponseService.obtener(id, SecurityUtils.getCurrentUser());
	}

	// PUT /api/tecnicos/plantillas/{id} → 200
	@PutMapping("/api/tecnicos/plantillas/{id}")
	public QuickResponseResponse actualizar(@PathVariable Long id, @Valid @RequestBody QuickResponseRequest request) {
		return quickResponseService.actualizar(id, request, SecurityUtils.getCurrentUser());
	}

	// DELETE /api/tecnicos/plantillas/{id} → 204 sin cuerpo
	@DeleteMapping("/api/tecnicos/plantillas/{id}")
	public ResponseEntity<Void> eliminar(@PathVariable Long id) {
		quickResponseService.eliminar(id, SecurityUtils.getCurrentUser());
		return ResponseEntity.noContent().build();
	}

	// POST /api/tickets/{id}/aplicar-plantilla/{plantillaId} — publica la plantilla como comentario → 201
	@PostMapping("/api/tickets/{id}/aplicar-plantilla/{plantillaId}")
	public ResponseEntity<CommentResponse> aplicar(@PathVariable Long id, @PathVariable Long plantillaId) {
		CommentResponse comentario = quickResponseService.aplicar(id, plantillaId, SecurityUtils.getCurrentUser());
		return ResponseEntity.status(HttpStatus.CREATED).body(comentario);
	}

}
