package com.technova.campusdesk.security;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Lee "Authorization: Bearer <token>" y, si el token es válido, deja al usuario autenticado
 * para esta petición. Si no hay token o no sirve, no corta la petición: la deja pasar sin
 * autenticar y SecurityConfig decide (401 en rutas privadas, nada en rutas públicas).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtService jwtService;
	private final UserDetailsServiceImpl userDetailsService;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		// 1. Se ejecuta una vez por petición, antes de llegar al controlador. Lee el header Authorization
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);

		if (header != null && header.startsWith(BEARER_PREFIX)
				&& SecurityContextHolder.getContext().getAuthentication() == null) {
			// 2. Solo si viene "Bearer <token>" y la petición aún no está autenticada
			authenticate(header.substring(BEARER_PREFIX.length()).trim(), request);
		}

		// 3. Sigue con el resto de filtros y el controlador (con o sin usuario autenticado)
		filterChain.doFilter(request, response);
	}

	private void authenticate(String token, HttpServletRequest request) {
		try {
			// Verifica firma y vencimiento; si el token es inválido o expiró lanza JwtException (CP-16)
			String email = jwtService.extractEmail(token);
			// Se vuelve a leer de la base: si el usuario fue borrado o cambió de rol, manda lo actual.
			UserDetails userDetails = userDetailsService.loadUserByUsername(email);

			/*
			 * [P4] Un JWT sigue siendo válido (firma y fecha) hasta que expira, aunque el ADMIN desactive la cuenta:
			 * el token no "sabe" nada de la base. Como aquí ya se relee el usuario en cada petición, basta con
			 * revisar isEnabled() para cortar el acceso de inmediato → la petición sigue sin usuario → 401.
			 * Otras opciones (si no se releyera el usuario): tokens de vida corta, lista negra de tokens, o
			 * guardar una "versión de token" en el usuario y en el JWT e incrementarla al desactivar.
			 */
			if (!userDetails.isEnabled()) {
				log.debug("Token de una cuenta desactivada en {}", request.getRequestURI());
				SecurityContextHolder.clearContext();
				return;
			}

			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
					userDetails, null, userDetails.getAuthorities());
			authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
			// Deja al usuario autenticado para esta petición: SecurityUtils.getCurrentUser() lo lee de aquí
			SecurityContextHolder.getContext().setAuthentication(authentication);
		} catch (JwtException | IllegalArgumentException | UsernameNotFoundException ex) {
			log.debug("Token rechazado en {}: {}", request.getRequestURI(), ex.getMessage());
			// Token inválido: la petición sigue sin usuario y SecurityConfig responde 401 en rutas privadas
			SecurityContextHolder.clearContext();
		}
	}

}
