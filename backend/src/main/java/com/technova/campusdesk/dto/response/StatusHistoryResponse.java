package com.technova.campusdesk.dto.response;

import com.technova.campusdesk.entity.enums.TicketStatus;
import java.time.LocalDateTime;

/** Un registro del historial: de qué estado a qué estado pasó el ticket, quién lo hizo y cuándo. */
public record StatusHistoryResponse(
    Long id,
    TicketStatus previousStatus, // null en el registro de creación
    TicketStatus newStatus,
    TicketResponse.UserSummary changedBy,
    LocalDateTime changedAt,
    String reason // [T2][T3][P4][P6] null en los pasos normales del ciclo
) {}
