/**
 * CampusDesk Login Page Controller
 * Client: TechNova Solutions
 * Manages login form submission, input validation, and redirection.
 */
'use strict';

// Login form: validates the fields, calls POST /api/auth/login (Auth.login) and goes to the dashboard
document.addEventListener('DOMContentLoaded', () => {
  // If user is already authenticated, forward to dashboard
  if (Auth.isAuthenticated()) {
    window.location.href = 'dashboard.html';
    return;
  }

  const form = document.getElementById('login-form');
  const emailInput = document.getElementById('email');
  const passwordInput = document.getElementById('password');
  const passwordToggle = document.getElementById('password-toggle');
  const submitBtn = document.getElementById('login-submit-btn');
  const btnText = document.getElementById('btn-text');
  const btnSpinner = document.getElementById('btn-spinner');
  const alertBox = document.getElementById('login-alert');

  // If redirected from registration, pre-fill newly registered email
  const prefilledEmail = sessionStorage.getItem('campusdesk_registered_email');
  if (prefilledEmail && emailInput) {
    emailInput.value = prefilledEmail;
    sessionStorage.removeItem('campusdesk_registered_email');
    if (passwordInput) {
      passwordInput.focus();
    }
  }

  // Toggle password visibility between text and password types
  if (passwordToggle && passwordInput) {
    passwordToggle.addEventListener('click', () => {
      const isPassword = passwordInput.getAttribute('type') === 'password';
      passwordInput.setAttribute('type', isPassword ? 'text' : 'password');
      passwordToggle.textContent = isPassword ? 'Hide' : 'Show';
    });
  }

  // Hides the field errors and the alert box
  function clearErrors() {
    alertBox.className = 'auth-alert-box';
    alertBox.textContent = '';
    document.getElementById('email-error')?.classList.remove('active');
    document.getElementById('password-error')?.classList.remove('active');
  }

  // Shows an error or success message above the form
  function showAlert(msg, isError = true) {
    alertBox.className = `auth-alert-box active ${isError ? 'auth-alert-error' : 'auth-alert-success'}`;
    alertBox.textContent = msg;
  }

  // Form submission handler
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    clearErrors();

    const email = emailInput.value.trim();
    const password = passwordInput.value;

    let hasError = false;

    // Validate email format
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!email || !emailRegex.test(email)) {
      document.getElementById('email-error')?.classList.add('active');
      hasError = true;
    }

    // Validate password
    if (!password) {
      document.getElementById('password-error')?.classList.add('active');
      hasError = true;
    }

    if (hasError) return;

    // Set loading state
    submitBtn.disabled = true;
    btnText.classList.add('d-none');
    btnSpinner.classList.remove('d-none');

    try {
      const user = await Auth.login(email, password);
      if (typeof Toast !== 'undefined') {
        Toast.success('Authentication Successful', `Welcome back, ${user.fullName}`);
      }
      setTimeout(() => {
        window.location.href = 'dashboard.html';
      }, 700);
    } catch (err) {
      showAlert(err.message || 'Invalid credentials. Please verify your email and password.');
      submitBtn.disabled = false;
      btnText.classList.remove('d-none');
      btnSpinner.classList.add('d-none');
    }
  });

  // Ensure current language translation is applied
  if (typeof I18n !== 'undefined') {
    I18n.translatePage();
  }
});
