package com.technova.campusdesk.api;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.technova.campusdesk.entity.Ticket;
import com.technova.campusdesk.entity.User;
import com.technova.campusdesk.entity.enums.Category;
import com.technova.campusdesk.entity.enums.Priority;
import com.technova.campusdesk.entity.enums.Role;
import com.technova.campusdesk.entity.enums.TicketStatus;
import com.technova.campusdesk.repository.CommentRepository;
import com.technova.campusdesk.repository.StatusHistoryRepository;
import com.technova.campusdesk.repository.TicketRepository;
import com.technova.campusdesk.repository.UserRepository;
import com.technova.campusdesk.security.JwtService;
import com.technova.campusdesk.security.UserPrincipal;

/**
 * Base de las pruebas de la API de punta a punta, según docs/api-contract.md:
 * petición HTTP → JWT → seguridad → controller → service → repositorio → H2 (perfil test).
 * Dueño: Persona 4.
 *
 * Cada prueba empieza con estos datos: un ADMIN, dos técnicos (Luis y Carla), dos USER (Ana y Pedro)
 * y tres tickets (uno OPEN de Ana, uno ASSIGNED de Ana a Luis y uno IN_PROGRESS de Pedro con Carla).
 *
 * No es @Transactional a propósito: así se detectan errores que solo aparecen fuera de una
 * transacción, como LazyInitializationException al mapear a DTO.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class ApiIntegrationTestSupport {

	@Autowired
	protected MockMvc mockMvc;

	@Autowired
	protected JwtService jwtService;

	@Autowired
	protected UserRepository userRepository;

	@Autowired
	protected TicketRepository ticketRepository;

	@Autowired
	protected CommentRepository commentRepository;

	@Autowired
	protected StatusHistoryRepository statusHistoryRepository;

	protected User admin;
	protected User luis;
	protected User carla;
	protected User ana;
	protected User pedro;

	protected Ticket anaOpen;
	protected Ticket anaAssignedToLuis;
	protected Ticket pedroInProgressWithCarla;

	@BeforeEach
	void setUpData() {
		commentRepository.deleteAll();
		statusHistoryRepository.deleteAll();
		ticketRepository.deleteAll();
		userRepository.deleteAll();

		admin = saveUser("Admin", "admin@technova.com", Role.ADMIN);
		luis = saveUser("Luis Gómez", "tech1@technova.com", Role.TECHNICIAN);
		carla = saveUser("Carla Ruiz", "tech2@technova.com", Role.TECHNICIAN);
		ana = saveUser("Ana Pérez", "ana@technova.com", Role.USER);
		pedro = saveUser("Pedro Díaz", "pedro@technova.com", Role.USER);

		anaOpen = saveTicket("No enciende el monitor", Category.HARDWARE, Priority.HIGH, TicketStatus.OPEN, ana, null);
		anaAssignedToLuis = saveTicket("Sin internet en el laboratorio", Category.NETWORK, Priority.CRITICAL,
				TicketStatus.ASSIGNED, ana, luis);
		pedroInProgressWithCarla = saveTicket("Error al abrir Office", Category.SOFTWARE, Priority.HIGH,
				TicketStatus.IN_PROGRESS, pedro, carla);
	}

	// ---------- utilidades ----------

	protected MockHttpServletRequestBuilder as(User user, MockHttpServletRequestBuilder request) {
		return request.header("Authorization", "Bearer " + jwtService.generateToken(new UserPrincipal(user)));
	}

	protected User saveUser(String fullName, String email, Role role) {
		return userRepository.save(User.builder()
				.fullName(fullName)
				.email(email)
				.password("$2a$10$hashDePruebaNoEsUnaContrasenaReal")
				.role(role)
				.build());
	}

	protected Ticket saveTicket(String title, Category category, Priority priority, TicketStatus status,
			User requester, User technician) {
		return ticketRepository.save(Ticket.builder()
				.title(title)
				.description("Descripción de prueba: " + title)
				.category(category)
				.priority(priority)
				.status(status)
				.requester(requester)
				.technician(technician)
				.build());
	}

	protected static String ticketJson(String title, String description, String category, String priority) {
		return """
				{"title":"%s","description":"%s","category":"%s","priority":"%s"}"""
				.formatted(title, description, category, priority);
	}

}
