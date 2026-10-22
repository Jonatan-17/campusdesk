/**
 * CampusDesk Interactive Notifications System
 * Client: TechNova Solutions
 * Real-time operational incident notifications engine:
 * - Solicitantes (USER): Notifies when a technician is assigned to their ticket, when it progresses, and when resolved.
 * - Técnicos (TECHNICIAN): Notifies when a new ticket is assigned to them, and when closed.
 * - Administradores (ADMIN): Notifies when a new incident is created, and when an incident is resolved or finalized.
 * - Persists read status in localStorage per user so state is never lost across navigation or refresh.
 * - Live polling with toast popups and bell animation on incoming events.
 */
'use strict';

const Notifications = {
  MAX_ITEMS: 15,
  items: [],
  pollingInterval: null,
  seenNotifIds: new Set(),
  isInitialLoad: true,

  getStorageKey() {
    const user = typeof Auth !== 'undefined' ? Auth.getCurrentUser() : null;
    return user ? `campusdesk_read_notifs_${user.email || user.id}` : 'campusdesk_read_notifs_guest';
  },

  getReadIds() {
    try {
      const stored = localStorage.getItem(this.getStorageKey());
      return stored ? new Set(JSON.parse(stored)) : new Set();
    } catch {
      return new Set();
    }
  },

  saveReadIds(set) {
    try {
      localStorage.setItem(this.getStorageKey(), JSON.stringify(Array.from(set)));
    } catch (e) {
      console.warn('Failed to save read notifications', e);
    }
  },

  // Loads the user's tickets (GET /api/tickets, already filtered by role) and keeps the most recently updated
  async load() {
    if (typeof apiFetch === 'undefined' || typeof StorageService === 'undefined' || !StorageService.getToken()) {
      return;
    }

    const user = typeof Auth !== 'undefined' ? Auth.getCurrentUser() : null;
    if (!user) return;

    try {
      const tickets = await apiFetch('/tickets');
      if (!Array.isArray(tickets)) {
        this.items = [];
        this.render();
        return;
      }

      const isEs = (typeof I18n !== 'undefined' && I18n.getCurrentLang() === 'es') || (navigator.language || '').startsWith('es');
      const generated = [];

      for (const t of tickets) {
        if (!t) continue;
        const reqName = t.requester?.fullName || (isEs ? 'Usuario' : 'User');
        const techName = t.technician?.fullName || (isEs ? 'Técnico asignado' : 'Assigned technician');

        // ==========================================
        // 1. NOTIFICACIONES PARA ADMIN
        // ==========================================
        if (user.role === 'ADMIN') {
          // A) Incidente nuevo creado (OPEN)
          if (t.status === 'OPEN') {
            generated.push({
              id: `notif-admin-open-${t.id}`,
              type: 'OPEN',
              typeLabel: isEs ? 'Nuevo Incidente' : 'New Incident',
              title: isEs ? 'Nuevo incidente creado' : 'New Incident Created',
              desc: isEs
                ? `${reqName} creó el ticket #${t.id}: "${t.title}". Requiere asignación técnica.`
                : `${reqName} submitted ticket #${t.id}: "${t.title}". Requires technician assignment.`,
              timestamp: new Date(t.createdAt || Date.now()).getTime(),
              time: this.timeAgo(t.createdAt),
              ticketId: t.id
            });
          }

          // B) Problema resuelto (RESOLVED)
          if (t.status === 'RESOLVED') {
            generated.push({
              id: `notif-admin-resolved-${t.id}-${t.updatedAt || ''}`,
              type: 'RESOLVED',
              typeLabel: isEs ? 'Resuelto' : 'Resolved',
              title: isEs ? 'Problema resuelto' : 'Problem Resolved',
              desc: isEs
                ? `La incidencia #${t.id} "${t.title}" fue resuelta por ${techName}. Pendiente de confirmación.`
                : `Incident #${t.id} "${t.title}" was resolved by ${techName}. Awaiting confirmation.`,
              timestamp: new Date(t.updatedAt || t.createdAt || Date.now()).getTime(),
              time: this.timeAgo(t.updatedAt || t.createdAt),
              ticketId: t.id
            });
          }

          // C) Tarea finalizada / cerrada (CLOSED)
          if (t.status === 'CLOSED') {
            generated.push({
              id: `notif-admin-closed-${t.id}-${t.updatedAt || ''}`,
              type: 'CLOSED',
              typeLabel: isEs ? 'Finalizada' : 'Finalized',
              title: isEs ? 'Tarea finalizada' : 'Task Finalized',
              desc: isEs
                ? `La incidencia #${t.id} "${t.title}" ha sido finalizada y cerrada por el solicitante.`
                : `Incident #${t.id} "${t.title}" was finalized and closed by requester.`,
              timestamp: new Date(t.updatedAt || t.createdAt || Date.now()).getTime(),
              time: this.timeAgo(t.updatedAt || t.createdAt),
              ticketId: t.id
            });
          }

          // D) Asignación registrada (ASSIGNED)
          if (t.status === 'ASSIGNED' && t.technician) {
            generated.push({
              id: `notif-admin-assigned-${t.id}-${t.technician.id}-${t.updatedAt || ''}`,
              type: 'ASSIGNED',
              typeLabel: isEs ? 'Asignado' : 'Assigned',
              title: isEs ? 'Técnico asignado' : 'Technician Assigned',
              desc: isEs
                ? `Incidencia #${t.id} asignada a ${t.technician.fullName}.`
                : `Incident #${t.id} assigned to ${t.technician.fullName}.`,
              timestamp: new Date(t.updatedAt || t.createdAt || Date.now()).getTime(),
              time: this.timeAgo(t.updatedAt || t.createdAt),
              ticketId: t.id
            });
          }
        }

        // ==========================================
        // 2. NOTIFICACIONES PARA USUARIO (USER / SOLICITANTE)
        // ==========================================
        else if (user.role === 'USER') {
          // A) Notificar cada vez que se le asigna un técnico a su tarea
          if (t.technician && (t.status === 'ASSIGNED' || t.status === 'IN_PROGRESS')) {
            generated.push({
              id: `notif-user-assigned-${t.id}-${t.technician.id}-${t.status}`,
              type: 'ASSIGNED',
              typeLabel: isEs ? 'Tarea Asignada' : 'Task Assigned',
              title: isEs ? 'Tarea asignada a un técnico' : 'Technician Assigned',
              desc: isEs
                ? `Se asignó al técnico ${t.technician.fullName} para atender tu ticket #${t.id}: "${t.title}".`
                : `Technician ${t.technician.fullName} was assigned to your ticket #${t.id}: "${t.title}".`,
              timestamp: new Date(t.updatedAt || t.createdAt || Date.now()).getTime(),
              time: this.timeAgo(t.updatedAt || t.createdAt),
              ticketId: t.id
            });
          }

          // B) Incidencia en atención / en progreso
          if (t.status === 'IN_PROGRESS' && t.technician) {
            generated.push({
              id: `notif-user-progress-${t.id}-${t.updatedAt || ''}`,
              type: 'PROGRESS',
              typeLabel: isEs ? 'En Progreso' : 'In Progress',
              title: isEs ? 'Incidencia en atención' : 'Work in Progress',
              desc: isEs
                ? `El técnico ${t.technician.fullName} ha comenzado a trabajar en tu incidencia #${t.id}.`
                : `Technician ${t.technician.fullName} has begun working on incident #${t.id}.`,
              timestamp: new Date(t.updatedAt || t.createdAt || Date.now()).getTime(),
              time: this.timeAgo(t.updatedAt || t.createdAt),
              ticketId: t.id
            });
          }

          // C) Incidencia resuelta
          if (t.status === 'RESOLVED') {
            generated.push({
              id: `notif-user-resolved-${t.id}-${t.updatedAt || ''}`,
              type: 'RESOLVED',
              typeLabel: isEs ? 'Resuelto' : 'Resolved',
              title: isEs ? 'Incidencia resuelta' : 'Incident Resolved',
              desc: isEs
                ? `El técnico ${techName} marcó como resuelta tu incidencia #${t.id}. Confirma y ciérrala.`
                : `Technician ${techName} marked incident #${t.id} as resolved. Please review and close.`,
              timestamp: new Date(t.updatedAt || t.createdAt || Date.now()).getTime(),
              time: this.timeAgo(t.updatedAt || t.createdAt),
              ticketId: t.id
            });
          }

          // D) Incidencia finalizada / cerrada
          if (t.status === 'CLOSED') {
            generated.push({
              id: `notif-user-closed-${t.id}-${t.updatedAt || ''}`,
              type: 'CLOSED',
              typeLabel: isEs ? 'Finalizada' : 'Finalized',
              title: isEs ? 'Incidencia finalizada' : 'Incident Finalized',
              desc: isEs
                ? `Tu incidencia #${t.id}: "${t.title}" ha sido finalizada y cerrada satisfactoriamente.`
                : `Your incident #${t.id}: "${t.title}" has been finalized and closed.`,
              timestamp: new Date(t.updatedAt || t.createdAt || Date.now()).getTime(),
              time: this.timeAgo(t.updatedAt || t.createdAt),
              ticketId: t.id
            });
          }
        }

        // ==========================================
        // 3. NOTIFICACIONES PARA TÉCNICO (TECHNICIAN)
        // ==========================================
        else if (user.role === 'TECHNICIAN') {
          // A) Notificar cuando se le asigna una nueva tarea
          if (t.status === 'ASSIGNED' || t.status === 'IN_PROGRESS') {
            generated.push({
              id: `notif-tech-assigned-${t.id}-${t.updatedAt || t.createdAt}`,
              type: 'ASSIGNED',
              typeLabel: isEs ? 'Nueva Asignación' : 'New Assignment',
              title: isEs ? 'Nueva tarea asignada' : 'New Task Assigned',
              desc: isEs
                ? `El administrador te asignó la incidencia #${t.id}: "${t.title}" (Solicitante: ${reqName}).`
                : `Administrator assigned you incident #${t.id}: "${t.title}" (Requester: ${reqName}).`,
              timestamp: new Date(t.updatedAt || t.createdAt || Date.now()).getTime(),
              time: this.timeAgo(t.updatedAt || t.createdAt),
              ticketId: t.id
            });
          }

          // B) Tarea cerrada por el usuario solicitante
          if (t.status === 'CLOSED') {
            generated.push({
              id: `notif-tech-closed-${t.id}-${t.updatedAt || ''}`,
              type: 'CLOSED',
              typeLabel: isEs ? 'Cerrada' : 'Closed',
              title: isEs ? 'Tarea cerrada por el usuario' : 'Task Closed by User',
              desc: isEs
                ? `El usuario ${reqName} confirmó la solución y cerró la incidencia #${t.id}.`
                : `Requester ${reqName} confirmed resolution and closed incident #${t.id}.`,
              timestamp: new Date(t.updatedAt || t.createdAt || Date.now()).getTime(),
              time: this.timeAgo(t.updatedAt || t.createdAt),
              ticketId: t.id
            });
          }
        }
      }

      // Ordenar por más reciente primero
      generated.sort((a, b) => b.timestamp - a.timestamp);

      // Cargar IDs leídos desde localStorage
      const readSet = this.getReadIds();
      this.items = generated.slice(0, this.MAX_ITEMS).map(n => ({
        ...n,
        unread: !readSet.has(n.id)
      }));

      // Detectar notificaciones nuevas en tiempo real para mostrar Toast y animar campana
      if (!this.isInitialLoad) {
        const freshUnreads = this.items.filter(n => n.unread && !this.seenNotifIds.has(n.id));
        if (freshUnreads.length > 0) {
          freshUnreads.slice(0, 2).forEach(n => {
            if (typeof Toast !== 'undefined') {
              Toast.info(n.title, n.desc, 7000);
            }
          });

          const bellBtn = document.getElementById('notifications-bell-btn');
          if (bellBtn) {
            bellBtn.classList.remove('bell-ring-active');
            void bellBtn.offsetWidth;
            bellBtn.classList.add('bell-ring-active');
          }
        }
      }

      // Registrar IDs vistos en la sesión
      this.items.forEach(n => this.seenNotifIds.add(n.id));
      this.isInitialLoad = false;

    } catch (e) {
      console.warn('Failed to load notifications:', e);
    }

    this.render();
  },

  // "5 min ago" style label from an ISO date
  timeAgo(dateText) {
    if (!dateText) return '';
    const diffMs = Date.now() - new Date(dateText).getTime();
    const minutes = Math.max(0, Math.round(diffMs / 60000));
    const isEs = (typeof I18n !== 'undefined' && I18n.getCurrentLang() === 'es') || (navigator.language || '').startsWith('es');

    if (minutes < 1) return isEs ? 'Justo ahora' : 'Just now';
    if (minutes < 60) return isEs ? `Hace ${minutes} min` : `${minutes} min ago`;
    const hours = Math.round(minutes / 60);
    if (hours < 24) return isEs ? `Hace ${hours} h` : `${hours} h ago`;
    const days = Math.round(hours / 24);
    return isEs ? `Hace ${days} d` : `${days} d ago`;
  },

  // Escapes ticket titles before putting them into HTML (prevents XSS)
  escapeHtml(text) {
    return String(text ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  },

  // Items with the in-memory read state applied
  getItems() {
    return this.items;
  },

  // Number shown on the bell badge
  getUnreadCount() {
    return this.items.filter(n => n.unread).length;
  },

  // Read state is kept only in memory: nothing is stored in the browser
  markAllAsRead() {
    const readSet = this.getReadIds();
    this.items.forEach(n => {
      n.unread = false;
      readSet.add(n.id);
    });
    this.saveReadIds(readSet);
    this.render();
  },

  markAsRead(id) {
    const readSet = this.getReadIds();
    readSet.add(id);
    this.saveReadIds(readSet);
    const target = this.items.find(n => n.id === id);
    if (target) target.unread = false;
    this.render();
  },

  // Builds the bell button and its dropdown in the header, then loads the data
  mount(container) {
    if (!container) return;

    container.innerHTML = `
      <div class="notifications-wrapper" id="notifications-wrapper">
        <button class="notifications-bell-btn" id="notifications-bell-btn" aria-label="View notifications">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
            <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
          </svg>
          <span class="notifications-badge-count" id="notifications-badge-count">0</span>
        </button>

        <div class="notifications-panel" id="notifications-panel">
          <div class="notifications-header">
            <span class="notifications-title" data-i18n="notifications_title">Notifications</span>
            <button class="notifications-mark-all-btn" id="notifications-mark-all-btn" data-i18n="notifications_mark_all">Mark all as read</button>
          </div>
          <div class="notifications-list" id="notifications-list"></div>
        </div>
      </div>
    `;

    const bellBtn = container.querySelector('#notifications-bell-btn');
    const panel = container.querySelector('#notifications-panel');
    const markAllBtn = container.querySelector('#notifications-mark-all-btn');

    bellBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      panel.classList.toggle('active');
    });

    markAllBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      this.markAllAsRead();
      if (typeof Toast !== 'undefined') {
        const isEs = (typeof I18n !== 'undefined' && I18n.getCurrentLang() === 'es') || (navigator.language || '').startsWith('es');
        Toast.info(isEs ? 'Notificaciones' : 'Notifications', isEs ? 'Todas las notificaciones fueron marcadas como leídas.' : 'All notifications marked as read.');
      }
    });

    document.addEventListener('click', (e) => {
      if (!container.contains(e.target)) {
        panel.classList.remove('active');
      }
    });

    this.render();
    this.load();
    this.startPolling();
  },

  startPolling() {
    if (this.pollingInterval) clearInterval(this.pollingInterval);
    // Sondeo periódico cada 12 segundos para actualizar estado en vivo
    this.pollingInterval = setInterval(() => {
      if (typeof StorageService !== 'undefined' && StorageService.getToken()) {
        this.load();
      }
    }, 12000);
  },

  // Draws the unread badge and the list of items
  render() {
    const badge = document.getElementById('notifications-badge-count');
    const list = document.getElementById('notifications-list');
    if (!badge || !list) return;

    const items = this.getItems();
    const unreadCount = items.filter(n => n.unread).length;

    badge.textContent = unreadCount;
    if (unreadCount === 0) {
      badge.classList.add('hidden');
    } else {
      badge.classList.remove('hidden');
    }

    if (items.length === 0) {
      list.innerHTML = `<div class="notifications-empty-state" data-i18n="notifications_empty">No notifications</div>`;
      return;
    }

    list.innerHTML = items.map(n => {
      const typeClass = (n.type || '').toLowerCase();
      return `
        <div class="notification-item ${n.unread ? 'unread' : 'read'}" data-id="${this.escapeHtml(n.id)}" data-ticket-id="${n.ticketId || ''}">
          <span class="notification-dot"></span>
          <div class="notification-item-content">
            <div class="notification-meta-row">
              <span class="notification-type-badge notification-type-${typeClass}">${this.escapeHtml(n.typeLabel || n.type)}</span>
              <span class="notification-item-time">${this.escapeHtml(n.time)}</span>
            </div>
            <div class="notification-item-title">${this.escapeHtml(n.title)}</div>
            <div class="notification-item-desc">${this.escapeHtml(n.desc)}</div>
          </div>
        </div>
      `;
    }).join('');

    list.querySelectorAll('.notification-item').forEach(el => {
      el.addEventListener('click', () => {
        const id = el.getAttribute('data-id');
        const ticketId = el.getAttribute('data-ticket-id');
        this.markAsRead(id);

        if (ticketId) {
          const isPages = window.location.pathname.includes('/pages/');
          window.location.href = isPages ? `ticket-detail.html?id=${ticketId}` : `pages/ticket-detail.html?id=${ticketId}`;
        }
      });
    });
  }
};
