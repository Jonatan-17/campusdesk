package com.technova.campusdesk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.technova.campusdesk.entity.Bodega;

// Repositorio JPA para operaciones CRUD sobre la tabla bodegas
@Repository
public interface BodegaRepository extends JpaRepository<Bodega, Long> {

	boolean existsByNombreIgnoreCase(String nombre);

}
