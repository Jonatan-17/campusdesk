package com.technova.campusdesk.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.technova.campusdesk.dto.request.ProductoRequest;
import com.technova.campusdesk.dto.response.ProductoResponse;
import com.technova.campusdesk.service.ProductoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// Controlador REST para gestion de productos e inventario de bodegas
@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
public class ProductoController {

	private final ProductoService productoService;

	// GET /api/productos - Listar todos los productos junto con su bodega
	@GetMapping
	public ResponseEntity<List<ProductoResponse>> listarProductos() {
		return ResponseEntity.ok(productoService.listarTodos());
	}

	// GET /api/productos/{id} - Obtener un producto por su ID
	@GetMapping("/{id}")
	public ResponseEntity<ProductoResponse> obtenerProducto(@PathVariable Long id) {
		return ResponseEntity.ok(productoService.buscarPorId(id));
	}

	// GET /api/productos/bodega/{bodegaId} - Listar productos pertenecientes a una bodega
	@GetMapping("/bodega/{bodegaId}")
	public ResponseEntity<List<ProductoResponse>> listarPorBodega(@PathVariable Long bodegaId) {
		return ResponseEntity.ok(productoService.listarPorBodega(bodegaId));
	}

	// POST /api/productos - Crear un producto asociado a una bodega existente
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ResponseEntity<ProductoResponse> crearProducto(@Valid @RequestBody ProductoRequest request) {
		ProductoResponse creado = productoService.crear(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(creado);
	}

}
