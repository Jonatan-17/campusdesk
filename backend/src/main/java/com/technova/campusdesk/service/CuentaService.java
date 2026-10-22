package com.technova.campusdesk.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.technova.campusdesk.dto.response.CuentaEstadoResponse;
import com.technova.campusdesk.entity.Ticket;
import com.technova.campusdesk.entity.User;
import com.technova.campusdesk.entity.enums.Role;
import com.technova.campusdesk.entity.enums.TicketStatus;
import com.technova.campusdesk.exception.BusinessRuleException;
import com.technova.campusdesk.exception.ResourceNotFoundException;
import com.technova.campusdesk.repository.TicketRepository;
import com.technova.campusdesk.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * [P4] Activar y desactivar cuentas (solo ADMIN: lo exige la regla /api/users/** de SecurityConfig).
 * Desactivar = no puede iniciar sesión (UserPrincipal.isEnabled) ni seguir usando su token (JwtAuthenticationFilter).
 * No se BORRA al usuario: sus tickets, comentarios e historial lo siguen referenciando.
 */
@Service
@RequiredArgsConstructor
public class CuentaService {

	private final UserRepository userRepository;
	private final TicketRepository ticketRepository;
	private final StatusHistoryService statusHistoryService;

	/*
	 * Si es técnico, sus tickets ACTIVOS (ASSIGNED, IN_PROGRESS) vuelven a OPEN sin técnico, para que el ADMIN
	 * los reasigne. Los RESOLVED se dejan: el trabajo ya está hecho y solo falta que el solicitante cierre.
	 *
	 * Se recorren uno a uno en vez de un UPDATE masivo ("UPDATE Ticket t SET t.technician = null WHERE ..."):
	 * el masivo sería una sola sentencia, pero se salta @PreUpdate (updatedAt) y no deja registro en el historial.
	 */
	@Transactional
	public CuentaEstadoResponse desactivar(Long userId, User admin) {
		User usuario = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + userId));

		// Un ADMIN que se desactiva a sí mismo podría dejar el sistema sin ningún ADMIN
		if (usuario.getId().equals(admin.getId())) {
			throw new BusinessRuleException("No puedes desactivar tu propia cuenta.");
		}
		if (!usuario.isActive()) {
			throw new BusinessRuleException("La cuenta ya está desactivada.");
		}

		usuario.setActive(false);

		List<Long> liberados = List.of();
		if (usuario.getRole() == Role.TECHNICIAN) {
			List<Ticket> activos = ticketRepository.findByTechnicianIdAndStatusIn(usuario.getId(),
					TicketService.ESTADOS_ACTIVOS);
			for (Ticket ticket : activos) {
				TicketStatus anterior = ticket.getStatus();
				ticket.setTechnician(null);
				ticket.setStatus(TicketStatus.OPEN);
				statusHistoryService.record(ticket, anterior, TicketStatus.OPEN, admin,
						"Técnico " + usuario.getFullName() + " desactivado");
			}
			liberados = activos.stream().map(Ticket::getId).toList();
		}

		// Sin save(): usuario y tickets están "managed" y se guardan al hacer commit
		return new CuentaEstadoResponse(usuario.getId(), usuario.getFullName(), false, liberados);
	}

	@Transactional
	public CuentaEstadoResponse activar(Long userId) {
		User usuario = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + userId));
		if (usuario.isActive()) {
			throw new BusinessRuleException("La cuenta ya está activa.");
		}
		usuario.setActive(true);
		return new CuentaEstadoResponse(usuario.getId(), usuario.getFullName(), true, List.of());
	}

}
