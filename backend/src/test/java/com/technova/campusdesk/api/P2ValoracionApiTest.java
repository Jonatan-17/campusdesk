package com.technova.campusdesk.api;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import com.technova.campusdesk.entity.Ticket;
import com.technova.campusdesk.entity.User;
import com.technova.campusdesk.entity.enums.Category;
import com.technova.campusdesk.entity.enums.Priority;
import com.technova.campusdesk.entity.enums.TicketStatus;

/** [P2] Valoración: POST/GET /api/tickets/{id}/valoracion y GET /api/reports/satisfaccion. */
class P2ValoracionApiTest extends EstudioApiTestSupport {

	@Test
	void elSolicitanteValoraUnaSolaVez() throws Exception {
		Ticket cerrado = cerrado(ana, luis);

		postAs(ana, url(cerrado, "/valoracion"), "{\"puntuacion\": 5, \"comentario\": \"Muy rápido\"}")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.puntuacion").value(5));
		postAs(ana, url(cerrado, "/valoracion"), "{\"puntuacion\": 1}").andExpect(status().isConflict());

		getAs(luis, url(cerrado, "/valoracion"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.comentario").value("Muy rápido"));
	}

	@Test
	void soloTicketsCerradosYSoloElSolicitante() throws Exception {
		postAs(ana, url(anaAssignedToLuis, "/valoracion"), "{\"puntuacion\": 4}").andExpect(status().isConflict());
		postAs(admin, url(cerrado(ana, luis), "/valoracion"), "{\"puntuacion\": 4}").andExpect(status().isForbidden());
	}

	@Test
	void puntuacionFueraDeRangoEs400YSinValoracionEs404() throws Exception {
		Ticket cerrado = cerrado(ana, luis);
		postAs(ana, url(cerrado, "/valoracion"), "{\"puntuacion\": 6}").andExpect(status().isBadRequest());
		getAs(ana, url(cerrado, "/valoracion")).andExpect(status().isNotFound());
	}

	@Test
	void reporteConPromedioPorTecnicoYTecnicosSinValoraciones() throws Exception {
		postAs(ana, url(cerrado(ana, luis), "/valoracion"), "{\"puntuacion\": 5}").andExpect(status().isCreated());
		postAs(pedro, url(cerrado(pedro, luis), "/valoracion"), "{\"puntuacion\": 4}").andExpect(status().isCreated());

		// Orden por nombre: Carla Ruiz, Luis Gómez
		getAs(admin, "/api/reports/satisfaccion")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].nombre").value("Carla Ruiz"))
				.andExpect(jsonPath("$[0].promedio").value(nullValue()))
				.andExpect(jsonPath("$[0].cantidad").value(0))
				.andExpect(jsonPath("$[1].nombre").value("Luis Gómez"))
				.andExpect(jsonPath("$[1].promedio").value(4.5))
				.andExpect(jsonPath("$[1].cantidad").value(2));

		getAs(ana, "/api/reports/satisfaccion").andExpect(status().isForbidden());
	}

	private Ticket cerrado(User solicitante, User tecnico) {
		return saveTicket("Ticket cerrado", Category.SOFTWARE, Priority.LOW, TicketStatus.CLOSED, solicitante, tecnico);
	}

}
