package com.technova.campusdesk.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * [T4] Body de POST /api/tickets/{id}/worklogs.
 * @NotNull además de @Min/@Max: las anotaciones de rango consideran válido el null, así que sin @NotNull
 * un body sin "minutosDedicados" pasaría la validación y fallaría en la base.
 */
public record WorklogRequest(
		@NotNull(message = "Los minutos son obligatorios")
		@Min(value = 5, message = "Mínimo 5 minutos por registro")
		@Max(value = 480, message = "Máximo 480 minutos (8 horas) por registro")
		Integer minutosDedicados,

		@NotBlank(message = "La descripción de la actividad es obligatoria")
		@Size(max = 500, message = "La descripción no puede superar 500 caracteres")
		String descripcionActividad) {
}
