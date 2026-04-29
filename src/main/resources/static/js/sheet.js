// QUICK-ADD SHEET
let sheetAmount = '';
let sheetCat = CATEGORY_LIST[0].label;
let sheetType = 'EXPENSE';

function renderCatPills() {
  const cats = sheetType === 'INCOME' ? INCOME_CATEGORY_LIST : CATEGORY_LIST;
  document.getElementById('sheet-cats').innerHTML = cats.map(c =>
    `<button class="cat-pill${c.label === sheetCat ? ' selected' : ''}" data-cat="${escHtml(c.label)}"><span class="cat-emoji">${c.emoji}</span><span>${escHtml(getDisplayLabel(c.label))}</span></button>`
  ).join('');
  document.querySelectorAll('.cat-pill').forEach(btn =>
    btn.addEventListener('click', () => { sheetCat = btn.dataset.cat; renderCatPills(); })
  );
}

function updateAmountDisplay() {
  const n = parseInt(sheetAmount || '0', 10);
  document.getElementById('sheet-amount-text').textContent = n.toLocaleString('id-ID');
}

function openSheet() {
  updateBarengToggle();
  sheetAmount = '';
  sheetType = 'EXPENSE';
  updateAmountDisplay();
  document.getElementById('sheet-note-input').value = '';
  sheetCat = CATEGORY_LIST[0].label;
  renderCatPills();
  document.querySelectorAll('.sheet-type-btn').forEach(btn => {
    btn.classList.toggle('active', btn.dataset.type === 'EXPENSE');
  });
  document.getElementById('sheet-amount-text').style.color = '';
  document.getElementById('sheet-overlay').style.display = 'block';
  document.getElementById('quick-add-sheet').classList.add('open');
}

function closeSheet() {
  document.getElementById('quick-add-sheet').classList.remove('open');
  document.getElementById('sheet-overlay').style.display = 'none';
  const btn = document.getElementById('sheet-simpan');
  if (btn) { btn.textContent = 'Simpan'; btn.style.background = ''; }
}

document.getElementById('sheet-overlay').addEventListener('click', closeSheet);

document.getElementById('sheet-numpad').addEventListener('click', e => {
  const k = e.target.closest('.np-key')?.dataset.k;
  if (!k) return;
  if (k === 'del') {
    sheetAmount = sheetAmount.slice(0, -1);
  } else {
    if (sheetAmount.length + k.length > 10) return;
    sheetAmount = String(parseInt((sheetAmount + k) || '0', 10));
  }
  updateAmountDisplay();
});

document.getElementById('sheet-simpan').addEventListener('click', async () => {
  const amount = parseInt(sheetAmount || '0', 10);
  if (!amount) return;
  const btn = document.getElementById('sheet-simpan');
  btn.disabled = true;
  try {
    const isBareng = document.getElementById('bareng-checkbox')?.checked && currentRumah;
    const url = isBareng ? '/rumah/' + currentRumah.id + '/expenses' : '/expenses';
    const res = await authFetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        amount,
        category: sheetCat,
        note: document.getElementById('sheet-note-input').value.trim(),
        date: today(),
        type: sheetType,
      }),
    });
    if (!res.ok) throw new Error();
    closeSheet();
    showToast(isBareng ? 'Tersimpan di Rumah! 🏠' : 'Tersimpan!');
    if (isBareng) loadRumahFeed(); else loadBeranda();
  } catch (_) { alert('Gagal menyimpan. Coba lagi.'); }
  finally { btn.disabled = false; }
});

document.querySelectorAll('.sheet-type-btn').forEach(btn => {
  btn.addEventListener('click', () => {
    sheetType = btn.dataset.type;
    document.querySelectorAll('.sheet-type-btn').forEach(b =>
      b.classList.toggle('active', b.dataset.type === sheetType)
    );
    const cats = sheetType === 'INCOME' ? INCOME_CATEGORY_LIST : CATEGORY_LIST;
    sheetCat = cats[0].label;
    document.getElementById('sheet-amount-text').style.color = sheetType === 'INCOME' ? 'var(--accent)' : '';
    renderCatPills();
  });
});
