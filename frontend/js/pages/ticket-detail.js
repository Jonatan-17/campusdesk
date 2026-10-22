/**
 * CampusDesk Ticket Detail Controller
 * Client: TechNova Solutions
 * Controls incident inspection, lifecycle step progression, comments thread,
 * technician assignment dialogs, immutable audit logs, and multi-language support.
 */
'use strict';

let currentTicket = null;

// Translates a fixed UI text with I18n (falls back to the English text)
const tr = (text) => (typeof I18n !== 'undefined' ? I18n.t(text, text) : text);
let currentTicketId = null;

document.addEventListener('DOMContentLoaded', async () => {
  if (!Auth.requireAuth()) return;

  Navigation.render('nav-tickets');

  const params = new URLSearchParams(window.location.search);
  currentTicketId = params.get('id');

  if (!currentTicketId) {
    Toast.error('Invalid Request', 'No incident ticket ID specified.');
    setTimeout(() => { window.location.href = 'tickets.html'; }, 1000);
    return;
  }

  await loadTicketDetails();

  // Bind comment submission
  const postCommentBtn = document.getElementById('post-comment-btn');
  if (postCommentBtn) {
    postCommentBtn.addEventListener('click', handleAddComment);
  }
});

/**
 * Fetch and render comprehensive ticket data
 */
async function loadTicketDetails() {
  const skeleton = document.getElementById('detail-skeleton');
  const container = document.getElementById('detail-container');

  skeleton.classList.remove('d-none');
  container.classList.add('d-none');

  try {
    const [ticket, comments, history] = await Promise.all([
      apiFetch(`/tickets/${currentTicketId}`),
      apiFetch(`/tickets/${currentTicketId}/comments`).catch(() => []),
      apiFetch(`/tickets/${currentTicketId}/history`).catch(() => [])
    ]);

    currentTicket = ticket;
    renderTicketOverview(ticket);
    renderLifecycleStepper(ticket.status);
    renderComments(comments, ticket.status);
    renderAuditHistory(history);
    renderRoleActions(ticket);

    skeleton.classList.add('d-none');
    container.classList.remove('d-none');

    if (typeof I18n !== 'undefined') {
      I18n.translatePage();
    }

  } catch (err) {
    skeleton.classList.add('d-none');
    Toast.error('Error Loading Ticket', err.message || 'Unable to fetch incident details.');
  }
}

/**
 * Populate ticket metadata and problem description
 */
function renderTicketOverview(ticket) {
  const formattedId = `#TK-${String(ticket.id).padStart(4, '0')}`;
  document.getElementById('ticket-header-id').textContent = `Incident ${formattedId}`;
  document.getElementById('detail-id-tag').textContent = formattedId;
  document.getElementById('detail-title').textContent = ticket.title;
  document.getElementById('detail-category-badge').textContent = ticket.category;
  document.getElementById('detail-priority-badge').innerHTML = getPriorityBadge(ticket.priority);
  document.getElementById('detail-status-badge').innerHTML = getStatusBadge(ticket.status);

  document.getElementById('detail-date-reported').textContent = `${tr('Reported')}: ${formatDate(ticket.createdAt)}`;
  document.getElementById('detail-description-body').textContent = ticket.description;

  document.getElementById('meta-requester-name').textContent = ticket.requester ? ticket.requester.fullName : 'Unknown Requester';
  document.getElementById('meta-technician-name').innerHTML = ticket.technician
    ? `<strong>${escapeHtml(ticket.technician.fullName)}</strong>`
    : `<span class="text-unassigned">${tr('Unassigned')}</span>`;
  document.getElementById('meta-created-at').textContent = formatDate(ticket.createdAt);
  document.getElementById('meta-updated-at').textContent = formatDate(ticket.updatedAt);
  document.getElementById('meta-status-badge').innerHTML = getStatusBadge(ticket.status);
}

/**
 * Update the visual lifecycle progress stepper
 */
function renderLifecycleStepper(currentStatus) {
  const steps = ['OPEN', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];
  const currentIndex = steps.indexOf(currentStatus);
  const fillPercentage = currentIndex >= 0 ? (currentIndex / (steps.length - 1)) * 100 : 0;

  const fillEl = document.getElementById('stepper-fill');
  if (fillEl) fillEl.style.width = `${fillPercentage}%`;

  steps.forEach((step, index) => {
    const el = document.getElementById(`step-${step}`);
    if (!el) return;

    el.classList.remove('active', 'completed');
    const node = el.querySelector('.step-node');

    if (index < currentIndex) {
      el.classList.add('completed');
      if (node) node.innerHTML = '✓';
    } else if (index === currentIndex) {
      el.classList.add('active');
      if (node) node.textContent = index + 1;
    } else {
      if (node) node.textContent = index + 1;
    }
  });
}

