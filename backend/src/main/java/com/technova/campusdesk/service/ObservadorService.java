package com.technova.campusdesk.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.technova.campusdesk.dto.response.TicketResponse;
import com.technova.campusdesk.entity.Ticket;
import com.technova.campusdesk.entity.User;
import com.technova.campusdesk.entity.enums.Role;
import com.technova.campusdesk.entity.enums.TicketStatus;
import com.technova.campusdesk.exception.BusinessRuleException;
import com.technova.campusdesk.exception.ForbiddenException;
import com.technova.campusdesk.exception.ResourceNotFoundException;
import com.technova.campusdesk.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * [P8] Observadores de un ticket: usuarios que pueden VER y COMENTAR el ticket sin ser sus dueños.
 *
 * Lo importante de este ejercicio no es este service, sino lo que obliga a cambiar en el resto:
 * - TicketService.getAccessibleTicket: un observador pasa el filtro de acceso (consulta existsObservador).
 * - TicketSpecification: el listado del USER incluye los tickets que observa (subconsulta EXISTS).
 * - TicketService.updateTicketStatus: ahora hay que exigir "ser el solicitante" para cerrar, porque un
 *   observador también tiene rol USER y TicketTransitionPolicy solo mira el rol.
 * Si se cambia solo uno de los dos primeros, el observador ve el ticket en el detalle pero no en su listado (o al revés).
 */
@Service
@RequiredArgsConstructor
public class ObservadorService {

	private final TicketService ticketService;
	private final UserRepository userRepository;

	@Transactional
	public List<TicketResponse.UserSummary> agregar(Long ticketId, Long usuarioId, User currentUser) {
		Ticket ticket = ticketDelSolicitante(ticketId, currentUser);

		if (ticket.getStatus() == TicketStatus.CLOSED) {
			throw new BusinessRuleException("No se agregan observadores a un ticket CLOSED.");
		}

		User observador = userRepository.findById(usuarioId)
				.orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + usuarioId));

		if (observador.getId().equals(currentUser.getId())) {
			throw new BusinessRuleException("El solicitante ya tiene acceso a su ticket.");
		}
		// Solo USER: ADMIN ya ve todo, y un TECHNICIAN observador abriría la puerta a que cambie estados ajenos
		if (observador.getRole() != Role.USER) {
			throw new BusinessRuleException("Solo se pueden agregar usuarios con rol USER como observadores.");
		}
		// Set.add devuelve false si ya estaba
		if (!ticket.getObservadores().add(observador)) {
			throw new BusinessRuleException(observador.getFullName() + " ya es observador de este ticket.");
		}
		// Sin save(): al hacer commit, Hibernate inserta la fila nueva en ticket_observadores

		return resumen(ticket);
	}

	@Transactional
	public void quitar(Long ticketId, Long usuarioId, User currentUser) {
		Ticket ticket = ticketDelSolicitante(ticketId, currentUser);
		boolean quitado = ticket.getObservadores().removeIf(u -> u.getId().equals(usuarioId));
		if (!quitado) {
			throw new ResourceNotFoundException("El usuario " + usuarioId + " no es observador de este ticket.");
		}
	}

	// Lo puede consultar cualquiera con acceso al ticket (incluidos los propios observadores)
	@Transactional(readOnly = true)
	public List<TicketResponse.UserSummary> listar(Long ticketId, User currentUser) {
		return resumen(ticketService.getAccessibleTicket(ticketId, currentUser));
	}

	// Agregar y quitar observadores es cosa del dueño del ticket
	private Ticket ticketDelSolicitante(Long ticketId, User currentUser) {
		Ticket ticket = ticketService.getAccessibleTicket(ticketId, currentUser);
		if (!TicketService.isRequester(ticket, currentUser)) {
			throw new ForbiddenException("Solo el solicitante puede gestionar los observadores.");
		}
		return ticket;
	}

	// Se recorre la colección LAZY dentro de la transacción, así que no hay LazyInitializationException
	private List<TicketResponse.UserSummary> resumen(Ticket ticket) {
		return ticket.getObservadores().stream()
				.sorted(Comparator.comparing(User::getFullName))
				.map(u -> new TicketResponse.UserSummary(u.getId(), u.getFullName()))
				.toList();
	}

}
