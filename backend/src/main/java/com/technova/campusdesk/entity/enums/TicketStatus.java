package com.technova.campusdesk.entity.enums;

/**
 * Estados del ciclo de vida de un ticket (RF-04): OPEN → ASSIGNED → IN_PROGRESS → RESOLVED → CLOSED.
 * Las transiciones permitidas y quién puede hacerlas están en TicketTransitionPolicy.
 */
public enum TicketStatus {
	// Recién creado, sin técnico
	OPEN,
	// El ADMIN le asignó un técnico (se puede reasignar en este estado)
	ASSIGNED,
	// El técnico asignado lo está atendiendo
	IN_PROGRESS,
	// El técnico lo dio por resuelto; espera la confirmación del solicitante
	RESOLVED,
	// El solicitante confirmó el cierre: ya no se modifica ni se comenta
	CLOSED
}
