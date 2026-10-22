package com.technova.campusdesk.security;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.technova.campusdesk.entity.User;

/**
 * El usuario autenticado dentro de Spring Security. Guarda la entidad User para que
 * SecurityUtils.getCurrentUser() la devuelva sin otra consulta.
 * La autoridad es "ROLE_" + rol, así funcionan hasRole("ADMIN") y @PreAuthorize("hasRole('ADMIN')").
 */
public class UserPrincipal implements UserDetails {

	private final User user;

	public UserPrincipal(User user) {
		this.user = user;
	}

	public User getUser() {
		return user;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
	}

	@Override
	public String getPassword() {
		return user.getPassword();
	}

	@Override
	public String getUsername() {
		return user.getEmail();
	}

	/*
	 * [P4] Cuenta activa. En el login, el AuthenticationManager lo revisa ANTES de comparar la contraseña y,
	 * si es false, lanza DisabledException. En el filtro JWT hay que revisarlo a mano (ver JwtAuthenticationFilter).
	 */
	@Override
	public boolean isEnabled() {
		return user.isActive();
	}

	// [P5] Bloqueada mientras locked_until sea una fecha futura. Si es false, el login lanza LockedException.
	@Override
	public boolean isAccountNonLocked() {
		return user.getLockedUntil() == null || !user.getLockedUntil().isAfter(LocalDateTime.now());
	}

}
