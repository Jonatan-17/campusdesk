package com.technova.campusdesk.dto.response;

import java.time.LocalDateTime;

/** [P2] Valoración de un ticket. */
public record ValoracionResponse(Long id, Long ticketId, Integer puntuacion, String comentario, LocalDateTime fecha) {
}
