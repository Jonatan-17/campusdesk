package com.technova.campusdesk.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.technova.campusdesk.entity.TicketWorklog;

/** [T4] Acceso a la tabla ticket_worklogs. */
public interface TicketWorklogRepository extends JpaRepository<TicketWorklog, Long> {

	/*
	 * Total de minutos calculado por la base (SUM), no sumando en Java.
	 * TRAMPAS:
	 * - SUM sobre cero filas devuelve NULL, no 0. COALESCE(..., 0) lo convierte en 0.
	 * - En JPQL, SUM de un Integer devuelve Long: el método debe devolver Long (o long), no Integer.
	 */
	@Query("SELECT COALESCE(SUM(w.minutosDedicados), 0) FROM TicketWorklog w WHERE w.ticket.id = :ticketId")
	Long sumarMinutosPorTicket(@Param("ticketId") Long ticketId);

	// Detalle, del más antiguo al más reciente, con el técnico cargado en la misma consulta
	@EntityGraph(attributePaths = "tecnico")
	List<TicketWorklog> findByTicketIdOrderByFechaRegistroAscIdAsc(Long ticketId);

}
