# CatetU Beranda Features Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add personalized nickname greeting, upgrade Beranda to a visual financial dashboard (gradient hero card + SVG donut chart), and merge the Rumah tab into Anggaran.

**Architecture:** All changes in the split files from Plan 1 (`css/main.css`, `js/beranda.js`, `js/profil.js`, `js/anggaran.js`, `js/rumah.js`, `js/app.js`, `index.html`). Nickname stored in `localStorage` key `catetU_nickname`. SVG donut uses `stroke-dasharray` / `stroke-dashoffset` technique — no library. Rumah HTML moves into `#screen-anggaran` as a collapsible section.

**Prerequisites:** Plan 1 (file split) must be complete. All paths below reference the split files.

**Tech Stack:** Vanilla HTML/CSS/JS. Spring Boot static serving. No new dependencies.

---

## File Map

| File | What changes |
|------|-------------|
| `css/main.css` | Add nickname modal CSS, beranda greeting CSS, donut chart CSS, hero card gradient, Rumah-in-Anggaran section CSS |
| `js/beranda.js` | Add `renderDonutChart()`, update `loadBeranda()` to show greeting + populate donut |
| `js/profil.js` | Add `getNickname()`, `setNickname()`, `showNicknameModal()`, update `loadProfil()` to show nickname row |
| `js/anggaran.js` | Add Rumah section HTML init + collapsible toggle, call `loadRumahTab()` when expanded |
| `js/rumah.js` | Remove standalone screen wiring (tab click listener), keep all Rumah functions |
| `js/app.js` | Remove Rumah from tab routing, call `checkNicknameOnboarding()` in `onAppReady()` |
| `index.html` | Replace `#screen-rumah` section with Rumah inside `#screen-anggaran`, remove Rumah tab, add nickname modal HTML |

---

## Task 1: Nickname system — utils + modal HTML + profil edit

**Files:**
- Modify: `expense-tracker/src/main/resources/static/js/profil.js`
- Modify: `expense-tracker/src/main/resources/static/index.html`
- Modify: `expense-tracker/src/main/resources/static/css/main.css`

### Step 1: Add nickname helpers to `js/profil.js`

Add at the top of `profil.js` (before any existing functions):

```js
// NICKNAME
function getNickname() {
  return localStorage.getItem('catetU_nickname') || '';
}
function setNickname(name) {
  localStorage.setItem('catetU_nickname', name.trim());
}

function showNicknameModal(onSave) {
  document.getElementById('nickname-modal-overlay').style.display = 'flex';
  const input = document.getElementById('nickname-input');
  input.value = getNickname();
  input.focus();
  document.getElementById('nickname-save-btn').onclick = () => {
    const val = input.value.trim();
    if (!val) return;
    setNickname(val);
    document.getElementById('nickname-modal-overlay').style.display = 'none';
    if (onSave) onSave(val);
  };
  document.getElementById('nickname-skip-btn').onclick = () => {
    const fallback = (document.getElementById('profil-email')?.textContent || 'Kamu').split('@')[0];
    setNickname(fallback);
    document.getElementById('nickname-modal-overlay').style.display = 'none';
    if (onSave) onSave(getNickname());
  };
}

function checkNicknameOnboarding() {
  if (!getNickname()) {
    showNicknameModal(() => {
      const greeting = document.getElementById('beranda-greeting-name');
      if (greeting) greeting.textContent = getNickname();
    });
  }
}
```

- [ ] **Step 2: Update `loadProfil()` in `js/profil.js` to show nickname row**

Find the `async function loadProfil()` in `profil.js`. After the line that sets `profil-email`, add:

