package com.technova.campusdesk.dto.request;

import com.technova.campusdesk.entity.enums.Category;
import com.technova.campusdesk.entity.enums.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body para crear (POST) y editar (PUT) un ticket. Las anotaciones validan los datos antes de llegar
 * al servicio: si algo falla, GlobalExceptionHandler responde 400 con el error de cada campo.
 */
public record TicketRequest(
    @NotBlank(message = "El título es obligatorio")
    @Size(min = 5, max = 150, message = "El título debe tener entre 5 y 150 caracteres")
    String title,

    @NotBlank(message = "La descripción es obligatoria")
    @Size(min = 10, max = 2000, message = "La descripción debe tener entre 10 y 2000 caracteres")
    String description,

    @NotNull(message = "La categoría es obligatoria")
    Category category,

    @NotNull(message = "La prioridad es obligatoria")
    Priority priority
) {}