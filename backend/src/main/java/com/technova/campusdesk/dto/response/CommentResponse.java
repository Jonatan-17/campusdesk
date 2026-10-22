package com.technova.campusdesk.dto.response;

import java.time.LocalDateTime;

/** Comentario como lo devuelve la API: el autor va resumido (id y nombre), sin datos sensibles. */
public record CommentResponse(
    Long id,
    String content,
    TicketResponse.UserSummary author,
    LocalDateTime createdAt
) {}
