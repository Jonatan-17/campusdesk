package com.technova.campusdesk.entity;

import java.time.LocalDateTime;

import com.technova.campusdesk.entity.enums.Role;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Usuario del sistema (tabla users; "user" es palabra reservada en PostgreSQL). Es un Java Bean:
 * constructor vacío, campos privados y getters/setters (generados por Lombok).
 * @Builder permite crearlo con User.builder()...build().
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

	// Clave primaria; la genera PostgreSQL (IDENTITY)
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "full_name", nullable = false, length = 100)
	private String fullName;

	// Único en la base (UNIQUE): no puede haber dos cuentas con el mismo correo
	@Column(nullable = false, unique = true, length = 150)
	private String email;

	// Siempre el hash BCrypt, nunca la contraseña en texto plano.
	@Column(nullable = false)
	private String password;

	// Se guarda como texto ("ADMIN", "TECHNICIAN", "USER"), no como número
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Role role;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	// [P4] false = cuenta desactivada por un ADMIN. UserPrincipal.isEnabled() lo devuelve a Spring Security.
	@Column(nullable = false)
	@Builder.Default
	private boolean active = true;

	// [P5] Contraseñas incorrectas seguidas. Se reinicia con un login correcto o al bloquear la cuenta.
	@Column(name = "failed_login_attempts", nullable = false)
	@Builder.Default
	private int failedLoginAttempts = 0;

	// [P5] Mientras sea una fecha futura, la cuenta está bloqueada (UserPrincipal.isAccountNonLocked()).
	@Column(name = "locked_until")
	private LocalDateTime lockedUntil;

	// Se ejecuta justo antes del INSERT: fija la fecha de creación
	@PrePersist
	void onCreate() {
		createdAt = LocalDateTime.now();
	}

}