/**
 * Render conversation comments thread
 */
function renderComments(comments, status) {
  const thread = document.getElementById('comments-thread');
  const countBadge = document.getElementById('comments-count-badge');
  const inputArea = document.getElementById('comment-input-area');
  const closedNotice = document.getElementById('comment-closed-notice');

  countBadge.textContent = `${comments.length} ${tr('Comments')}`;

  // Disable commenting if ticket is CLOSED per business rules
  if (status === 'CLOSED') {
    inputArea.classList.add('d-none');
    closedNotice.classList.remove('d-none');
  } else {
    inputArea.classList.remove('d-none');
    closedNotice.classList.add('d-none');
  }

  if (comments.length === 0) {
    thread.innerHTML = `
      <div class="empty-state-desc text-center">
        No discussion notes posted yet. Add a comment below to start technical triage.
      </div>
    `;
    return;
  }

  thread.innerHTML = comments.map(c => {
    const authorName = c.author && c.author.fullName ? c.author.fullName : 'System User';
    const initials = authorName.split(' ').filter(Boolean).map(n => n[0]).slice(0, 2).join('').toUpperCase() || 'U';

    return `
      <div class="comment-card">
        <div class="comment-avatar">${escapeHtml(initials)}</div>
        <div class="w-100">
          <div class="comment-header-row">
            <div class="comment-author-name">
              <span>${escapeHtml(authorName)}</span>
            </div>
            <div class="comment-timestamp">${formatDate(c.createdAt)}</div>
          </div>
          <div class="comment-body-text">${escapeHtml(c.content)}</div>
        </div>
      </div>
    `;
  }).join('');
}

/**
 * Post new comment to the conversation
 */
async function handleAddComment() {
  const textarea = document.getElementById('new-comment-text');
  const content = textarea.value.trim();

  if (!content) {
    Toast.warning('Validation', 'Please enter comment text before submitting.');
    return;
  }

  const postBtn = document.getElementById('post-comment-btn');
  postBtn.disabled = true;

  try {
    await apiFetch(`/tickets/${currentTicketId}/comments`, {
      method: 'POST',
      body: JSON.stringify({ content })
    });

    textarea.value = '';
    Toast.success('Comment Posted', 'Your technical note was added to the conversation.');
    
    // Refresh comments and audit timeline
    const [comments, history] = await Promise.all([
      apiFetch(`/tickets/${currentTicketId}/comments`),
      apiFetch(`/tickets/${currentTicketId}/history`).catch(() => [])
    ]);
    renderComments(comments, currentTicket.status);
    renderAuditHistory(history);

  } catch (err) {
    Toast.error('Comment Failed', err.message || 'Unable to post comment.');
  } finally {
    postBtn.disabled = false;
  }
}

/**
 * Render immutable status audit timeline
 */
function renderAuditHistory(history) {
  const timeline = document.getElementById('history-timeline');

  if (history.length === 0) {
    timeline.innerHTML = `
      <div class="empty-state-desc">
        Initial intake record created with status OPEN.
      </div>
    `;
    return;
  }

  timeline.innerHTML = history.map(h => {
    const actorName = h.changedBy ? h.changedBy.fullName : 'Authorized User';
    const transitionText = h.previousStatus
      ? `${h.previousStatus} → <strong>${h.newStatus}</strong>`
      : `${tr('Initial Intake')}: <strong>${h.newStatus}</strong>`;

    return `
      <div class="history-item">
        <div class="history-date">${formatDate(h.changedAt)}</div>
        <div class="history-change">${transitionText}</div>
        <div class="history-actor">${tr('Actor')}: ${escapeHtml(actorName)}</div>
      </div>
    `;
  }).join('');
}

/**
 * Render role-specific action buttons
 */
