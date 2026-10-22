package com.technova.campusdesk.exception;

/**
 * [T2][T3] La petición está bien formada, pero su contenido no tiene sentido para el negocio y el enunciado
 * pide 400, no 409. Por ejemplo: reabrir pasadas las 48 horas o transferir a la misma categoría.
 * Diferencia con BusinessRuleException (409): el 409 indica un conflicto con el estado actual del recurso;
 * el 400 indica que el cliente pidió algo inválido.
 */
public class BadRequestException extends RuntimeException {

	public BadRequestException(String message) {
		super(message);
	}

}
