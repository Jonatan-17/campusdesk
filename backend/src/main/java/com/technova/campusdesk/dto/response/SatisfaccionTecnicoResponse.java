package com.technova.campusdesk.dto.response;

/**
 * [P2] Fila de GET /api/reports/satisfaccion. La crea la consulta JPQL (SELECT new ...).
 * promedio es null si el técnico no tiene valoraciones: un 0 se leería como "pésimo", y no es lo mismo.
 */
public record SatisfaccionTecnicoResponse(Long tecnicoId, String nombre, Double promedio, Long cantidad) {

	// Constructor compacto: redondea el promedio a 2 decimales (4.3333 → 4.33)
	public SatisfaccionTecnicoResponse {
		if (promedio != null) {
			promedio = Math.round(promedio * 100) / 100.0;
		}
	}

}
