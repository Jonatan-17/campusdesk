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

/**
 * [T1] Etiqueta transversal de un ticket (URGENTE, VPN, IMPRESORA...). El nombre se guarda siempre en mayúsculas
 * (lo normaliza TagService) y es UNIQUE en la base. La relación con Ticket se declara en Ticket.tags.
 * Sin @Data a propósito: un equals/hashCode generado sobre todos los campos rompería los Set de JPA.
 */
@Entity
@Table(name = "tags")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tag {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 50)
	private String nombre;

}
