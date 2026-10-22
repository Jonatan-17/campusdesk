package com.technova.campusdesk.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.technova.campusdesk.dto.response.TicketResponse;
import com.technova.campusdesk.entity.StatusHistory;
import com.technova.campusdesk.entity.Ticket;
import com.technova.campusdesk.entity.User;
import com.technova.campusdesk.entity.enums.TicketStatus;
import com.technova.campusdesk.exception.BadRequestException;
import com.technova.campusdesk.exception.BusinessRuleException;
import com.technova.campusdesk.exception.ForbiddenException;
import com.technova.campusdesk.repository.StatusHistoryRepository;
import com.technova.campusdesk.repository.TicketRepository;

import lombok.RequiredArgsConstructor;

/**
 * [T2] Reapertura de tickets resueltos: RESOLVED → IN_PROGRESS, solo el solicitante y solo dentro de 48 horas.
 *
 * Decisión clave: NO se toca TicketTransitionPolicy. Si allí se permitiera RESOLVED → IN_PROGRESS, cualquiera
 * podría reabrir con PATCH /status saltándose el motivo, el contador y el límite de 48 horas.
 * La reapertura es una acción aparte, con sus propias reglas.
 */
@Service
@RequiredArgsConstructor
public class ReaperturaService {

	static final long HORAS_PARA_REABRIR = 48;

	private final TicketService ticketService;
	private final TicketRepository ticketRepository;
	private final StatusHistoryRepository statusHistoryRepository;
	private final StatusHistoryService statusHistoryService;

	/*
	 * Orden de las comprobaciones (de lo general a lo particular):
	 * 404 no existe → 403 no es el solicitante → 409 no está RESOLVED → 400 pasaron las 48 h.
	 * Todo en UNA transacción: estado, contador e historial se guardan juntos o no se guarda nada.
	 */
	@Transactional
	public TicketResponse reabrir(Long ticketId, String motivo, User currentUser) {
		Ticket ticket = ticketService.getAccessibleTicket(ticketId, currentUser);

		// "Solo el USER creador": no basta con el rol USER (un observador también es USER) [P8]
		if (!TicketService.isRequester(ticket, currentUser)) {
			throw new ForbiddenException("Solo el solicitante puede reabrir este ticket.");
		}
		if (ticket.getStatus() != TicketStatus.RESOLVED) {
			throw new BusinessRuleException("Solo se puede reabrir un ticket en estado RESOLVED (está en "
					+ ticket.getStatus() + ").");
		}

		LocalDateTime resueltoEn = fechaDeResolucion(ticket);
		LocalDateTime limite = resueltoEn.plusHours(HORAS_PARA_REABRIR);
		/*
		 * Trampa de ChronoUnit.HOURS.between(resueltoEn, ahora) > 48: TRUNCA las horas. Con 48 h y 59 min
		 * devuelve 48 y dejaría reabrir casi 49 horas después. Comparar fechas es exacto:
		 * "ahora es posterior a (resuelto + 48 h)".
		 */
		if (LocalDateTime.now().isAfter(limite)) {
			throw new BadRequestException("Pasaron más de " + HORAS_PARA_REABRIR
					+ " horas desde que se resolvió el ticket. Crea un nuevo ticket.");
		}

		ticket.setStatus(TicketStatus.IN_PROGRESS);
		ticket.setReaperturasCount(ticket.getReaperturasCount() + 1);
		// El técnico asignado se conserva: el mismo técnico retoma el trabajo

		Ticket guardado = ticketRepository.save(ticket);
		statusHistoryService.record(guardado, TicketStatus.RESOLVED, TicketStatus.IN_PROGRESS, currentUser,
				"Reapertura: " + motivo.trim());

		return ticketService.toResponse(guardado);
	}

	// Tickets reabiertos al menos una vez (solo ADMIN: lo exige @PreAuthorize en el controller)
	@Transactional(readOnly = true)
	public List<TicketResponse> listarReabiertos() {
		return ticketRepository.findByReaperturasCountGreaterThanOrderByReaperturasCountDescIdAsc(0).stream()
				.map(ticketService::toResponse)
				.toList();
	}

	/*
	 * ¿Cuándo pasó a RESOLVED? El ticket no guarda esa fecha, pero el historial sí: es la fuente de verdad.
	 * Se toma el ÚLTIMO paso a RESOLVED (un ticket reabierto pudo resolverse varias veces).
	 * Alternativa válida: agregar una columna resolved_at al ticket y llenarla al resolver.
	 * Respaldo: si no hay historial (datos cargados a mano), se usa la última modificación del ticket.
	 */
	private LocalDateTime fechaDeResolucion(Ticket ticket) {
		return statusHistoryRepository
				.findFirstByTicketIdAndNewStatusOrderByChangedAtDescIdDesc(ticket.getId(), TicketStatus.RESOLVED)
				.map(StatusHistory::getChangedAt)
				.orElse(ticket.getUpdatedAt());
	}

}
