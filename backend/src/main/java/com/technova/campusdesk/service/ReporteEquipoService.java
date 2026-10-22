package com.technova.campusdesk.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.technova.campusdesk.dto.response.CargaTecnicoResponse;
import com.technova.campusdesk.dto.response.SatisfaccionTecnicoResponse;
import com.technova.campusdesk.entity.enums.Role;
import com.technova.campusdesk.repository.TicketRepository;
import com.technova.campusdesk.repository.ValoracionRepository;

import lombok.RequiredArgsConstructor;

/**
 * [P2][P3] Reportes del equipo técnico para el ADMIN. Todo el cálculo (AVG, COUNT, SUM, GROUP BY) lo hace
 * la base en una consulta; aquí no se recorre ninguna lista para sumar.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReporteEquipoService {

	private final ValoracionRepository valoracionRepository;
	private final TicketRepository ticketRepository;

	// [P2] Promedio de satisfacción por técnico
	public List<SatisfaccionTecnicoResponse> satisfaccionPorTecnico() {
		return valoracionRepository.satisfaccionPorTecnico(Role.TECHNICIAN);
	}

	// [P3] Tickets activos por técnico frente al límite
	public List<CargaTecnicoResponse> cargaPorTecnico() {
		return ticketRepository.cargaPorTecnico(Role.TECHNICIAN);
	}

}
