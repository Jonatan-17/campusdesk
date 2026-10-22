package com.technova.campusdesk.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * [P1] Body de POST /api/tickets/{id}/repuestos: {"items": [{"productoId": 1, "cantidad": 2}, ...]}.
 * List<@Valid Item>: sin @Valid, Spring valida la lista (que no esté vacía) pero NO entra a validar cada Item,
 * y una cantidad 0 o negativa pasaría.
 */
public record RepuestosRequest(
		@NotEmpty(message = "Envía al menos un repuesto")
		List<@Valid Item> items) {

	public record Item(
			@NotNull(message = "El producto es obligatorio")
			Long productoId,

			@NotNull(message = "La cantidad es obligatoria")
			@Min(value = 1, message = "La cantidad debe ser al menos 1")
			Integer cantidad) {
	}

}
