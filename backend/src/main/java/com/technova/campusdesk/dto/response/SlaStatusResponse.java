package com.technova.campusdesk.dto.response;

public record SlaStatusResponse(long minutosRestantes, boolean estaVencido, double porcentajeConsumido) {
}
