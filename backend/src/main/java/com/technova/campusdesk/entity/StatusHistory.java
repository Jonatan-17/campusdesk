package com.technova.campusdesk.entity;

import java.time.LocalDateTime;

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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Registro del historial de estados (tabla status_history, RF-06). Se crea uno por cada cambio de estado,
 * en la misma transacción que el cambio, y nunca se modifica después.
 */
@Entity
@Table(name = "status_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatusHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ticket_id", nullable = false)
	private Ticket ticket;

	// null en el registro de creación del ticket.
	@Enumerated(EnumType.STRING)
	@Column(name = "previous_status", length = 20)
	private TicketStatus previousStatus;

	// Estado al que pasó el ticket
	@Enumerated(EnumType.STRING)
	@Column(name = "new_status", nullable = false, length = 20)
	private TicketStatus newStatus;

	// Usuario responsable del cambio
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "changed_by_id", nullable = false)
	private User changedBy;

	@Column(name = "changed_at", nullable = false, updatable = false)
	private LocalDateTime changedAt;

	// [T2][T3][P4][P6] Motivo del cambio. null en los pasos normales del ciclo de vida.
	@Column(length = 500)
	private String reason;

	// Fecha y hora del cambio, fijada al guardarlo
	@PrePersist
	void onCreate() {
		changedAt = LocalDateTime.now();
	}

}
