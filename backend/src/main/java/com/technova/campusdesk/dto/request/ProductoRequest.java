package com.technova.campusdesk.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// DTO para la creacion o actualizacion de productos asociados a una bodega
public record ProductoRequest(
		@NotBlank(message = "El nombre del producto es obligatorio")
		@Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
		String nombre,

		@NotNull(message = "El stock es obligatorio")
		@Min(value = 0, message = "El stock no puede ser negativo")
		Integer stock,

		@NotNull(message = "El ID de la bodega es obligatorio")
		Long bodegaId) {
}
