package com.technova.campusdesk.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.technova.campusdesk.dto.response.TicketResponse;
import com.technova.campusdesk.entity.Tag;
import com.technova.campusdesk.entity.Ticket;
import com.technova.campusdesk.entity.User;
import com.technova.campusdesk.entity.enums.Role;
import com.technova.campusdesk.exception.BadRequestException;
import com.technova.campusdesk.exception.ForbiddenException;
import com.technova.campusdesk.repository.TagRepository;
import com.technova.campusdesk.repository.TicketRepository;

import lombok.RequiredArgsConstructor;

/**
 * [T1] Etiquetas de los tickets: asociarlas (creándolas si no existen) y buscar tickets por etiqueta.
 */
@Service
@RequiredArgsConstructor
public class TagService {

	private static final int MAX_LARGO_TAG = 50;

	private final TagRepository tagRepository;
	private final TicketRepository ticketRepository;
	private final TicketService ticketService;

	/*
	 * Asocia etiquetas a un ticket. Pasos:
	 * 1. Acceso (404/403) y permiso: ADMIN o el TECHNICIAN asignado.
	 * 2. Normalizar: trim + mayúsculas + sin repetidos ("vpn", " VPN " → una sola VPN).
	 * 3. UNA consulta con IN para traer las que ya existen; crear solo las que faltan.
	 * 4. Agregarlas al Set del ticket. Al ser un Set, volver a enviar una etiqueta que ya tiene no la duplica.
	 * No hace falta llamar a save(ticket): el ticket está "managed" dentro de la transacción y Hibernate
	 * inserta las filas nuevas de ticket_tags al hacer commit (dirty checking).
	 */
	@Transactional
	public TicketResponse agregarTags(Long ticketId, List<String> nombres, User currentUser) {
		Ticket ticket = ticketService.getAccessibleTicket(ticketId, currentUser);

		boolean esAdmin = currentUser.getRole() == Role.ADMIN;
		if (!esAdmin && !TicketService.isAssignedTechnician(ticket, currentUser)) {
			throw new ForbiddenException("Solo un ADMIN o el técnico asignado pueden etiquetar este ticket.");
		}

		Set<String> normalizados = normalizar(nombres);

		Map<String, Tag> existentes = tagRepository.buscarPorNombres(normalizados).stream()
				.collect(Collectors.toMap(Tag::getNombre, Function.identity()));

		for (String nombre : normalizados) {
			// "Crear o vincular": si ya existe se reutiliza la misma fila; si no, se crea
			Tag tag = existentes.computeIfAbsent(nombre, n -> tagRepository.save(Tag.builder().nombre(n).build()));
			ticket.getTags().add(tag);
		}

		return ticketService.toResponse(ticket);
	}

	// Tickets con esa etiqueta que el usuario puede ver: se combina el filtro de etiqueta con la visibilidad por rol
	@Transactional(readOnly = true)
	public List<TicketResponse> buscarPorTag(String nombre, User currentUser) {
		String normalizado = normalizarUno(nombre);
		var spec = TicketSpecification.filterByRoleAndCriteria(currentUser, null)
				.and(TicketSpecification.conTag(normalizado));
		return ticketRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt", "id")).stream()
				.map(ticketService::toResponse)
				.toList();
	}

	// La lista llega "cruda" (List<String>), así que se valida a mano: no hay un DTO con anotaciones
	private Set<String> normalizar(List<String> nombres) {
		if (nombres == null || nombres.isEmpty()) {
			throw new BadRequestException("Envía al menos una etiqueta.");
		}
		// LinkedHashSet: quita repetidos y conserva el orden en que llegaron
		Set<String> resultado = new LinkedHashSet<>();
		for (String nombre : nombres) {
			resultado.add(normalizarUno(nombre));
		}
		return resultado;
	}

	private String normalizarUno(String nombre) {
		if (nombre == null || nombre.isBlank()) {
			throw new BadRequestException("El nombre de la etiqueta no puede estar vacío.");
		}
		// Locale.ROOT: con el idioma del sistema en turco, "vpn".toUpperCase() no daría "VPN"
		String normalizado = nombre.trim().toUpperCase(Locale.ROOT);
		if (normalizado.length() > MAX_LARGO_TAG) {
			throw new BadRequestException("La etiqueta no puede tener más de " + MAX_LARGO_TAG + " caracteres.");
		}
		return normalizado;
	}

}
