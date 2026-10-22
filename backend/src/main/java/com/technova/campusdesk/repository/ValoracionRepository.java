package com.technova.campusdesk.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.technova.campusdesk.dto.response.SatisfaccionTecnicoResponse;
import com.technova.campusdesk.entity.Valoracion;
import com.technova.campusdesk.entity.enums.Role;

/** [P2] Acceso a la tabla valoraciones. */
public interface ValoracionRepository extends JpaRepository<Valoracion, Long> {

	boolean existsByTicketId(Long ticketId);

	Optional<Valoracion> findByTicketId(Long ticketId);

	/*
	 * Promedio y cantidad de valoraciones por técnico, en una sola consulta.
	 * - Se parte de User con LEFT JOIN: los técnicos SIN valoraciones también salen (AVG = null, COUNT = 0).
	 *   Partiendo de Valoracion con JOIN normal, esos técnicos no aparecerían en el reporte.
	 * - COUNT(v) cuenta solo las filas donde v no es null; COUNT(*) contaría 1 para un técnico sin nada.
	 * - AVG de un Integer devuelve Double; COUNT devuelve Long.
	 */
	@Query("""
			SELECT new com.technova.campusdesk.dto.response.SatisfaccionTecnicoResponse(
			    u.id, u.fullName, AVG(v.puntuacion), COUNT(v))
			FROM User u
			LEFT JOIN Ticket t ON t.technician = u
			LEFT JOIN Valoracion v ON v.ticket = t
			WHERE u.role = :role
			GROUP BY u.id, u.fullName
			ORDER BY u.fullName
			""")
	List<SatisfaccionTecnicoResponse> satisfaccionPorTecnico(@Param("role") Role role);

}
