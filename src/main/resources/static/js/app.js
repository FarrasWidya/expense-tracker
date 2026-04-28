// TAB ROUTER
function showScreen(name) {
  document.querySelectorAll('.screen').forEach(s => s.classList.remove('active'));
  document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
  document.getElementById('screen-' + name).classList.add('active');
  document.querySelector(`.tab[data-screen="${name}"]`)?.classList.add('active');
}
document.querySelectorAll('.tab[data-screen]').forEach(btn =>
  btn.addEventListener('click', () => showScreen(btn.dataset.screen))
);
document.getElementById('fab').addEventListener('click', openSheet);

// ENTRY POINT
function onAppReady() {
  document.getElementById('landing').style.display = 'none';
  document.getElementById('app').classList.add('visible');
  hideAuthOverlay();
  loadBeranda();
  loadRumahTab(); // pre-load so bareng toggle works without visiting Rumah tab first
}

// ?token= pickup (Google OAuth)
const urlToken = new URLSearchParams(location.search).get('token');
if (urlToken) { setToken(urlToken); history.replaceState({}, '', '/'); }

if (getToken()) { onAppReady(); } else { showLanding(); }
