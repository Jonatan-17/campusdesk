package com.technova.campusdesk.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.technova.campusdesk.dto.request.ChangePasswordRequest;
import com.technova.campusdesk.dto.request.LoginRequest;
import com.technova.campusdesk.dto.request.RegisterRequest;
import com.technova.campusdesk.dto.response.AuthResponse;
import com.technova.campusdesk.dto.response.UserResponse;
import com.technova.campusdesk.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Registro e inicio de sesión. Son las únicas rutas de la API que no piden token (ver SecurityConfig). */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	// POST /api/auth/register — crea una cuenta USER → 201 (correo duplicado: 409; datos inválidos: 400)
	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public UserResponse register(@Valid @RequestBody RegisterRequest request) {
		return authService.register(request);
	}

	// POST /api/auth/login — valida correo y contraseña y devuelve el JWT → 200 (credenciales incorrectas: 401)
	@PostMapping("/login")
	public AuthResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}

	// POST /api/auth/change-password - Actualizar contrasena de usuario autenticado
	@PostMapping("/change-password")
	public ResponseEntity<Map<String, String>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
		authService.changePassword(request);
		return ResponseEntity.ok(Map.of("message", "Contraseña actualizada exitosamente"));
	}

}
