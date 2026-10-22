package com.technova.campusdesk.dto.request;

import jakarta.validation.constraints.NotNull;

/** [P8] Body de POST /api/tickets/{id}/observadores. */
public record ObservadorRequest(
		@NotNull(message = "El usuario es obligatorio")
		Long usuarioId) {
}
