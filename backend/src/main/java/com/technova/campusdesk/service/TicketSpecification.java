package com.technova.campusdesk.service;

import com.technova.campusdesk.dto.request.TicketFilter;
import com.technova.campusdesk.entity.Tag;
import com.technova.campusdesk.entity.Ticket;
import com.technova.campusdesk.entity.User;
import com.technova.campusdesk.entity.enums.Role;
import com.technova.campusdesk.entity.enums.TicketStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Arma la consulta del listado de tickets con JPA Criteria (Specification): agrega una condición por cada
 * filtro que llega, más la restricción del rol. Todo se resuelve en una sola consulta SQL en PostgreSQL.
 * Las Specification se combinan con .and(): los ejercicios [T1] y [P7] reutilizan la visibilidad por rol
 * en vez de repetirla.
 */
public class TicketSpecification {

    public static Specification<Ticket> filterByRoleAndCriteria(User currentUser, TicketFilter filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Regla de negocio: Filtrar por rol
            if (currentUser.getRole() == Role.USER) {
                // [P8] Sus tickets O los que observa. Con una subconsulta (EXISTS) y no con un JOIN a observadores:
                // el JOIN repetiría el ticket por cada fila y obligaría a usar DISTINCT.
                Subquery<Long> observa = query.subquery(Long.class);
                var ticketObservado = observa.from(Ticket.class);
                Join<Ticket, User> observador = ticketObservado.join("observadores");
                observa.select(ticketObservado.get("id"))
                        .where(criteriaBuilder.equal(ticketObservado.get("id"), root.get("id")),
                               criteriaBuilder.equal(observador.get("id"), currentUser.getId()));

                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.equal(root.get("requester").get("id"), currentUser.getId()),
                        criteriaBuilder.exists(observa)));
            } else if (currentUser.getRole() == Role.TECHNICIAN) {
                predicates.add(criteriaBuilder.equal(root.get("technician").get("id"), currentUser.getId()));
            }
            // Si es ADMIN, no agregamos restricción de usuario (ve todos)

            // 2. Filtros dinámicos (si vienen en el request)
            if (filter != null) {
                if (filter.status() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("status"), filter.status()));
                }
                if (filter.priority() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("priority"), filter.priority()));
                }
                if (filter.category() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("category"), filter.category()));
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    // [T1] Tickets que tienen la etiqueta indicada (nombre ya normalizado en mayúsculas).
    // También con EXISTS: un JOIN directo a "tags" chocaría con el fetch de tags del @EntityGraph.
    public static Specification<Ticket> conTag(String nombre) {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            var ticket = sub.from(Ticket.class);
            Join<Ticket, Tag> tag = ticket.join("tags");
            sub.select(ticket.get("id"))
                    .where(cb.equal(ticket.get("id"), root.get("id")), cb.equal(tag.get("nombre"), nombre));
            return cb.exists(sub);
        };
    }

}
