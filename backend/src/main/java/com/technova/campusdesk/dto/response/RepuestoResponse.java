package com.technova.campusdesk.dto.response;

import java.time.LocalDateTime;

/** [P1] Repuesto consumido en un ticket, con el stock actual del producto. */
public record RepuestoResponse(
		Long id,
		Long productoId,
		String producto,
		Integer cantidad,
		Integer stockActual, // stock del producto en este momento
		TicketResponse.UserSummary tecnico,
		LocalDateTime fechaRegistro) {
}