function renderRoleActions(ticket) {
  const panel = document.getElementById('role-actions-panel');
  const user = Auth.getCurrentUser();

  if (ticket.status === 'CLOSED') {
    panel.innerHTML = `
      <div class="text-center">
        <span class="badge badge-status-closed">CLOSED</span>
        <p class="empty-state-desc">${tr('This incident is archived. No further workflow actions can be executed.')}</p>
      </div>
    `;
    return;
  }

  let actionsHtml = '';

  // ADMIN Actions: Technician Assignment / Reassignment
  // The backend only allows assigning an OPEN ticket or reassigning an ASSIGNED one (RF-04 rule 9)
  if (user.role === 'ADMIN' && (ticket.status === 'OPEN' || ticket.status === 'ASSIGNED')) {
    const isAssigned = !!ticket.technician;
    actionsHtml += `
      <div class="d-flex flex-column gap-12">
        <p class="activity-meta">
          ${isAssigned ? tr('Reassign this incident to another qualified technician.') : tr('Assign a designated specialist to triage and resolve this ticket.')}
        </p>
        <button id="btn-assign-technician" class="btn btn-primary w-100">
          ${isAssigned ? tr('Reassign Technician') : tr('Assign Technician')}
        </button>
      </div>
    `;
  }

  // TECHNICIAN Actions: Lifecycle State Progression
  if (user.role === 'TECHNICIAN' && ticket.technician && ticket.technician.id === user.id) {
    if (ticket.status === 'ASSIGNED') {
      actionsHtml += `
        <div class="d-flex flex-column gap-12">
          <p class="activity-meta">${tr('Begin active diagnostic investigation and troubleshooting.')}</p>
          <button id="btn-start-progress" class="btn btn-primary w-100">
            ${tr('Start Working (IN PROGRESS)')}
          </button>
        </div>
      `;
    } else if (ticket.status === 'IN_PROGRESS') {
      actionsHtml += `
        <div class="d-flex flex-column gap-12">
          <p class="activity-meta">${tr('Mark this ticket as resolved and await requester verification.')}</p>
          <button id="btn-resolve-ticket" class="btn btn-primary btn-resolve-ticket w-100">
            ${tr('Mark as Resolved (RESOLVED)')}
          </button>
        </div>
      `;
    }
  }

  // USER Actions: Verify and Close Ticket
  if (user.role === 'USER' && ticket.requester && ticket.requester.id === user.id) {
    if (ticket.status === 'RESOLVED') {
      actionsHtml += `
        <div class="d-flex flex-column gap-12">
          <p class="activity-meta">${tr('The technician has reported this issue as resolved. Please verify and confirm closure.')}</p>
          <button id="btn-close-ticket" class="btn btn-secondary btn-close-ticket w-100">
            ${tr('Confirm Resolution & Close (CLOSED)')}
          </button>
        </div>
      `;
    } else if (ticket.status === 'OPEN' && !ticket.technician) {
      // RF-01: the requester can edit the ticket while it is OPEN and has no technician
      actionsHtml += `
        <div class="d-flex flex-column gap-12">
          <p class="empty-state-desc">${tr('Your ticket is awaiting assignment by an administrator. You can still edit it.')}</p>
          <button id="btn-edit-ticket" class="btn btn-secondary w-100">${tr('Edit Ticket')}</button>
        </div>
      `;
    }
  }

  panel.innerHTML = actionsHtml || `
    <div class="empty-state-desc text-center">
      ${tr('No operational actions required from your role at this stage.')}
    </div>
  `;

  // Attach event handlers for actions
  document.getElementById('btn-assign-technician')?.addEventListener('click', openAssignmentModal);
  document.getElementById('btn-start-progress')?.addEventListener('click', () => updateTicketStatus('IN_PROGRESS'));
  document.getElementById('btn-resolve-ticket')?.addEventListener('click', () => updateTicketStatus('RESOLVED'));
  document.getElementById('btn-close-ticket')?.addEventListener('click', confirmCloseTicket);
  document.getElementById('btn-edit-ticket')?.addEventListener('click', openEditModal);
}

/**
 * Edit modal for the requester (PUT /api/tickets/{id}). Limits match the API contract.
 */
