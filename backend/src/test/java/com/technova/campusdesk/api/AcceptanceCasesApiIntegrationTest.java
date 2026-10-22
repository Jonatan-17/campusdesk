package com.technova.campusdesk.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;

import com.technova.campusdesk.entity.Ticket;
import com.technova.campusdesk.entity.User;
import com.technova.campusdesk.entity.enums.Category;
import com.technova.campusdesk.entity.enums.Priority;
import com.technova.campusdesk.entity.enums.Role;
import com.technova.campusdesk.entity.enums.TicketStatus;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Casos de prueba obligatorios del enunciado (CP-01 a CP-16), uno por prueba y en el mismo orden.
 * Sirven para demostrarlos en la sustentación: ./gradlew test --tests '*AcceptanceCases*'
 * Datos de partida en ApiIntegrationTestSupport. La tabla de casos está en el README.
 */
class AcceptanceCasesApiIntegrationTest extends ApiIntegrationTestSupport {

	private static final String FRONTEND_ORIGIN = "http://127.0.0.1:5500";

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Value("${app.jwt.secret}")
	private String jwtSecret;

	@Test
	@DisplayName("CP-01: registrar un usuario válido crea la cuenta con rol USER")
	void cp01RegisterValidUser() throws Exception {
		// Aunque se intente enviar un rol, la cuenta siempre es USER
		register("{\"fullName\":\"Laura Mora\",\"email\":\"laura@technova.com\",\"password\":\"Laura12345\",\"role\":\"ADMIN\"}")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value("laura@technova.com"))
				.andExpect(jsonPath("$.role").value("USER"))
				.andExpect(jsonPath("$.password").doesNotExist());

