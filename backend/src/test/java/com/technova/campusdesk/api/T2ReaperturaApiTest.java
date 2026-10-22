package com.technova.campusdesk.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import com.technova.campusdesk.entity.StatusHistory;
import com.technova.campusdesk.entity.Ticket;
import com.technova.campusdesk.entity.enums.Category;
import com.technova.campusdesk.entity.enums.Priority;
import com.technova.campusdesk.entity.enums.TicketStatus;

/** [T2] Reapertura: POST /api/tickets/{id}/reabrir y GET /api/tickets/reabiertos. */
class T2ReaperturaApiTest extends EstudioApiTestSupport {

	private static final String MOTIVO = "{\"motivo\": \"La impresora volvió a fallar\"}";

	@Test
	void elSolicitanteReabreDentroDe48Horas() throws Exception {
		Ticket ticket = ticketResueltoHace(10);

		postAs(ana, url(ticket, "/reabrir"), MOTIVO)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("IN_PROGRESS"))
				.andExpect(jsonPath("$.reaperturasCount").value(1))
				// El técnico se conserva
				.andExpect(jsonPath("$.technician.id").value(luis.getId()));

		StatusHistory ultimo = statusHistoryRepository.findByTicketIdOrderByChangedAtAscIdAsc(ticket.getId()).getLast();
		assertThat(ultimo.getPreviousStatus()).isEqualTo(TicketStatus.RESOLVED);
		assertThat(ultimo.getNewStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
		assertThat(ultimo.getReason()).contains("La impresora volvió a fallar");
	}

	@Test
	void pasadas48HorasEs400YElTicketNoCambia() throws Exception {
		Ticket ticket = ticketResueltoHace(49);

		postAs(ana, url(ticket, "/reabrir"), MOTIVO).andExpect(status().isBadRequest());

		assertThat(ticketRepository.findById(ticket.getId()).orElseThrow().getStatus()).isEqualTo(TicketStatus.RESOLVED);
	}

	@Test
	void conHorasTruncadasNoSeCuela48hYMedia() throws Exception {
		// ChronoUnit.HOURS.between daría 48 (no > 48) y lo dejaría pasar; comparando fechas se rechaza
		Ticket ticket = ticketResueltoHace(48.5);
		postAs(ana, url(ticket, "/reabrir"), MOTIVO).andExpect(status().isBadRequest());
	}

	@Test
	void soloElSolicitantePuedeReabrir() throws Exception {
		Ticket ticket = ticketResueltoHace(1);
		postAs(admin, url(ticket, "/reabrir"), MOTIVO).andExpect(status().isForbidden());
		postAs(luis, url(ticket, "/reabrir"), MOTIVO).andExpect(status().isForbidden());
	}

	@Test
	void unTicketNoResueltoNoSeReabre() throws Exception {
		postAs(ana, url(anaAssignedToLuis, "/reabrir"), MOTIVO).andExpect(status().isConflict());
	}

	@Test
	void elMotivoEsObligatorio() throws Exception {
		Ticket ticket = ticketResueltoHace(1);
		postAs(ana, url(ticket, "/reabrir"), "{\"motivo\": \"  \"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.motivo").exists());
	}

	@Test
	void soloElAdminVeLosReabiertos() throws Exception {
		Ticket ticket = ticketResueltoHace(1);
		postAs(ana, url(ticket, "/reabrir"), MOTIVO).andExpect(status().isOk());

		getAs(admin, "/api/tickets/reabiertos")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(ticket.getId()));
		getAs(ana, "/api/tickets/reabiertos").andExpect(status().isForbidden());
	}

	// Ticket RESOLVED de Ana con Luis, con su paso a RESOLVED en el historial hace "horas" horas
	private Ticket ticketResueltoHace(double horas) {
		Ticket ticket = saveTicket("La impresora no imprime", Category.HARDWARE, Priority.MEDIUM,
				TicketStatus.RESOLVED, ana, luis);
		jdbcTemplate.update("""
				INSERT INTO status_history (ticket_id, previous_status, new_status, changed_by_id, changed_at)
				VALUES (?, 'IN_PROGRESS', 'RESOLVED', ?, ?)""", ticket.getId(), luis.getId(), haceHoras(horas));
		return ticket;
	}

}
