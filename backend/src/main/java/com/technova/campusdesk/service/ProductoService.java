package com.technova.campusdesk.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.technova.campusdesk.dto.request.ProductoRequest;
import com.technova.campusdesk.dto.response.ProductoResponse;
import com.technova.campusdesk.entity.Bodega;
import com.technova.campusdesk.entity.Producto;
import com.technova.campusdesk.exception.ResourceNotFoundException;
import com.technova.campusdesk.repository.BodegaRepository;
import com.technova.campusdesk.repository.ProductoRepository;

import lombok.RequiredArgsConstructor;

// Servicio de logica de negocio para gestion de productos e inventario
@Service
@RequiredArgsConstructor
public class ProductoService {

	private final ProductoRepository productoRepository;
	private final BodegaRepository bodegaRepository;

	// Listar todos los productos con los datos de su bodega asociada
	@Transactional(readOnly = true)
	public List<ProductoResponse> listarTodos() {
		return productoRepository.findAll()
				.stream()
				.map(ProductoResponse::fromEntity)
				.toList();
	}

	// Buscar un producto por ID
	@Transactional(readOnly = true)
	public ProductoResponse buscarPorId(Long id) {
		Producto producto = productoRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));
		return ProductoResponse.fromEntity(producto);
	}

	// Listar productos filtrados por una bodega especifica
	@Transactional(readOnly = true)
	public List<ProductoResponse> listarPorBodega(Long bodegaId) {
		if (!bodegaRepository.existsById(bodegaId)) {
			throw new ResourceNotFoundException("Bodega no encontrada con ID: " + bodegaId);
		}
		return productoRepository.findByBodegaId(bodegaId)
				.stream()
				.map(ProductoResponse::fromEntity)
				.toList();
	}

	// Crear y registrar un nuevo producto asociado a una bodega existente
	@Transactional
	public ProductoResponse crear(ProductoRequest request) {
		Bodega bodega = bodegaRepository.findById(request.bodegaId())
				.orElseThrow(() -> new ResourceNotFoundException("No se puede crear el producto: la bodega con ID " + request.bodegaId() + " no existe"));

		Producto producto = Producto.builder()
				.nombre(request.nombre().trim())
				.stock(request.stock())
				.bodega(bodega)
				.build();

		Producto guardado = productoRepository.save(producto);
		return ProductoResponse.fromEntity(guardado);
	}

}
