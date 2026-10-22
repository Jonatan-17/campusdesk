package com.technova.campusdesk.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import com.technova.campusdesk.entity.Comment;
import com.technova.campusdesk.entity.StatusHistory;
import com.technova.campusdesk.entity.enums.TicketStatus;

/** [T3] Transferencia: PUT /api/tickets/{id}/transferir. */
class T3TransferenciaApiTest extends EstudioApiTestSupport {

	private static final String A_ACCESS = """
			{"nuevaCategoria": "ACCESS", "justificacion": "Es un problema de permisos, no de red"}""";

	@Test
	void elTecnicoAsignadoTransfiereYSeDesvinculaVuelveAOpenYQuedaLaNota() throws Exception {
		putAs(luis, url(anaAssignedToLuis, "/transferir"), A_ACCESS)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.category").value("ACCESS"))
				.andExpect(jsonPath("$.status").value("OPEN"))
				.andExpect(jsonPath("$.technician").value(nullValue()));

		// Nota automática escrita por el listener del evento
		Comment nota = commentRepository.findByTicketIdOrderByCreatedAtAscIdAsc(anaAssignedToLuis.getId()).getLast();
		assertThat(nota.getContent()).startsWith("[Transferencia automática]").contains("NETWORK → ACCESS");
		assertThat(nota.getAuthor().getId()).isEqualTo(luis.getId());

		// Y el retroceso de estado quedó en el historial
		StatusHistory ultimo = statusHistoryRepository
				.findByTicketIdOrderByChangedAtAscIdAsc(anaAssignedToLuis.getId()).getLast();
		assertThat(ultimo.getPreviousStatus()).isEqualTo(TicketStatus.ASSIGNED);
		assertThat(ultimo.getNewStatus()).isEqualTo(TicketStatus.OPEN);
	}

	@Test
	void laMismaCategoriaEs400() throws Exception {
		putAs(admin, url(anaAssignedToLuis, "/transferir"), """
				{"nuevaCategoria": "NETWORK", "justificacion": "x"}""")
				.andExpect(status().isBadRequest());
	}

	@Test
	void unaCategoriaQueNoExisteEs400() throws Exception {
		putAs(admin, url(anaAssignedToLuis, "/transferir"), """
				{"nuevaCategoria": "REDES", "justificacion": "x"}""")
				.andExpect(status().isBadRequest());
	}

	@Test
	void soloAdminOTecnicoAsignado() throws Exception {
		putAs(ana, url(anaAssignedToLuis, "/transferir"), A_ACCESS).andExpect(status().isForbidden());
		putAs(carla, url(anaAssignedToLuis, "/transferir"), A_ACCESS).andExpect(status().isForbidden());
	}

	@Test
	void siFallaNoQuedaNiTransferenciaNiNota() throws Exception {
		long comentariosAntes = commentRepository.count();
		putAs(admin, url(anaAssignedToLuis, "/transferir"), """
				{"nuevaCategoria": "NETWORK", "justificacion": "x"}""").andExpect(status().isBadRequest());
		assertThat(commentRepository.count()).isEqualTo(comentariosAntes);
	}

}
