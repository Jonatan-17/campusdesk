package com.technova.campusdesk.dto.response;

import com.technova.campusdesk.entity.enums.Category;
import com.technova.campusdesk.entity.enums.Priority;
import com.technova.campusdesk.entity.enums.TicketStatus;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Ticket como lo devuelve la API. Nunca se devuelve la entidad Ticket directamente:
 * así no se exponen datos internos (como la contraseña del solicitante) ni relaciones lazy.
 */
public record TicketResponse(
    Long id,
    String title,
    String description,
    Category category,
    Priority priority,
    TicketStatus status,
    UserSummary requester,
    UserSummary technician, // Puede ser null
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<String> tags,          // [T1] nombres de las etiquetas, en orden alfabético
    Integer reaperturasCount,   // [T2]
    LocalDateTime fechaLimite,  // [P7]
    Long duplicadoDeId          // [P6] null si no es duplicado
) {
    // Resumen de un usuario dentro de otras respuestas (solicitante, técnico, autor de un comentario)
    public record UserSummary(Long id, String fullName) {}
}
