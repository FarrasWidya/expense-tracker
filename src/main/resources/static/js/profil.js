// PROFIL
function scheduleNudge(todayCount) {
  if (localStorage.getItem('catetu_nudge_enabled') === 'false') return;
  const timeStr = localStorage.getItem('catetu_nudge_time') || '21:00';
  const [hh, mm] = timeStr.split(':').map(Number);
  const now = new Date();
  const fireAt = new Date(now.getFullYear(), now.getMonth(), now.getDate(), hh, mm, 0);
  const msUntil = fireAt - now;
  if (msUntil <= 0) return;
  setTimeout(async () => {
    try {
      const t = today();
      const r = await authFetch(`/expenses?startDate=${t}&endDate=${t}`);
      const exp = await r.json();
      if (exp.length === 0) showNudgeToast();
    } catch (_) { if (todayCount === 0) showNudgeToast(); }
  }, msUntil);
}

function showNudgeToast() {
  const toast = document.getElementById('nudge-toast');
  if (toast) toast.style.display = 'block';
  if ('Notification' in window) {
    if (Notification.permission === 'granted') {
      new Notification('CatetU', { body: 'Udah catat pengeluaran hari ini? 📝' });
    } else if (Notification.permission === 'default') {
      Notification.requestPermission().then(p => {
        if (p === 'granted') new Notification('CatetU', { body: 'Udah catat pengeluaran hari ini? 📝' });
      });
    }
  }
  setTimeout(closeNudgeToast, 8000);
}

function closeNudgeToast() {
  const toast = document.getElementById('nudge-toast');
  if (toast) toast.style.display = 'none';
}

function openQuickAdd() { document.getElementById('fab')?.click(); }

const RAMADAN_RANGES = [
  { year: 2026, start: new Date('2026-02-18T00:00:00'), end: new Date('2026-03-19T23:59:59') },
  { year: 2027, start: new Date('2027-02-07T00:00:00'), end: new Date('2027-03-08T23:59:59') },
  { year: 2028, start: new Date('2028-01-27T00:00:00'), end: new Date('2028-02-25T23:59:59') },
];

function getCurrentRamadanWindow(now = new Date()) {
  const leadUpMs = 14 * 86400000;
  return RAMADAN_RANGES.find(r => now >= new Date(r.start.getTime() - leadUpMs) && now <= r.end) || null;
}

function isRamadanActive() { return localStorage.getItem('catetu_ramadan_mode') === 'true'; }

function getDisplayLabel(label) {
  if (!isRamadanActive()) return label;
  const map = { 'Makan & Minum': 'Sahur & Berbuka', 'Sedekah & Sosial': 'Zakat & Infaq' };
  return map[label] || label;
}

function checkRamadanSuggestion() {
  const now = new Date();
  const range = getCurrentRamadanWindow(now);
  if (!range) return;
  if (now >= range.start) return;
  if (isRamadanActive()) return;
  const key = `catetu_ramadan_suggestion_${range.year}`;
  if (localStorage.getItem(key)) return;
  const el = document.getElementById('ramadan-suggestion');
  if (el) el.style.display = 'block';
}

function activateRamadan() {
  const range = getCurrentRamadanWindow();
  if (range) localStorage.setItem(`catetu_ramadan_suggestion_${range.year}`, 'shown');
  localStorage.setItem('catetu_ramadan_mode', 'true');
  const el = document.getElementById('ramadan-suggestion');
  if (el) el.style.display = 'none';
  const cb = document.getElementById('ramadan-toggle-cb');
  if (cb) cb.checked = true;
  renderCatPills();
}

function dismissRamadanSuggestion() {
  const range = getCurrentRamadanWindow();
  if (range) localStorage.setItem(`catetu_ramadan_suggestion_${range.year}`, 'shown');
  const el = document.getElementById('ramadan-suggestion');
  if (el) el.style.display = 'none';
}

function checkPaydayBanner() {
  const paydayDate = parseInt(localStorage.getItem('catetu_payday_date'), 10);
  const banner = document.getElementById('payday-banner');
  if (!banner) return;
  if (!paydayDate || isNaN(paydayDate)) { banner.style.display = 'none'; return; }

  const now = new Date();
  const payday = new Date(now.getFullYear(), now.getMonth(), paydayDate);
  const diff = Math.round((now - payday) / 86400000);

  if (Math.abs(diff) > 3) { banner.style.display = 'none'; return; }

  const dismissKey = 'catetu_payday_dismissed_month';
  const curMonth = `${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,'0')}`;
  if (localStorage.getItem(dismissKey) === curMonth) { banner.style.display = 'none'; return; }

  banner.style.display = 'flex';
  if (!banner.dataset.dismissAdded) {
    banner.dataset.dismissAdded = '1';
    const btn = document.createElement('button');
    btn.textContent = '×';
    btn.style.cssText = 'background:none;border:none;cursor:pointer;font-size:20px;color:var(--text-2);flex-shrink:0';
    btn.addEventListener('click', () => {
      localStorage.setItem(dismissKey, curMonth);
      banner.style.display = 'none';
    });
    banner.appendChild(btn);
  }
}

