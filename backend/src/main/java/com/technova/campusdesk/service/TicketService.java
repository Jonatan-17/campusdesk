package com.technova.campusdesk.service;

import com.technova.campusdesk.dto.request.TicketFilter;
import com.technova.campusdesk.dto.request.TicketRequest;
import com.technova.campusdesk.dto.response.StatusHistoryResponse;
import com.technova.campusdesk.dto.response.TicketResponse;
import com.technova.campusdesk.entity.Tag;
import com.technova.campusdesk.entity.Ticket;
import com.technova.campusdesk.entity.User;
import com.technova.campusdesk.entity.enums.Role;
import com.technova.campusdesk.entity.enums.TicketStatus;
import com.technova.campusdesk.exception.BusinessRuleException;
import com.technova.campusdesk.exception.ForbiddenException;
import com.technova.campusdesk.exception.ResourceNotFoundException;
import com.technova.campusdesk.repository.TicketRepository;
import com.technova.campusdesk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Reglas del negocio de los tickets (RF-03, RF-04). Aquí se decide quién puede ver, crear, editar,
 * asignar y cambiar el estado de un ticket. Los métodos que escriben son @Transactional: si algo falla
 * a mitad de camino (por ejemplo, al guardar el historial), se deshace todo.
 * Los errores se lanzan como excepciones y GlobalExceptionHandler los convierte en 403, 404 o 409.
 */
@Service
@RequiredArgsConstructor
public class TicketService {

    // [P3] Máximo de tickets activos (ASSIGNED + IN_PROGRESS) que puede tener un técnico a la vez
    public static final int MAX_TICKETS_ACTIVOS_POR_TECNICO = 5;
    // [P3][P4] Estados en los que un ticket ocupa al técnico
    public static final List<TicketStatus> ESTADOS_ACTIVOS = List.of(TicketStatus.ASSIGNED, TicketStatus.IN_PROGRESS);

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final StatusHistoryService statusHistoryService;

    // Crea un ticket en OPEN a nombre del usuario autenticado y guarda el primer registro del historial (null → OPEN)
    @Transactional
    public TicketResponse createTicket(TicketRequest request, User currentUser) {
        // Solo el rol USER crea solicitudes: ADMIN y TECHNICIAN reciben 403
        if (currentUser.getRole() != Role.USER) {
            throw new ForbiddenException("Solo los usuarios (USER) pueden crear tickets.");
        }

        Ticket ticket = new Ticket();
        ticket.setTitle(request.title());
        ticket.setDescription(request.description());
        ticket.setCategory(request.category());
        ticket.setPriority(request.priority());
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setRequester(currentUser);

        // INSERT del ticket; en la misma transacción se guarda el historial
        Ticket savedTicket = ticketRepository.save(ticket);
        statusHistoryService.record(savedTicket, null, TicketStatus.OPEN, currentUser);

        return toResponse(savedTicket);
    }

    // Listado según el rol (ADMIN todos, TECHNICIAN asignados, USER propios) más los filtros opcionales
    public List<TicketResponse> listTickets(TicketFilter filter, User currentUser) {
        Specification<Ticket> spec = TicketSpecification.filterByRoleAndCriteria(currentUser, filter);
        // Más recientes primero, para que el listado tenga siempre el mismo orden
        List<Ticket> tickets = ticketRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        return tickets.stream().map(this::toResponse).toList();
    }

    /*
     * Busca el ticket (404 si no existe) y comprueba que el usuario sea el dueño, el técnico asignado,
     * un observador [P8] o ADMIN (si no, 403). Visible en el paquete service: los demás services usan las mismas reglas.
     *
     * OJO [P8]: "puede VER el ticket" no es lo mismo que "puede MODIFICARLO". Desde que existen observadores,
     * un USER que pasa este filtro no es necesariamente el dueño: cada acción de escritura debe comprobar
     * además su propio permiso (ver updateTicketStatus y los services de los ejercicios).
     */
    Ticket getAccessibleTicket(Long id, User currentUser) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado con id: " + id));

        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        boolean isRequester = isRequester(ticket, currentUser);
        boolean isAssignedTechnician = isAssignedTechnician(ticket, currentUser);

