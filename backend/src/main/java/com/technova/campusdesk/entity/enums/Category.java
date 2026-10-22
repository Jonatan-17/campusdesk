package com.technova.campusdesk.entity.enums;

/** Tipo de incidencia. Se guarda como texto en tickets.category, restringido con CHECK en schema.sql. */
public enum Category {
	HARDWARE,
	SOFTWARE,
	NETWORK,
	ACCESS,
	OTHER
}
