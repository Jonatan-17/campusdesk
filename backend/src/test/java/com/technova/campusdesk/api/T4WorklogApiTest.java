package com.technova.campusdesk.api;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

/** [T4] Worklog: POST /api/tickets/{id}/worklogs y GET /api/tickets/{id}/tiempo-total. */
class T4WorklogApiTest extends EstudioApiTestSupport {

	@Test
	void elTecnicoRegistraTiempoYElTotalSeSumaEnLaBase() throws Exception {
		postAs(carla, url(pedroInProgressWithCarla, "/worklogs"), worklog(30)).andExpect(status().isCreated());
		postAs(carla, url(pedroInProgressWithCarla, "/worklogs"), worklog(45))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.tecnico.id").value(carla.getId()));

		// Lo puede consultar el solicitante
		getAs(pedro, url(pedroInProgressWithCarla, "/tiempo-total"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ticketId").value(pedroInProgressWithCarla.getId()))
				.andExpect(jsonPath("$.totalMinutos").value(75))
				.andExpect(jsonPath("$.registros", hasSize(2)));
	}

	@Test
	void sinRegistrosElTotalEs0YNoNull() throws Exception {
		getAs(admin, url(anaAssignedToLuis, "/tiempo-total"))
				.andExpect(jsonPath("$.totalMinutos").value(0))
				.andExpect(jsonPath("$.registros", hasSize(0)));
	}

	@Test
	void losMinutosDebenEstarEntre5Y480() throws Exception {
		postAs(carla, url(pedroInProgressWithCarla, "/worklogs"), worklog(4)).andExpect(status().isBadRequest());
		postAs(carla, url(pedroInProgressWithCarla, "/worklogs"), worklog(481)).andExpect(status().isBadRequest());
		postAs(carla, url(pedroInProgressWithCarla, "/worklogs"), "{\"descripcionActividad\": \"x\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.minutosDedicados").exists());
	}

	@Test
	void soloElTecnicoAsignadoNiSiquieraElAdmin() throws Exception {
		postAs(admin, url(pedroInProgressWithCarla, "/worklogs"), worklog(30)).andExpect(status().isForbidden());
		postAs(luis, url(pedroInProgressWithCarla, "/worklogs"), worklog(30)).andExpect(status().isForbidden());
	}

	@Test
	void soloEnTicketsInProgress() throws Exception {
		postAs(luis, url(anaAssignedToLuis, "/worklogs"), worklog(30)).andExpect(status().isConflict());
	}

	private static String worklog(int minutos) {
		return """
				{"minutosDedicados": %d, "descripcionActividad": "Revisión del equipo"}""".formatted(minutos);
	}

}
