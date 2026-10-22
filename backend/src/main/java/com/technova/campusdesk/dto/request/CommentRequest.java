package com.technova.campusdesk.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body de POST /api/tickets/{id}/comments. @NotBlank rechaza vacíos y solo espacios (400). */
public record CommentRequest(
    @NotBlank(message = "El comentario no puede estar vacío")
    @Size(max = 1000, message = "El comentario no puede exceder 1000 caracteres")
    String content
) {}
