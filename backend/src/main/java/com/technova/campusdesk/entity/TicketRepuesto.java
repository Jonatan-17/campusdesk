package com.technova.campusdesk.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * [P1] Producto consumido en un ticket (tabla ticket_repuestos).
 * Es la "tabla intermedia" entre Ticket y Producto, pero como entidad propia y no como @ManyToMany:
 * un @ManyToMany solo guarda las dos FK, y aquí hace falta guardar también cantidad, técnico y fecha.
 * Regla práctica: si la relación tiene datos propios, se convierte en entidad con dos @ManyToOne.
 */
@Entity
@Table(name = "ticket_repuestos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketRepuesto {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ticket_id", nullable = false)
	private Ticket ticket;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "producto_id", nullable = false)
	private Producto producto;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "tecnico_id", nullable = false)
	private User tecnico;

	@Column(nullable = false)
	private Integer cantidad;

	@Column(name = "fecha_registro", nullable = false, updatable = false)
	private LocalDateTime fechaRegistro;

	@PrePersist
	void onCreate() {
		fechaRegistro = LocalDateTime.now();
	}

}
