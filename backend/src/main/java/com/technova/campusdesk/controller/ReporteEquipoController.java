package com.technova.campusdesk.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.technova.campusdesk.dto.response.CargaTecnicoResponse;
import com.technova.campusdesk.dto.response.SatisfaccionTecnicoResponse;
import com.technova.campusdesk.service.ReporteEquipoService;

import lombok.RequiredArgsConstructor;

/** [P2][P3] Reportes del equipo técnico. Solo ADMIN (@PreAuthorize en la clase → 403 para los demás). */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ReporteEquipoController {

	private final ReporteEquipoService reporteEquipoService;

	// GET /api/reports/satisfaccion — [{tecnicoId, nombre, promedio, cantidad}] → 200
	@GetMapping("/satisfaccion")
	public List<SatisfaccionTecnicoResponse> satisfaccion() {
		return reporteEquipoService.satisfaccionPorTecnico();
	}

	// GET /api/reports/carga-tecnicos — [{tecnicoId, nombre, asignados, enProceso, activos, limite}] → 200
	@GetMapping("/carga-tecnicos")
	public List<CargaTecnicoResponse> cargaTecnicos() {
		return reporteEquipoService.cargaPorTecnico();
	}

}
