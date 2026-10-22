package com.technova.campusdesk.dto.response;

import java.util.List;

/** [P4] Resultado de activar o desactivar una cuenta: estado final y tickets que se le quitaron (si era técnico). */
public record CuentaEstadoResponse(Long userId, String fullName, boolean active, List<Long> ticketsLiberados) {
}
