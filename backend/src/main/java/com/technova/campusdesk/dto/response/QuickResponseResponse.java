package com.technova.campusdesk.dto.response;

import com.technova.campusdesk.entity.QuickResponse;
import com.technova.campusdesk.entity.enums.Category;

/** [T5] Plantilla como la devuelve la API. */
public record QuickResponseResponse(Long id, String titulo, String contenido, Category categoria) {

	public static QuickResponseResponse fromEntity(QuickResponse q) {
		return new QuickResponseResponse(q.getId(), q.getTitulo(), q.getContenido(), q.getCategoria());
	}

}
