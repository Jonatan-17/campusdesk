package com.technova.campusdesk.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.technova.campusdesk.dto.request.TransferenciaRequest;
import com.technova.campusdesk.dto.response.TicketResponse;
import com.technova.campusdesk.entity.Ticket;
import com.technova.campusdesk.entity.User;
import com.technova.campusdesk.entity.enums.Category;
import com.technova.campusdesk.entity.enums.Role;
import com.technova.campusdesk.entity.enums.TicketStatus;
import com.technova.campusdesk.exception.BadRequestException;
import com.technova.campusdesk.exception.BusinessRuleException;
import com.technova.campusdesk.exception.ForbiddenException;
import com.technova.campusdesk.repository.TicketRepository;

import lombok.RequiredArgsConstructor;

/**
 * [T3] Transferencia de un ticket a otra categoría (área). Efectos: cambia la categoría, desvincula al técnico,
 * vuelve a OPEN (con su registro en el historial) y deja una nota automática (vía evento).
 * Los nombres del enunciado se traducen al modelo del proyecto: REDES → NETWORK, ABIERTA → OPEN.
 */
@Service
@RequiredArgsConstructor
public class TransferenciaService {

	private final TicketService ticketService;
	private final TicketRepository ticketRepository;
	private final StatusHistoryService statusHistoryService;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public TicketResponse transferir(Long ticketId, TransferenciaRequest request, User currentUser) {
		Ticket ticket = ticketService.getAccessibleTicket(ticketId, currentUser);

		/*
		 * TRAMPA: el permiso se comprueba ANTES de desvincular. Si primero se hiciera technician = null,
		 * ya no se podría saber si quien transfiere era el técnico asignado.
		 */
		boolean esAdmin = currentUser.getRole() == Role.ADMIN;
		if (!esAdmin && !TicketService.isAssignedTechnician(ticket, currentUser)) {
			throw new ForbiddenException("Solo un ADMIN o el técnico asignado pueden transferir este ticket.");
		}

		// El enunciado no lo dice; decisión propia: un ticket cerrado ya no se mueve de área
		if (ticket.getStatus() == TicketStatus.CLOSED) {
			throw new BusinessRuleException("No se puede transferir un ticket CLOSED.");
		}

		Category anterior = ticket.getCategory();
		if (anterior == request.nuevaCategoria()) {
			throw new BadRequestException("El ticket ya pertenece a la categoría " + anterior + ".");
		}

		TicketStatus estadoAnterior = ticket.getStatus();
		String justificacion = request.justificacion().trim();

		ticket.setCategory(request.nuevaCategoria());
		// "Desvinculación segura": solo se pone la FK en null. El técnico (User) no se toca ni se borra.
		ticket.setTechnician(null);
		ticket.setStatus(TicketStatus.OPEN);

		Ticket guardado = ticketRepository.save(ticket);

		// El retroceso a OPEN es un cambio de estado: se registra en el historial como cualquier otro (RF-06).
		// Si ya estaba OPEN, no hay cambio de estado que registrar.
		if (estadoAnterior != TicketStatus.OPEN) {
			statusHistoryService.record(guardado, estadoAnterior, TicketStatus.OPEN, currentUser,
					"Transferencia " + anterior + " → " + request.nuevaCategoria() + ": " + justificacion);
		}

		// La nota en comentarios la escribe AuditoriaTransferenciaListener antes del commit
		eventPublisher.publishEvent(new TicketTransferidoEvent(guardado.getId(), currentUser.getId(), anterior,
				request.nuevaCategoria(), justificacion));

		return ticketService.toResponse(guardado);
	}

}
