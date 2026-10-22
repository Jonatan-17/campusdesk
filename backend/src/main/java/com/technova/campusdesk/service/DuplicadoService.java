package com.technova.campusdesk.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.technova.campusdesk.dto.response.TicketResponse;
import com.technova.campusdesk.entity.Ticket;
import com.technova.campusdesk.entity.User;
import com.technova.campusdesk.entity.enums.Role;
import com.technova.campusdesk.entity.enums.TicketStatus;
import com.technova.campusdesk.exception.BadRequestException;
import com.technova.campusdesk.exception.BusinessRuleException;
import com.technova.campusdesk.exception.ForbiddenException;
import com.technova.campusdesk.exception.ResourceNotFoundException;
import com.technova.campusdesk.repository.TicketRepository;

import lombok.RequiredArgsConstructor;

/**
 * [P6] Marcar un ticket como duplicado de otro: el duplicado pasa a CLOSED y apunta al original.
 *
 * Esto SE SALTA la máquina de estados a propósito (por ejemplo OPEN → CLOSED, que TicketTransitionPolicy prohíbe):
 * cerrar por duplicado no es "el solicitante confirma la solución", es una acción administrativa distinta.
 * Por eso es un método aparte, con sus propias reglas, y deja el motivo en el historial.
 */
@Service
@RequiredArgsConstructor
public class DuplicadoService {

	private final TicketService ticketService;
	private final TicketRepository ticketRepository;
	private final StatusHistoryService statusHistoryService;

	@Transactional
	public TicketResponse marcarDuplicado(Long ticketId, Long originalId, User currentUser) {
		if (ticketId.equals(originalId)) {
			throw new BadRequestException("Un ticket no puede ser duplicado de sí mismo.");
		}

		Ticket duplicado = ticketService.getAccessibleTicket(ticketId, currentUser);
		boolean esAdmin = currentUser.getRole() == Role.ADMIN;
		if (!esAdmin && !TicketService.isAssignedTechnician(duplicado, currentUser)) {
			throw new ForbiddenException("Solo un ADMIN o el técnico asignado pueden marcar este ticket como duplicado.");
		}

		// El original se busca sin getAccessibleTicket: el técnico puede no tener acceso a él y aun así
		// reconocer que es el mismo problema. Solo se expone su id en la respuesta.
		Ticket original = ticketRepository.findById(originalId)
				.orElseThrow(() -> new ResourceNotFoundException("Ticket original no encontrado con id: " + originalId));

		if (duplicado.getStatus() == TicketStatus.CLOSED) {
			throw new BusinessRuleException("El ticket ya está cerrado.");
		}
		if (original.getStatus() == TicketStatus.CLOSED) {
			throw new BusinessRuleException("El original está cerrado: no tiene sentido redirigir la atención a él.");
		}
		/*
		 * Evitar cadenas y ciclos (A → B → A). Con dos reglas basta:
		 * 1. El original no puede ser a su vez duplicado: hay que apuntar a la raíz.
		 * 2. El ticket que se marca no puede ser original de otros (si no, esos quedarían en cadena).
		 * Como un duplicado siempre queda CLOSED, la regla 1 ya la cubre la comprobación anterior; se deja
		 * explícita porque el mensaje es más claro y no depende de esa coincidencia.
		 */
		if (original.getDuplicadoDe() != null) {
			throw new BusinessRuleException("El ticket " + originalId + " ya es duplicado del "
					+ original.getDuplicadoDe().getId() + ": márcalo como duplicado de ese.");
		}
		if (ticketRepository.existsByDuplicadoDeId(duplicado.getId())) {
			throw new BusinessRuleException("Otros tickets apuntan a este como original; no puede ser duplicado.");
		}

		TicketStatus anterior = duplicado.getStatus();
		duplicado.setDuplicadoDe(original);
		duplicado.setStatus(TicketStatus.CLOSED);

		Ticket guardado = ticketRepository.save(duplicado);
		statusHistoryService.record(guardado, anterior, TicketStatus.CLOSED, currentUser,
				"Duplicado del ticket " + originalId);

		return ticketService.toResponse(guardado);
	}

}
