## EVIDENCIAS 
-**cambios** lo que agregue como combios la proyecto son un modleo de datos que al momento de crear un ticket los clasifique como prioridad "low,medium,hihg,critical" para su gestion de tickets en la cual se implkementa la funcion de priorizar por tiempo y poner fecha limite con su respectiva categoria 



agregamos la funcion  de la fexha limite  **Endpoint:** `GET /api/tickets/vencidos`.
- **Archivos:** `Priority` (enum con campo `horasSla`), `Ticket.recalcularFechaLimite`, `TicketService.updateTicket`, `SlaService`, `SlaController`, `data.sql`.
- **Trampas:** recalcular cuando cambia la prioridad, contando desde la creación; los INSERT directos de `data.sql` no pasan por `@PrePersist`.

no alcance a terminar las epscificacione spero agrgeo la funcion de la prioridad y tambien se modifico se get appi su get status con su catewgoria y descripcion 
