package com.technova.campusdesk.dto.request;

import com.technova.campusdesk.entity.enums.Category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * [T3] Body de PUT /api/tickets/{id}/transferir.
 * Si nuevaCategoria no es un valor del enum (por ejemplo "REDES"), Jackson no puede convertirla y
 * GlobalExceptionHandler responde 400 (HttpMessageNotReadableException) sin llegar al service.
 */
public record TransferenciaRequest(
		@NotNull(message = "La nueva categoría es obligatoria")
		Category nuevaCategoria,

		@NotBlank(message = "La justificación es obligatoria")
		@Size(max = 500, message = "La justificación no puede superar 500 caracteres")
		String justificacion) {
}