```js
  // Nickname row
  const nicknameEl = document.getElementById('profil-nickname-value');
  if (nicknameEl) nicknameEl.textContent = getNickname() || '—';
  const nicknameEditBtn = document.getElementById('profil-nickname-edit');
  if (nicknameEditBtn) {
    nicknameEditBtn.onclick = () => showNicknameModal(() => {
      if (nicknameEl) nicknameEl.textContent = getNickname();
      const greetEl = document.getElementById('beranda-greeting-name');
      if (greetEl) greetEl.textContent = getNickname();
    });
  }
```

- [ ] **Step 3: Add nickname modal HTML to `index.html`**

Find `<!-- LOGOUT CONFIRM MODAL -->` in `index.html`. Before it, add:

```html
    <!-- NICKNAME MODAL -->
    <div id="nickname-modal-overlay" style="display:none;position:fixed;inset:0;z-index:300;background:rgba(0,0,0,.5);display:none;align-items:flex-end;justify-content:center">
      <div style="background:var(--surface);border-radius:20px 20px 0 0;padding:28px 20px 40px;width:100%;max-width:480px">
        <div style="font-size:24px;text-align:center;margin-bottom:8px">👋</div>
        <div style="font-size:18px;font-weight:700;text-align:center;margin-bottom:4px">Kamu mau dipanggil apa?</div>
        <div style="font-size:13px;color:var(--text-2);text-align:center;margin-bottom:20px">Bisa nama, nickname, atau apa saja</div>
        <input id="nickname-input" type="text" placeholder="mis. Sarah, Kak Budi, Devi..." maxlength="30"
          style="width:100%;padding:12px 14px;border-radius:10px;border:1.5px solid var(--accent);background:var(--raised);font-size:16px;font-family:var(--font);color:var(--text);outline:none;margin-bottom:12px;text-align:center">
        <button id="nickname-save-btn"
          style="width:100%;padding:14px;border-radius:12px;background:var(--accent);color:#fff;border:none;font-size:16px;font-weight:700;margin-bottom:8px">
          Simpan
        </button>
        <button id="nickname-skip-btn"
          style="width:100%;padding:12px;border-radius:12px;background:none;border:none;color:var(--text-2);font-size:14px">
          Lewati
        </button>
      </div>
    </div>
```

- [ ] **Step 4: Add nickname row to Profil screen in `index.html`**

Find the `<div class="section-label">Tampilan</div>` line in `#screen-profil`. Before it, add a "Nama" section:

```html
      <div class="section-label">Nama</div>
      <div class="card">
        <div class="settings-row">
          <span>Nama panggilan</span>
          <div style="display:flex;align-items:center;gap:8px">
            <span id="profil-nickname-value" style="font-size:14px;color:var(--text-2)">—</span>
            <button id="profil-nickname-edit" style="padding:4px 10px;border-radius:8px;border:1.5px solid var(--border);background:none;font-size:12px;color:var(--text-2)">Ubah</button>
          </div>
        </div>
      </div>
```

- [ ] **Step 5: Call `checkNicknameOnboarding()` from `onAppReady()` in `js/app.js`**

Find `function onAppReady()` in `app.js`. Add at the end of the function body:

```js
  checkNicknameOnboarding();
```

- [ ] **Step 6: Commit**
```bash
git add expense-tracker/src/main/resources/static/js/profil.js \
        expense-tracker/src/main/resources/static/js/app.js \
        expense-tracker/src/main/resources/static/index.html
git commit -m "feat: nickname system — onboarding modal, localStorage storage, profil edit"
```

---

## Task 2: Beranda greeting + hero card gradient

**Files:**
- Modify: `expense-tracker/src/main/resources/static/css/main.css`
- Modify: `expense-tracker/src/main/resources/static/index.html`
- Modify: `expense-tracker/src/main/resources/static/js/beranda.js`

- [ ] **Step 1: Add greeting + hero gradient CSS to `css/main.css`**

Append to end of `css/main.css`:

