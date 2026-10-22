package com.technova.campusdesk.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.technova.campusdesk.entity.User;
import com.technova.campusdesk.entity.enums.Role;

/**
 * Acceso a la tabla users. Spring Data JPA genera la implementación: con extender JpaRepository
 * ya hay save, findById, findAll, delete, etc., y cada método de abajo se traduce a SQL por su nombre.
 */
public interface UserRepository extends JpaRepository<User, Long> {

	// Login y carga del usuario en el filtro JWT: SELECT ... WHERE email = ?
	Optional<User> findByEmail(String email);

	// Registro (correo duplicado) y DataInitializer (no crear dos veces)
	boolean existsByEmail(String email);

	// Lista de técnicos para el ADMIN, ordenada por nombre
	List<User> findByRoleOrderByFullNameAsc(Role role);

}
