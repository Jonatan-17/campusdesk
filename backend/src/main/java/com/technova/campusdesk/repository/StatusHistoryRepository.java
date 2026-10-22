package com.technova.campusdesk.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.technova.campusdesk.entity.StatusHistory;
import com.technova.campusdesk.entity.enums.TicketStatus;

/** Acceso a la tabla status_history. Solo se lee en orden cronológico; los registros los crea StatusHistoryService. */
public interface StatusHistoryRepository extends JpaRepository<StatusHistory, Long> {

	// Orden cronológico; el id desempata cambios hechos en el mismo instante.
	@EntityGraph(attributePaths = "changedBy")
	List<StatusHistory> findByTicketIdOrderByChangedAtAscIdAsc(Long ticketId);

	// [T2] Último registro en el que el ticket pasó a un estado (por ejemplo RESOLVED): "findFirst" + "OrderBy...Desc"
	Optional<StatusHistory> findFirstByTicketIdAndNewStatusOrderByChangedAtDescIdDesc(Long ticketId, TicketStatus newStatus);

}