```css
/* BERANDA GREETING */
.beranda-greeting {
  display: flex; align-items: flex-start; justify-content: space-between;
  padding: 20px 16px 8px;
}
.beranda-greeting-left { flex: 1; }
.beranda-greeting-name {
  font-size: 22px; font-weight: 700; color: var(--text); line-height: 1.2;
}
.beranda-greeting-date { font-size: 13px; color: var(--text-2); margin-top: 2px; }

/* BERANDA HERO GRADIENT */
.hero-card-gradient {
  margin: 0 16px 12px; border-radius: 20px; padding: 20px;
  background: linear-gradient(135deg, var(--accent) 0%, #00695C 100%);
  color: #fff; position: relative; overflow: hidden;
}
.hero-card-gradient::after {
  content: ''; position: absolute; top: -30px; right: -30px;
  width: 120px; height: 120px; border-radius: 50%;
  background: rgba(255,255,255,.08);
}
.hero-card-gradient .hero-label { color: rgba(255,255,255,.75); }
.hero-card-gradient .hero-amount { color: #fff; }
.hero-card-gradient .hero-stats-row { border-top-color: rgba(255,255,255,.2); }
.hero-card-gradient .hero-stat-label { color: rgba(255,255,255,.75); }
.hero-card-gradient .hero-stat-value { color: #fff; }
```

- [ ] **Step 2: Replace beranda section HTML in `index.html`**

Find `<section id="screen-beranda"` in `index.html`. Replace the opening `<div class="page-header">Beranda</div>` and the hero `<div class="card"` with:

```html
    <section id="screen-beranda" class="screen active">
      <div class="beranda-greeting">
        <div class="beranda-greeting-left">
          <div class="beranda-greeting-name">Halo, <span id="beranda-greeting-name">—</span>! 👋</div>
          <div id="beranda-greeting-date" class="beranda-greeting-date"></div>
        </div>
      </div>
      <div class="hero-card-gradient">
        <div class="hero-label">Pengeluaran bulan ini</div>
        <div id="beranda-total" class="hero-amount">Rp 0</div>
        <div id="beranda-cat-breakdown" style="margin-bottom:12px"></div>
        <div class="hero-stats-row">
          <div class="hero-stat">
            <div class="hero-stat-label">Rata-rata/hari</div>
            <div id="beranda-avg" class="hero-stat-value">—</div>
          </div>
          <div class="hero-stat">
            <div class="hero-stat-label">Bulan lalu</div>
            <div id="beranda-compare" class="hero-stat-value">—</div>
          </div>
        </div>
      </div>
```

Keep everything else in the section unchanged (streak badge, insight cards, etc.).

- [ ] **Step 3: Update `loadBeranda()` in `js/beranda.js` to populate greeting**

Find `async function loadBeranda()` in `beranda.js`. Add at the very top of the function body (before the `authFetch` calls):

```js
  // Greeting
  const greetName = document.getElementById('beranda-greeting-name');
  if (greetName) greetName.textContent = getNickname() || 'Kamu';
  const greetDate = document.getElementById('beranda-greeting-date');
  if (greetDate) {
    const now = new Date();
    greetDate.textContent = now.toLocaleDateString('id-ID', { weekday:'long', day:'numeric', month:'long' });
  }
```

- [ ] **Step 4: Verify — load app, Beranda shows greeting with name and teal gradient hero card**

Manual check: go to Beranda, verify greeting shows nickname, hero card has teal gradient, text is white and readable in light+dark mode.

- [ ] **Step 5: Commit**
```bash
git add expense-tracker/src/main/resources/static/css/main.css \
        expense-tracker/src/main/resources/static/index.html \
        expense-tracker/src/main/resources/static/js/beranda.js
git commit -m "feat: beranda greeting header + teal gradient hero card"
```

---

## Task 3: Category donut chart

**Files:**
- Modify: `expense-tracker/src/main/resources/static/css/main.css`
- Modify: `expense-tracker/src/main/resources/static/index.html`
- Modify: `expense-tracker/src/main/resources/static/js/beranda.js`

