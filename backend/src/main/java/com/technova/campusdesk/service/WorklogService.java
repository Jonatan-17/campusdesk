package com.technova.campusdesk.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.technova.campusdesk.dto.request.WorklogRequest;
import com.technova.campusdesk.dto.response.TicketResponse;
import com.technova.campusdesk.dto.response.WorklogResponse;
import com.technova.campusdesk.entity.Ticket;
import com.technova.campusdesk.entity.TicketWorklog;
import com.technova.campusdesk.entity.User;
import com.technova.campusdesk.entity.enums.TicketStatus;
import com.technova.campusdesk.exception.BusinessRuleException;
import com.technova.campusdesk.exception.ForbiddenException;
import com.technova.campusdesk.repository.TicketWorklogRepository;

import lombok.RequiredArgsConstructor;

/** [T4] Registro de tiempo de los técnicos sobre los tickets. */
@Service
@RequiredArgsConstructor
public class WorklogService {

	private final TicketService ticketService;
	private final TicketWorklogRepository worklogRepository;

	/*
	 * Reglas: solo el TECHNICIAN asignado (ni siquiera el ADMIN: el tiempo es de quien trabajó) y solo en IN_PROGRESS.
	 * El rango 5-480 ya lo validó @Valid en el controller (400) antes de llegar aquí.
	 * Orden: 404 → 403 (permiso) → 409 (estado). Se valida el permiso antes que el estado para no revelarle
	 * a alguien sin permiso en qué estado está el ticket.
	 */
	@Transactional
	public WorklogResponse registrar(Long ticketId, WorklogRequest request, User currentUser) {
		Ticket ticket = ticketService.getAccessibleTicket(ticketId, currentUser);

		if (!TicketService.isAssignedTechnician(ticket, currentUser)) {
			throw new ForbiddenException("Solo el técnico asignado puede registrar tiempo en este ticket.");
		}
		if (ticket.getStatus() != TicketStatus.IN_PROGRESS) {
			throw new BusinessRuleException("Solo se registra tiempo en tickets IN_PROGRESS (está en "
					+ ticket.getStatus() + ").");
		}

		TicketWorklog worklog = TicketWorklog.builder()
				.ticket(ticket)
				.tecnico(currentUser)
				.minutosDedicados(request.minutosDedicados())
				.descripcionActividad(request.descripcionActividad().trim())
				.build();

		return toResponse(worklogRepository.save(worklog));
	}

	// Lo puede consultar cualquiera que vea el ticket (el enunciado no lo restringe más)
	@Transactional(readOnly = true)
	public WorklogResponse.TiempoTotal tiempoTotal(Long ticketId, User currentUser) {
		Ticket ticket = ticketService.getAccessibleTicket(ticketId, currentUser);
		Long total = worklogRepository.sumarMinutosPorTicket(ticket.getId());
		var registros = worklogRepository.findByTicketIdOrderByFechaRegistroAscIdAsc(ticket.getId()).stream()
				.map(this::toResponse)
				.toList();
		return new WorklogResponse.TiempoTotal(ticket.getId(), total, registros);
	}

	private WorklogResponse toResponse(TicketWorklog w) {
		return new WorklogResponse(w.getId(), w.getMinutosDedicados(), w.getDescripcionActividad(),
				w.getFechaRegistro(),
				new TicketResponse.UserSummary(w.getTecnico().getId(), w.getTecnico().getFullName()));
	}

}
