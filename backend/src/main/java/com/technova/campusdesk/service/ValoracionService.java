package com.technova.campusdesk.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.technova.campusdesk.dto.request.ValoracionRequest;
import com.technova.campusdesk.dto.response.ValoracionResponse;
import com.technova.campusdesk.entity.Ticket;
import com.technova.campusdesk.entity.User;
import com.technova.campusdesk.entity.Valoracion;
import com.technova.campusdesk.entity.enums.TicketStatus;
import com.technova.campusdesk.exception.BusinessRuleException;
import com.technova.campusdesk.exception.ForbiddenException;
import com.technova.campusdesk.exception.ResourceNotFoundException;
import com.technova.campusdesk.repository.ValoracionRepository;

import lombok.RequiredArgsConstructor;

/** [P2] Encuesta de satisfacción: el solicitante valora un ticket cerrado, una sola vez. */
@Service
@RequiredArgsConstructor
public class ValoracionService {

	private final TicketService ticketService;
	private final ValoracionRepository valoracionRepository;

	// Orden: 404 → 403 (no es el solicitante, aunque sea ADMIN) → 409 (no está CLOSED) → 409 (ya valorado)
	@Transactional
	public ValoracionResponse valorar(Long ticketId, ValoracionRequest request, User currentUser) {
		Ticket ticket = ticketService.getAccessibleTicket(ticketId, currentUser);

		if (!TicketService.isRequester(ticket, currentUser)) {
			throw new ForbiddenException("Solo el solicitante puede valorar este ticket.");
		}
		if (ticket.getStatus() != TicketStatus.CLOSED) {
			throw new BusinessRuleException("Solo se pueden valorar tickets CLOSED.");
		}
		// Comprobación previa para dar un mensaje claro; el UNIQUE(ticket_id) de la base cubre la carrera
		if (valoracionRepository.existsByTicketId(ticket.getId())) {
			throw new BusinessRuleException("Este ticket ya fue valorado.");
		}

		String comentario = request.comentario() == null || request.comentario().isBlank()
				? null : request.comentario().trim();

		Valoracion valoracion = valoracionRepository.save(Valoracion.builder()
				.ticket(ticket)
				.puntuacion(request.puntuacion())
				.comentario(comentario)
				.build());
		return toResponse(valoracion);
	}

	// La ve cualquiera con acceso al ticket; 404 si todavía no hay valoración
	@Transactional(readOnly = true)
	public ValoracionResponse obtener(Long ticketId, User currentUser) {
		Ticket ticket = ticketService.getAccessibleTicket(ticketId, currentUser);
		return valoracionRepository.findByTicketId(ticket.getId())
				.map(this::toResponse)
				.orElseThrow(() -> new ResourceNotFoundException("El ticket " + ticketId + " no tiene valoración."));
	}

	private ValoracionResponse toResponse(Valoracion v) {
		// v.getTicket().getId() no dispara consulta: el proxy LAZY ya conoce su id
		return new ValoracionResponse(v.getId(), v.getTicket().getId(), v.getPuntuacion(), v.getComentario(),
				v.getFecha());
	}

}