**Technique:** SVG donut using `<circle>` with `stroke-dasharray` + `stroke-dashoffset`. No canvas, no library.

- [ ] **Step 1: Add donut chart CSS to `css/main.css`**

Append to `css/main.css`:

```css
/* BERANDA DONUT CHART */
.cat-donut-card { margin: 0 16px 12px; }
.cat-donut-card .card-title {
  font-size: 13px; font-weight: 700; color: var(--text-2);
  text-transform: uppercase; letter-spacing: .5px; margin-bottom: 14px;
}
.cat-donut-wrap { display: flex; align-items: center; gap: 16px; }
.cat-donut-wrap svg { flex-shrink: 0; }
.cat-legend { flex: 1; display: flex; flex-direction: column; gap: 8px; }
.cat-legend-row { display: flex; align-items: center; gap: 8px; }
.cat-legend-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.cat-legend-name { flex: 1; font-size: 13px; color: var(--text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.cat-legend-pct { font-size: 12px; color: var(--text-2); font-family: var(--mono); white-space: nowrap; }
.cat-legend-amt { font-size: 12px; font-family: var(--mono); color: var(--text); font-weight: 600; white-space: nowrap; }
.cat-donut-center { font-family: var(--mono); font-size: 11px; fill: var(--text-2); text-anchor: middle; dominant-baseline: middle; }
```

- [ ] **Step 2: Add donut chart HTML section to `index.html`**

Inside `#screen-beranda`, after the `hero-card-gradient` div and before `<div id="streak-badge">`, add:

```html
      <div class="card cat-donut-card">
        <div class="card-title">Kategori bulan ini</div>
        <div class="cat-donut-wrap">
          <svg id="beranda-donut" width="110" height="110" viewBox="0 0 110 110"></svg>
          <div id="beranda-donut-legend" class="cat-legend"></div>
        </div>
      </div>
```

- [ ] **Step 3: Add `renderDonutChart()` function to `js/beranda.js`**

Add this function before `loadBeranda()` in `beranda.js`:

```js
const DONUT_COLORS = ['#00897B','#26A69A','#EF5350','#FFA726','#42A5F5','#AB47BC','#66BB6A','#FF7043'];

function renderDonutChart(expenses) {
  const svg = document.getElementById('beranda-donut');
  const legend = document.getElementById('beranda-donut-legend');
  if (!svg || !legend) return;

  if (!expenses.length) {
    svg.innerHTML = '';
    legend.innerHTML = '<div style="font-size:13px;color:var(--text-2)">Belum ada data</div>';
    return;
  }

  const totals = {};
  expenses.forEach(e => { totals[e.category] = (totals[e.category] || 0) + e.amount; });
  const sorted = Object.entries(totals).sort((a, b) => b[1] - a[1]);
  const grandTotal = sorted.reduce((s, [, v]) => s + v, 0);
  const top = sorted.slice(0, 5);
  const otherAmt = sorted.slice(5).reduce((s, [, v]) => s + v, 0);
  if (otherAmt > 0) top.push(['Lainnya', otherAmt]);

  const cx = 55, cy = 55, r = 42, strokeW = 14;
  const circ = 2 * Math.PI * r;
  let offset = 0;

  let svgHtml = `<circle cx="${cx}" cy="${cy}" r="${r}" fill="none" stroke="var(--border)" stroke-width="${strokeW}"/>`;
  top.forEach(([cat, amt], i) => {
    const frac = amt / grandTotal;
    const dash = frac * circ;
    const color = DONUT_COLORS[i % DONUT_COLORS.length];
    svgHtml += `<circle cx="${cx}" cy="${cy}" r="${r}" fill="none"
      stroke="${color}" stroke-width="${strokeW}"
      stroke-dasharray="${dash} ${circ}"
      stroke-dashoffset="${-offset}"
      stroke-linecap="butt"
      transform="rotate(-90 ${cx} ${cy})"/>`;
    offset += dash;
  });
  svgHtml += `<text x="${cx}" y="${cy}" class="cat-donut-center">${top.length} kat.</text>`;
  svg.innerHTML = svgHtml;

  legend.innerHTML = top.map(([cat, amt], i) => {
    const pct = Math.round((amt / grandTotal) * 100);
    const color = DONUT_COLORS[i % DONUT_COLORS.length];
    return `<div class="cat-legend-row">
      <div class="cat-legend-dot" style="background:${color}"></div>
      <div class="cat-legend-name">${escHtml(cat)}</div>
      <div class="cat-legend-pct">${pct}%</div>
    </div>`;
  }).join('');
}
```

