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

/** [T4] Un bloque de trabajo de un técnico sobre un ticket (tabla ticket_worklogs). Dos @ManyToOne: ticket y técnico. */
@Entity
@Table(name = "ticket_worklogs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketWorklog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ticket_id", nullable = false)
	private Ticket ticket;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "tecnico_id", nullable = false)
	private User tecnico;

	// Entre 5 y 480: lo valida el DTO (@Min/@Max → 400) y lo repite un CHECK en schema.sql
	@Column(name = "minutos_dedicados", nullable = false)
	private Integer minutosDedicados;

	@Column(name = "descripcion_actividad", nullable = false, length = 500)
	private String descripcionActividad;

	@Column(name = "fecha_registro", nullable = false, updatable = false)
	private LocalDateTime fechaRegistro;

	@PrePersist
	void onCreate() {
		fechaRegistro = LocalDateTime.now();
	}

}
