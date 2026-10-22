package com.technova.campusdesk.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Entidad JPA para productos e insumos asociados a una bodega (relacion ManyToOne)
@Entity
@Table(name = "productos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Producto {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String nombre;

	@Column(nullable = false)
	private Integer stock;

	// Relacion ManyToOne: muchos productos pertenecen a una bodega
	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "bodega_id", nullable = false)
	private Bodega bodega;

	/*
	 * [P1] Bloqueo optimista. Hibernate hace "UPDATE productos SET stock=?, version=version+1 WHERE id=? AND version=?".
	 * Si otro técnico descontó stock entre nuestra lectura y nuestro UPDATE, la versión ya no coincide, el UPDATE
	 * no afecta ninguna fila y Hibernate lanza OptimisticLockException (409 en GlobalExceptionHandler).
	 * Sin esto, los dos leerían stock=10, restarían 8 cada uno y guardarían 2: se pierde una de las dos restas.
	 */
	@Version
	private Long version;

}
