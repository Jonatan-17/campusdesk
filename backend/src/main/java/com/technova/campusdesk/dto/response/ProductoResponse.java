package com.technova.campusdesk.dto.response;

import com.technova.campusdesk.entity.Producto;

// DTO de respuesta para la entidad Producto con los datos de su Bodega asociada
public record ProductoResponse(
		Long id,
		String nombre,
		Integer stock,
		BodegaResponse bodega) {

	// Metodo de conversion de Entidad Producto a DTO
	public static ProductoResponse fromEntity(Producto producto) {
		if (producto == null) return null;
		return new ProductoResponse(
				producto.getId(),
				producto.getNombre(),
				producto.getStock(),
				BodegaResponse.fromEntity(producto.getBodega()));
	}
}
