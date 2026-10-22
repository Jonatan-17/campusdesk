package com.technova.campusdesk.api;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import com.technova.campusdesk.entity.enums.Category;
import com.technova.campusdesk.entity.enums.Priority;
import com.technova.campusdesk.entity.enums.TicketStatus;

/** [P3] Límite de 5 tickets activos por técnico y GET /api/reports/carga-tecnicos. */
class P3CargaTecnicoApiTest extends EstudioApiTestSupport {

	@Test
	void noSeAsignaAUnTecnicoQueYaTiene5Activos() throws Exception {
		// Luis ya tiene 1 (anaAssignedToLuis); con 4 más llega al límite
		for (int i = 0; i < 4; i++) {
			saveTicket("Activo " + i, Category.OTHER, Priority.LOW, TicketStatus.IN_PROGRESS, pedro, luis);
		}

		patchAs(admin, url(anaOpen, "/assign"), "{\"technicianId\": " + luis.getId() + "}")
				.andExpect(status().isConflict());
		// Carla tiene 1: sí se le puede asignar
		patchAs(admin, url(anaOpen, "/assign"), "{\"technicianId\": " + carla.getId() + "}")
				.andExpect(status().isOk());
	}

	@Test
	void losTicketsResueltosOCerradosNoCuentan() throws Exception {
		for (int i = 0; i < 6; i++) {
			saveTicket("Terminado " + i, Category.OTHER, Priority.LOW, TicketStatus.CLOSED, pedro, luis);
		}
		patchAs(admin, url(anaOpen, "/assign"), "{\"technicianId\": " + luis.getId() + "}")
				.andExpect(status().isOk());
	}

	@Test
	void reporteDeCargaPorTecnico() throws Exception {
		getAs(admin, "/api/reports/carga-tecnicos")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].nombre").value("Carla Ruiz"))
				.andExpect(jsonPath("$[0].asignados").value(0))
				.andExpect(jsonPath("$[0].enProceso").value(1))
				.andExpect(jsonPath("$[1].nombre").value("Luis Gómez"))
				.andExpect(jsonPath("$[1].asignados").value(1))
				.andExpect(jsonPath("$[1].activos").value(1))
				.andExpect(jsonPath("$[1].limite").value(5));

		getAs(luis, "/api/reports/carga-tecnicos").andExpect(status().isForbidden());
	}

}