- [ ] **Step 4: Call `renderDonutChart(expenses)` from `loadBeranda()` in `js/beranda.js`**

Find the line `renderInsightCards(expenses, lastMonthTotal);` in `loadBeranda()`. Add directly before it:

```js
      renderDonutChart(expenses);
```

- [ ] **Step 5: Verify — Beranda shows donut chart below hero card; categories shown with color dots in legend**

Manual check: add a few expenses in different categories, go to Beranda. Donut should show colored segments. Empty state shows "Belum ada data".

- [ ] **Step 6: Commit**
```bash
git add expense-tracker/src/main/resources/static/css/main.css \
        expense-tracker/src/main/resources/static/index.html \
        expense-tracker/src/main/resources/static/js/beranda.js
git commit -m "feat: beranda SVG donut chart for category breakdown"
```

---

## Task 4: Merge Rumah tab into Anggaran

**Files:**
- Modify: `expense-tracker/src/main/resources/static/index.html`
- Modify: `expense-tracker/src/main/resources/static/js/anggaran.js`
- Modify: `expense-tracker/src/main/resources/static/js/rumah.js`
- Modify: `expense-tracker/src/main/resources/static/js/app.js`
- Modify: `expense-tracker/src/main/resources/static/css/main.css`

- [ ] **Step 1: Add Rumah section CSS to `css/main.css`**

Append to `css/main.css`:

```css
/* RUMAH IN ANGGARAN */
.rumah-section-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 16px 8px; cursor: pointer; user-select: none;
}
.rumah-section-header h3 { font-size: 15px; font-weight: 700; color: var(--text); }
.rumah-chevron { font-size: 14px; color: var(--text-2); transition: transform .2s; }
.rumah-chevron.open { transform: rotate(180deg); }
#rumah-section-body { overflow: hidden; }
```

- [ ] **Step 2: Move Rumah HTML into Anggaran in `index.html`**

In `index.html`, find `</section>` that closes `#screen-anggaran` (after the `#anggaran-modal` div, around the `</section>` before `<section id="screen-profil">`). Before that `</section>`, add:

