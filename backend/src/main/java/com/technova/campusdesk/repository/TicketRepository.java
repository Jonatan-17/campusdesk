package com.technova.campusdesk.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.technova.campusdesk.dto.response.CargaTecnicoResponse;
import com.technova.campusdesk.entity.Ticket;
import com.technova.campusdesk.entity.enums.Role;
import com.technova.campusdesk.entity.enums.TicketStatus;

/**
 * Los listados usan Specification para combinar los filtros opcionales (status, priority, category)
 * con la visibilidad según el rol (ADMIN todos, TECHNICIAN asignados, USER propios).
 * El EntityGraph carga requester y technician en la misma consulta: con open-in-view en false,
 * sin él el mapper fallaría con LazyInitializationException.
 * [T1] También carga "tags", porque TicketResponse ahora incluye las etiquetas. Es un Set: se puede traer
 * junto con las otras relaciones sin MultipleBagFetchException.
 * No quitar métodos sin avisar a Persona 4: los usan TicketService, ReportService y las pruebas.
 */
public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

	@Override
	@EntityGraph(attributePaths = { "requester", "technician", "tags" })
	Optional<Ticket> findById(Long id);

	@Override
	@EntityGraph(attributePaths = { "requester", "technician", "tags" })
	List<Ticket> findAll(Specification<Ticket> spec);

	@Override
	@EntityGraph(attributePaths = { "requester", "technician", "tags" })
	List<Ticket> findAll(Specification<Ticket> spec, Sort sort);

	// Indicadores del dashboard (GET /api/reports/summary)
	long countByStatus(TicketStatus status);

	long countByRequesterIdAndStatus(Long requesterId, TicketStatus status);

	long countByTechnicianIdAndStatus(Long technicianId, TicketStatus status);

	// [T2] Consulta derivada: "...WHERE reaperturas_count > ? ORDER BY reaperturas_count DESC, id ASC"
	@EntityGraph(attributePaths = { "requester", "technician", "tags" })
	List<Ticket> findByReaperturasCountGreaterThanOrderByReaperturasCountDescIdAsc(int minimo);

	// [P3] Tickets activos de un técnico: "...WHERE technician_id = ? AND status IN (?, ?)"
	long countByTechnicianIdAndStatusIn(Long technicianId, Collection<TicketStatus> statuses);

	// [P4] Tickets activos de un técnico, para liberarlos cuando se le desactiva
	List<Ticket> findByTechnicianIdAndStatusIn(Long technicianId, Collection<TicketStatus> statuses);

	// [P7] Tickets vencidos en estados abiertos, ordenados por fecha límite más antigua primero
	@EntityGraph(attributePaths = { "requester", "technician", "tags" })
	List<Ticket> findByStatusInAndFechaLimiteBeforeOrderByFechaLimiteAscIdAsc(
			Collection<TicketStatus> statuses, LocalDateTime fechaLimite);

	// [P6] ¿Algún ticket apunta a este como su original?
	boolean existsByDuplicadoDeId(Long ticketId);

	/*
	 * [P8] ¿El usuario es observador del ticket? Se pregunta con una consulta en vez de recorrer
	 * ticket.getObservadores(): esa colección es LAZY y getAccessibleTicket se llama también fuera de
	 * transacción (detalle del ticket), donde recorrerla lanzaría LazyInitializationException.
	 */
	@Query("SELECT COUNT(t) > 0 FROM Ticket t JOIN t.observadores o WHERE t.id = :ticketId AND o.id = :userId")
	boolean existsObservador(@Param("ticketId") Long ticketId, @Param("userId") Long userId);

	/*
	 * [P3] Carga de trabajo por técnico, en UNA consulta:
	 * - FROM User ... LEFT JOIN Ticket ON ...: el LEFT JOIN hace que aparezcan también los técnicos sin tickets
	 *   (con un JOIN normal desaparecerían). "JOIN entidad ON condición" es JPQL de Hibernate 6+.
	 * - SUM(CASE WHEN ... THEN 1 ELSE 0 END): cuenta por estado sin tener que agrupar por estado.
	 * - SELECT new ...: proyección directa al DTO; el record necesita un constructor con esos tipos y en ese orden.
	 * - Todo lo que está en el SELECT y no es agregado debe ir en el GROUP BY.
	 */
	@Query("""
			SELECT new com.technova.campusdesk.dto.response.CargaTecnicoResponse(
			    u.id, u.fullName,
			    SUM(CASE WHEN t.status = com.technova.campusdesk.entity.enums.TicketStatus.ASSIGNED THEN 1 ELSE 0 END),
			    SUM(CASE WHEN t.status = com.technova.campusdesk.entity.enums.TicketStatus.IN_PROGRESS THEN 1 ELSE 0 END))
			FROM User u LEFT JOIN Ticket t ON t.technician = u
			WHERE u.role = :role AND u.active = true
			GROUP BY u.id, u.fullName
			ORDER BY u.fullName
			""")
	List<CargaTecnicoResponse> cargaPorTecnico(@Param("role") Role role);

}
