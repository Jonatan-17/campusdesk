package com.technova.campusdesk.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.technova.campusdesk.entity.StatusHistory;
import com.technova.campusdesk.entity.Ticket;
import com.technova.campusdesk.entity.User;
import com.technova.campusdesk.entity.enums.TicketStatus;

/**
 * Asignación (PATCH /api/tickets/{id}/assign) y cambio de estado (PATCH /api/tickets/{id}/status).
 * Datos de partida en ApiIntegrationTestSupport.
 */
class TicketWorkflowApiIntegrationTest extends ApiIntegrationTestSupport {

	// ---------- PATCH /api/tickets/{id}/assign ----------

	@Test
	void adminAssignsOpenTicketAndHistoryIsRecorded() throws Exception {
		assign(admin, anaOpen, luis.getId())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ASSIGNED"))
				.andExpect(jsonPath("$.technician.id").value(luis.getId()))
				.andExpect(jsonPath("$.technician.fullName").value("Luis Gómez"));

		assertThat(history(anaOpen))
				.singleElement()
				.satisfies(h -> {
					assertThat(h.getPreviousStatus()).isEqualTo(TicketStatus.OPEN);
					assertThat(h.getNewStatus()).isEqualTo(TicketStatus.ASSIGNED);
					assertThat(h.getChangedBy().getId()).isEqualTo(admin.getId());
				});
	}

	@Test
	void onlyAdminCanAssign() throws Exception {
		assign(ana, anaOpen, luis.getId()).andExpect(status().isForbidden());
		assign(luis, anaOpen, luis.getId()).andExpect(status().isForbidden());
	}

	@Test
	void assignToNonTechnicianIsRejected() throws Exception {
		// El contrato dice 400; hoy responde 409 (decisión pendiente, ver README)
		assign(admin, anaOpen, pedro.getId()).andExpect(status().isConflict());
	}

	@Test
	void assignWithUnknownTicketOrTechnicianIs404() throws Exception {
		assign(admin, anaOpen, 999999L).andExpect(status().isNotFound());
		mockMvc.perform(as(admin, patch("/api/tickets/999999/assign")
				.contentType(MediaType.APPLICATION_JSON).content("{\"technicianId\":" + luis.getId() + "}")))
				.andExpect(status().isNotFound());
	}

