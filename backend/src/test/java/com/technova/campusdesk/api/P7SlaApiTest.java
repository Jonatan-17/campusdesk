package com.technova.campusdesk.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.technova.campusdesk.entity.Ticket;
import com.technova.campusdesk.entity.enums.Category;
import com.technova.campusdesk.entity.enums.Priority;
import com.technova.campusdesk.entity.enums.TicketStatus;

/** [P7] Fecha límite según prioridad y GET /api/tickets/vencidos. */
class P7SlaApiTest extends EstudioApiTestSupport {

	@Test
	void laFechaLimiteSeCalculaAlCrearSegunLaPrioridad() throws Exception {
		for (String[] prioridadYHoras : new String[][] { { "CRITICAL", "4" }, { "HIGH", "8" },
				{ "MEDIUM", "24" }, { "LOW", "48" } }) {
			Long id = idDe(postAs(ana, "/api/tickets",
					ticketJson("Servidor caído", "No responde el servidor de archivos", "NETWORK", prioridadYHoras[0]))
					.andExpect(status().isCreated()));
			Ticket ticket = ticketRepository.findById(id).orElseThrow();
			assertThat(ticket.getFechaLimite())
					.isEqualTo(ticket.getCreatedAt().plusHours(Long.parseLong(prioridadYHoras[1])));
		}
	}

	@Test
	void cambiarLaPrioridadRecalculaDesdeLaCreacion() throws Exception {
		putAs(ana, url(anaOpen, ""), ticketJson("No enciende el monitor", "Sigue igual", "HARDWARE", "LOW"))
				.andExpect(status().isOk());

		Ticket ticket = ticketRepository.findById(anaOpen.getId()).orElseThrow();
		assertThat(ticket.getFechaLimite()).isEqualTo(ticket.getCreatedAt().plusHours(48));
	}

	@Test
	void vencidosSoloIncluyeSinResolverYRespetaLaVisibilidad() throws Exception {
		Ticket cerrado = saveTicket("Viejo", Category.OTHER, Priority.LOW, TicketStatus.CLOSED, ana, luis);
		vencer(anaOpen);
		vencer(pedroInProgressWithCarla);
		vencer(cerrado); // cerrado: no cuenta aunque su fecha haya pasado

		getAs(admin, "/api/tickets/vencidos").andExpect(jsonPath("$", hasSize(2)));
		getAs(ana, "/api/tickets/vencidos").andExpect(status().isForbidden());
		getAs(luis, "/api/tickets/vencidos").andExpect(status().isForbidden());
	}

	@Test
	void slaStatusRequiereDueñoTecnicoAsignadoOAdmin() throws Exception {
		getAs(ana, url(anaOpen, "/sla-status"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.minutosRestantes").isNumber())
				.andExpect(jsonPath("$.estaVencido").value(false))
				.andExpect(jsonPath("$.porcentajeConsumido").isNumber());
		getAs(admin, url(anaOpen, "/sla-status")).andExpect(status().isOk());
		getAs(luis, url(anaAssignedToLuis, "/sla-status")).andExpect(status().isOk());
		getAs(pedro, url(anaOpen, "/sla-status")).andExpect(status().isForbidden());
		getAs(carla, url(anaOpen, "/sla-status")).andExpect(status().isForbidden());
	}

	@Test
	void slaStatusSeCongelaAlMomentoDeResolucion() throws Exception {
		LocalDateTime ahora = LocalDateTime.now();
		LocalDateTime fechaLimite = ahora.minusMinutes(1);
		jdbcTemplate.update("UPDATE tickets SET fecha_limite = ? WHERE id = ?",
				Timestamp.valueOf(fechaLimite), pedroInProgressWithCarla.getId());

		patchAs(carla, url(pedroInProgressWithCarla, "/status"), "{\"status\":\"RESOLVED\"}")
				.andExpect(status().isOk());
		jdbcTemplate.update("UPDATE status_history SET changed_at = ? WHERE ticket_id = ? AND new_status = 'RESOLVED'",
				Timestamp.valueOf(ahora.minusMinutes(5)), pedroInProgressWithCarla.getId());

		getAs(pedro, url(pedroInProgressWithCarla, "/sla-status"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.minutosRestantes").value(4))
				.andExpect(jsonPath("$.estaVencido").value(false));
	}

	private void vencer(Ticket ticket) {
		jdbcTemplate.update("UPDATE tickets SET fecha_limite = ? WHERE id = ?", haceHoras(1), ticket.getId());
	}

}
