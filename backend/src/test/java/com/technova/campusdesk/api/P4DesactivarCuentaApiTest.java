package com.technova.campusdesk.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;

import com.technova.campusdesk.entity.StatusHistory;
import com.technova.campusdesk.entity.Ticket;
import com.technova.campusdesk.entity.enums.TicketStatus;

/** [P4] PATCH /api/users/{id}/desactivar y /activar: login, token y tickets del técnico. */
class P4DesactivarCuentaApiTest extends EstudioApiTestSupport {

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void desactivarUnTecnicoLiberaSusTicketsActivos() throws Exception {
		patchAs(admin, "/api/users/" + luis.getId() + "/desactivar")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(false))
				.andExpect(jsonPath("$.ticketsLiberados", contains(anaAssignedToLuis.getId().intValue())));

		Ticket ticket = ticketRepository.findById(anaAssignedToLuis.getId()).orElseThrow();
		assertThat(ticket.getStatus()).isEqualTo(TicketStatus.OPEN);
		assertThat(ticket.getTechnician()).isNull();

		StatusHistory ultimo = statusHistoryRepository.findByTicketIdOrderByChangedAtAscIdAsc(ticket.getId()).getLast();
		assertThat(ultimo.getReason()).contains("desactivado");
	}

	@Test
	void elTokenQueYaTeniaDejaDeServir() throws Exception {
		getAs(luis, "/api/tickets").andExpect(status().isOk());

		patchAs(admin, "/api/users/" + luis.getId() + "/desactivar").andExpect(status().isOk());

		// Mismo tipo de token, firmado y sin vencer: ahora es 401
		getAs(luis, "/api/tickets").andExpect(status().isUnauthorized());
	}

	@Test
	void unaCuentaDesactivadaNoIniciaSesionYElMensajeEsGenerico() throws Exception {
		conContrasena("Clave12345");
		patchAs(admin, "/api/users/" + luis.getId() + "/desactivar").andExpect(status().isOk());

		login("Clave12345")
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Correo o contraseña incorrectos"));
	}

	@Test
	void reactivarDevuelveElAcceso() throws Exception {
		patchAs(admin, "/api/users/" + luis.getId() + "/desactivar").andExpect(status().isOk());
		patchAs(admin, "/api/users/" + luis.getId() + "/activar").andExpect(status().isOk());
		getAs(luis, "/api/tickets").andExpect(status().isOk());
	}

	@Test
	void noSeAsignanTicketsAUnTecnicoDesactivado() throws Exception {
		patchAs(admin, "/api/users/" + carla.getId() + "/desactivar").andExpect(status().isOk());
		patchAs(admin, url(anaOpen, "/assign"), "{\"technicianId\": " + carla.getId() + "}")
				.andExpect(status().isConflict());
	}

	@Test
	void reglasDePermiso() throws Exception {
		patchAs(admin, "/api/users/" + admin.getId() + "/desactivar").andExpect(status().isConflict());
		// /api/users/** es solo ADMIN por SecurityConfig
		patchAs(ana, "/api/users/" + pedro.getId() + "/desactivar").andExpect(status().isForbidden());
	}

	private void conContrasena(String clave) {
		luis.setPassword(passwordEncoder.encode(clave));
		userRepository.save(luis);
	}

	private ResultActions login(String clave) throws Exception {
		return mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\": \"tech1@technova.com\", \"password\": \"" + clave + "\"}"));
	}

}
