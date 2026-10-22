package com.technova.campusdesk.controller;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.technova.campusdesk.dto.response.CuentaEstadoResponse;
import com.technova.campusdesk.security.SecurityUtils;
import com.technova.campusdesk.service.CuentaService;

import lombok.RequiredArgsConstructor;

/**
 * [P4] Activar y desactivar cuentas.
 * Aquí NO hace falta @PreAuthorize: la ruta empieza con /api/users/ y SecurityConfig ya exige ADMIN para
 * "/api/users/**". Son dos formas de proteger: por URL (SecurityConfig) o por método (@PreAuthorize).
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class CuentaController {

	private final CuentaService cuentaService;

	// PATCH /api/users/{id}/desactivar → 200 con los tickets liberados (si era técnico)
	@PatchMapping("/{id}/desactivar")
	public CuentaEstadoResponse desactivar(@PathVariable Long id) {
		return cuentaService.desactivar(id, SecurityUtils.getCurrentUser());
	}

	// PATCH /api/users/{id}/activar → 200
	@PatchMapping("/{id}/activar")
	public CuentaEstadoResponse activar(@PathVariable Long id) {
		return cuentaService.activar(id);
	}

}
