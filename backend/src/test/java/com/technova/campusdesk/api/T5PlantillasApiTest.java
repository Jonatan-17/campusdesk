package com.technova.campusdesk.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import com.technova.campusdesk.entity.User;

/** [T5] Plantillas: CRUD en /api/tecnicos/plantillas y POST /api/tickets/{id}/aplicar-plantilla/{plantillaId}. */
class T5PlantillasApiTest extends EstudioApiTestSupport {

	private static final String PLANTILLAS = "/api/tecnicos/plantillas";

	@Test
	void elTituloEsUnicoPorTecnicoNoEnTodaLaTabla() throws Exception {
		crear(luis, "Reinicio de modem").andExpect(status().isCreated());
		// Otro técnico puede tener el mismo título
		crear(carla, "Reinicio de modem").andExpect(status().isCreated());
		// El mismo técnico no, aunque cambien las mayúsculas
		crear(luis, "REINICIO DE MODEM").andExpect(status().isConflict());
	}

	@Test
	void cadaTecnicoSoloVeSusPlantillas() throws Exception {
		Long deLuis = idDe(crear(luis, "Limpiar cache"));
		crear(carla, "Otra").andExpect(status().isCreated());

		getAs(luis, PLANTILLAS).andExpect(jsonPath("$", hasSize(1)));
		// La de otro técnico responde 404, no 403: no se revela que existe
		getAs(carla, PLANTILLAS + "/" + deLuis).andExpect(status().isNotFound());
		deleteAs(carla, PLANTILLAS + "/" + deLuis).andExpect(status().isNotFound());
	}

	@Test
	void soloLosTecnicosUsanPlantillas() throws Exception {
		crear(ana, "x").andExpect(status().isForbidden());
		getAs(admin, PLANTILLAS).andExpect(status().isForbidden());
	}

	@Test
	void aplicarPublicaLaPlantillaComoComentario() throws Exception {
		Long plantilla = idDe(crear(carla, "Reinicio de modem"));

		postAs(carla, url(pedroInProgressWithCarla, "/aplicar-plantilla/" + plantilla))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.content").value("Desconecta el módem 30 segundos y vuelve a conectarlo."))
				.andExpect(jsonPath("$.author.id").value(carla.getId()));

		assertThat(commentRepository.findByTicketIdOrderByCreatedAtAscIdAsc(pedroInProgressWithCarla.getId()))
				.hasSize(1);
	}

	@Test
	void aplicarExigePlantillaPropiaTicketAsignadoEInProgress() throws Exception {
		Long deLuis = idDe(crear(luis, "Reinicio de modem"));
		// Plantilla ajena
		postAs(carla, url(pedroInProgressWithCarla, "/aplicar-plantilla/" + deLuis)).andExpect(status().isNotFound());
		// Ticket que Luis no tiene asignado
		postAs(luis, url(pedroInProgressWithCarla, "/aplicar-plantilla/" + deLuis)).andExpect(status().isForbidden());
		// Ticket de Luis pero en ASSIGNED, no IN_PROGRESS
		postAs(luis, url(anaAssignedToLuis, "/aplicar-plantilla/" + deLuis)).andExpect(status().isConflict());
	}

	@Test
	void editarYBorrar() throws Exception {
		Long id = idDe(crear(luis, "Vieja"));
		putAs(luis, PLANTILLAS + "/" + id, json("Nueva"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.titulo").value("Nueva"));
		// Guardar con su mismo título no cuenta como duplicado
		putAs(luis, PLANTILLAS + "/" + id, json("Nueva")).andExpect(status().isOk());

		deleteAs(luis, PLANTILLAS + "/" + id).andExpect(status().isNoContent());
		getAs(luis, PLANTILLAS + "/" + id).andExpect(status().isNotFound());
	}

	private ResultActions crear(User user, String titulo) throws Exception {
		return postAs(user, PLANTILLAS, json(titulo));
	}

	private static String json(String titulo) {
		return """
				{"titulo": "%s", "contenido": "Desconecta el módem 30 segundos y vuelve a conectarlo.", "categoria": "NETWORK"}"""
				.formatted(titulo);
	}

}
