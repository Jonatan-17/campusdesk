package com.technova.campusdesk.api;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import com.technova.campusdesk.entity.Ticket;
import com.technova.campusdesk.entity.enums.Category;
import com.technova.campusdesk.entity.enums.Priority;
import com.technova.campusdesk.entity.enums.TicketStatus;

/** [P8] Observadores: ven y comentan el ticket (detalle Y listado), pero no lo modifican. */
class P8ObservadoresApiTest extends EstudioApiTestSupport {

	@Test
	void unObservadorVeElTicketEnElDetalleYEnSuListadoYPuedeComentar() throws Exception {
		agregar(anaOpen, pedro.getId())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].fullName").value("Pedro Díaz"));

		getAs(pedro, url(anaOpen, "")).andExpect(status().isOk());
		getAs(pedro, "/api/tickets")
				.andExpect(jsonPath("$", hasSize(2))) // el suyo + el que observa
				.andExpect(jsonPath("$[*].id", hasItem(anaOpen.getId().intValue())));
		postAs(pedro, url(anaOpen, "/comments"), "{\"content\": \"A mí me pasa lo mismo\"}")
				.andExpect(status().isCreated());
	}

	@Test
	void unObservadorNoPuedeCerrarAunqueTengaRolUser() throws Exception {
		Ticket resuelto = saveTicket("Resuelto", Category.SOFTWARE, Priority.LOW, TicketStatus.RESOLVED, ana, luis);
		agregar(resuelto, pedro.getId()).andExpect(status().isOk());

		patchAs(pedro, url(resuelto, "/status"), "{\"status\": \"CLOSED\"}").andExpect(status().isForbidden());
		patchAs(ana, url(resuelto, "/status"), "{\"status\": \"CLOSED\"}").andExpect(status().isOk());
	}

	@Test
	void soloElSolicitanteGestionaYSoloSeAgreganUsuariosUser() throws Exception {
		agregar(anaOpen, pedro.getId()).andExpect(status().isOk());

		// Pedro observa, pero no es el dueño
		postAs(pedro, url(anaOpen, "/observadores"), "{\"usuarioId\": " + ana.getId() + "}")
				.andExpect(status().isForbidden());
		agregar(anaOpen, luis.getId()).andExpect(status().isConflict());   // no es USER
		agregar(anaOpen, ana.getId()).andExpect(status().isConflict());    // es ella misma
		agregar(anaOpen, pedro.getId()).andExpect(status().isConflict());  // ya está
	}

	@Test
	void alQuitarloPierdeElAcceso() throws Exception {
		agregar(anaOpen, pedro.getId()).andExpect(status().isOk());
		deleteAs(ana, url(anaOpen, "/observadores/" + pedro.getId())).andExpect(status().isNoContent());
		getAs(pedro, url(anaOpen, "")).andExpect(status().isForbidden());
	}

	private ResultActions agregar(Ticket ticket, Long usuarioId) throws Exception {
		return postAs(ana, url(ticket, "/observadores"), "{\"usuarioId\": " + usuarioId + "}");
	}

}
