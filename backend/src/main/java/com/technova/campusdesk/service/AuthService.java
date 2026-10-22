package com.technova.campusdesk.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.technova.campusdesk.dto.request.ChangePasswordRequest;
import com.technova.campusdesk.dto.request.LoginRequest;
import com.technova.campusdesk.dto.request.RegisterRequest;
import com.technova.campusdesk.dto.response.AuthResponse;
import com.technova.campusdesk.dto.response.UserResponse;
import com.technova.campusdesk.entity.User;
import com.technova.campusdesk.entity.enums.Role;
import com.technova.campusdesk.exception.BusinessRuleException;
import com.technova.campusdesk.mapper.UserMapper;
import com.technova.campusdesk.repository.UserRepository;
import com.technova.campusdesk.security.JwtService;
import com.technova.campusdesk.security.SecurityUtils;
import com.technova.campusdesk.security.UserDetailsServiceImpl;
import com.technova.campusdesk.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

/** Registro y login (RF-02). Las contraseñas se guardan con hash BCrypt, nunca en texto plano. */
@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;

	// El registro público siempre crea un USER. El ADMIN y los técnicos los crea el DataInitializer.
	@Transactional
	public UserResponse register(RegisterRequest request) {
		String email = UserDetailsServiceImpl.normalizeEmail(request.email());
		if (userRepository.existsByEmail(email)) {
			throw new BusinessRuleException("Ya existe una cuenta con el correo " + email);
		}

		User user = User.builder()
				.fullName(request.fullName().trim())
				.email(email)
				.password(passwordEncoder.encode(request.password()))
				.role(Role.USER)
				.build();

		return UserMapper.toResponse(userRepository.save(user));
	}

	/**
	 * Si el correo no existe o la contraseña no coincide, AuthenticationManager lanza
	 * BadCredentialsException y GlobalExceptionHandler responde 401 (sin decir cuál de los dos falló).
	 */
	public AuthResponse login(LoginRequest request) {
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(UserDetailsServiceImpl.normalizeEmail(request.email()),
						request.password()));

		UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
		return AuthResponse.bearer(jwtService.generateToken(principal), jwtService.getExpirationSeconds(),
				UserMapper.toResponse(principal.getUser()));
	}

	// Actualiza la contrasena del usuario autenticado en la base de datos
	@Transactional
	public void changePassword(ChangePasswordRequest request) {
		User principalUser = SecurityUtils.getCurrentUser();
		User user = userRepository.findById(principalUser.getId())
				.orElseThrow(() -> new BusinessRuleException("Usuario no encontrado"));

		// 1. Validar que la contrasena actual sea la correcta
		if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
			throw new BusinessRuleException("La contraseña actual es incorrecta");
		}

		// 2. Validar que la nueva contrasena no sea identica a la actual
		if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
			throw new BusinessRuleException("La nueva contraseña no puede ser igual a la contraseña actual");
		}

		// 3. Validar que la nueva contrasena coincida con su confirmacion
		if (!request.newPassword().equals(request.confirmPassword())) {
			throw new BusinessRuleException("La nueva contraseña y su confirmación no coinciden");
		}

		// 4. Validar que ninguna otra cuenta corporativa tenga esta misma contrasena
		for (User other : userRepository.findAll()) {
			if (!other.getId().equals(user.getId()) && passwordEncoder.matches(request.newPassword(), other.getPassword())) {
				throw new BusinessRuleException("Por seguridad, no puedes usar una contraseña repetida de otra cuenta corporativa");
			}
		}

		// 5. Cifrar con BCrypt y persistir el cambio en PostgreSQL
		user.setPassword(passwordEncoder.encode(request.newPassword()));
		userRepository.save(user);
	}

}