        // La consulta de observador va al final: solo se ejecuta si las comprobaciones baratas fallaron
        if (!isAdmin && !isRequester && !isAssignedTechnician
                && !ticketRepository.existsObservador(ticket.getId(), currentUser.getId())) {
            throw new ForbiddenException("No tienes permiso para acceder a este ticket.");
        }

        return ticket;
    }

    // Utilidades de permisos que reutilizan los services de los ejercicios
    static boolean isRequester(Ticket ticket, User user) {
        return ticket.getRequester().getId().equals(user.getId());
    }

    static boolean isAssignedTechnician(Ticket ticket, User user) {
        return ticket.getTechnician() != null && ticket.getTechnician().getId().equals(user.getId());
    }

    // Detalle de un ticket, si el usuario tiene acceso
    public TicketResponse getTicketById(Long id, User currentUser) {
        Ticket ticket = getAccessibleTicket(id, currentUser);
        return toResponse(ticket);
    }

    // Historial de estados; mismas reglas de acceso que el detalle
    public List<StatusHistoryResponse> getTicketHistory(Long id, User currentUser) {
        Ticket ticket = getAccessibleTicket(id, currentUser);
        return statusHistoryService.findByTicket(ticket.getId());
    }

    // Edición (PUT): solo el solicitante, y solo mientras el ticket esté OPEN y sin técnico (RF-01)
    @Transactional
    public TicketResponse updateTicket(Long id, TicketRequest request, User currentUser) {
        Ticket ticket = getAccessibleTicket(id, currentUser);

        if (!isRequester(ticket, currentUser)) {
            throw new ForbiddenException("Solo el solicitante puede editar este ticket.");
        }

        if (ticket.getStatus() != TicketStatus.OPEN) {
            throw new BusinessRuleException("Solo se pueden editar tickets con estado OPEN.");
        }
        if (ticket.getTechnician() != null) {
            throw new BusinessRuleException("No se puede editar un ticket que ya tiene un técnico asignado.");
        }

        boolean cambiaPrioridad = ticket.getPriority() != request.priority();

        ticket.setTitle(request.title());
        ticket.setDescription(request.description());
        ticket.setCategory(request.category());
        ticket.setPriority(request.priority());

        // [P7] Si cambia la prioridad, cambia el plazo. Se cuenta desde la creación, no desde la edición:
        // si no, bajar y volver a subir la prioridad "reiniciaría" el reloj del SLA.
        if (cambiaPrioridad) {
            ticket.recalcularFechaLimite();
        }

        Ticket updatedTicket = ticketRepository.save(ticket);
        return toResponse(updatedTicket);
    }

    // Asignación o reasignación de técnico (solo ADMIN). Deja el ticket en ASSIGNED y lo registra en el historial
    @Transactional
    public TicketResponse assignTicket(Long id, Long technicianId, User currentUser) {
        if (currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Solo un ADMIN puede asignar tickets.");
        }

        Ticket ticket = getAccessibleTicket(id, currentUser);

        User technician = userRepository.findById(technicianId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + technicianId));

        if (technician.getRole() != Role.TECHNICIAN) {
            throw new BusinessRuleException("El usuario destino debe tener rol TECHNICIAN.");
        }
        // [P4] Un técnico desactivado no puede recibir tickets
        if (!technician.isActive()) {
            throw new BusinessRuleException("El técnico " + technician.getFullName() + " está desactivado.");
        }

        TicketStatus oldStatus = ticket.getStatus();
        TicketTransitionPolicy.validateTransition(oldStatus, TicketStatus.ASSIGNED, currentUser.getRole());

        // Reasignar al mismo técnico no cambia nada: se rechaza para no dejar un registro vacío en el historial
        if (ticket.getTechnician() != null && ticket.getTechnician().getId().equals(technician.getId())) {
            throw new BusinessRuleException("El ticket ya está asignado a " + technician.getFullName() + ".");
        }

        // [P3] Límite de carga. Se cuenta DESPUÉS de descartar "mismo técnico": este ticket todavía no es suyo.
        long activos = ticketRepository.countByTechnicianIdAndStatusIn(technician.getId(), ESTADOS_ACTIVOS);
        if (activos >= MAX_TICKETS_ACTIVOS_POR_TECNICO) {
            throw new BusinessRuleException(technician.getFullName() + " ya tiene " + activos
                    + " tickets activos (máximo " + MAX_TICKETS_ACTIVOS_POR_TECNICO + ").");
        }

        ticket.setTechnician(technician);
        ticket.setStatus(TicketStatus.ASSIGNED);

        Ticket updatedTicket = ticketRepository.save(ticket);
        statusHistoryService.record(updatedTicket, oldStatus, TicketStatus.ASSIGNED, currentUser);

        return toResponse(updatedTicket);
    }

    // Cambio de estado (técnico asignado: IN_PROGRESS y RESOLVED; solicitante: CLOSED). La validación está en TicketTransitionPolicy
    @Transactional
    public TicketResponse updateTicketStatus(Long id, TicketStatus newStatus, User currentUser) {
        Ticket ticket = getAccessibleTicket(id, currentUser);
        TicketStatus oldStatus = ticket.getStatus();

        if (newStatus == TicketStatus.ASSIGNED) {
            throw new BusinessRuleException("Para asignar un ticket usa PATCH /api/tickets/{id}/assign.");
        }
        if (newStatus == oldStatus) {
            throw new BusinessRuleException("El ticket ya está en estado " + oldStatus + ".");
        }

        // [P8] TicketTransitionPolicy solo mira el ROL (USER puede cerrar). Antes bastaba porque el único USER que
        // pasaba getAccessibleTicket era el dueño; con observadores ya no, así que se exige ser el solicitante.
        if (currentUser.getRole() == Role.USER && !isRequester(ticket, currentUser)) {
            throw new ForbiddenException("Solo el solicitante puede cambiar el estado de este ticket.");
        }

        TicketTransitionPolicy.validateTransition(oldStatus, newStatus, currentUser.getRole());

        ticket.setStatus(newStatus);
        Ticket updatedTicket = ticketRepository.save(ticket);

        statusHistoryService.record(updatedTicket, oldStatus, newStatus, currentUser);

        return toResponse(updatedTicket);
    }

    // Convierte la entidad en el DTO de respuesta (nunca se devuelve la entidad).
    // Visible en el paquete: los services de los ejercicios devuelven el ticket con el mismo formato.
    TicketResponse toResponse(Ticket ticket) {
        return new TicketResponse(
            ticket.getId(),
            ticket.getTitle(),
            ticket.getDescription(),
            ticket.getCategory(),
            ticket.getPriority(),
            ticket.getStatus(),
            new TicketResponse.UserSummary(ticket.getRequester().getId(), ticket.getRequester().getFullName()),
            ticket.getTechnician() != null ? new TicketResponse.UserSummary(ticket.getTechnician().getId(), ticket.getTechnician().getFullName()) : null,
            ticket.getCreatedAt(),
            ticket.getUpdatedAt(),
            // [T1] Las etiquetas vienen cargadas por el @EntityGraph del repositorio (o dentro de la transacción)
            ticket.getTags().stream().map(Tag::getNombre).sorted().toList(),
            ticket.getReaperturasCount(),
            ticket.getFechaLimite(),
            // [P6] getId() de un proxy LAZY no dispara consulta: Hibernate ya conoce el id por la FK
            ticket.getDuplicadoDe() != null ? ticket.getDuplicadoDe().getId() : null
        );
    }
}
