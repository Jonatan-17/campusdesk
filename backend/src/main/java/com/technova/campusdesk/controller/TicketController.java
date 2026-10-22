package com.technova.campusdesk.controller;

import com.technova.campusdesk.dto.request.AssignRequest;
import com.technova.campusdesk.dto.request.CommentRequest;
import com.technova.campusdesk.dto.request.StatusChangeRequest;
import com.technova.campusdesk.dto.request.TicketFilter;
import com.technova.campusdesk.dto.request.TicketRequest;
import com.technova.campusdesk.dto.response.CommentResponse;
import com.technova.campusdesk.dto.response.StatusHistoryResponse;
import com.technova.campusdesk.dto.response.TicketResponse;
import com.technova.campusdesk.entity.User;
import com.technova.campusdesk.entity.enums.Category;
import com.technova.campusdesk.entity.enums.Priority;
import com.technova.campusdesk.entity.enums.TicketStatus;
import com.technova.campusdesk.security.SecurityUtils;
import com.technova.campusdesk.service.CommentService;
import com.technova.campusdesk.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints REST de tickets (/api/tickets). El controlador no tiene lógica de negocio: obtiene el usuario
 * autenticado, delega en TicketService o CommentService y devuelve DTOs. Las reglas y permisos se
 * validan en los servicios; los errores los convierte en JSON GlobalExceptionHandler.
 */
@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;
    private final CommentService commentService;

    // POST /api/tickets — crea un ticket (solo USER) en OPEN → 201
    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(@Valid @RequestBody TicketRequest request) {
        // Usuario de la petición actual, que dejó el filtro JWT en el SecurityContext
        User currentUser = SecurityUtils.getCurrentUser(); 
        TicketResponse response = ticketService.createTicket(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET /api/tickets?status=&priority=&category= — lista según el rol, con filtros opcionales → 200
    @GetMapping
    public ResponseEntity<List<TicketResponse>> listTickets(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Category category
    ) {
        User currentUser = SecurityUtils.getCurrentUser();
        TicketFilter filter = new TicketFilter(status, priority, category);
        List<TicketResponse> tickets = ticketService.listTickets(filter, currentUser);
        return ResponseEntity.ok(tickets);
    }

    // GET /api/tickets/{id} — detalle (dueño, técnico asignado o ADMIN; si no, 403) → 200
    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getTicketById(@PathVariable Long id) {
        User currentUser = SecurityUtils.getCurrentUser();
        TicketResponse ticket = ticketService.getTicketById(id, currentUser);
        return ResponseEntity.ok(ticket);
    }

    // PUT /api/tickets/{id} — edita (solo el dueño, en OPEN y sin técnico) → 200
    @PutMapping("/{id}")
    public ResponseEntity<TicketResponse> updateTicket(
            @PathVariable Long id,
            @Valid @RequestBody TicketRequest request
    ) {
        User currentUser = SecurityUtils.getCurrentUser();
        TicketResponse updatedTicket = ticketService.updateTicket(id, request, currentUser);
        return ResponseEntity.ok(updatedTicket);
    }

    // PATCH /api/tickets/{id}/assign — asigna o reasigna un técnico (solo ADMIN) → 200, estado ASSIGNED
    @PatchMapping("/{id}/assign")
    public ResponseEntity<TicketResponse> assignTicket(
            @PathVariable Long id,
            @Valid @RequestBody AssignRequest request
    ) {
        User currentUser = SecurityUtils.getCurrentUser();
        TicketResponse ticket = ticketService.assignTicket(id, request.technicianId(), currentUser);
        return ResponseEntity.ok(ticket);
    }

    // PATCH /api/tickets/{id}/status — cambia el estado (técnico asignado: IN_PROGRESS, RESOLVED · dueño: CLOSED) → 200
    @PatchMapping("/{id}/status")
    public ResponseEntity<TicketResponse> updateTicketStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusChangeRequest request
    ) {
        User currentUser = SecurityUtils.getCurrentUser();
        TicketResponse ticket = ticketService.updateTicketStatus(id, request.status(), currentUser);
        return ResponseEntity.ok(ticket);
    }

    // GET /api/tickets/{id}/history — historial de estados en orden cronológico → 200
    @GetMapping("/{id}/history")
    public ResponseEntity<List<StatusHistoryResponse>> getTicketHistory(@PathVariable Long id) {
        User currentUser = SecurityUtils.getCurrentUser();
        List<StatusHistoryResponse> history = ticketService.getTicketHistory(id, currentUser);
        return ResponseEntity.ok(history);
    }

    // GET /api/tickets/{id}/comments — comentarios en orden cronológico (dueño, técnico asignado o ADMIN) → 200
    @GetMapping("/{id}/comments")
    public ResponseEntity<List<CommentResponse>> listComments(@PathVariable Long id) {
        User currentUser = SecurityUtils.getCurrentUser();
        List<CommentResponse> comments = commentService.listComments(id, currentUser);
        return ResponseEntity.ok(comments);
    }

    // POST /api/tickets/{id}/comments — agrega un comentario (no en tickets CLOSED: 409) → 201
    @PostMapping("/{id}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable Long id,
            @Valid @RequestBody CommentRequest request
    ) {
        User currentUser = SecurityUtils.getCurrentUser();
        CommentResponse comment = commentService.addComment(id, request.content(), currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(comment);
    }
}