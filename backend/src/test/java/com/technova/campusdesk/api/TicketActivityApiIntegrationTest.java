package com.technova.campusdesk.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.technova.campusdesk.entity.Ticket;
import com.technova.campusdesk.entity.User;
import com.technova.campusdesk.entity.enums.Category;
import com.technova.campusdesk.entity.enums.Priority;
import com.technova.campusdesk.entity.enums.TicketStatus;

/**
 * Historial (GET /api/tickets/{id}/history) y comentarios (GET y POST /api/tickets/{id}/comments).
 * Datos de partida en ApiIntegrationTestSupport.
 */
class TicketActivityApiIntegrationTest extends ApiIntegrationTestSupport {

	// ---------- GET /api/tickets/{id}/history ----------

	@Test
	void historyShowsEveryChangeInOrderWithWhoMadeIt() throws Exception {
		long id = createTicketAsAna();
		mockMvc.perform(as(admin, patch("/api/tickets/" + id + "/assign")
				.contentType(MediaType.APPLICATION_JSON).content("{\"technicianId\":" + luis.getId() + "}")))
				.andExpect(status().isOk());
		mockMvc.perform(as(luis, patch("/api/tickets/" + id + "/status")
				.contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"IN_PROGRESS\"}")))
				.andExpect(status().isOk());

		mockMvc.perform(as(ana, get("/api/tickets/" + id + "/history")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(3))
				.andExpect(jsonPath("$[0].previousStatus").isEmpty())
				.andExpect(jsonPath("$[0].newStatus").value("OPEN"))
				.andExpect(jsonPath("$[0].changedBy.fullName").value("Ana Pérez"))
				.andExpect(jsonPath("$[1].previousStatus").value("OPEN"))
				.andExpect(jsonPath("$[1].newStatus").value("ASSIGNED"))
				.andExpect(jsonPath("$[1].changedBy.fullName").value("Admin"))
				.andExpect(jsonPath("$[2].newStatus").value("IN_PROGRESS"))
				.andExpect(jsonPath("$[2].changedBy.fullName").value("Luis Gómez"))
				.andExpect(jsonPath("$[2].changedAt").exists())
				.andExpect(jsonPath("$[0].changedBy.password").doesNotExist());
	}

	@Test
	void historyFollowsTicketAccessRules() throws Exception {
		String path = "/api/tickets/" + anaAssignedToLuis.getId() + "/history";
		for (User viewer : new User[] { ana, luis, admin }) {
			mockMvc.perform(as(viewer, get(path))).andExpect(status().isOk());
		}
		mockMvc.perform(as(pedro, get(path))).andExpect(status().isForbidden());
		mockMvc.perform(as(carla, get(path))).andExpect(status().isForbidden());
		mockMvc.perform(as(admin, get("/api/tickets/999999/history"))).andExpect(status().isNotFound());
		mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
	}

	// ---------- POST /api/tickets/{id}/comments ----------

	@Test
	void ownerAssignedTechnicianAndAdminCanComment() throws Exception {
		comment(ana, anaAssignedToLuis, "Sigue sin conexión")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.content").value("Sigue sin conexión"))
				.andExpect(jsonPath("$.author.fullName").value("Ana Pérez"))
				.andExpect(jsonPath("$.createdAt").exists())
				.andExpect(jsonPath("$.author.password").doesNotExist());
		comment(luis, anaAssignedToLuis, "Voy en camino").andExpect(status().isCreated());
		comment(admin, anaAssignedToLuis, "Prioridad alta").andExpect(status().isCreated());
	}

	@Test
	void othersCannotComment() throws Exception {
		comment(pedro, anaAssignedToLuis, "No es mi ticket").andExpect(status().isForbidden());
		comment(carla, anaAssignedToLuis, "No estoy asignada").andExpect(status().isForbidden());
		mockMvc.perform(post("/api/tickets/" + anaAssignedToLuis.getId() + "/comments")
				.contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"Sin sesión\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void commentIsTrimmedAndValidated() throws Exception {
		comment(ana, anaOpen, "   Con espacios   ")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.content").value("Con espacios"));
		comment(ana, anaOpen, "   ").andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.content").exists());
		comment(ana, anaOpen, "x".repeat(1001)).andExpect(status().isBadRequest());
		mockMvc.perform(as(ana, post("/api/tickets/" + anaOpen.getId() + "/comments")
				.contentType(MediaType.APPLICATION_JSON).content("{}")))
				.andExpect(status().isBadRequest());
	}

	@Test
	void closedTicketCanBeReadButNotCommented() throws Exception {
		Ticket closed = saveTicket("Cable HDMI para la sala", Category.OTHER, Priority.LOW, TicketStatus.CLOSED, ana, luis);

		comment(ana, closed, "Gracias").andExpect(status().isConflict());
		comment(admin, closed, "Cerrado").andExpect(status().isConflict());
		mockMvc.perform(as(ana, get("/api/tickets/" + closed.getId() + "/comments"))).andExpect(status().isOk());
	}

	@Test
	void commentOnUnknownTicketIs404() throws Exception {
		mockMvc.perform(as(admin, post("/api/tickets/999999/comments")
				.contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"Hola\"}")))
				.andExpect(status().isNotFound());
	}

	// ---------- GET /api/tickets/{id}/comments ----------

	@Test
	void commentsAreListedInOrderWithAuthor() throws Exception {
		comment(ana, anaAssignedToLuis, "Primero").andExpect(status().isCreated());
		comment(luis, anaAssignedToLuis, "Segundo").andExpect(status().isCreated());
		comment(admin, anaAssignedToLuis, "Tercero").andExpect(status().isCreated());

		for (User viewer : new User[] { ana, luis, admin }) {
			mockMvc.perform(as(viewer, get("/api/tickets/" + anaAssignedToLuis.getId() + "/comments")))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.length()").value(3))
					.andExpect(jsonPath("$[0].content").value("Primero"))
					.andExpect(jsonPath("$[0].author.fullName").value("Ana Pérez"))
					.andExpect(jsonPath("$[1].author.fullName").value("Luis Gómez"))
					.andExpect(jsonPath("$[2].author.fullName").value("Admin"));
		}
		mockMvc.perform(as(pedro, get("/api/tickets/" + anaAssignedToLuis.getId() + "/comments")))
				.andExpect(status().isForbidden());
		mockMvc.perform(as(admin, get("/api/tickets/999999/comments"))).andExpect(status().isNotFound());
	}

	// ---------- utilidades ----------

	private ResultActions comment(User user, Ticket ticket, String content) throws Exception {
		return mockMvc.perform(as(user, post("/api/tickets/" + ticket.getId() + "/comments")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"content\":\"" + content + "\"}")));
	}

	/** Crea un ticket por la API (así se guarda el registro de creación del historial) y devuelve su id. */
	private long createTicketAsAna() throws Exception {
		String body = mockMvc.perform(as(ana, post("/api/tickets").contentType(MediaType.APPLICATION_JSON)
				.content(ticketJson("Proyector sin señal", "El proyector del aula 3 no recibe señal", "HARDWARE", "HIGH"))))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return Long.parseLong(body.replaceAll(".*?\"id\":(\\d+).*", "$1"));
	}

}
