package com.technova.campusdesk.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** [T2] Body de POST /api/tickets/{id}/reabrir. El motivo queda en el historial. */
public record ReaperturaRequest(
		@NotBlank(message = "El motivo es obligatorio")
		@Size(max = 250, message = "El motivo no puede superar 250 caracteres")
		String motivo) {
}
