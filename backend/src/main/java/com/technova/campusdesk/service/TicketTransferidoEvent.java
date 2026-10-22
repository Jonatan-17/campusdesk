package com.technova.campusdesk.service;

import com.technova.campusdesk.entity.enums.Category;

/**
 * [T3] Evento "se transfirió un ticket". Lo publica TransferenciaService y lo escucha AuditoriaTransferenciaListener.
 * Lleva ids y no entidades: así el evento no depende de que la sesión de Hibernate siga abierta.
 */
public record TicketTransferidoEvent(
		Long ticketId,
		Long autorId,
		Category categoriaAnterior,
		Category categoriaNueva,
		String justificacion) {
}
