package com.technova.campusdesk.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.technova.campusdesk.dto.response.UserResponse;
import com.technova.campusdesk.service.UserService;

import lombok.RequiredArgsConstructor;

/** Consulta de usuarios. Solo ADMIN: SecurityConfig exige ese rol para /api/users/** (otro rol: 403). */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	// GET /api/users — todos los usuarios, sin contraseñas
	@GetMapping
	public List<UserResponse> findAll() {
		return userService.findAll();
	}

	// GET /api/users/technicians — técnicos disponibles para asignar
	@GetMapping("/technicians")
	public List<UserResponse> findTechnicians() {
		return userService.findTechnicians();
	}

}
