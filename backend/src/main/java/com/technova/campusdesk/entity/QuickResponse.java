package com.technova.campusdesk.entity;

import com.technova.campusdesk.entity.enums.Category;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * [T5] Plantilla de respuesta de un técnico (tabla quick_responses).
 * "Título único POR TÉCNICO": no basta unique = true en la columna (sería único en toda la tabla y dos técnicos
 * no podrían tener "Reinicio de módem"). Se declara un UNIQUE compuesto (tecnico_id, titulo).
 */
@Entity
@Table(name = "quick_responses",
		uniqueConstraints = @UniqueConstraint(name = "uk_quick_responses_tecnico_titulo",
				columnNames = { "tecnico_id", "titulo" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuickResponse {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// Técnico dueño: todas las consultas filtran por él, así nadie ve ni toca plantillas ajenas
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "tecnico_id", nullable = false)
	private User tecnico;

	@Column(nullable = false, length = 100)
	private String titulo;

	@Column(nullable = false, length = 1000)
	private String contenido;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Category categoria;

}
