package com.technova.campusdesk.service;

import com.technova.campusdesk.dto.response.CommentResponse;
import com.technova.campusdesk.dto.response.TicketResponse;
import com.technova.campusdesk.entity.Comment;
import com.technova.campusdesk.entity.Ticket;
import com.technova.campusdesk.entity.User;
import com.technova.campusdesk.entity.enums.TicketStatus;
import com.technova.campusdesk.exception.BusinessRuleException;
import com.technova.campusdesk.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Comentarios de los tickets (RF-05). Usa las mismas reglas de acceso que el detalle del ticket. */
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final TicketService ticketService;

    // Comentarios en orden cronológico (dueño, técnico asignado o ADMIN)
    @Transactional(readOnly = true)
    public List<CommentResponse> listComments(Long ticketId, User currentUser) {
        Ticket ticket = ticketService.getAccessibleTicket(ticketId, currentUser);
        return commentRepository.findByTicketIdOrderByCreatedAtAscIdAsc(ticket.getId()).stream()
                .map(this::mapToResponse)
                .toList();
    }

    // Agrega un comentario a nombre del usuario autenticado. En un ticket CLOSED no se puede (409, RF-04 regla 7)
    @Transactional
    public CommentResponse addComment(Long ticketId, String content, User currentUser) {
        Ticket ticket = ticketService.getAccessibleTicket(ticketId, currentUser);

        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new BusinessRuleException("No se puede comentar en un ticket CLOSED.");
        }

        Comment comment = new Comment();
        comment.setTicket(ticket);
        comment.setAuthor(currentUser);
        comment.setContent(content.trim());

        Comment savedComment = commentRepository.save(comment);
        return mapToResponse(savedComment);
    }

    private CommentResponse mapToResponse(Comment comment) {
        return new CommentResponse(
            comment.getId(),
            comment.getContent(),
            new TicketResponse.UserSummary(comment.getAuthor().getId(), comment.getAuthor().getFullName()),
            comment.getCreatedAt()
        );
    }
}
