package com.technova.campusdesk.mapper;

import com.technova.campusdesk.dto.response.UserResponse;
import com.technova.campusdesk.entity.User;

/** Convierte la entidad User en el DTO UserResponse (sin la contraseña). Clase de utilidades: no se instancia. */
public final class UserMapper {

	private UserMapper() {
	}

	public static UserResponse toResponse(User user) {
		return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole());
	}

}
