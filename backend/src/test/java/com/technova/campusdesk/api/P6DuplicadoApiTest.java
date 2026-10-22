package com.technova.campusdesk.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import com.technova.campusdesk.entity.StatusHistory;
import com.technova.campusdesk.entity.User;

/** [P6] PATCH /api/tickets/{id}/duplicado-de/{originalId}. */
class P6DuplicadoApiTest extends EstudioApiTestSupport {

	@Test
	void adminMarcaDuplicadoYSeCierraSaltandoLaMaquinaDeEstados() throws Exception {
		// anaOpen está OPEN: OPEN → CLOSED no lo permite TicketTransitionPolicy, pero esto es otra acción
		marcar(anaOpen.getId(), anaAssignedToLuis.getId())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CLOSED"))
				.andExpect(jsonPath("$.duplicadoDeId").value(anaAssignedToLuis.getId()));

		StatusHistory ultimo = statusHistoryRepository.findByTicketIdOrderByChangedAtAscIdAsc(anaOpen.getId()).getLast();
		assertThat(ultimo.getReason()).isEqualTo("Duplicado del ticket " + anaAssignedToLuis.getId());
	}

	@Test
	void elTecnicoAsignadoTambienPuede() throws Exception {
		marcarComo(luis, anaAssignedToLuis.getId(), pedroInProgressWithCarla.getId()).andExpect(status().isOk());
	}

	@Test
	void validacionesBasicas() throws Exception {
		marcar(anaOpen.getId(), anaOpen.getId()).andExpect(status().isBadRequest());
		marcar(anaOpen.getId(), 999999L).andExpect(status().isNotFound());
		marcarComo(ana, anaOpen.getId(), anaAssignedToLuis.getId()).andExpect(status().isForbidden());
	}

	@Test
	void noSeFormanCadenas() throws Exception {
		marcar(anaOpen.getId(), anaAssignedToLuis.getId()).andExpect(status().isOk());

		// anaOpen ya es duplicado (y está cerrado): no puede ser original de otro
		marcar(pedroInProgressWithCarla.getId(), anaOpen.getId()).andExpect(status().isConflict());
		// anaAssignedToLuis es original de anaOpen: no puede volverse duplicado
		marcar(anaAssignedToLuis.getId(), pedroInProgressWithCarla.getId()).andExpect(status().isConflict());
	}

	private ResultActions marcar(Long id, Long original) throws Exception {
		return marcarComo(admin, id, original);
	}

	private ResultActions marcarComo(User user, Long id, Long original) throws Exception {
		return patchAs(user, "/api/tickets/" + id + "/duplicado-de/" + original);
	}

}