		User saved = userRepository.findByEmail("laura@technova.com").orElseThrow();
		assertThat(saved.getRole()).isEqualTo(Role.USER);
		assertThat(saved.getPassword()).startsWith("$2").isNotEqualTo("Laura12345");
	}

	@Test
	@DisplayName("CP-02: registrar un correo duplicado se rechaza")
	void cp02RegisterDuplicateEmail() throws Exception {
		register("{\"fullName\":\"Otra Ana\",\"email\":\"ana@technova.com\",\"password\":\"Ana123456\"}")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409));
		// El correo se compara sin distinguir mayúsculas
		register("{\"fullName\":\"Otra Ana\",\"email\":\"ANA@TechNova.com\",\"password\":\"Ana123456\"}")
				.andExpect(status().isConflict());
	}

	@Test
	@DisplayName("CP-03: iniciar sesión correctamente devuelve un JWT válido")
	void cp03LoginReturnsValidJwt() throws Exception {
		saveUserWithPassword("Marta Ríos", "marta@technova.com", "Marta12345");

		String body = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"marta@technova.com\",\"password\":\"Marta12345\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token", not(emptyString())))
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.user.role").value("USER"))
				.andReturn().getResponse().getContentAsString();
		String token = body.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");

		// El token sirve para entrar a un endpoint protegido
		mockMvc.perform(get("/api/tickets").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"marta@technova.com\",\"password\":\"Incorrecta1\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("CP-04: un endpoint protegido sin token responde 401")
	void cp04ProtectedEndpointWithoutTokenIs401() throws Exception {
		mockMvc.perform(get("/api/tickets"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401));
		mockMvc.perform(get("/api/reports/summary")).andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("CP-05: crear un ticket lo deja en estado OPEN")
	void cp05CreateTicketStartsOpen() throws Exception {
		mockMvc.perform(as(ana, post("/api/tickets").contentType(MediaType.APPLICATION_JSON)
				.content(ticketJson("Teclado sin respuesta", "El teclado del puesto 7 no responde", "HARDWARE", "MEDIUM"))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("OPEN"))
				.andExpect(jsonPath("$.requester.fullName").value("Ana Pérez"));
	}

	@Test
	@DisplayName("CP-06: un USER que consulta un ticket ajeno recibe 403")
	void cp06UserCannotSeeOthersTicket() throws Exception {
		mockMvc.perform(as(ana, get("/api/tickets/" + pedroInProgressWithCarla.getId())))
				.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("CP-07: el ADMIN asigna un técnico y el ticket pasa a ASSIGNED")
	void cp07AdminAssignsTechnician() throws Exception {
		assign(admin, anaOpen, luis)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ASSIGNED"))
				.andExpect(jsonPath("$.technician.fullName").value("Luis Gómez"));
	}

	@Test
	@DisplayName("CP-08: un USER que intenta asignar recibe 403")
	void cp08UserCannotAssign() throws Exception {
		assign(ana, anaOpen, luis).andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("CP-09: un técnico no puede modificar un ticket que no tiene asignado")
	void cp09TechnicianCannotModifyOthersTicket() throws Exception {
		changeStatus(carla, anaAssignedToLuis, "IN_PROGRESS").andExpect(status().isForbidden());
		mockMvc.perform(as(carla, put("/api/tickets/" + anaAssignedToLuis.getId()).contentType(MediaType.APPLICATION_JSON)
				.content(ticketJson("Cambio no autorizado", "Un técnico ajeno intenta editarlo", "NETWORK", "LOW"))))
				.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("CP-10: resolver un ticket sin pasar por IN_PROGRESS responde 409")
	void cp10ResolveWithoutInProgressIs409() throws Exception {
		changeStatus(luis, anaAssignedToLuis, "RESOLVED")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409));
	}

	@Test
	@DisplayName("CP-11: un cambio de estado crea un registro en el historial")
	void cp11StatusChangeIsRecorded() throws Exception {
		changeStatus(luis, anaAssignedToLuis, "IN_PROGRESS").andExpect(status().isOk());

		assertThat(statusHistoryRepository.findByTicketIdOrderByChangedAtAscIdAsc(anaAssignedToLuis.getId()))
				.singleElement()
				.satisfies(h -> {
					assertThat(h.getPreviousStatus()).isEqualTo(TicketStatus.ASSIGNED);
					assertThat(h.getNewStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
					assertThat(h.getChangedBy().getId()).isEqualTo(luis.getId());
					assertThat(h.getChangedAt()).isNotNull();
				});
	}

	@Test
	@DisplayName("CP-12: el solicitante cierra su ticket RESOLVED y queda CLOSED")
	void cp12RequesterClosesResolvedTicket() throws Exception {
		Ticket resolved = saveTicket("Restablecer contraseña", Category.ACCESS, Priority.MEDIUM, TicketStatus.RESOLVED, ana, luis);

		changeStatus(ana, resolved, "CLOSED")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CLOSED"));
	}

	@Test
	@DisplayName("CP-13: comentar un ticket CLOSED se rechaza")
	void cp13CommentOnClosedTicketIsRejected() throws Exception {
		Ticket closed = saveTicket("Cable HDMI para la sala", Category.OTHER, Priority.LOW, TicketStatus.CLOSED, ana, luis);

		mockMvc.perform(as(ana, post("/api/tickets/" + closed.getId() + "/comments")
				.contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"Gracias\"}")))
				.andExpect(status().isConflict());
	}

	@Test
	@DisplayName("CP-14: las estadísticas coinciden con lo guardado en la base")
	void cp14SummaryMatchesDatabase() throws Exception {
		saveTicket("Resuelto de Pedro", Category.SOFTWARE, Priority.LOW, TicketStatus.RESOLVED, pedro, carla);
		saveTicket("Cerrado de Ana", Category.OTHER, Priority.LOW, TicketStatus.CLOSED, ana, luis);

		// ADMIN: todos los tickets
		mockMvc.perform(as(admin, get("/api/reports/summary")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.total").value(ticketRepository.count()))
				.andExpect(jsonPath("$.open").value(ticketRepository.countByStatus(TicketStatus.OPEN)))
				.andExpect(jsonPath("$.assigned").value(ticketRepository.countByStatus(TicketStatus.ASSIGNED)))
				.andExpect(jsonPath("$.inProgress").value(ticketRepository.countByStatus(TicketStatus.IN_PROGRESS)))
				.andExpect(jsonPath("$.resolved").value(ticketRepository.countByStatus(TicketStatus.RESOLVED)))
				.andExpect(jsonPath("$.closed").value(ticketRepository.countByStatus(TicketStatus.CLOSED)));

		// USER: solo los suyos (Ana tiene uno OPEN, uno ASSIGNED y uno CLOSED)
		mockMvc.perform(as(ana, get("/api/reports/summary")))
				.andExpect(jsonPath("$.total").value(3))
				.andExpect(jsonPath("$.open").value(1))
				.andExpect(jsonPath("$.assigned").value(1))
				.andExpect(jsonPath("$.closed").value(1));

		// TECHNICIAN: solo los asignados (Carla tiene uno IN_PROGRESS y uno RESOLVED)
		mockMvc.perform(as(carla, get("/api/reports/summary")))
				.andExpect(jsonPath("$.total").value(2))
				.andExpect(jsonPath("$.inProgress").value(1))
				.andExpect(jsonPath("$.resolved").value(1));
	}

	@Test
	@DisplayName("CP-15: el frontend autorizado puede consumir la API (CORS)")
	void cp15AuthorizedFrontendCanCallApi() throws Exception {
		// Petición previa (preflight) que manda el navegador desde Live Server
		mockMvc.perform(options("/api/tickets")
				.header("Origin", FRONTEND_ORIGIN)
				.header("Access-Control-Request-Method", "GET")
				.header("Access-Control-Request-Headers", "Authorization"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", FRONTEND_ORIGIN));

		mockMvc.perform(as(ana, get("/api/tickets").header("Origin", FRONTEND_ORIGIN)))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", FRONTEND_ORIGIN));

		// Un origen que no está en CORS_ALLOWED_ORIGINS se rechaza
		mockMvc.perform(options("/api/tickets")
				.header("Origin", "http://sitio-no-autorizado.com")
				.header("Access-Control-Request-Method", "GET"))
				.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("CP-16: un JWT inválido o expirado responde 401")
	void cp16InvalidOrExpiredJwtIs401() throws Exception {
		// Token mal formado
		mockMvc.perform(get("/api/tickets").header("Authorization", "Bearer esto-no-es-un-jwt"))
				.andExpect(status().isUnauthorized());

		// Token expirado, firmado con la clave correcta
		String expired = Jwts.builder()
				.subject(ana.getEmail())
				.claim("role", "USER")
				.issuedAt(new Date(System.currentTimeMillis() - 7_200_000))
				.expiration(new Date(System.currentTimeMillis() - 3_600_000))
				.signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
				.compact();
		mockMvc.perform(get("/api/tickets").header("Authorization", "Bearer " + expired))
				.andExpect(status().isUnauthorized());

		// Token vigente pero firmado con otra clave
		String forged = Jwts.builder()
				.subject(admin.getEmail())
				.claim("role", "ADMIN")
				.expiration(new Date(System.currentTimeMillis() + 3_600_000))
				.signWith(Keys.hmacShaKeyFor("otra-clave-secreta-de-al-menos-32-bytes!!".getBytes(StandardCharsets.UTF_8)))
				.compact();
		mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + forged))
				.andExpect(status().isUnauthorized());
	}

	// ---------- utilidades ----------

	private ResultActions register(String json) throws Exception {
		return mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private ResultActions assign(User user, Ticket ticket, User technician) throws Exception {
		return mockMvc.perform(as(user, patch("/api/tickets/" + ticket.getId() + "/assign")
				.contentType(MediaType.APPLICATION_JSON).content("{\"technicianId\":" + technician.getId() + "}")));
	}

	private ResultActions changeStatus(User user, Ticket ticket, String newStatus) throws Exception {
		return mockMvc.perform(as(user, patch("/api/tickets/" + ticket.getId() + "/status")
				.contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + newStatus + "\"}")));
	}

	private void saveUserWithPassword(String fullName, String email, String rawPassword) {
		userRepository.save(User.builder()
				.fullName(fullName)
				.email(email)
				.password(passwordEncoder.encode(rawPassword))
				.role(Role.USER)
				.build());
	}

}
