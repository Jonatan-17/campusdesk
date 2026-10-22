/**
 * CampusDesk Centralized API Client & Session Storage
 * Client: TechNova Solutions
 *
 * Every piece of data comes from the Spring Boot REST API (http://localhost:8081/api), which stores it
 * in PostgreSQL. Nothing is simulated or persisted in the browser: if the backend is unreachable the
 * request fails and a persistent banner tells the user.
 *
 * The only thing kept in the browser is the session (JWT and basic profile of the signed-in user), in
 * sessionStorage: it is required to call protected endpoints, it is per tab and it is deleted when the
 * tab is closed or the user signs out.
 */
'use strict';

// Spring Boot REST API Base URL
const API_BASE_URL = window.CAMPUSDESK_API_URL || 'http://localhost:8081/api';

/**
 * Session storage for the JWT and the signed-in user profile (id, fullName, email, role)
 */
const StorageService = {
  TOKEN_KEY: 'campusdesk_token',
  USER_KEY: 'campusdesk_user',

  getToken() {
    return sessionStorage.getItem(this.TOKEN_KEY);
  },

  setToken(token) {
    if (token) sessionStorage.setItem(this.TOKEN_KEY, token);
    else sessionStorage.removeItem(this.TOKEN_KEY);
  },

  getUser() {
    try {
      const data = sessionStorage.getItem(this.USER_KEY);
      return data ? JSON.parse(data) : null;
    } catch (e) {
      return null;
    }
  },

  setUser(user) {
    if (user) sessionStorage.setItem(this.USER_KEY, JSON.stringify(user));
    else sessionStorage.removeItem(this.USER_KEY);
  },

  clearSession() {
    sessionStorage.removeItem(this.TOKEN_KEY);
    sessionStorage.removeItem(this.USER_KEY);
  }
};

/**
 * Removes data that older versions of CampusDesk left in localStorage (offline mode, session,
 * notifications and language). Only CampusDesk keys are touched.
 */
(function removeLegacyLocalData() {
  try {
    [
      'campusdesk_token', 'campusdesk_user',
      'campusdesk_local_users', 'campusdesk_local_tickets', 'campusdesk_local_comments', 'campusdesk_local_history',
      'campusdesk_notifications', 'campusdesk_lang'
    ].forEach(key => localStorage.removeItem(key));
  } catch (e) {
    // Storage blocked by the browser: nothing to clean
  }
})();

/**
 * Persistent banner (plus one toast) shown while the Spring Boot backend is unreachable.
 * Shown only once even if the page makes several requests; removed when the backend answers again.
 */
function showBackendOfflineAlert() {
  if (document.getElementById('backend-offline-banner')) return;

  const banner = document.createElement('div');
  banner.id = 'backend-offline-banner';
  banner.className = 'backend-offline-alert-banner';
  banner.setAttribute('role', 'alert');
  banner.innerHTML = `
    <div class="backend-offline-content">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
        <line x1="12" y1="9" x2="12" y2="13"/>
        <line x1="12" y1="17" x2="12.01" y2="17"/>
      </svg>
      <span><strong>Connection error:</strong> cannot connect to the backend at <code>${API_BASE_URL}</code>. Make sure the Spring Boot service is running.</span>
    </div>
    <button type="button" class="backend-offline-close" aria-label="Close alert">&times;</button>
  `;
  banner.querySelector('.backend-offline-close').addEventListener('click', () => banner.remove());
  document.body.prepend(banner);

  if (typeof Toast !== 'undefined') {
    Toast.error('Server unavailable', `Cannot connect to the CampusDesk server at ${API_BASE_URL}.`);
  }
}

function hideBackendOfflineAlert() {
  document.getElementById('backend-offline-banner')?.remove();
}

/**
 * Primary API request dispatcher: adds the JWT, parses the JSON response and turns API errors
 * (ErrorResponse) into exceptions with a readable message and the HTTP status.
 */
async function apiFetch(endpoint, options = {}) {
  const url = `${API_BASE_URL}${endpoint.startsWith('/') ? endpoint : '/' + endpoint}`;
  const token = StorageService.getToken();

  const headers = {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
    ...(options.headers || {})
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const config = {
    ...options,
    headers
  };

  try {
    const response = await fetch(url, config);
    hideBackendOfflineAlert();

    // Handle 401 Unauthorized
    if (response.status === 401) {
      if (endpoint.includes('/auth/login')) {
        const errorData = await response.json().catch(() => ({}));
        const msg = errorData.message || errorData.error || 'Invalid email or password.';
        const err = new Error(msg);
        err.status = 401;
        throw err;
      }

      StorageService.clearSession();
      if (typeof Toast !== 'undefined') {
        Toast.error('Session Expired', 'Your session has expired. Please sign in again.');
      }
      setTimeout(() => {
        const isPages = window.location.pathname.includes('/pages/');
        window.location.href = isPages ? 'login.html' : 'pages/login.html';
      }, 1200);
      const err = new Error('Your session has expired. Please sign in again.');
      err.status = 401;
      throw err;
    }

    // Handle 403 Forbidden
    if (response.status === 403) {
      const errorData = await response.json().catch(() => ({}));
      const msg = errorData.message || 'You do not have permission to perform this action.';
      if (typeof Toast !== 'undefined') {
        Toast.error('Access Denied', msg);
      }
      const err = new Error(msg);
      err.status = 403;
      throw err;
    }

    // Parse JSON response body
    const text = await response.text();
    const responseData = text ? JSON.parse(text) : {};

    if (!response.ok) {
      let errorMsg = responseData.message || responseData.error || `HTTP error ${response.status}`;

      if (responseData.errors && typeof responseData.errors === 'object') {
        const fieldErrors = Object.entries(responseData.errors)
          .map(([field, msg]) => `${field}: ${msg}`)
          .join(', ');
        if (fieldErrors) {
          errorMsg = `${errorMsg} (${fieldErrors})`;
        }
      }

      const err = new Error(errorMsg);
      err.status = response.status;
      err.data = responseData;
      throw err;
    }

    return responseData;

  } catch (error) {
    // Backend unreachable (stopped, wrong port or CORS): show it instead of hiding it
    const isNetworkFailure = error.name === 'TypeError' && !error.status;
    if (isNetworkFailure) {
      showBackendOfflineAlert();
      const err = new Error('Cannot connect to the CampusDesk server. Check that the backend is running at ' + API_BASE_URL + '.');
      err.status = 0;
      throw err;
    }

    throw error;
  }
}
