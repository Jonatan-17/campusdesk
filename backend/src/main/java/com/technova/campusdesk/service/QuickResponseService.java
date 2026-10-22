package com.technova.campusdesk.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.technova.campusdesk.dto.request.QuickResponseRequest;
import com.technova.campusdesk.dto.response.CommentResponse;
import com.technova.campusdesk.dto.response.QuickResponseResponse;
import com.technova.campusdesk.entity.QuickResponse;
import com.technova.campusdesk.entity.Ticket;
import com.technova.campusdesk.entity.User;
import com.technova.campusdesk.entity.enums.TicketStatus;
import com.technova.campusdesk.exception.BusinessRuleException;
import com.technova.campusdesk.exception.ForbiddenException;
import com.technova.campusdesk.exception.ResourceNotFoundException;
import com.technova.campusdesk.repository.QuickResponseRepository;

import lombok.RequiredArgsConstructor;

/**
 * [T5] Plantillas de respuesta rápida. El rol TECHNICIAN lo exige @PreAuthorize en el controller;
 * aquí se controla la PERTENENCIA (cada técnico solo ve y toca las suyas).
 */
@Service
@RequiredArgsConstructor
public class QuickResponseService {

	private final QuickResponseRepository quickResponseRepository;
	private final TicketService ticketService;
	private final CommentService commentService;

	@Transactional
	public QuickResponseResponse crear(QuickResponseRequest request, User tecnico) {
		String titulo = request.titulo().trim();
		/*
		 * Doble defensa de la unicidad:
		 * 1. Esta comprobación da un 409 con un mensaje claro (y no distingue mayúsculas).
		 * 2. El UNIQUE (tecnico_id, titulo) de la base cubre la carrera de dos peticiones simultáneas:
		 *    la segunda falla con DataIntegrityViolationException → también 409 (GlobalExceptionHandler).
		 */
		if (quickResponseRepository.existsByTecnicoIdAndTituloIgnoreCase(tecnico.getId(), titulo)) {
			throw new BusinessRuleException("Ya tienes una plantilla con el título \"" + titulo + "\".");
		}

		QuickResponse plantilla = QuickResponse.builder()
				.tecnico(tecnico)
				.titulo(titulo)
				.contenido(request.contenido().trim())
				.categoria(request.categoria())
				.build();
		return QuickResponseResponse.fromEntity(quickResponseRepository.save(plantilla));
	}

	@Transactional(readOnly = true)
	public List<QuickResponseResponse> listarPropias(User tecnico) {
		return quickResponseRepository.findByTecnicoIdOrderByTituloAsc(tecnico.getId()).stream()
				.map(QuickResponseResponse::fromEntity)
				.toList();
	}

	@Transactional(readOnly = true)
	public QuickResponseResponse obtener(Long id, User tecnico) {
		return QuickResponseResponse.fromEntity(buscarPropia(id, tecnico));
	}

	@Transactional
	public QuickResponseResponse actualizar(Long id, QuickResponseRequest request, User tecnico) {
		QuickResponse plantilla = buscarPropia(id, tecnico);
		String titulo = request.titulo().trim();
		if (quickResponseRepository.existsByTecnicoIdAndTituloIgnoreCaseAndIdNot(tecnico.getId(), titulo, id)) {
			throw new BusinessRuleException("Ya tienes otra plantilla con el título \"" + titulo + "\".");
		}
		plantilla.setTitulo(titulo);
		plantilla.setContenido(request.contenido().trim());
		plantilla.setCategoria(request.categoria());
		// Sin save(): la entidad está "managed" y Hibernate guarda los cambios al hacer commit (dirty checking)
		return QuickResponseResponse.fromEntity(plantilla);
	}

	@Transactional
	public void eliminar(Long id, User tecnico) {
		quickResponseRepository.delete(buscarPropia(id, tecnico));
	}

	/*
	 * Publica la plantilla como comentario. Comprueba, en este orden:
	 * 1. La plantilla es del técnico autenticado (404 si no).
	 * 2. El ticket existe y lo puede ver (404/403) y además es SU técnico asignado (403).
	 * 3. El ticket está IN_PROGRESS (409).
	 * Luego REUTILIZA CommentService.addComment en vez de crear el Comment a mano: así cualquier regla futura
	 * de los comentarios (largo, tickets cerrados, notificaciones) se aplica también aquí.
	 */
	@Transactional
	public CommentResponse aplicar(Long ticketId, Long plantillaId, User tecnico) {
		QuickResponse plantilla = buscarPropia(plantillaId, tecnico);
		Ticket ticket = ticketService.getAccessibleTicket(ticketId, tecnico);

		if (!TicketService.isAssignedTechnician(ticket, tecnico)) {
			throw new ForbiddenException("Solo puedes aplicar plantillas en tickets que tienes asignados.");
		}
		if (ticket.getStatus() != TicketStatus.IN_PROGRESS) {
			throw new BusinessRuleException("Solo se aplican plantillas en tickets IN_PROGRESS.");
		}

		return commentService.addComment(ticketId, plantilla.getContenido(), tecnico);
	}

	/*
	 * Una plantilla ajena responde 404, no 403. Con 403 le confirmaríamos al técnico que esa plantilla existe
	 * (y que es de otro); con 404 no sabe si existe. Es la forma habitual de proteger recursos privados.
	 */
	private QuickResponse buscarPropia(Long id, User tecnico) {
		return quickResponseRepository.findByIdAndTecnicoId(id, tecnico.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Plantilla no encontrada con id: " + id));
	}

}
