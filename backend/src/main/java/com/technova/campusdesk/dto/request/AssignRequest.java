package com.technova.campusdesk.dto.request;

import jakarta.validation.constraints.NotNull;

/** Body de PATCH /api/tickets/{id}/assign: el id del técnico que se asigna. */
public record AssignRequest(
    @NotNull(message = "El técnico es obligatorio")
    Long technicianId
) {}
