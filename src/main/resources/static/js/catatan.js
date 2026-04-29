// CATATAN
let allExpenses   = [];
let catMonth      = null;
let catFilterCats = new Set();
let catSearchQ    = '';

async function loadCatatan(range) {
  const { from, to } = range || monthRange();
  catMonth = range || null;
  const res  = await authFetch(`/expenses?startDate=${from}&endDate=${to}`);
  allExpenses = await res.json();
  renderCatatan();
}

function renderCatatan() {
  let list = allExpenses;
  if (catFilterCats.size) list = list.filter(e => catFilterCats.has(e.category));
  if (catSearchQ) list = list.filter(e =>
    (e.note || '').toLowerCase().includes(catSearchQ) || e.category.toLowerCase().includes(catSearchQ)
  );

  const expTotal = list.filter(e => e.type !== 'INCOME').reduce((s, e) => s + e.amount, 0);
  document.getElementById('cat-summary-bar').textContent = `${list.length} catatan · ${formatRp(expTotal)}`;

  const cont  = document.getElementById('cat-list');
  const empty = document.getElementById('cat-empty');
  if (!list.length) { cont.innerHTML = ''; empty.style.display = 'block'; return; }
  empty.style.display = 'none';

  const groups = {};
  list.forEach(e => { (groups[e.date || today()] = groups[e.date || today()] || []).push(e); });
  const todayStr = today();
  const yest = new Date(Date.now() - 86400000).toISOString().split('T')[0];

  cont.innerHTML = Object.entries(groups)
    .sort(([a], [b]) => b.localeCompare(a))
    .map(([date, items]) => {
      const label = date === todayStr ? 'Hari ini' : date === yest ? 'Kemarin'
        : new Date(date + 'T00:00:00').toLocaleDateString('id-ID', { day:'numeric', month:'long' });
      return `
        <div class="day-group-header">${label}</div>
        <div class="day-group">
          ${items.map(e => `
            <div class="swipe-row" data-id="${e.id}">
              <div class="swipe-action-bg">Hapus</div>
              <div class="swipe-row-content expense-row">
                <div class="expense-icon">${CATS[e.category] || '📦'}</div>
                <div class="expense-row-body">
                  <div class="expense-row-name">${escHtml(e.note || e.category)}</div>
                  <div class="expense-row-cat">${escHtml(e.category)}</div>
                </div>
                <div class="expense-row-amount${e.type === 'INCOME' ? ' income-amount' : ''}">${e.type === 'INCOME' ? '+' : ''}${formatRp(e.amount)}</div>
              </div>
            </div>`).join('')}
        </div>`;
    }).join('');

  cont.querySelectorAll('.swipe-row').forEach(row => {
    const inner = row.querySelector('.swipe-row-content');
    let startX = 0, dx = 0;
    inner.addEventListener('pointerdown', e => { startX = e.clientX; dx = 0; inner.setPointerCapture(e.pointerId); });
    inner.addEventListener('pointermove', e => {
      dx = e.clientX - startX;
      if (dx < 0) inner.style.transform = `translateX(${Math.max(dx, -72)}px)`;
    });
    inner.addEventListener('pointerup', async () => {
      if (dx < -50) {
        await authFetch(`/expenses/${row.dataset.id}`, { method: 'DELETE' });
        allExpenses = allExpenses.filter(e => String(e.id) !== row.dataset.id);
        loadBeranda();
        renderCatatan();
      } else {
        inner.style.transform = '';
      }
    });
  });
}

function buildMonthChips() {
  const cont = document.getElementById('cat-months');
  const months = Array.from({ length: 6 }, (_, i) => {
    const r = monthRange(-i);
    const d = new Date();
    d.setMonth(d.getMonth() - i);
    return { label: d.toLocaleDateString('id-ID', { month:'long', year:'numeric' }), range: r };
  });
  cont.innerHTML = months.map((m, i) =>
    `<button class="month-chip${i === 0 ? ' active' : ''}" data-idx="${i}">${m.label}</button>`
  ).join('');
  cont.querySelectorAll('.month-chip').forEach(btn =>
    btn.addEventListener('click', () => {
      cont.querySelectorAll('.month-chip').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      loadCatatan(months[+btn.dataset.idx].range);
    })
  );
}

function openFilter() {
  document.getElementById('filter-cat-pills').innerHTML = CATEGORY_LIST.map(c =>
    `<button class="cat-pill${catFilterCats.has(c.label) ? ' selected' : ''}" data-cat="${escHtml(c.label)}">${c.emoji} ${escHtml(c.label)}</button>`
  ).join('');
  document.querySelectorAll('#filter-cat-pills .cat-pill').forEach(btn =>
    btn.addEventListener('click', () => {
      catFilterCats.has(btn.dataset.cat) ? catFilterCats.delete(btn.dataset.cat) : catFilterCats.add(btn.dataset.cat);
      btn.classList.toggle('selected');
    })
  );
  document.getElementById('filter-overlay').style.display = 'block';
  document.getElementById('filter-drawer').classList.add('open');
}
function closeFilter() {
  document.getElementById('filter-overlay').style.display = 'none';
  document.getElementById('filter-drawer').classList.remove('open');
}

document.getElementById('filter-overlay').addEventListener('click', closeFilter);
document.getElementById('cat-filter-btn').addEventListener('click', openFilter);
document.getElementById('filter-apply-btn').addEventListener('click', () => { closeFilter(); renderCatatan(); });

let searchTimer;
document.getElementById('cat-search').addEventListener('input', e => {
  clearTimeout(searchTimer);
  searchTimer = setTimeout(() => { catSearchQ = e.target.value.toLowerCase(); renderCatatan(); }, 200);
});

document.querySelector('.tab[data-screen="catatan"]').addEventListener('click', () => {
  buildMonthChips();
  loadCatatan();
});