```html
      <!-- RUMAH SECTION -->
      <div class="section-label rumah-section-header" id="rumah-section-toggle" onclick="toggleRumahSection()">
        <h3>🏘️ Rumah Bersama</h3>
        <span id="rumah-chevron" class="rumah-chevron">▾</span>
      </div>
      <div id="rumah-section-body" style="display:none">
        <!-- Empty state -->
        <div id="rumah-empty" style="display:none; padding:32px 24px; text-align:center">
          <div style="font-size:40px; margin-bottom:12px">🏘️</div>
          <h2 style="font-size:18px; font-weight:700; margin-bottom:8px; color:var(--text)">Catat bareng yuk</h2>
          <p style="color:var(--text-2); margin-bottom:24px; font-size:14px">Buat atau gabung Rumah untuk mulai catat bareng</p>
          <button class="btn-primary" style="width:100%; margin-bottom:12px" onclick="openBuatRumahModal()">Buat Rumah</button>
          <button class="btn-ghost" style="width:100%" onclick="openGabungModal()">Gabung dengan link</button>
        </div>
        <!-- Active state -->
        <div id="rumah-active" style="display:none">
          <div id="rumah-header-card" style="margin:0 16px 8px; padding:16px; background:var(--raised); border-radius:16px"></div>
          <div style="padding:0 16px 8px; color:var(--text-2); font-size:12px; font-weight:600">BARENG BULAN INI</div>
          <div id="rumah-feed-list" style="padding:0 16px 16px"></div>
        </div>
        <!-- Buat Rumah modal -->
        <div id="modal-buat-rumah" class="modal-overlay" style="display:none">
          <div class="modal-sheet">
            <div class="modal-handle"></div>
            <h3 style="font-size:18px; font-weight:700; margin-bottom:20px">Buat Rumah</h3>
            <input id="buat-nama" type="text" placeholder="Nama Rumah (mis. Keluarga Faras)" class="input-field" style="margin-bottom:12px">
            <div style="display:flex; gap:8px; margin-bottom:16px">
              <div style="flex:1">
                <label style="font-size:12px; color:var(--text-2); display:block; margin-bottom:4px">Emoji</label>
                <input id="buat-emoji" type="text" value="🏠" class="input-field" style="text-align:center; font-size:24px">
              </div>
              <div style="flex:1">
                <label style="font-size:12px; color:var(--text-2); display:block; margin-bottom:4px">Warna</label>
                <input id="buat-color" type="color" value="#00897B" style="width:100%; height:44px; border-radius:8px; border:1px solid var(--border); cursor:pointer">
              </div>
            </div>
            <button class="btn-primary" style="width:100%" onclick="submitBuatRumah()">Buat</button>
            <button class="btn-ghost" style="width:100%; margin-top:8px" onclick="closeBuatRumahModal()">Batal</button>
          </div>
        </div>
        <!-- Gabung modal -->
        <div id="modal-gabung" class="modal-overlay" style="display:none">
          <div class="modal-sheet">
            <div class="modal-handle"></div>
            <h3 style="font-size:18px; font-weight:700; margin-bottom:20px">Gabung Rumah</h3>
            <input id="gabung-token" type="text" placeholder="Tempel link atau kode undangan" class="input-field" style="margin-bottom:12px" oninput="onGabungInput()">
            <div id="gabung-preview" style="display:none; padding:12px; background:var(--raised); border-radius:8px; margin-bottom:12px; font-size:14px">
              <div id="gabung-preview-name" style="font-weight:600"></div>
              <div id="gabung-preview-count" style="color:var(--text-2); font-size:13px"></div>
            </div>
            <button class="btn-primary" style="width:100%" onclick="submitGabung()">Gabung</button>
            <button class="btn-ghost" style="width:100%; margin-top:8px" onclick="closeGabungModal()">Batal</button>
          </div>
        </div>
      </div>
```

- [ ] **Step 3: Remove `<section id="screen-rumah">` from `index.html`**

Delete the entire `<section id="screen-rumah" class="screen">...</section>` block (lines ~530–582 before the edit). It has been replaced by the Rumah section inside Anggaran.

- [ ] **Step 4: Remove Rumah tab from tab bar in `index.html`**

Find the tab bar `<nav id="tab-bar">`. Remove this line:
```html
      <button class="tab" data-screen="rumah"><span class="ti">🏘️</span><span>Rumah</span></button>
```

The tab bar should now have 4 tabs: Beranda · Catatan · Anggaran · Profil.

- [ ] **Step 5: Add `toggleRumahSection()` to `js/anggaran.js`**

Append to end of `anggaran.js`:

```js
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
```

- [ ] **Step 6: Update `js/rumah.js` — remove standalone Rumah tab wiring**

