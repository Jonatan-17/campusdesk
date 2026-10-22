package com.technova.campusdesk.entity.enums;

/**
 * Rol del usuario. Define qué puede hacer cada cuenta (ver la matriz de permisos en el README).
 * Spring Security lo usa como autoridad "ROLE_" + nombre (ver UserPrincipal).
 */
public enum Role {
	// Ve todo, asigna técnicos y consulta usuarios. Lo crea el DataInitializer
	ADMIN,
	// Atiende los tickets que tiene asignados. Lo crea el DataInitializer
	TECHNICIAN,
	// Crea y sigue sus propios tickets. Es el único rol que da el registro público
	USER
}
