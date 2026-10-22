package com.technova.campusdesk.service;

import com.technova.campusdesk.dto.response.StatusHistoryResponse;
import com.technova.campusdesk.dto.response.TicketResponse;
import com.technova.campusdesk.entity.StatusHistory;
import com.technova.campusdesk.entity.Ticket;
import com.technova.campusdesk.entity.User;
import com.technova.campusdesk.entity.enums.TicketStatus;
import com.technova.campusdesk.repository.StatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Historial de cambios de estado (RF-06). record() se llama dentro de la transacción del servicio que
 * cambia el estado (@Transactional sin REQUIRES_NEW): el cambio y su registro se guardan juntos o no se guarda ninguno.
 */
@Service
@RequiredArgsConstructor
public class StatusHistoryService {

    private final StatusHistoryRepository statusHistoryRepository;

    // Guarda un registro: estado anterior (null al crear), estado nuevo y quién hizo el cambio
    @Transactional
    public void record(Ticket ticket, TicketStatus previousStatus, TicketStatus newStatus, User changedBy) {
        record(ticket, previousStatus, newStatus, changedBy, null);
    }

    // [T2][T3][P4][P6] Igual que el anterior, pero con el motivo del cambio (reapertura, transferencia...)
    @Transactional
    public void record(Ticket ticket, TicketStatus previousStatus, TicketStatus newStatus, User changedBy, String reason) {
        StatusHistory history = new StatusHistory();
        history.setTicket(ticket);
        history.setPreviousStatus(previousStatus);
        history.setNewStatus(newStatus);
        history.setChangedBy(changedBy);
        history.setReason(reason);

        statusHistoryRepository.save(history);
    }

    // Historial de un ticket en orden cronológico. El acceso al ticket lo valida TicketService.
    @Transactional(readOnly = true)
    public List<StatusHistoryResponse> findByTicket(Long ticketId) {
        return statusHistoryRepository.findByTicketIdOrderByChangedAtAscIdAsc(ticketId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    private StatusHistoryResponse mapToResponse(StatusHistory history) {
        return new StatusHistoryResponse(
            history.getId(),
            history.getPreviousStatus(),
            history.getNewStatus(),
            new TicketResponse.UserSummary(history.getChangedBy().getId(), history.getChangedBy().getFullName()),
            history.getChangedAt(),
            history.getReason()
        );
    }
}