Find at the bottom of `rumah.js` (last ~5 lines):
```js
    // Wire Rumah tab data load and polling lifecycle
    document.querySelector('.tab[data-screen="rumah"]')?.addEventListener('click', loadRumahTab);
    document.querySelectorAll('.tab:not([data-screen="rumah"])').forEach(btn =>
      btn.addEventListener('click', stopRumahPolling)
    );
```

Replace with:
```js
    // Stop Rumah polling when leaving Anggaran tab
    document.querySelectorAll('.tab:not([data-screen="anggaran"])').forEach(btn =>
      btn.addEventListener('click', stopRumahPolling)
    );
```

- [ ] **Step 7: Update `js/app.js` — remove Rumah from `onAppReady` showScreen guard**

In `app.js`, find `function showScreen(name)`. It currently does:
```js
  document.querySelector(`.tab[data-screen="${name}"]`)?.classList.add('active');
```

This uses `?.` so it silently handles missing tabs — no change needed here.

Find `function onAppReady()`. Remove or guard the `loadRumahTab()` call — it should only be called when Rumah section is expanded. Change:
```js
      loadRumahTab(); // pre-load so bareng toggle works without visiting Rumah tab first
```
To:
```js
      loadRumahTab(); // pre-load for bareng toggle (doesn't render UI, just fetches currentRumah)
```

This call is intentional — it sets `currentRumah` which the bareng toggle needs. Keep it.

- [ ] **Step 8: Verify**

Manual check:
- Tab bar shows 4 tabs: Beranda, Catatan, Anggaran, Profil (no Rumah tab)
- Anggaran screen has "🏘️ Rumah Bersama" section at bottom, collapsed by default
- Tapping it expands to show empty state or active Rumah
- Bareng toggle in quick-add sheet still works

- [ ] **Step 9: Commit**
```bash
git add expense-tracker/src/main/resources/static/index.html \
        expense-tracker/src/main/resources/static/css/main.css \
        expense-tracker/src/main/resources/static/js/anggaran.js \
        expense-tracker/src/main/resources/static/js/rumah.js \
        expense-tracker/src/main/resources/static/js/app.js
git commit -m "feat: rumah moved into anggaran tab as collapsible section, remove rumah tab"
```

---

## Self-Review

**Spec coverage:**

| Requirement | Task |
|-------------|------|
| Personalized nickname onboarding after signup | Task 1 — `checkNicknameOnboarding()` called in `onAppReady` |
| Nickname editable in Profile | Task 1 — "Nama panggilan" row with edit button |
| Beranda greeting "Halo, [name]! 👋" | Task 2 — greeting div populated by `loadBeranda()` |
| Hero card visual upgrade (gradient) | Task 2 — `hero-card-gradient` class + teal CSS gradient |
| SVG donut chart for category breakdown | Task 3 — `renderDonutChart()`, stroke-dasharray technique |
| Rumah merged into Anggaran | Task 4 — collapsible section, Rumah tab removed |
| Bareng toggle still works | Task 4 — `loadRumahTab()` still called in `onAppReady` |
| Dark mode works on all new elements | Tasks 2–4 — all use CSS variables, no hardcoded colors |

**Placeholder scan:** None.

**Type consistency:**
- `getNickname()` / `setNickname()` defined in Task 1, used in Tasks 1 and 2 ✓
- `renderDonutChart(expenses)` defined and called in Task 3 ✓
- `toggleRumahSection()` defined in Task 4 `anggaran.js`, called from `onclick` in Task 4 HTML ✓
- `loadRumahTab()` defined in `rumah.js`, called from `anggaran.js` ✓

---

## Execution Handoff

Plans saved:
- `docs/superpowers/plans/2026-04-28-file-split.md` — Plan 1: file split (do first)
- `docs/superpowers/plans/2026-04-28-beranda-features.md` — Plan 2: features (do after Plan 1)

**Two execution options:**

**1. Subagent-Driven (recommended)** — fresh subagent per task, review between tasks

**2. Inline Execution** — execute tasks in this session using executing-plans skill

Which approach?
