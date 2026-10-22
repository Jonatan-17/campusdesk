package com.technova.campusdesk.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Entidad JPA para la gestion de bodegas de almacenamiento
@Entity
@Table(name = "bodegas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bodega {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String nombre;

	@Column(nullable = false, length = 150)
	private String ubicacion;

}
