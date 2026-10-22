package com.technova.campusdesk.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// DTO para solicitud de cambio seguro de contrasena
public record ChangePasswordRequest(
		@NotBlank(message = "La contraseña actual es obligatoria")
		String currentPassword,

		@NotBlank(message = "La nueva contraseña es obligatoria")
		@Size(min = 8, max = 72, message = "La nueva contraseña debe tener entre 8 y 72 caracteres")
		@Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
				message = "La nueva contraseña debe tener al menos una mayúscula, una minúscula y un número")
		String newPassword,

		@NotBlank(message = "La confirmación de la contraseña es obligatoria")
		String confirmPassword) {
}
