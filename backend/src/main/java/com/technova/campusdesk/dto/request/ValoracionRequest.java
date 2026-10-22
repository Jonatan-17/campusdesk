package com.technova.campusdesk.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** [P2] Body de POST /api/tickets/{id}/valoracion. El comentario es opcional (puede ser null). */
public record ValoracionRequest(
		@NotNull(message = "La puntuación es obligatoria")
		@Min(value = 1, message = "La puntuación mínima es 1")
		@Max(value = 5, message = "La puntuación máxima es 5")
		Integer puntuacion,

		@Size(max = 500, message = "El comentario no puede superar 500 caracteres")
		String comentario) {
}
