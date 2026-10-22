package com.technova.campusdesk.dto.request;

import com.technova.campusdesk.entity.enums.TicketStatus;
import jakarta.validation.constraints.NotNull;

/** Body de PATCH /api/tickets/{id}/status: el estado al que se quiere pasar el ticket. */
public record StatusChangeRequest(
    @NotNull(message = "El estado es obligatorio")
    TicketStatus status
) {}
