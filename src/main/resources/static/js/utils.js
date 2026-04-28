// TOKEN HELPERS
const getToken   = () => localStorage.getItem('jwt');
const setToken   = t  => localStorage.setItem('jwt', t);
const clearToken = () => localStorage.removeItem('jwt');

function authFetch(url, opts = {}) {
  const token = getToken();
  const headers = { ...(opts.headers || {}), ...(token ? { 'Authorization': 'Bearer ' + token } : {}) };
  return fetch(url, { ...opts, headers }).then(res => {
    if (res.status === 401) { clearToken(); showAuthOverlay(); throw new Error('401'); }
    return res;
  });
}

function formatRp(n) { return 'Rp ' + Math.round(n).toLocaleString('id-ID'); }
function today() { return new Date().toISOString().split('T')[0]; }

let _toastTimer = null;
function showToast(msg) {
  let el = document.getElementById('simple-toast');
  if (!el) {
    el = document.createElement('div');
    el.id = 'simple-toast';
    el.style.cssText = 'position:fixed;bottom:90px;left:50%;transform:translateX(-50%);background:rgba(0,0,0,.8);color:#fff;padding:8px 18px;border-radius:20px;font-size:13px;font-weight:600;z-index:9999;pointer-events:none;transition:opacity .3s';
    document.body.appendChild(el);
  }
  el.textContent = msg;
  el.style.opacity = '1';
  if (_toastTimer) clearTimeout(_toastTimer);
  _toastTimer = setTimeout(() => { el.style.opacity = '0'; }, 2000);
}
function escHtml(s) {
  return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}
function monthRange(offsetMonths = 0) {
  const d = new Date();
  d.setMonth(d.getMonth() + offsetMonths);
  const from = new Date(d.getFullYear(), d.getMonth(), 1).toISOString().split('T')[0];
  const to   = new Date(d.getFullYear(), d.getMonth() + 1, 0).toISOString().split('T')[0];
  return { from, to };
}
