package com.technova.campusdesk.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.technova.campusdesk.dto.response.SlaStatusResponse;
import com.technova.campusdesk.dto.response.TicketResponse;
import com.technova.campusdesk.entity.Ticket;
import com.technova.campusdesk.entity.User;
import com.technova.campusdesk.entity.enums.Role;
import com.technova.campusdesk.entity.enums.TicketStatus;
import com.technova.campusdesk.exception.ForbiddenException;
import com.technova.campusdesk.exception.ResourceNotFoundException;
import com.technova.campusdesk.repository.StatusHistoryRepository;
import com.technova.campusdesk.repository.TicketRepository;

import lombok.RequiredArgsConstructor;

/**
 * [P7] Tickets vencidos (SLA). La fecha límite la calcula Ticket.recalcularFechaLimite() al crear el ticket
 * (@PrePersist) y TicketService.updateTicket cuando cambia la prioridad. Las horas por prioridad están en el enum Priority.
 */
@Service
@RequiredArgsConstructor
public class SlaService {

	private final TicketRepository ticketRepository;
	private final StatusHistoryRepository statusHistoryRepository;
	private final TicketService ticketService;

	// Solo ADMIN puede consultar; el controller aplica también @PreAuthorize.
	@Transactional(readOnly = true)
	public List<TicketResponse> vencidos(User currentUser) {
		if (currentUser.getRole() != Role.ADMIN) {
			throw new ForbiddenException("Solo un ADMIN puede consultar los tickets vencidos.");
		}
		return ticketRepository.findByStatusInAndFechaLimiteBeforeOrderByFechaLimiteAscIdAsc(
				List.of(TicketStatus.OPEN, TicketStatus.ASSIGNED, TicketStatus.IN_PROGRESS), LocalDateTime.now()).stream()
				.map(ticketService::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public SlaStatusResponse estado(Long ticketId, User currentUser) {
		Ticket ticket = ticketRepository.findById(ticketId)
				.orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado con id: " + ticketId));
		if (!puedeConsultar(ticket, currentUser)) {
			throw new ForbiddenException("No tienes permiso para consultar el SLA de este ticket.");
		}
		if (ticket.getFechaLimite() == null) {
			throw new IllegalStateException("El ticket no tiene una fecha límite de SLA.");
		}

		LocalDateTime ahora = LocalDateTime.now();
		LocalDateTime referencia = ahora;
		if (ticket.getStatus() == TicketStatus.RESOLVED || ticket.getStatus() == TicketStatus.CLOSED) {
			referencia = statusHistoryRepository
					.findFirstByTicketIdAndNewStatusOrderByChangedAtDescIdDesc(ticketId, TicketStatus.RESOLVED)
					.map(history -> history.getChangedAt())
					.orElse(ticket.getUpdatedAt());
		}

		long minutosRestantes = Math.max(0, java.time.Duration.between(referencia, ticket.getFechaLimite()).toMinutes());
		boolean vencido = referencia.isAfter(ticket.getFechaLimite());
		long minutosTotales = java.time.Duration.between(ticket.getCreatedAt(), ticket.getFechaLimite()).toMinutes();
		long minutosConsumidos = java.time.Duration.between(ticket.getCreatedAt(), referencia).toMinutes();
		double porcentajeConsumido = minutosTotales <= 0 ? 100.0
				: Math.clamp(minutosConsumidos * 100.0 / minutosTotales, 0.0, 100.0);
		porcentajeConsumido = Math.round(porcentajeConsumido * 100.0) / 100.0;

		return new SlaStatusResponse(minutosRestantes, vencido, porcentajeConsumido);
	}

	private boolean puedeConsultar(Ticket ticket, User user) {
		return user.getRole() == Role.ADMIN
				|| ticket.getRequester().getId().equals(user.getId())
				|| (ticket.getTechnician() != null && ticket.getTechnician().getId().equals(user.getId()));
	}

}
