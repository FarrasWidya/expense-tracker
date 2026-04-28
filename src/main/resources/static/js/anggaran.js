// ANGGARAN
function getBudgets() { return JSON.parse(localStorage.getItem('catetu_budgets') || '{}'); }
function saveBudgets(b) { localStorage.setItem('catetu_budgets', JSON.stringify(b)); }

function openAnggaranModal() {
  document.getElementById('anggaran-cat').innerHTML = CATEGORY_LIST.map(c =>
    `<option value="${escHtml(c.label)}">${c.emoji} ${escHtml(c.label)}</option>`
  ).join('');
  document.getElementById('anggaran-limit').value = '';
  document.getElementById('anggaran-overlay').style.display = 'block';
  document.getElementById('anggaran-modal').classList.add('open');
}
function closeAnggaranModal() {
  document.getElementById('anggaran-overlay').style.display = 'none';
  document.getElementById('anggaran-modal').classList.remove('open');
}

document.getElementById('anggaran-overlay').addEventListener('click', closeAnggaranModal);
document.getElementById('anggaran-add-btn').addEventListener('click', openAnggaranModal);
document.getElementById('anggaran-save-btn').addEventListener('click', () => {
  const cat   = document.getElementById('anggaran-cat').value;
  const limit = parseFloat(document.getElementById('anggaran-limit').value);
  if (!limit || limit <= 0) return;
  const b = getBudgets(); b[cat] = limit; saveBudgets(b);
  closeAnggaranModal(); loadAnggaran();
});

async function loadAnggaran() {
  const budgets = getBudgets();
  const { from, to } = monthRange();
  const res = await authFetch(`/expenses?startDate=${from}&endDate=${to}`);
  const expenses = await res.json();

  const spent = {};
  expenses.forEach(e => { spent[e.category] = (spent[e.category] || 0) + e.amount; });

  const list    = document.getElementById('anggaran-list');
  const empty   = document.getElementById('anggaran-empty');
  const entries = Object.entries(budgets);

  if (!entries.length) { list.innerHTML = ''; empty.style.display = 'block'; return; }
  empty.style.display = 'none';

  list.innerHTML = entries.map(([cat, limit]) => {
    const s   = spent[cat] || 0;
    const pct = Math.min(Math.round((s / limit) * 100), 100);
    const color = pct >= 95 ? 'var(--danger)' : pct >= 80 ? 'var(--warn)' : 'var(--accent)';
    const nudge = pct >= 95 ? ' · 🔴 Habis!' : pct >= 80 ? ' · ⚠️ Hampir habis' : '';
    return `
      <div class="card budget-card">
        <div class="budget-card-top">
          <div class="budget-label">${CATS[cat] || '📦'} ${escHtml(cat)}</div>
          <div class="budget-amounts">${formatRp(s)} / ${formatRp(limit)}</div>
        </div>
        <div class="budget-track"><div class="budget-fill" style="width:${pct}%;background:${color}"></div></div>
        <div class="budget-sisa">Sisa ${formatRp(Math.max(limit - s, 0))}${nudge}</div>
      </div>`;
  }).join('');
}

document.querySelector('.tab[data-screen="anggaran"]').addEventListener('click', loadAnggaran);

// RUMAH IN ANGGARAN
let rumahSectionOpen = false;

function toggleRumahSection() {
  rumahSectionOpen = !rumahSectionOpen;
  const body = document.getElementById('rumah-section-body');
  const chevron = document.getElementById('rumah-chevron');
  if (body) body.style.display = rumahSectionOpen ? 'block' : 'none';
  if (chevron) chevron.classList.toggle('open', rumahSectionOpen);
  if (rumahSectionOpen) loadRumahTab();
}
