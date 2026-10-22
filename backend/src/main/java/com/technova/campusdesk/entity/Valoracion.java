package com.technova.campusdesk.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * [P2] Valoración del solicitante sobre un ticket cerrado (tabla valoraciones).
 * @OneToOne: un ticket tiene como máximo una valoración. La FK va en valoraciones (no en tickets) porque la
 * valoración es opcional y nace después: así tickets no necesita una columna casi siempre vacía.
 * En la base, lo que garantiza el "uno a uno" es el UNIQUE de ticket_id; @OneToOne por sí solo no crea nada.
 */
@Entity
@Table(name = "valoraciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Valoracion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ticket_id", nullable = false, unique = true)
	private Ticket ticket;

	@Column(nullable = false)
	private Integer puntuacion;

	@Column(length = 500)
	private String comentario;

	@Column(nullable = false, updatable = false)
	private LocalDateTime fecha;

	@PrePersist
	void onCreate() {
		fecha = LocalDateTime.now();
	}

}
