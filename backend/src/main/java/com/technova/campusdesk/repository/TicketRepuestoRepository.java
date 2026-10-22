package com.technova.campusdesk.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.technova.campusdesk.entity.TicketRepuesto;

/** [P1] Acceso a ticket_repuestos. */
public interface TicketRepuestoRepository extends JpaRepository<TicketRepuesto, Long> {

	@EntityGraph(attributePaths = { "producto", "tecnico" })
	List<TicketRepuesto> findByTicketIdOrderByFechaRegistroAscIdAsc(Long ticketId);

}
