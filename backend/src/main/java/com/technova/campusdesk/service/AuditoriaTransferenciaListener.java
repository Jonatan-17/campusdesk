package com.technova.campusdesk.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.technova.campusdesk.entity.Comment;
import com.technova.campusdesk.repository.CommentRepository;
import com.technova.campusdesk.repository.TicketRepository;
import com.technova.campusdesk.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * [T3] "Evento de auditoría transaccional": escribe la nota automática de la transferencia en los comentarios.
 *
 * @TransactionalEventListener solo se ejecuta si el evento se publicó dentro de una transacción, y en la FASE
 * que se le indique:
 * - BEFORE_COMMIT (la usada aquí): corre dentro de la MISMA transacción, justo antes del commit. Si guardar el
 *   comentario falla, se deshace también la transferencia → transferencia y nota son atómicas.
 * - AFTER_COMMIT (la de por defecto): corre cuando la transferencia ya está guardada. Sirve para cosas externas
 *   (enviar un correo), pero para escribir en la base necesitaría su propia transacción (REQUIRES_NEW) y,
 *   si fallara, la transferencia quedaría guardada sin su nota.
 * - Si la transacción hace rollback, este listener ni se ejecuta: no queda una nota de algo que no pasó.
 *
 * Ventaja frente a llamar commentRepository directamente desde TransferenciaService: la auditoría queda
 * desacoplada. Mañana se puede agregar otro listener (correo, notificación) sin tocar el service.
 */
@Component
@RequiredArgsConstructor
public class AuditoriaTransferenciaListener {

	private final CommentRepository commentRepository;
	private final TicketRepository ticketRepository;
	private final UserRepository userRepository;

	@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
	public void registrarNota(TicketTransferidoEvent event) {
		Comment nota = new Comment();
		// getReferenceById no hace SELECT: crea un proxy con el id, suficiente para llenar la FK
		nota.setTicket(ticketRepository.getReferenceById(event.ticketId()));
		/*
		 * comments.author_id es NOT NULL, así que una "nota del sistema" necesita un autor. Se usa quien hizo
		 * la transferencia, con un prefijo que la distingue. Alternativa: un campo boolean "sistema" en Comment.
		 * OJO: no se usa CommentService.addComment: comprueba acceso al ticket, y el técnico que transfiere
		 * acaba de ser desvinculado, así que ya no tendría acceso y recibiría 403.
		 */
		nota.setAuthor(userRepository.getReferenceById(event.autorId()));
		nota.setContent("[Transferencia automática] Categoría " + event.categoriaAnterior() + " → "
				+ event.categoriaNueva() + ". Motivo: " + event.justificacion());
		commentRepository.save(nota);
	}

}
