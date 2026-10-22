/**
 * CampusDesk Profile Page Controller
 * Client: TechNova Solutions
 * Renders verified user information, handles secure session sign out, and password change.
 */
'use strict';

// Shows the signed-in user's data (from the session, no extra request) and the logout button
document.addEventListener('DOMContentLoaded', () => {
  if (!Auth.requireAuth()) return;

  Navigation.render('nav-profile');

  const user = Auth.getCurrentUser();
  if (!user) return;

  const names = (user.fullName || 'User').split(' ');
  const firstName = names[0] || 'User';
  const lastName = names.slice(1).join(' ') || 'Account';
  const initials = `${firstName[0] || ''}${lastName[0] || ''}`.toUpperCase();

  document.getElementById('profile-avatar-large').textContent = initials;
  document.getElementById('profile-name-header').textContent = user.fullName;
  document.getElementById('profile-role-badge').textContent = user.role;
  document.getElementById('profile-first-name').textContent = firstName;
  document.getElementById('profile-last-name').textContent = lastName;
  document.getElementById('profile-email').textContent = user.email;
  document.getElementById('profile-role-text').textContent = `${user.role} Authority Access`;

  document.getElementById('profile-logout-btn')?.addEventListener('click', () => {
    Auth.logout();
  });

  // Modal de Cambio de Contraseña
  const pwdModal = document.getElementById('change-password-modal');
  const openPwdBtn = document.getElementById('profile-change-password-btn');
  const closePwdBtn = document.getElementById('change-pwd-close-btn');
  const cancelPwdBtn = document.getElementById('change-pwd-cancel-btn');
  const pwdForm = document.getElementById('change-password-form');
  const pwdAlert = document.getElementById('change-pwd-alert');
  const submitBtn = document.getElementById('change-pwd-submit-btn');
  const btnText = document.getElementById('change-pwd-btn-text');
  const btnSpinner = document.getElementById('change-pwd-spinner');

  const currentInput = document.getElementById('change-pwd-current');
  const newInput = document.getElementById('change-pwd-new');
  const confirmInput = document.getElementById('change-pwd-confirm');

  function getI18n(key, fallback) {
    return window.I18n ? window.I18n.t(key, fallback) : fallback;
  }

  function openPasswordModal() {
    pwdForm.reset();
    hideAlert();
    clearFieldErrors();
    // Restablecer tipo de inputs a password
    [currentInput, newInput, confirmInput].forEach(inp => {
      inp.type = 'password';
    });
    pwdModal.querySelectorAll('.password-toggle-btn').forEach(btn => {
      btn.textContent = getI18n('show_password', 'Show');
    });

    pwdModal.classList.add('active');
    pwdModal.setAttribute('aria-hidden', 'false');

    if (window.I18n) {
      window.I18n.translateDOM(pwdModal);
    }

    setTimeout(() => currentInput.focus(), 100);
  }

  function closePasswordModal() {
    pwdModal.classList.remove('active');
    pwdModal.setAttribute('aria-hidden', 'true');
    hideAlert();
    clearFieldErrors();
  }

  function showAlert(msg) {
    pwdAlert.textContent = msg;
    pwdAlert.classList.remove('d-none');
  }

  function hideAlert() {
    pwdAlert.textContent = '';
    pwdAlert.classList.add('d-none');
  }

  function clearFieldErrors() {
    ['change-pwd-current-error', 'change-pwd-new-error', 'change-pwd-confirm-error'].forEach(id => {
      const el = document.getElementById(id);
      if (el) {
        el.textContent = '';
        el.classList.add('d-none');
      }
    });
  }

  function showFieldError(errorId, message) {
    const el = document.getElementById(errorId);
    if (el) {
      el.textContent = message;
      el.classList.remove('d-none');
    }
  }

  openPwdBtn?.addEventListener('click', openPasswordModal);
  closePwdBtn?.addEventListener('click', closePasswordModal);
  cancelPwdBtn?.addEventListener('click', closePasswordModal);

  // Cerrar al hacer clic fuera del modal
  pwdModal?.addEventListener('click', (e) => {
    if (e.target === pwdModal) closePasswordModal();
  });

  // Cerrar con Escape
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && pwdModal?.classList.contains('active')) {
      closePasswordModal();
    }
  });

  // Alternar visibilidad de contraseña (Ver / Ocultar)
  pwdModal?.querySelectorAll('.password-toggle-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      const targetId = btn.getAttribute('data-target');
      const input = document.getElementById(targetId);
      if (input) {
        const isPassword = input.type === 'password';
        input.type = isPassword ? 'text' : 'password';
        btn.textContent = isPassword
          ? getI18n('hide_password', 'Hide')
          : getI18n('show_password', 'Show');
      }
    });
  });

  // Procesar envío del formulario
  pwdForm?.addEventListener('submit', async (e) => {
    e.preventDefault();
    hideAlert();
    clearFieldErrors();

    const currentPassword = currentInput.value.trim();
    const newPassword = newInput.value.trim();
    const confirmPassword = confirmInput.value.trim();

    // Validaciones del lado del cliente
    let hasError = false;

    if (!currentPassword) {
      showFieldError('change-pwd-current-error', getI18n('password_required_error', 'Please enter your current password.'));
      hasError = true;
    }

    if (!newPassword) {
      showFieldError('change-pwd-new-error', getI18n('password_required_error', 'Please enter a new password.'));
      hasError = true;
    } else if (newPassword.length < 8 || newPassword.length > 72) {
      showFieldError('change-pwd-new-error', getI18n('password_error', 'Password must be between 8 and 72 characters.'));
      hasError = true;
    } else if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/.test(newPassword)) {
      showFieldError('change-pwd-new-error', getI18n('password_hint', 'At least 8 characters, with 1 uppercase, 1 lowercase, and 1 number.'));
      hasError = true;
    } else if (newPassword === currentPassword) {
      showFieldError('change-pwd-new-error', getI18n('password_identical_error', 'New password cannot be identical to current password.'));
      hasError = true;
    }

    if (!confirmPassword) {
      showFieldError('change-pwd-confirm-error', getI18n('confirm_password_error', 'Please confirm your new password.'));
      hasError = true;
    } else if (newPassword && newPassword !== confirmPassword) {
      showFieldError('change-pwd-confirm-error', getI18n('confirm_password_error', 'Passwords do not match.'));
      hasError = true;
    }

    if (hasError) return;

    // Estado cargando
    submitBtn.disabled = true;
    btnSpinner.classList.remove('d-none');
    btnText.textContent = getI18n('updating_password', 'Updating...');

    try {
      await Auth.changePassword(currentPassword, newPassword, confirmPassword);
      closePasswordModal();
      if (typeof Toast !== 'undefined') {
        Toast.success(
          getI18n('password_updated_title', 'Password Updated'),
          getI18n('password_updated_success', 'Your password has been changed successfully.')
        );
      }
    } catch (err) {
      const msg = err.message || getI18n('general_error', 'Could not update password.');
      showAlert(msg);
    } finally {
      submitBtn.disabled = false;
      btnSpinner.classList.add('d-none');
      btnText.textContent = getI18n('update_password', 'Update Password');
    }
  });

  if (window.I18n) {
    I18n.translatePage();
  }
});
