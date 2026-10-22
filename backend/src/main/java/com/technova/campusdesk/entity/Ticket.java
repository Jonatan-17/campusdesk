package com.technova.campusdesk.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.technova.campusdesk.entity.enums.Category;
import com.technova.campusdesk.entity.enums.Priority;
import com.technova.campusdesk.entity.enums.TicketStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Solicitud de soporte (tabla tickets). Tiene dos relaciones con User: quien la pide (requester)
 * y el técnico asignado (technician, puede ser null).
 * Las relaciones son LAZY: el usuario relacionado se carga solo cuando se necesita. Por eso
 * TicketRepository usa @EntityGraph para traerlos en la misma consulta cuando se van a mostrar.
 */
@Entity
@Table(name = "tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 150)
	private String title;

	@Column(nullable = false, length = 2000)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Category category;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Priority priority;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TicketStatus status;

	// Muchos tickets pertenecen a un mismo solicitante → clave foránea requester_id
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "requester_id", nullable = false)
	private User requester;

	// null mientras el ticket no está asignado.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "technician_id")
	private User technician;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	/*
	 * [T1] Etiquetas. Ticket es el lado "dueño" de la relación: el que tiene @JoinTable y el que se guarda.
	 * - Set y no List: con List (un "bag"), Hibernate borra y reinserta TODAS las filas de ticket_tags en cada cambio,
	 *   y si haces fetch de dos List a la vez lanza MultipleBagFetchException.
	 * - @Builder.Default: sin él, Ticket.builder()...build() deja el Set en null (Lombok ignora el inicializador).
	 * - Sin cascade: las etiquetas se guardan aparte (TagService) porque se comparten entre tickets.
	 * - Es unidireccional: Tag no tiene "List<Ticket>". Para buscar tickets por etiqueta basta un JOIN desde Ticket.
	 */
	@ManyToMany
	@JoinTable(name = "ticket_tags",
			joinColumns = @JoinColumn(name = "ticket_id"),
			inverseJoinColumns = @JoinColumn(name = "tag_id"))
	@Builder.Default
	private Set<Tag> tags = new HashSet<>();

	// [P8] Usuarios que pueden ver y comentar el ticket sin ser dueños. Mismas reglas que tags.
	// No se lee en TicketService.getAccessibleTicket (sería LAZY fuera de transacción): allí se usa una consulta.
	@ManyToMany
	@JoinTable(name = "ticket_observadores",
			joinColumns = @JoinColumn(name = "ticket_id"),
			inverseJoinColumns = @JoinColumn(name = "user_id"))
	@Builder.Default
	private Set<User> observadores = new HashSet<>();

	// [T2] Veces que el solicitante reabrió el ticket. @Builder.Default para que no quede en null al usar el builder.
	@Column(name = "reaperturas_count", nullable = false)
	@Builder.Default
	private Integer reaperturasCount = 0;

	// [P7] Fecha límite de atención según la prioridad (SLA). Se calcula, nunca la manda el cliente.
	@Column(name = "fecha_limite")
	private LocalDateTime fechaLimite;

	// [P6] Relación de la tabla consigo misma: este ticket es duplicado de otro (null si no lo es).
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "duplicado_de_id")
	private Ticket duplicadoDe;

	// Antes del INSERT: fechas iniciales y estado OPEN si no se indicó otro (RF-04 regla 1)
	@PrePersist
	void onCreate() {
		createdAt = LocalDateTime.now();
		updatedAt = createdAt;
		if (status == null) {
			status = TicketStatus.OPEN;
		}
		recalcularFechaLimite();
	}

	// [P7] fecha límite = creación + horas de la prioridad. TicketService la vuelve a llamar si cambia la prioridad
	// (en @PreUpdate no sabríamos si la prioridad cambió, porque ya no tenemos el valor anterior).
	public void recalcularFechaLimite() {
		if (createdAt != null && priority != null) {
			fechaLimite = createdAt.plusHours(priority.getHorasSla());
		}
	}

	// Antes de cada UPDATE: actualiza la fecha de última modificación
	@PreUpdate
	void onUpdate() {
		updatedAt = LocalDateTime.now();
	}

}
