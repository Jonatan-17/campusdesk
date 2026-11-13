# Extensión individual: SLA por prioridad

Autor: Jonatan Sarmiento

## Qué se agregó

- **Prioridad con SLA:** cada ticket se clasifica como `LOW`, `MEDIUM`, `HIGH` o `CRITICAL`. El enum `Priority` guarda las horas de SLA de cada nivel (`horasSla`).
- **Fecha límite automática:** al crear un ticket se calcula su fecha límite según la prioridad. Si la prioridad cambia, la fecha se recalcula contando desde la creación del ticket.
- **Tickets vencidos:** `GET /api/tickets/vencidos` devuelve los tickets que pasaron su fecha límite sin cerrarse.
- **Estados con categoría y descripción:** el endpoint de estados ahora devuelve también la categoría y una descripción de cada estado.

## Archivos principales

`Priority`, `Ticket.recalcularFechaLimite`, `TicketService.updateTicket`, `SlaService`, `SlaController` y `data.sql`.

## A tener en cuenta

- La fecha límite se recalcula cuando cambia la prioridad, siempre contando desde la fecha de creación.
- Los `INSERT` directos de `data.sql` no pasan por `@PrePersist`, así que esos registros traen la fecha límite calculada en el propio script.

## Pendiente

No se alcanzaron a terminar todas las especificaciones planteadas para esta extensión; lo implementado es la prioridad con SLA, la consulta de vencidos y la ampliación del endpoint de estados.
