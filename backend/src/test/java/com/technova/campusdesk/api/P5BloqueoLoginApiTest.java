package com.technova.campusdesk.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;

/** [P5] Bloqueo de 15 minutos tras 3 contraseñas incorrectas seguidas. */
class P5BloqueoLoginApiTest extends EstudioApiTestSupport {

	private static final String CLAVE = "Clave12345";

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void anaConContrasenaReal() {
		ana.setPassword(passwordEncoder.encode(CLAVE));
		ana = userRepository.save(ana);
	}

	@Test
	void tresFallosBloqueanInclusoConLaContrasenaCorrecta() throws Exception {
		login("ana@technova.com", "mala1").andExpect(status().isUnauthorized());
		login("ana@technova.com", "mala2").andExpect(status().isUnauthorized());
		login("ana@technova.com", "mala3").andExpect(status().isUnauthorized());

		login("ana@technova.com", CLAVE).andExpect(status().isLocked());
	}

	@Test
	void unLoginCorrectoReiniciaElContador() throws Exception {
		login("ana@technova.com", "mala").andExpect(status().isUnauthorized());
		login("ana@technova.com", "mala").andExpect(status().isUnauthorized());
		login("ana@technova.com", CLAVE).andExpect(status().isOk());
		// Cuenta desde cero otra vez: dos fallos más no bloquean
		login("ana@technova.com", "mala").andExpect(status().isUnauthorized());
		login("ana@technova.com", "mala").andExpect(status().isUnauthorized());
		login("ana@technova.com", CLAVE).andExpect(status().isOk());
	}

	@Test
	void cuandoPasaElTiempoSePuedeEntrar() throws Exception {
		ana.setLockedUntil(LocalDateTime.now().minusMinutes(1));
		userRepository.save(ana);
		login("ana@technova.com", CLAVE).andExpect(status().isOk());
	}

	@Test
	void unCorreoQueNoExisteSiempreRecibeElMismo401() throws Exception {
		for (int i = 0; i < 4; i++) {
			login("nadie@technova.com", "mala").andExpect(status().isUnauthorized());
		}
	}

	private ResultActions login(String email, String clave) throws Exception {
		return mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\": \"%s\", \"password\": \"%s\"}".formatted(email, clave)));
	}

}
