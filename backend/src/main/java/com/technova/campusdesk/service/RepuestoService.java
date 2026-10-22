package com.technova.campusdesk.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.technova.campusdesk.dto.request.RepuestosRequest;
import com.technova.campusdesk.dto.response.RepuestoResponse;
import com.technova.campusdesk.dto.response.TicketResponse;
import com.technova.campusdesk.entity.Producto;
import com.technova.campusdesk.entity.Ticket;
import com.technova.campusdesk.entity.TicketRepuesto;
import com.technova.campusdesk.entity.User;
import com.technova.campusdesk.entity.enums.TicketStatus;
import com.technova.campusdesk.exception.BusinessRuleException;
import com.technova.campusdesk.exception.ForbiddenException;
import com.technova.campusdesk.exception.ResourceNotFoundException;
import com.technova.campusdesk.repository.ProductoRepository;
import com.technova.campusdesk.repository.TicketRepuestoRepository;

import lombok.RequiredArgsConstructor;

/** [P1] Repuestos del inventario consumidos al atender un ticket. */
@Service
@RequiredArgsConstructor
public class RepuestoService {

	private final TicketService ticketService;
	private final ProductoRepository productoRepository;
	private final TicketRepuestoRepository repuestoRepository;

	/*
	 * TODO O NADA: @Transactional hace que, si un producto de la lista falla (no existe o no hay stock), se deshagan
	 * también los descuentos de los productos anteriores de la misma lista.
	 * TRAMPA: Spring solo hace rollback automático con RuntimeException (y Error). Con una excepción "checked"
	 * (extends Exception) haría COMMIT de lo que alcanzó a hacer, salvo que se ponga rollbackFor.
	 * Las excepciones del proyecto extienden RuntimeException, así que aquí funciona.
	 */
	@Transactional
	public List<RepuestoResponse> registrar(Long ticketId, RepuestosRequest request, User currentUser) {
		Ticket ticket = ticketService.getAccessibleTicket(ticketId, currentUser);

		if (!TicketService.isAssignedTechnician(ticket, currentUser)) {
			throw new ForbiddenException("Solo el técnico asignado puede registrar repuestos en este ticket.");
		}
		if (ticket.getStatus() != TicketStatus.IN_PROGRESS) {
			throw new BusinessRuleException("Solo se registran repuestos en tickets IN_PROGRESS.");
		}

		// TRAMPA: si el mismo producto viene dos veces ([{1, 3}, {1, 4}]), hay que sumar antes de validar el stock.
		// Validando cada línea por separado, con stock 5 pasarían las dos (3 ≤ 5 y 4 ≤ 5) y el stock quedaría en -2.
		Map<Long, Integer> cantidadPorProducto = new LinkedHashMap<>();
		for (RepuestosRequest.Item item : request.items()) {
			cantidadPorProducto.merge(item.productoId(), item.cantidad(), Integer::sum);
		}

		List<TicketRepuesto> registrados = cantidadPorProducto.entrySet().stream().map(entry -> {
			Producto producto = productoRepository.findById(entry.getKey())
					.orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + entry.getKey()));
			int cantidad = entry.getValue();

			if (producto.getStock() < cantidad) {
				// Al lanzar esto se deshace TODO lo anterior de esta petición
				throw new BusinessRuleException("Stock insuficiente de \"" + producto.getNombre() + "\": hay "
						+ producto.getStock() + " y se piden " + cantidad + ".");
			}

			/*
			 * El UPDATE de stock sale al hacer commit e incluye "AND version = ?" (@Version en Producto).
			 * Si otro técnico descontó el mismo producto mientras tanto → 409, y esta transacción se deshace entera.
			 * Alternativa sin @Version: un UPDATE atómico en el repositorio,
			 * "UPDATE Producto p SET p.stock = p.stock - :c WHERE p.id = :id AND p.stock >= :c", y si devuelve 0 filas → 409.
			 */
			producto.setStock(producto.getStock() - cantidad);

			return repuestoRepository.save(TicketRepuesto.builder()
					.ticket(ticket)
					.producto(producto)
					.tecnico(currentUser)
					.cantidad(cantidad)
					.build());
		}).toList();

		return registrados.stream().map(this::toResponse).toList();
	}

	@Transactional(readOnly = true)
	public List<RepuestoResponse> listar(Long ticketId, User currentUser) {
		Ticket ticket = ticketService.getAccessibleTicket(ticketId, currentUser);
		return repuestoRepository.findByTicketIdOrderByFechaRegistroAscIdAsc(ticket.getId()).stream()
				.map(this::toResponse)
				.toList();
	}

	private RepuestoResponse toResponse(TicketRepuesto r) {
		return new RepuestoResponse(r.getId(), r.getProducto().getId(), r.getProducto().getNombre(), r.getCantidad(),
				r.getProducto().getStock(),
				new TicketResponse.UserSummary(r.getTecnico().getId(), r.getTecnico().getFullName()),
				r.getFechaRegistro());
	}

}
