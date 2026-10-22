package com.technova.campusdesk.entity.enums;

/**
 * Urgencia del ticket, de menor a mayor. Se guarda como texto (EnumType.STRING).
 * [P7] Cada prioridad trae las horas de su SLA: un enum puede tener campos y constructor como una clase.
 * Como se guarda por nombre (STRING), agregar este campo no cambia nada en la base.
 */
public enum Priority {
	LOW(48),
	MEDIUM(24),
	HIGH(8),
	CRITICAL(4);

	private final int horasSla;

	Priority(int horasSla) {
		this.horasSla = horasSla;
	}

	public int getHorasSla() {
		return horasSla;
	}
}
