package com.technova.campusdesk.security;

import java.time.LocalDateTime;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.technova.campusdesk.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * [P5] Bloqueo de la cuenta tras 3 contraseñas incorrectas seguidas, durante 15 minutos.
 *
 * Cómo encaja con Spring Security, sin tocar AuthService:
 * 1. En cada login, el AuthenticationManager publica un evento: AuthenticationSuccessEvent si va bien,
 *    AuthenticationFailureBadCredentialsEvent si la contraseña (o el correo) no coincide.
 *    Spring Boot registra el DefaultAuthenticationEventPublisher que los publica.
 * 2. Aquí se escuchan esos eventos con @EventListener para contar fallos o reiniciar el contador.
 * 3. El bloqueo en sí lo aplica Spring: antes de comparar la contraseña llama a UserPrincipal.isAccountNonLocked();
 *    si es false lanza LockedException → 423 (GlobalExceptionHandler). Mientras está bloqueada, ni la contraseña
 *    correcta sirve, y esos intentos no cuentan como fallos (el evento es otro: AuthenticationFailureLockedEvent).
 *
 * TRAMPA de transacciones: el fallo del login termina en excepción. Si el conteo se hiciera dentro de una
 * transacción que también abarca el login (por ejemplo, poniendo @Transactional en AuthService.login), la
 * excepción haría ROLLBACK y el contador nunca subiría. Aquí funciona porque login() no es transaccional:
 * cada método de este listener abre y confirma su propia transacción.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

	static final int MAX_INTENTOS = 3;
	static final int MINUTOS_BLOQUEO = 15;

	private final UserRepository userRepository;

	@EventListener
	@Transactional
	public void onLoginFallido(AuthenticationFailureBadCredentialsEvent event) {
		// En el login el "principal" que se intentó es el correo, tal como lo envió AuthService (ya normalizado)
		String email = UserDetailsServiceImpl.normalizeEmail(event.getAuthentication().getName());

		// Si el correo no existe no hay nada que contar. La respuesta al cliente es la misma (401 genérico),
		// así que no se revela si la cuenta existe.
		userRepository.findByEmail(email).ifPresent(user -> {
			int intentos = user.getFailedLoginAttempts() + 1;
			if (intentos >= MAX_INTENTOS) {
				user.setLockedUntil(LocalDateTime.now().plusMinutes(MINUTOS_BLOQUEO));
				user.setFailedLoginAttempts(0); // al terminar el bloqueo, vuelve a tener 3 intentos
				log.warn("Cuenta {} bloqueada {} minutos por intentos fallidos", email, MINUTOS_BLOQUEO);
			} else {
				user.setFailedLoginAttempts(intentos);
			}
		});
	}

	@EventListener
	@Transactional
	public void onLoginCorrecto(AuthenticationSuccessEvent event) {
		if (!(event.getAuthentication().getPrincipal() instanceof UserPrincipal principal)) {
			return;
		}
		// Se relee de la base: la entidad del principal se cargó en otra sesión de Hibernate
		userRepository.findById(principal.getUser().getId()).ifPresent(user -> {
			if (user.getFailedLoginAttempts() != 0 || user.getLockedUntil() != null) {
				user.setFailedLoginAttempts(0);
				user.setLockedUntil(null);
			}
		});
	}

}
