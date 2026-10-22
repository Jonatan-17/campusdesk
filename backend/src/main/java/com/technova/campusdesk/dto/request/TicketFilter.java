package com.technova.campusdesk.dto.request;

import com.technova.campusdesk.entity.enums.Category;
import com.technova.campusdesk.entity.enums.Priority;
import com.technova.campusdesk.entity.enums.TicketStatus;

/** Filtros opcionales de GET /api/tickets (?status=&priority=&category=). Un campo null no filtra. */
public record TicketFilter(
    TicketStatus status,
    Priority priority,
    Category category
) {}