function openEditModal() {
  const categories = ['HARDWARE', 'SOFTWARE', 'NETWORK', 'ACCESS', 'OTHER'];
  const priorities = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
  const options = (values, selected) => values
    .map(v => `<option value="${v}" ${v === selected ? 'selected' : ''}>${v}</option>`)
    .join('');

  Modal.open({
    title: tr('Edit Ticket'),
    content: `
      <div class="form-group">
        <label class="form-label" for="edit-title">${tr('Title (5-150 characters)')}</label>
        <input id="edit-title" class="form-control" maxlength="150" value="${escapeHtml(currentTicket.title)}">
      </div>
      <div class="form-group">
        <label class="form-label" for="edit-description">${tr('Description (10-2000 characters)')}</label>
        <textarea id="edit-description" class="form-control" rows="5" maxlength="2000">${escapeHtml(currentTicket.description)}</textarea>
      </div>
      <div class="form-group">
        <label class="form-label" for="edit-category">${tr('Category')}</label>
        <select id="edit-category" class="form-select">${options(categories, currentTicket.category)}</select>
      </div>
      <div class="form-group">
        <label class="form-label" for="edit-priority">${tr('Priority')}</label>
        <select id="edit-priority" class="form-select">${options(priorities, currentTicket.priority)}</select>
      </div>
    `,
    confirmText: tr('Save Changes'),
    onConfirm: async () => {
      // Read the form before the modal closes
      const payload = {
        title: document.getElementById('edit-title').value.trim(),
        description: document.getElementById('edit-description').value.trim(),
        category: document.getElementById('edit-category').value,
        priority: document.getElementById('edit-priority').value
      };

      if (payload.title.length < 5 || payload.title.length > 150) {
        Toast.error('Invalid Title', 'The title must be between 5 and 150 characters.');
        return;
      }
      if (payload.description.length < 10 || payload.description.length > 2000) {
        Toast.error('Invalid Description', 'The description must be between 10 and 2000 characters.');
        return;
      }

      try {
        await apiFetch(`/tickets/${currentTicketId}`, {
          method: 'PUT',
          body: JSON.stringify(payload)
        });
        Toast.success('Ticket Updated', 'Your changes were saved.');
        await loadTicketDetails();
      } catch (err) {
        Toast.error('Update Error', err.message || 'The ticket could not be updated.');
      }
    }
  });
}

/**
 * Open assignment modal dialog for Administrator
 */
async function openAssignmentModal() {
  try {
    const technicians = await apiFetch('/users/technicians');
    
    if (technicians.length === 0) {
      Toast.warning('No Technicians', 'There are no active technicians available for dispatch.');
      return;
    }

    const currentTechId = currentTicket.technician ? currentTicket.technician.id : '';

    const optionsHtml = technicians.map(t => `
      <option value="${t.id}" ${t.id === currentTechId ? 'selected' : ''}>
        ${escapeHtml(t.fullName)} (${escapeHtml(t.email)})
      </option>
    `).join('');

    const modalContent = `
      <p class="activity-meta mb-16">
        ${tr('Select an authorized technician to take ownership of incident')} <strong>#TK-${String(currentTicket.id).padStart(4, '0')}</strong>:
      </p>
      <div class="form-group">
        <label class="form-label" for="modal-tech-select">${tr('Designated Technician')}</label>
        <select id="modal-tech-select" class="form-select">
          ${optionsHtml}
        </select>
      </div>
    `;

    Modal.open({
      title: currentTicket.technician ? tr('Reassign Incident Technician') : tr('Assign Incident Technician'),
      content: modalContent,
      confirmText: tr('Confirm Assignment'),
      onConfirm: async () => {
        const select = document.getElementById('modal-tech-select');
        const technicianId = parseInt(select.value, 10);
        
        try {
          await apiFetch(`/tickets/${currentTicketId}/assign`, {
            method: 'PATCH',
            body: JSON.stringify({ technicianId })
          });
          Toast.success('Assignment Updated', 'The technician was assigned to this ticket successfully.');
          await loadTicketDetails();
          if (typeof Notifications !== 'undefined') Notifications.load();
        } catch (err) {
          Toast.error('Assignment Error', err.message || 'Failed to update technician.');
        }
      }
    });

  } catch (err) {
    Toast.error('Error', 'Unable to fetch available technicians.');
  }
}

/**
 * Update ticket lifecycle status
 */
async function updateTicketStatus(newStatus) {
  try {
    await apiFetch(`/tickets/${currentTicketId}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status: newStatus })
    });
    Toast.success('Status Updated', `Incident status advanced to ${newStatus}.`);
    await loadTicketDetails();
    if (typeof Notifications !== 'undefined') Notifications.load();
  } catch (err) {
    Toast.error('Transition Error', err.message || 'Status transition not permitted.');
  }
}

/**
 * Confirmation dialog for closing resolved ticket
 */
function confirmCloseTicket() {
  Modal.open({
    title: tr('Confirm Ticket Closure'),
    content: `<p>${tr('Are you satisfied with the solution provided? Confirming closure will permanently archive this incident.')}</p>`,
    confirmText: tr('Confirm & Close'),
    cancelText: 'Cancel',
    onConfirm: async () => {
      await updateTicketStatus('CLOSED');
    }
  });
}
