package com.technova.campusdesk.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// DTO para la creacion o actualizacion de bodegas
public record BodegaRequest(
		@NotBlank(message = "El nombre de la bodega es obligatorio")
		@Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
		String nombre,

		@NotBlank(message = "La ubicacion de la bodega es obligatoria")
		@Size(max = 150, message = "La ubicacion no puede exceder 150 caracteres")
		String ubicacion) {
}
