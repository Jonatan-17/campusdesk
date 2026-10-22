package com.technova.campusdesk.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;
import com.technova.campusdesk.entity.Ticket;
import com.technova.campusdesk.entity.User;

/**
 * Base de las pruebas de los ejercicios de estudio (docs/estudio-examen.md). Hereda los datos de
 * ApiIntegrationTestSupport: ADMIN, técnicos Luis y Carla, usuarios Ana y Pedro, y tres tickets:
 * anaOpen (OPEN), anaAssignedToLuis (ASSIGNED, NETWORK) y pedroInProgressWithCarla (IN_PROGRESS, SOFTWARE).
 * Agrega atajos para no repetir el armado de cada petición.
 */
abstract class EstudioApiTestSupport extends ApiIntegrationTestSupport {

	// SQL directo para preparar datos que la API no deja fijar (fechas en el pasado)
	@Autowired
	protected JdbcTemplate jdbcTemplate;

	protected ResultActions getAs(User user, String url) throws Exception {
		return mockMvc.perform(as(user, get(url)));
	}

	protected ResultActions postAs(User user, String url, String json) throws Exception {
		return mockMvc.perform(as(user, post(url).contentType(MediaType.APPLICATION_JSON).content(json)));
	}

	protected ResultActions postAs(User user, String url) throws Exception {
		return mockMvc.perform(as(user, post(url)));
	}

	protected ResultActions putAs(User user, String url, String json) throws Exception {
		return mockMvc.perform(as(user, put(url).contentType(MediaType.APPLICATION_JSON).content(json)));
	}

	protected ResultActions patchAs(User user, String url, String json) throws Exception {
		return mockMvc.perform(as(user, patch(url).contentType(MediaType.APPLICATION_JSON).content(json)));
	}

	protected ResultActions patchAs(User user, String url) throws Exception {
		return mockMvc.perform(as(user, patch(url)));
	}

	protected ResultActions deleteAs(User user, String url) throws Exception {
		return mockMvc.perform(as(user, delete(url)));
	}

	// Lee el campo "id" de la respuesta JSON (para usar lo recién creado en el siguiente paso)
	protected static Long idDe(ResultActions result) throws Exception {
		MvcResult mvcResult = result.andReturn();
		Number id = JsonPath.read(mvcResult.getResponse().getContentAsString(), "$.id");
		return id.longValue();
	}

	protected static String url(Ticket ticket, String sufijo) {
		return "/api/tickets/" + ticket.getId() + sufijo;
	}

	protected static Timestamp haceHoras(double horas) {
		return Timestamp.valueOf(LocalDateTime.now().minusMinutes(Math.round(horas * 60)));
	}

}