	@Test
	void assignWithoutTechnicianIs400() throws Exception {
		mockMvc.perform(as(admin, patch("/api/tickets/" + anaOpen.getId() + "/assign")
				.contentType(MediaType.APPLICATION_JSON).content("{}")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.technicianId").exists());
	}

	@Test
	void reassignInProgressTicketIsRejected() throws Exception {
		// Decisión pendiente del equipo: hoy solo se reasigna en ASSIGNED
		assign(admin, pedroInProgressWithCarla, luis.getId()).andExpect(status().isConflict());
	}

	@Test
	void adminReassignsAssignedTicket() throws Exception {
		// RF-04 regla 9: un ticket ASSIGNED se puede reasignar (solo ADMIN)
		assign(admin, anaAssignedToLuis, carla.getId())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ASSIGNED"))
				.andExpect(jsonPath("$.technician.fullName").value("Carla Ruiz"));

		// Luis ya no tiene acceso; Carla sí
		changeStatus(luis, anaAssignedToLuis, "IN_PROGRESS").andExpect(status().isForbidden());
		changeStatus(carla, anaAssignedToLuis, "IN_PROGRESS").andExpect(status().isOk());
	}

	@Test
	void reassignToSameTechnicianIsRejected() throws Exception {
		assign(admin, anaAssignedToLuis, luis.getId()).andExpect(status().isConflict());
		assertThat(history(anaAssignedToLuis)).isEmpty();
	}

	// ---------- PATCH /api/tickets/{id}/status ----------

	@Test
	void fullLifecycleIsRecordedInHistory() throws Exception {
		assign(admin, anaOpen, luis.getId()).andExpect(status().isOk());
		changeStatus(luis, anaOpen, "IN_PROGRESS").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("IN_PROGRESS"));
		changeStatus(luis, anaOpen, "RESOLVED").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RESOLVED"));
		changeStatus(ana, anaOpen, "CLOSED").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CLOSED"));

		assertThat(history(anaOpen))
				.extracting(h -> h.getPreviousStatus() + " > " + h.getNewStatus())
				.containsExactly("OPEN > ASSIGNED", "ASSIGNED > IN_PROGRESS", "IN_PROGRESS > RESOLVED", "RESOLVED > CLOSED");
	}

	@Test
	void onlyAssignedTechnicianMovesTheTicket() throws Exception {
		changeStatus(carla, anaAssignedToLuis, "IN_PROGRESS").andExpect(status().isForbidden());
		changeStatus(ana, anaAssignedToLuis, "IN_PROGRESS").andExpect(status().isForbidden());
		changeStatus(luis, anaAssignedToLuis, "IN_PROGRESS").andExpect(status().isOk());
	}

	@Test
	void onlyOwnerClosesResolvedTicket() throws Exception {
		changeStatus(carla, pedroInProgressWithCarla, "RESOLVED").andExpect(status().isOk());

		changeStatus(carla, pedroInProgressWithCarla, "CLOSED").andExpect(status().isForbidden());
		changeStatus(ana, pedroInProgressWithCarla, "CLOSED").andExpect(status().isForbidden());
		changeStatus(pedro, pedroInProgressWithCarla, "CLOSED").andExpect(status().isOk());
	}

	@Test
	void adminCannotCloseResolvedTicket() throws Exception {
		// RF-04 regla 6: solo el solicitante confirma el cierre
		changeStatus(carla, pedroInProgressWithCarla, "RESOLVED").andExpect(status().isOk());
		changeStatus(admin, pedroInProgressWithCarla, "CLOSED").andExpect(status().isForbidden());
	}

	@Test
	void skippingStepsOrGoingBackIs409() throws Exception {
		changeStatus(luis, anaAssignedToLuis, "RESOLVED").andExpect(status().isConflict());
		changeStatus(carla, pedroInProgressWithCarla, "OPEN").andExpect(status().isConflict());
		changeStatus(carla, pedroInProgressWithCarla, "IN_PROGRESS").andExpect(status().isConflict());
	}

	@Test
	void assignedCannotBeSetThroughStatusEndpoint() throws Exception {
		// Solo /assign deja un ticket en ASSIGNED, porque es el que asigna al técnico
		changeStatus(admin, anaOpen, "ASSIGNED").andExpect(status().isConflict());
		mockMvc.perform(as(admin, get("/api/tickets/" + anaOpen.getId())))
				.andExpect(jsonPath("$.status").value("OPEN"))
				.andExpect(jsonPath("$.technician").isEmpty());
	}

	@Test
	void invalidStatusValueIs400() throws Exception {
		changeStatus(luis, anaAssignedToLuis, "EN_PROCESO").andExpect(status().isBadRequest());
		mockMvc.perform(as(luis, patch("/api/tickets/" + anaAssignedToLuis.getId() + "/status")
				.contentType(MediaType.APPLICATION_JSON).content("{}")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.status").exists());
	}

	@Test
	void statusChangeWithoutTokenIs401() throws Exception {
		mockMvc.perform(patch("/api/tickets/" + anaAssignedToLuis.getId() + "/status")
				.contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"IN_PROGRESS\"}"))
				.andExpect(status().isUnauthorized());
	}

	// ---------- utilidades ----------

	private ResultActions assign(User user, Ticket ticket, Long technicianId) throws Exception {
		return mockMvc.perform(as(user, patch("/api/tickets/" + ticket.getId() + "/assign")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"technicianId\":" + technicianId + "}")));
	}

	private ResultActions changeStatus(User user, Ticket ticket, String newStatus) throws Exception {
		return mockMvc.perform(as(user, patch("/api/tickets/" + ticket.getId() + "/status")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"" + newStatus + "\"}")));
	}

	private List<StatusHistory> history(Ticket ticket) {
		return statusHistoryRepository.findByTicketIdOrderByChangedAtAscIdAsc(ticket.getId());
	}

}
