// AUTH
function showAuthOverlay() { document.getElementById('auth-overlay').classList.add('open'); }
function hideAuthOverlay() { document.getElementById('auth-overlay').classList.remove('open'); }
function showLanding() {
  document.getElementById('landing').style.display = 'flex';
  document.getElementById('app').classList.remove('visible');
  hideAuthOverlay();
}
function openAuthModal(tab) {
  showTab(tab);
  showAuthOverlay();
}

function showTab(tab) {
  document.getElementById('tab-login').classList.toggle('active', tab === 'login');
  document.getElementById('tab-register').classList.toggle('active', tab === 'register');
  document.getElementById('auth-submit-btn').textContent = tab === 'login' ? 'Masuk' : 'Daftar';
  document.getElementById('auth-error').style.display = 'none';
  document.getElementById('auth-email').value = '';
  document.getElementById('auth-password').value = '';
}

async function submitAuth() {
  const btn    = document.getElementById('auth-submit-btn');
  const errEl  = document.getElementById('auth-error');
  const email  = document.getElementById('auth-email').value.trim();
  const pass   = document.getElementById('auth-password').value;
  const isLogin = document.getElementById('tab-login').classList.contains('active');
  errEl.style.display = 'none';
  btn.disabled = true;
  try {
    const res = await fetch(isLogin ? '/auth/login' : '/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password: pass }),
    });
    if (res.status === 401) { showAuthError(errEl, 'Email atau password salah.'); return; }
    if (res.status === 409) { showAuthError(errEl, 'Email sudah terdaftar.'); return; }
    if (!res.ok)            { showAuthError(errEl, 'Terjadi kesalahan.'); return; }
    const data = await res.json();
    setToken(data.token);
    document.getElementById('auth-email').value = '';
    document.getElementById('auth-password').value = '';
    hideAuthOverlay();
    onAppReady();
  } catch (_) { showAuthError(errEl, 'Server tidak bisa dihubungi.'); }
  finally { btn.disabled = false; }
}

function showAuthError(el, msg) { el.textContent = msg; el.style.display = 'block'; }
function cancelAuth() {
  hideAuthOverlay();
  if (!getToken()) showLanding();
}
function logout() {
  document.getElementById('logout-modal-overlay').classList.add('open');
}
function confirmLogout() {
  document.getElementById('logout-modal-overlay').classList.remove('open');
  clearToken();
  showLanding();
}
function cancelLogout() {
  document.getElementById('logout-modal-overlay').classList.remove('open');
}
