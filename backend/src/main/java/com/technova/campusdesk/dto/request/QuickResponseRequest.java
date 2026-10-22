package com.technova.campusdesk.dto.request;

import com.technova.campusdesk.entity.enums.Category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** [T5] Body para crear o editar una plantilla. El técnico dueño NO viene aquí: es el usuario autenticado. */
public record QuickResponseRequest(
		@NotBlank(message = "El título es obligatorio")
		@Size(max = 100, message = "El título no puede superar 100 caracteres")
		String titulo,

		// 1000 = el máximo de un comentario: la plantilla tiene que caber al aplicarla
		@NotBlank(message = "El contenido es obligatorio")
		@Size(max = 1000, message = "El contenido no puede superar 1000 caracteres")
		String contenido,

		@NotNull(message = "La categoría es obligatoria")
		Category categoria) {
}
