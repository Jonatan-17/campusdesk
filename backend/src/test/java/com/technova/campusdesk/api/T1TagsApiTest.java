package com.technova.campusdesk.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.technova.campusdesk.repository.TagRepository;

/** [T1] Etiquetas: POST /api/tickets/{id}/tags y GET /api/tickets/por-tag. */
class T1TagsApiTest extends EstudioApiTestSupport {

	@Autowired
	private TagRepository tagRepository;

	@Test
	void adminAgregaEtiquetasNormalizadasYSinRepetir() throws Exception {
		postAs(admin, url(anaAssignedToLuis, "/tags"), "[\"vpn\", \" VPN \", \"Remoto\"]")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tags", contains("REMOTO", "VPN")));
	}

	@Test
	void unaEtiquetaExistenteSeReutilizaNoSeDuplica() throws Exception {
		postAs(luis, url(anaAssignedToLuis, "/tags"), "[\"vpn\"]").andExpect(status().isOk());
		postAs(carla, url(pedroInProgressWithCarla, "/tags"), "[\"Vpn\"]").andExpect(status().isOk());

		assertThat(tagRepository.buscarPorNombres(List.of("VPN"))).hasSize(1);
	}

	@Test
	void soloAdminOTecnicoAsignadoPuedenEtiquetar() throws Exception {
		// Ana es la dueña, pero no es ADMIN ni técnico
		postAs(ana, url(anaAssignedToLuis, "/tags"), "[\"vpn\"]").andExpect(status().isForbidden());
		// Carla es técnico, pero no el asignado
		postAs(carla, url(anaAssignedToLuis, "/tags"), "[\"vpn\"]").andExpect(status().isForbidden());
	}

	@Test
	void listaVaciaOEtiquetaEnBlancoSon400() throws Exception {
		postAs(admin, url(anaOpen, "/tags"), "[]").andExpect(status().isBadRequest());
		postAs(admin, url(anaOpen, "/tags"), "[\"  \"]").andExpect(status().isBadRequest());
	}

	@Test
	void buscarPorEtiquetaRespetaLaVisibilidadDeCadaRol() throws Exception {
		postAs(admin, url(anaAssignedToLuis, "/tags"), "[\"impresora\"]").andExpect(status().isOk());
		postAs(admin, url(pedroInProgressWithCarla, "/tags"), "[\"impresora\"]").andExpect(status().isOk());

		getAs(admin, "/api/tickets/por-tag?nombre=Impresora").andExpect(jsonPath("$", hasSize(2)));
		getAs(ana, "/api/tickets/por-tag?nombre=impresora")
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(anaAssignedToLuis.getId()));
		getAs(carla, "/api/tickets/por-tag?nombre=IMPRESORA")
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(pedroInProgressWithCarla.getId()));
	}

	@Test
	void buscarSinNombreEs400() throws Exception {
		getAs(admin, "/api/tickets/por-tag").andExpect(status().isBadRequest());
	}

}
