package com.technova.campusdesk.dto.response;

import com.technova.campusdesk.service.TicketService;

/**
 * [P3] Fila del reporte GET /api/reports/carga-tecnicos. La crea directamente la consulta JPQL
 * (SELECT new ...), por eso necesita un constructor que coincida en orden y tipos con el SELECT
 * (el de 4 parámetros de abajo). Ojo: SUM devuelve Long, no Integer.
 */
public record CargaTecnicoResponse(
		Long tecnicoId,
		String nombre,
		Long asignados,
		Long enProceso,
		long activos,
		int limite) {

	// Constructor que usa la consulta: activos y limite se calculan aquí, no en SQL
	public CargaTecnicoResponse(Long tecnicoId, String nombre, Long asignados, Long enProceso) {
		this(tecnicoId, nombre, asignados, enProceso, asignados + enProceso,
				TicketService.MAX_TICKETS_ACTIVOS_POR_TECNICO);
	}

}
