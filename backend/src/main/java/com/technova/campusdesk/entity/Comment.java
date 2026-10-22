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
 * Comentario de un ticket (tabla comments). Pertenece a un ticket y a un autor (ManyToOne a cada uno).
 * Se muestran en orden cronológico (ver CommentRepository).
 */
@Entity
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// Ticket comentado → clave foránea ticket_id (si se borra el ticket, se borran sus comentarios)
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ticket_id", nullable = false)
	private Ticket ticket;

	// Quién escribió el comentario → clave foránea author_id
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "author_id", nullable = false)
	private User author;

	@Column(nullable = false, length = 1000)
	private String content;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	// Fecha del comentario, fijada al guardarlo
	@PrePersist
	void onCreate() {
		createdAt = LocalDateTime.now();
	}

}
