package com.technova.campusdesk.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.technova.campusdesk.entity.Producto;

// Repositorio JPA para operaciones CRUD sobre la tabla productos
@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

	// Consulta derivada para obtener todos los productos de una bodega especifica
	List<Producto> findByBodegaId(Long bodegaId);

}
