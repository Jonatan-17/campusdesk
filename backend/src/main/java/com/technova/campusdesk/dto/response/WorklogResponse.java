package com.technova.campusdesk.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/** [T4] Un registro de trabajo, y el DTO de GET /tiempo-total que los agrupa. */
public record WorklogResponse(
		Long id,
		Integer minutosDedicados,
		String descripcionActividad,
		LocalDateTime fechaRegistro,
		TicketResponse.UserSummary tecnico) {

	public record TiempoTotal(Long ticketId, Long totalMinutos, List<WorklogResponse> registros) {
	}

}
