package com.technova.campusdesk.dto.response;

import com.technova.campusdesk.entity.Bodega;

// DTO de respuesta para la entidad Bodega
public record BodegaResponse(
		Long id,
		String nombre,
		String ubicacion) {

	// Metodo de conversion de Entidad a DTO
	public static BodegaResponse fromEntity(Bodega bodega) {
		if (bodega == null) return null;
		return new BodegaResponse(
				bodega.getId(),
				bodega.getNombre(),
				bodega.getUbicacion());
	}
}