async function checkMonthlySummary() {
  const now = new Date();
  if (now.getDate() > 3) return;
  const curMonth = `${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,'0')}`;
  if (localStorage.getItem('catetu_last_summary_month') === curMonth) return;

  const { from: lf, to: lt } = monthRange(-1);
  const { from: llf, to: llt } = monthRange(-2);
  try {
    const [r1, r2] = await Promise.all([
      authFetch(`/expenses?startDate=${lf}&endDate=${lt}`),
      authFetch(`/expenses?startDate=${llf}&endDate=${llt}`),
    ]);
    const prev = await r1.json();
    const prevPrev = await r2.json();
    if (prev.length === 0) return;

    const total = prev.reduce((s, e) => s + e.amount, 0);
    const totalPP = prevPrev.reduce((s, e) => s + e.amount, 0);
    const pct = totalPP > 0 ? Math.round((total / totalPP) * 100) : null;
    const pctLabel = pct !== null
      ? (pct < 100 ? `↓ hemat ${100-pct}% dari sebelumnya` : `↑ ${pct-100}% dari sebelumnya`)
      : '';

    const catTotals = {};
    prev.forEach(e => { catTotals[e.category] = (catTotals[e.category]||0)+e.amount; });
    const top3 = Object.entries(catTotals).sort((a,b)=>b[1]-a[1]).slice(0,3);

    const prevDate = new Date(now.getFullYear(), now.getMonth()-1, 1);
    const monthLabel = prevDate.toLocaleDateString('id-ID', {month:'long', year:'numeric'});

    document.getElementById('monthly-summary-title').textContent = `Ringkasan ${monthLabel}`;
    document.getElementById('monthly-summary-sub').textContent = pctLabel;
    document.getElementById('monthly-summary-total').textContent = formatRp(total);
    document.getElementById('monthly-summary-rows').innerHTML =
      top3.map(([cat, amt]) =>
        `<div class="monthly-row"><span>${CATS[cat]||'📦'} ${escHtml(cat)}</span><span>${formatRp(amt)}</span></div>`
      ).join('') +
      `<div class="monthly-row"><span style="color:var(--text-2)">${prev.length} transaksi</span><span></span></div>`;

    document.getElementById('monthly-summary-overlay').style.display = 'flex';
  } catch (_) {}
}

function closeMonthlySummary() {
  const now = new Date();
  localStorage.setItem('catetu_last_summary_month',
    `${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,'0')}`);
  const overlay = document.getElementById('monthly-summary-overlay');
  if (overlay) overlay.style.display = 'none';
}

function loadProfilSettings() {
  const nudgeTime = document.getElementById('nudge-time-input');
  if (nudgeTime) nudgeTime.value = localStorage.getItem('catetu_nudge_time') || '21:00';
  const nudgeEnabled = document.getElementById('nudge-enabled-cb');
  if (nudgeEnabled) nudgeEnabled.checked = localStorage.getItem('catetu_nudge_enabled') !== 'false';
  const paydayInput = document.getElementById('payday-date-input');
  if (paydayInput) paydayInput.value = localStorage.getItem('catetu_payday_date') || '';
  const ramadanCb = document.getElementById('ramadan-toggle-cb');
  if (ramadanCb) ramadanCb.checked = localStorage.getItem('catetu_ramadan_mode') === 'true';
}
function saveNudgeSetting() {
  const t = document.getElementById('nudge-time-input')?.value || '21:00';
  const en = document.getElementById('nudge-enabled-cb')?.checked !== false;
  localStorage.setItem('catetu_nudge_time', t);
  localStorage.setItem('catetu_nudge_enabled', String(en));
}
function savePaydaySetting() {
  const v = document.getElementById('payday-date-input')?.value;
  if (v) localStorage.setItem('catetu_payday_date', v); else localStorage.removeItem('catetu_payday_date');
}
function saveRamadanSetting() {
  const on = document.getElementById('ramadan-toggle-cb')?.checked === true;
  localStorage.setItem('catetu_ramadan_mode', String(on));
  renderCatPills();
}

async function loadProfil() {
  try {
    const res = await authFetch('/auth/me');
    const user = await res.json();
    const email = user.email || '';
    document.getElementById('profil-email').textContent = email;
    document.getElementById('profil-avatar').textContent = email.split('@')[0].slice(0, 2).toUpperCase();
  } catch (_) {}
  loadProfilSettings();
}

function applyTheme(theme) {
  document.documentElement.setAttribute('data-theme', theme);
  localStorage.setItem('catetu_theme', theme);
  document.querySelectorAll('.theme-btn').forEach(b => b.classList.toggle('active', b.dataset.pickTheme === theme));
}
document.querySelectorAll('.theme-btn').forEach(btn => btn.addEventListener('click', () => applyTheme(btn.dataset.pickTheme)));
applyTheme(localStorage.getItem('catetu_theme') || 'light');

document.querySelector('.tab[data-screen="profil"]').addEventListener('click', loadProfil);
