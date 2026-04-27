# Phase 5 — Depth & Delight Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add 7 delight features (rotating insight cards, quiet streak, weekly insight card, in-app nudge, monthly summary modal, payday banner, Ramadan mode) as inline JS/HTML to `index.html` (all 7 features) and `desktop.html` (insight cards, weekly card, monthly summary, Ramadan mode).

**Architecture:** All features are frontend-only, computed from existing `/expenses?startDate=&endDate=` API. No new backend endpoints. State stored in `localStorage` under `catetu_*` keys. Inline JS only — no new static files. Mobile nudge uses browser `Notification` API with graceful degradation.

**Tech Stack:** Spring Boot 3.x · Vanilla HTML/CSS/JS · `localStorage` · Browser `Notification` API

**Spec:** `docs/superpowers/specs/2026-04-27-phase5-design.md`

---

## File Map

| File | Action | What Changes |
|---|---|---|
| `src/main/resources/static/index.html` | Modify | CSS + HTML (Beranda layout, Profil settings, overlay modal, toast) + JS (all 7 features) |
| `src/main/resources/static/desktop.html` | Modify | CSS + HTML (Beranda template, monthly modal) + JS (features 1, 3, 5, 7) |

### Key existing patterns (read before editing)

**`index.html`:**
- `CATS` = `{ 'Makan & Minum': '🍽️', ... }` — category → emoji map
- `CATEGORY_LIST` = array of `{emoji, label}` — used by `renderCatPills()`
- `formatRp(n)` — formats as "Rp 1.000"
- `today()` — returns today as YYYY-MM-DD
- `monthRange(offsetMonths)` — returns `{from, to}` for a calendar month
- `authFetch(url, opts)` — authenticated fetch
- `escHtml(s)` — HTML escaping
- `loadBeranda()` — fetches current + last month, renders Beranda

**`desktop.html`:**
- `CATEGORIES` = array of `{emoji, label}`
- `fmtRp(amount)` — formats as "Rp 1.000"
- `catEmoji(label)` — returns emoji for a label
- `api(method, path, body)` — authenticated fetch
- `buildCatPills(scope, containerId)` — renders category pills
- `loadBeranda()` — rewrites `#view-beranda` innerHTML entirely on each call

---

## Task 1 — CSS foundations (both files)

**Files:**
- Modify: `src/main/resources/static/index.html`
- Modify: `src/main/resources/static/desktop.html`

- [ ] **Step 1: Add Phase 5 CSS to `index.html`**

Find in the `<style>` block of `src/main/resources/static/index.html`:
```css
    .theme-btn.active { border-color: var(--accent); background: var(--accent-dim); color: var(--accent); font-weight: 600; }
```

Insert after it:
```css
    /* ── Phase 5: Depth & Delight ── */
    #streak-badge { padding: 4px 16px 2px; font-size: 12px; color: var(--text-2); }
    .insight-row { display: flex; gap: 10px; overflow-x: auto; padding: 0 16px 4px; scrollbar-width: none; }
    .insight-row::-webkit-scrollbar { display: none; }
    .insight-card { flex: 0 0 calc(100% - 40px); background: var(--card-bg, #fff); border: 1px solid rgba(0,0,0,.07); border-radius: 12px; padding: 14px 34px 14px 14px; position: relative; font-size: 13px; line-height: 1.5; color: var(--text-1); box-shadow: 0 1px 4px rgba(0,0,0,.04); }
    .insight-dismiss { position: absolute; top: 8px; right: 10px; background: none; border: none; cursor: pointer; color: var(--text-2); font-size: 18px; line-height: 1; padding: 2px 4px; }
    #weekly-insight-wrap { display: none; padding: 0 16px 8px; }
    .weekly-card { background: var(--accent); border-radius: 12px; padding: 14px 34px 14px 14px; font-size: 13px; color: #fff; line-height: 1.5; position: relative; }
    .weekly-card .insight-dismiss { color: rgba(255,255,255,.75); }
    #payday-banner { display: none; margin: 0 16px 8px; background: #E0F2F1; border-radius: 12px; padding: 12px 14px; flex-direction: row; align-items: center; justify-content: space-between; gap: 10px; }
    #ramadan-suggestion { display: none; margin: 0 16px 8px; background: #FFF8E7; border: 1px solid #F59E0B; border-radius: 12px; padding: 14px; font-size: 13px; }
    .ramadan-btns { display: flex; gap: 8px; margin-top: 10px; }
    .btn-ramadan-on { background: var(--accent); color: #fff; border-radius: 8px; padding: 8px 14px; font-size: 13px; font-weight: 600; border: none; cursor: pointer; }
    .btn-ramadan-off { background: none; border: 1px solid var(--text-2); border-radius: 8px; padding: 8px 14px; font-size: 13px; color: var(--text-2); cursor: pointer; }
    #nudge-toast { position: fixed; bottom: 90px; left: 16px; right: 16px; background: #fff; border: 1px solid rgba(0,0,0,.09); border-radius: 14px; padding: 14px 16px; box-shadow: 0 4px 24px rgba(0,0,0,.13); display: none; z-index: 999; }
    #nudge-toast .toast-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
    .monthly-overlay { position: fixed; inset: 0; background: rgba(0,0,0,.5); z-index: 200; display: flex; align-items: flex-end; }
    .monthly-sheet { background: #fff; border-radius: 20px 20px 0 0; padding: 24px 20px 32px; width: 100%; }
    .monthly-sheet h2 { font-size: 18px; font-weight: 700; margin-bottom: 4px; }
    .monthly-sub { font-size: 13px; color: var(--text-2); margin-bottom: 20px; }
    .monthly-total { font-family: var(--mono); font-size: 28px; font-weight: 700; margin: 8px 0 16px; }
    .monthly-row { display: flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid rgba(0,0,0,.06); font-size: 14px; }
    .monthly-close { width: 100%; padding: 14px; background: var(--accent); color: #fff; border-radius: 10px; font-weight: 600; font-size: 15px; margin-top: 20px; border: none; cursor: pointer; }
```

- [ ] **Step 2: Verify CSS was added (no duplicate)**

```bash
grep -c "insight-card\|weekly-card\|nudge-toast\|monthly-overlay\|ramadan-suggestion" src/main/resources/static/index.html
```
Expected: each class name appears exactly once.

- [ ] **Step 3: Add Phase 5 CSS to `desktop.html`**

Find in the `<style>` block of `src/main/resources/static/desktop.html`:
```css
.stat-tile:hover{box-shadow:0 2px 12px rgba(0,0,0,.08)}
```

Insert after it:
```css
/* ── Phase 5: Depth & Delight ── */
.insight-row-d{display:flex;gap:10px;margin-bottom:16px;overflow-x:auto}
.insight-card-d{flex:0 0 280px;background:#fff;border:1px solid var(--border);border-radius:8px;padding:12px 30px 12px 14px;font-size:13px;line-height:1.45;color:var(--text);position:relative}
.insight-dismiss-d{position:absolute;top:6px;right:8px;background:none;border:none;cursor:pointer;color:var(--text-muted);font-size:16px;padding:2px 4px}
.weekly-card-d{background:var(--accent);border-radius:8px;padding:12px 30px 12px 14px;font-size:13px;color:#fff;position:relative;margin-bottom:16px}
.weekly-dismiss-d{position:absolute;top:6px;right:8px;background:none;border:none;cursor:pointer;color:rgba(255,255,255,.75);font-size:16px;padding:2px 4px}
.ramadan-banner-d{background:#FFF8E7;border:1px solid #F59E0B;border-radius:8px;padding:12px 14px;font-size:13px;margin-bottom:16px;display:flex;align-items:center;justify-content:space-between;gap:12px}
.monthly-overlay-d{position:fixed;inset:0;background:rgba(0,0,0,.5);z-index:100;display:flex;align-items:center;justify-content:center}
.monthly-card-d{background:#fff;border-radius:12px;padding:28px 28px 24px;width:420px;max-width:90vw}
.monthly-card-d h2{font-size:18px;font-weight:700;margin-bottom:4px}
.m-sub{font-size:13px;color:var(--text-muted);margin-bottom:20px}
.m-total{font-family:'JetBrains Mono',monospace;font-size:28px;font-weight:700;margin:8px 0 16px}
.m-row{display:flex;justify-content:space-between;padding:8px 0;border-bottom:1px solid var(--border);font-size:14px}
.m-close{width:100%;padding:12px;background:var(--accent);color:#fff;border-radius:8px;font-weight:600;font-size:14px;margin-top:20px;border:none;cursor:pointer}
```

- [ ] **Step 4: Verify CSS was added**

```bash
grep -c "insight-card-d\|weekly-card-d\|monthly-overlay-d\|ramadan-banner-d" src/main/resources/static/desktop.html
```
Expected: each class name appears exactly once.

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/static/index.html src/main/resources/static/desktop.html
git commit -m "feat: add Phase 5 CSS to index.html and desktop.html"
```

---

## Task 2 — Mobile: Insight Engine

**Files:**
- Modify: `src/main/resources/static/index.html`

- [ ] **Step 1: Replace the existing single-insight-card HTML with the Phase 5 Beranda containers**

Find in `src/main/resources/static/index.html`:
```html
      <div id="beranda-insight" class="card" style="padding:14px 16px;display:none">
        <div id="beranda-insight-text" style="font-size:13px;color:var(--text-2)"></div>
      </div>
      <div class="section-label">Pengeluaran Terbaru</div>
```

Replace with:
```html
      <div id="streak-badge"></div>
      <div id="weekly-insight-wrap">
        <div class="weekly-card">
          <span id="weekly-insight-text"></span>
          <button class="insight-dismiss" onclick="dismissWeeklyCard()">×</button>
        </div>
      </div>
      <div id="ramadan-suggestion" class="ramadan-suggestion">
        <div>🌙 Ramadan sebentar lagi — aktifkan Mode Ramadan?</div>
        <div class="ramadan-btns">
          <button class="btn-ramadan-on" onclick="activateRamadan()">Aktifkan</button>
          <button class="btn-ramadan-off" onclick="dismissRamadanSuggestion()">Nanti saja</button>
        </div>
      </div>
      <div id="payday-banner">
        <span style="font-size:13px">Gajian nih — yuk rencanain bulannya 💸</span>
        <button onclick="showScreen('anggaran')" style="font-size:12px;font-weight:600;color:var(--accent);background:none;border:none;cursor:pointer">Atur Anggaran →</button>
      </div>
      <div id="insight-cards" class="insight-row"></div>
      <div class="section-label">Pengeluaran Terbaru</div>
```

- [ ] **Step 2: Add insight engine JS functions**

Find in `src/main/resources/static/index.html`:
```javascript
    async function loadBeranda() {
```

Insert directly before it:
```javascript
    // ── Phase 5: Insight Engine ──
    function isoWeek(date = new Date()) {
      const d = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()));
      const day = d.getUTCDay() || 7;
      d.setUTCDate(d.getUTCDate() + 4 - day);
      const y = new Date(Date.UTC(d.getUTCFullYear(), 0, 1));
      return `${d.getUTCFullYear()}-W${String(Math.ceil((((d - y) / 86400000) + 1) / 7)).padStart(2, '0')}`;
    }

    function getDismissed() { return JSON.parse(localStorage.getItem('catetu_dismissed_insights') || '[]'); }
    function addDismissed(id) {
      const d = getDismissed();
      if (!d.includes(id)) d.push(id);
      localStorage.setItem('catetu_dismissed_insights', JSON.stringify(d));
    }
    function maybeClearMonthlyDismissals() {
      const key = 'catetu_dismissed_insights_month';
      const now = new Date();
      const cur = `${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,'0')}`;
      if (now.getDate() === 1 && localStorage.getItem(key) !== cur) {
        localStorage.removeItem('catetu_dismissed_insights');
        localStorage.setItem(key, cur);
      }
    }

    function computeInsights(expenses, lastMonthTotal) {
      maybeClearMonthlyDismissals();
      const dismissed = getDismissed();
      const now = new Date();
      const daysElapsed = Math.max(now.getDate(), 1);
      const results = [];

      if (!expenses || expenses.length === 0) return results;

      // top-category
      if (!dismissed.includes('top-category')) {
        const totals = {};
        expenses.forEach(e => { totals[e.category] = (totals[e.category]||0) + e.amount; });
        const [cat, amt] = Object.entries(totals).sort((a,b) => b[1]-a[1])[0];
        results.push({ id: 'top-category', text: `Pengeluaran terbesar bulan ini: ${CATS[cat]||'📦'} ${cat} — ${formatRp(amt)}` });
      }

      // savings-vs-last (only when spending less than last month)
      if (!dismissed.includes('savings-vs-last') && lastMonthTotal > 0) {
        const thisTotal = expenses.reduce((s, e) => s + e.amount, 0);
        const pct = Math.round((thisTotal / lastMonthTotal) * 100);
        if (pct < 100) {
          results.push({ id: 'savings-vs-last', text: `Kamu udah hemat ${100 - pct}% dibanding bulan lalu 🎉` });
        }
      }

      // busiest-day
      if (!dismissed.includes('busiest-day') && expenses.length >= 3) {
        const byDay = {};
        expenses.forEach(e => { const d = (e.date||'').substring(0,10); byDay[d] = (byDay[d]||0)+1; });
        const [day, count] = Object.entries(byDay).sort((a,b) => b[1]-a[1])[0];
        if (count >= 3) {
          const label = new Date(day + 'T00:00:00').toLocaleDateString('id-ID', {weekday:'long', day:'numeric', month:'short'});
          results.push({ id: 'busiest-day', text: `Hari tersibuk: ${label} — ${count} transaksi` });
        }
      }

      // daily-avg
      if (!dismissed.includes('daily-avg')) {
        const avg = Math.round(expenses.reduce((s,e)=>s+e.amount,0) / daysElapsed);
        results.push({ id: 'daily-avg', text: `Rata-rata harian bulan ini: ${formatRp(avg)}` });
      }

      // top-frequency
      if (!dismissed.includes('top-frequency') && expenses.length >= 5) {
        const freq = {};
        expenses.forEach(e => { freq[e.category] = (freq[e.category]||0)+1; });
        const [cat, n] = Object.entries(freq).sort((a,b) => b[1]-a[1])[0];
        if (n >= 3) results.push({ id: 'top-frequency', text: `Paling sering: ${CATS[cat]||'📦'} ${cat} (${n}x bulan ini)` });
      }

      return results.slice(0, 3);
    }

    function renderInsightCards(expenses, lastMonthTotal) {
      const container = document.getElementById('insight-cards');
      if (!container) return;
      const cards = computeInsights(expenses, lastMonthTotal);
      if (!cards.length) { container.innerHTML = ''; return; }
      container.innerHTML = cards.map(c =>
        `<div class="insight-card" data-insight-id="${c.id}">
          ${escHtml(c.text)}
          <button class="insight-dismiss" onclick="dismissInsight('${c.id}')">×</button>
        </div>`
      ).join('');
    }

    function dismissInsight(id) {
      addDismissed(id);
      document.querySelector(`.insight-card[data-insight-id="${id}"]`)?.remove();
    }

```

- [ ] **Step 3: Replace the old insight logic in `loadBeranda()` with the new call**

Find in `src/main/resources/static/index.html`:
```javascript
      if (expenses.length >= 3) {
        const totals = {};
        expenses.forEach(e => { totals[e.category] = (totals[e.category] || 0) + e.amount; });
        const [topCat, topAmt] = Object.entries(totals).sort((a, b) => b[1] - a[1])[0];
        document.getElementById('beranda-insight-text').textContent =
          `${CATS[topCat] || '📦'} ${topCat} jadi pengeluaran terbesar bulan ini (${formatRp(topAmt)})`;
        document.getElementById('beranda-insight').style.display = 'block';
      }

      renderDayGroups(expenses.slice(0, 20), 'beranda-list', 'beranda-empty');
```

Replace with:
```javascript
      const lastMonthTotal = lastMonth.reduce((s, e) => s + e.amount, 0);
      renderInsightCards(expenses, lastMonthTotal);

      renderDayGroups(expenses.slice(0, 20), 'beranda-list', 'beranda-empty');
```

- [ ] **Step 4: Manual test**

```bash
# From expense-tracker/ directory
make run
```
Log in, go to Beranda. With expenses this month: insight cards appear in a horizontal scroll row. Click × on a card → it disappears. Refresh → dismissed card is still gone. In DevTools console: `localStorage.removeItem('catetu_dismissed_insights')` → refresh → all cards reappear.

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/static/index.html
git commit -m "feat: add rotating insight cards to Beranda (mobile)"
```

---

## Task 3 — Mobile: Quiet Streak Display

**Files:**
- Modify: `src/main/resources/static/index.html`

- [ ] **Step 1: Add streak JS functions**

Find in `src/main/resources/static/index.html`:
```javascript
    function dismissInsight(id) {
      addDismissed(id);
      document.querySelector(`.insight-card[data-insight-id="${id}"]`)?.remove();
    }
```

Insert after it:
```javascript

    function computeStreak(expenses) {
      const dateSet = new Set(expenses.map(e => (e.date||'').substring(0,10)));
      const now = new Date();
      let streak = 0;
      for (let i = 0; i < 365; i++) {
        const d = new Date(now);
        d.setDate(d.getDate() - i);
        if (dateSet.has(d.toISOString().split('T')[0])) { streak++; } else { break; }
      }
      return streak;
    }

    function renderStreak(expenses) {
      const badge = document.getElementById('streak-badge');
      if (!badge) return;
      const s = computeStreak(expenses);
      if (s < 2) { badge.style.display = 'none'; return; }
      badge.style.display = 'block';
      badge.textContent = `📅 ${s} hari berturut-turut`;
    }

```

- [ ] **Step 2: Call `renderStreak()` from `loadBeranda()`**

Find in `src/main/resources/static/index.html`:
```javascript
      renderInsightCards(expenses, lastMonthTotal);

      renderDayGroups(expenses.slice(0, 20), 'beranda-list', 'beranda-empty');
```

Replace with:
```javascript
      renderInsightCards(expenses, lastMonthTotal);
      renderStreak(expenses);

      renderDayGroups(expenses.slice(0, 20), 'beranda-list', 'beranda-empty');
```

- [ ] **Step 3: Manual test**

Log an expense on today's date and one on yesterday's date. Reload Beranda → streak badge shows "📅 2 hari berturut-turut". In DevTools console to verify hiding: `localStorage.removeItem(...)` is not needed — just check with only 1 day of expenses → badge is hidden.

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/static/index.html
git commit -m "feat: add quiet streak display to Beranda (mobile)"
```

---

## Task 4 — Mobile: Weekly Insight Card

**Files:**
- Modify: `src/main/resources/static/index.html`

- [ ] **Step 1: Add weekly insight JS functions**

Find in `src/main/resources/static/index.html`:
```javascript
    function computeStreak(expenses) {
```

Insert before it:
```javascript
    async function checkWeeklyInsight() {
      const weekKey = 'catetu_weekly_insight_week';
      const cardKey = 'catetu_weekly_insight_card';
      const dismissKey = 'catetu_weekly_card_dismissed_week';
      const now = new Date();
      const curWeek = isoWeek(now);
      const wrap = document.getElementById('weekly-insight-wrap');
      const textEl = document.getElementById('weekly-insight-text');
      if (!wrap || !textEl) return;

      if (localStorage.getItem(dismissKey) === curWeek) { wrap.style.display = 'none'; return; }

      // Generate on Mondays when not yet generated this week
      if (now.getDay() === 1 && localStorage.getItem(weekKey) !== curWeek) {
        const end = now.toISOString().split('T')[0];
        const startD = new Date(now); startD.setDate(startD.getDate() - 6);
        const start = startD.toISOString().split('T')[0];
        try {
          const r = await authFetch(`/expenses?startDate=${start}&endDate=${end}`);
          const exp = await r.json();
          if (exp.length > 0) {
            const totals = {};
            exp.forEach(e => { totals[e.category] = (totals[e.category]||0)+e.amount; });
            const [cat, amt] = Object.entries(totals).sort((a,b)=>b[1]-a[1])[0];
            localStorage.setItem(cardKey, `Minggu lalu, ${CATS[cat]||'📦'} ${cat} jadi pengeluaran terbesar (${formatRp(amt)})`);
            localStorage.setItem(weekKey, curWeek);
          }
        } catch (_) {}
      }

      const saved = localStorage.getItem(cardKey);
      if (saved && localStorage.getItem(weekKey) === curWeek) {
        textEl.textContent = saved;
        wrap.style.display = 'block';
      } else {
        wrap.style.display = 'none';
      }
    }

    function dismissWeeklyCard() {
      localStorage.setItem('catetu_weekly_card_dismissed_week', isoWeek());
      const wrap = document.getElementById('weekly-insight-wrap');
      if (wrap) wrap.style.display = 'none';
    }

```

- [ ] **Step 2: Call `checkWeeklyInsight()` from `loadBeranda()`**

Find:
```javascript
      renderInsightCards(expenses, lastMonthTotal);
      renderStreak(expenses);
```

Replace with:
```javascript
      renderInsightCards(expenses, lastMonthTotal);
      renderStreak(expenses);
      checkWeeklyInsight();
```

- [ ] **Step 3: Manual test (simulate Monday generation)**

In DevTools console:
```javascript
// Force: pretend last week's card was never generated
localStorage.setItem('catetu_weekly_insight_week', '1999-W01');
localStorage.removeItem('catetu_weekly_insight_card');
// Then manually call (works any day of the week for testing):
// loadBeranda() will call checkWeeklyInsight() but won't generate unless today is Monday
// To test card display, set a card manually:
localStorage.setItem('catetu_weekly_insight_card', 'Minggu lalu, 🍽️ Makan & Minum jadi pengeluaran terbesar (Rp 300.000)');
localStorage.setItem('catetu_weekly_insight_week', isoWeek(new Date()));
```
Reload → teal weekly card appears. Click × → gone. Refresh → still gone. DevTools: remove `catetu_weekly_card_dismissed_week` → reload → reappears.

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/static/index.html
git commit -m "feat: add weekly insight card to Beranda (mobile)"
```

---

## Task 5 — Mobile: In-App Nudge + Profil Settings

**Files:**
- Modify: `src/main/resources/static/index.html`

- [ ] **Step 1: Add nudge toast HTML**

Find in `src/main/resources/static/index.html`:
```html
    <div id="sheet-overlay"></div>
```

Insert before it:
```html
    <div id="nudge-toast">
      <div class="toast-row">
        <span style="font-size:14px">Udah catat pengeluaran hari ini? 📝</span>
        <button onclick="closeNudgeToast()" style="background:none;border:none;cursor:pointer;font-size:20px;color:var(--text-2);line-height:1">×</button>
      </div>
      <button onclick="openQuickAdd();closeNudgeToast()" style="width:100%;margin-top:10px;padding:10px;background:var(--accent);color:#fff;border-radius:8px;font-size:14px;font-weight:600;border:none;cursor:pointer">Catat Sekarang</button>
    </div>
    <div id="monthly-summary-overlay" style="display:none" class="monthly-overlay">
      <div class="monthly-sheet">
        <h2 id="monthly-summary-title">Ringkasan Bulan Lalu</h2>
        <div class="monthly-sub" id="monthly-summary-sub"></div>
        <div class="monthly-total" id="monthly-summary-total"></div>
        <div id="monthly-summary-rows"></div>
        <button class="monthly-close" onclick="closeMonthlySummary()">Tutup</button>
      </div>
    </div>
```

- [ ] **Step 2: Add nudge JS functions**

Find in `src/main/resources/static/index.html`:
```javascript
    function dismissWeeklyCard() {
```

Insert before it:
```javascript
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

```

- [ ] **Step 3: Add Profil settings HTML**

Find in `src/main/resources/static/index.html`:
```html
      <div class="section-label">Tentang</div>
```

Insert before it:
```html
      <div class="section-label">Pengaturan</div>
      <div class="card">
        <div class="settings-row">
          <span style="font-size:14px">Pengingat harian</span>
          <input type="time" id="nudge-time-input" value="21:00" onchange="saveNudgeSetting()" style="border:1px solid rgba(0,0,0,.12);border-radius:6px;padding:4px 8px;font-size:13px">
        </div>
        <div class="settings-row" style="border-top:1px solid rgba(0,0,0,.05);padding-top:12px;margin-top:4px">
          <span style="font-size:14px">Aktifkan pengingat</span>
          <input type="checkbox" id="nudge-enabled-cb" onchange="saveNudgeSetting()" style="width:18px;height:18px;accent-color:var(--accent);cursor:pointer">
        </div>
        <div class="settings-row" style="border-top:1px solid rgba(0,0,0,.05);padding-top:12px;margin-top:4px">
          <span style="font-size:14px">Tanggal gajian</span>
          <input type="number" id="payday-date-input" min="1" max="31" placeholder="—" onchange="savePaydaySetting()" style="width:60px;border:1px solid rgba(0,0,0,.12);border-radius:6px;padding:4px 8px;font-size:13px;text-align:center">
        </div>
        <div class="settings-row" style="border-top:1px solid rgba(0,0,0,.05);padding-top:12px;margin-top:4px">
          <span style="font-size:14px">Mode Ramadan</span>
          <input type="checkbox" id="ramadan-toggle-cb" onchange="saveRamadanSetting()" style="width:18px;height:18px;accent-color:var(--accent);cursor:pointer">
        </div>
      </div>
```

- [ ] **Step 4: Add Profil settings JS + update `loadProfil()`**

Find in `src/main/resources/static/index.html`:
```javascript
    async function loadProfil() {
```

Insert before it:
```javascript
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

```

Find:
```javascript
    async function loadProfil() {
      try {
        const res = await authFetch('/auth/me');
        const user = await res.json();
        const email = user.email || '';
        document.getElementById('profil-email').textContent = email;
        document.getElementById('profil-avatar').textContent = email.split('@')[0].slice(0, 2).toUpperCase();
      } catch (_) {}
    }
```

Replace with:
```javascript
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
```

- [ ] **Step 5: Wire `scheduleNudge()` into `loadBeranda()`**

Find:
```javascript
      renderInsightCards(expenses, lastMonthTotal);
      renderStreak(expenses);
      checkWeeklyInsight();
```

Replace with:
```javascript
      renderInsightCards(expenses, lastMonthTotal);
      renderStreak(expenses);
      checkWeeklyInsight();
      const todayStr = today();
      const todayCount = expenses.filter(e => (e.date||'').substring(0,10) === todayStr).length;
      scheduleNudge(todayCount);
```

- [ ] **Step 6: Manual test**

Go to Profil → Pengaturan. Verify inputs load from localStorage. Set nudge time to 2 minutes from now. No expenses today. Stay on Beranda and wait → toast appears. Click "Catat Sekarang" → Quick-Add sheet opens. Test checkbox toggles persist after navigating away and back.

- [ ] **Step 7: Commit**

```bash
git add src/main/resources/static/index.html
git commit -m "feat: add in-app nudge and Profil settings (mobile)"
```

---

## Task 6 — Mobile: Monthly Summary Modal

**Files:**
- Modify: `src/main/resources/static/index.html`

(The modal HTML was already added in Task 5 Step 1 as `#monthly-summary-overlay`.)

- [ ] **Step 1: Add monthly summary JS**

Find in `src/main/resources/static/index.html`:
```javascript
    function loadProfilSettings() {
```

Insert before it:
```javascript
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

```

- [ ] **Step 2: Call `checkMonthlySummary()` from `loadBeranda()`**

Find:
```javascript
      const todayStr = today();
      const todayCount = expenses.filter(e => (e.date||'').substring(0,10) === todayStr).length;
      scheduleNudge(todayCount);
```

Replace with:
```javascript
      const todayStr = today();
      const todayCount = expenses.filter(e => (e.date||'').substring(0,10) === todayStr).length;
      scheduleNudge(todayCount);
      checkMonthlySummary();
```

- [ ] **Step 3: Manual test**

In DevTools console:
```javascript
localStorage.setItem('catetu_last_summary_month', '2020-01');
```
If today is day 1–3, reload Beranda → monthly summary modal appears over the screen showing last month's total and top 3 categories. Click "Tutup" → gone. Reload → does not reappear (same month stored).

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/static/index.html
git commit -m "feat: add monthly summary modal (mobile)"
```

---

## Task 7 — Mobile: Payday Banner

**Files:**
- Modify: `src/main/resources/static/index.html`

- [ ] **Step 1: Add payday JS**

Find in `src/main/resources/static/index.html`:
```javascript
    async function checkMonthlySummary() {
```

Insert before it:
```javascript
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
      // Add dismiss button once
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

```

- [ ] **Step 2: Call `checkPaydayBanner()` from `loadBeranda()`**

Find:
```javascript
      checkMonthlySummary();
```

Replace with:
```javascript
      checkMonthlySummary();
      checkPaydayBanner();
```

- [ ] **Step 3: Manual test**

In DevTools console:
```javascript
localStorage.setItem('catetu_payday_date', String(new Date().getDate()));
```
Reload Beranda → green banner appears: "Gajian nih — yuk rencanain bulannya 💸". Click × → gone. Reload → still gone (dismissed for this month). Click "Atur Anggaran →" → navigates to Anggaran tab.

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/static/index.html
git commit -m "feat: add payday mode banner to Beranda (mobile)"
```

---

## Task 8 — Mobile: Ramadan Mode

**Files:**
- Modify: `src/main/resources/static/index.html`

- [ ] **Step 1: Add Ramadan JS**

Find in `src/main/resources/static/index.html`:
```javascript
    function checkPaydayBanner() {
```

Insert before it:
```javascript
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
      if (now >= range.start) return; // in Ramadan already, suggestion no longer needed
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

```

- [ ] **Step 2: Call `checkRamadanSuggestion()` from `loadBeranda()`**

Find:
```javascript
      checkMonthlySummary();
      checkPaydayBanner();
```

Replace with:
```javascript
      checkMonthlySummary();
      checkPaydayBanner();
      checkRamadanSuggestion();
```

- [ ] **Step 3: Apply Ramadan label overrides in `renderCatPills()`**

Find in `src/main/resources/static/index.html`:
```javascript
    function renderCatPills() {
      document.getElementById('sheet-cats').innerHTML = CATEGORY_LIST.map(c =>
        `<button class="cat-pill${c.label === sheetCat ? ' selected' : ''}" data-cat="${escHtml(c.label)}">${c.emoji} ${escHtml(c.label)}</button>`
      ).join('');
```

Replace with:
```javascript
    function renderCatPills() {
      document.getElementById('sheet-cats').innerHTML = CATEGORY_LIST.map(c =>
        `<button class="cat-pill${c.label === sheetCat ? ' selected' : ''}" data-cat="${escHtml(c.label)}">${c.emoji} ${escHtml(getDisplayLabel(c.label))}</button>`
      ).join('');
```

- [ ] **Step 4: Manual test**

In DevTools console:
```javascript
localStorage.setItem('catetu_ramadan_mode', 'true');
```
Reload app, open Quick-Add sheet → category pills show "Sahur & Berbuka" and "Zakat & Infaq". An expense saved still stores the original category label (verify via Catatan view). Toggle off via Profil → Pengaturan → labels revert instantly.

To test Ramadan suggestion: set system date to Feb 4, 2026 (14 days before Feb 18), or:
```javascript
localStorage.removeItem('catetu_ramadan_suggestion_2026');
// Then manually trigger:
checkRamadanSuggestion();
```
→ Yellow suggestion card appears on Beranda.

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/static/index.html
git commit -m "feat: add Ramadan mode — relabeled category pills and suggestion card (mobile)"
```

---

## Task 9 — Desktop: Insight Engine + Weekly Card

**Files:**
- Modify: `src/main/resources/static/desktop.html`

- [ ] **Step 1: Add insight engine + ISO week JS to `desktop.html`**

Find in `src/main/resources/static/desktop.html`:
```javascript
async function loadBeranda() {
```

Insert before it:
```javascript
// ── Phase 5: Insight Engine (desktop) ──
function isoWeek(date = new Date()) {
  const d = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()));
  const day = d.getUTCDay() || 7;
  d.setUTCDate(d.getUTCDate() + 4 - day);
  const y = new Date(Date.UTC(d.getUTCFullYear(), 0, 1));
  return `${d.getUTCFullYear()}-W${String(Math.ceil((((d - y) / 86400000) + 1) / 7)).padStart(2, '0')}`;
}

function getDismissedD() { return JSON.parse(localStorage.getItem('catetu_dismissed_insights') || '[]'); }
function addDismissedD(id) {
  const d = getDismissedD();
  if (!d.includes(id)) d.push(id);
  localStorage.setItem('catetu_dismissed_insights', JSON.stringify(d));
}
function maybeClearMonthlyDismissals() {
  const key = 'catetu_dismissed_insights_month';
  const now = new Date();
  const cur = `${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,'0')}`;
  if (now.getDate() === 1 && localStorage.getItem(key) !== cur) {
    localStorage.removeItem('catetu_dismissed_insights');
    localStorage.setItem(key, cur);
  }
}

function computeInsightsD(expenses, lastMonthTotal) {
  maybeClearMonthlyDismissals();
  const dismissed = getDismissedD();
  const now = new Date();
  const daysElapsed = Math.max(now.getDate(), 1);
  const results = [];

  if (!expenses || expenses.length === 0) return results;

  if (!dismissed.includes('top-category')) {
    const totals = {};
    expenses.forEach(e => { totals[e.category] = (totals[e.category]||0)+e.amount; });
    const [cat, amt] = Object.entries(totals).sort((a,b) => b[1]-a[1])[0];
    const emoji = catEmoji(cat);
    results.push({ id: 'top-category', text: `Pengeluaran terbesar bulan ini: ${emoji} ${cat} — ${fmtRp(amt)}` });
  }
  if (!dismissed.includes('savings-vs-last') && lastMonthTotal > 0) {
    const thisTotal = expenses.reduce((s, e) => s + e.amount, 0);
    const pct = Math.round((thisTotal / lastMonthTotal) * 100);
    if (pct < 100) results.push({ id: 'savings-vs-last', text: `Hemat ${100-pct}% dibanding bulan lalu 🎉` });
  }
  if (!dismissed.includes('busiest-day') && expenses.length >= 3) {
    const byDay = {};
    expenses.forEach(e => { const d = (e.date||'').substring(0,10); byDay[d] = (byDay[d]||0)+1; });
    const [day, count] = Object.entries(byDay).sort((a,b) => b[1]-a[1])[0];
    if (count >= 3) {
      const label = new Date(day+'T00:00:00').toLocaleDateString('id-ID', {weekday:'long', day:'numeric', month:'short'});
      results.push({ id: 'busiest-day', text: `Hari tersibuk: ${label} — ${count} transaksi` });
    }
  }
  if (!dismissed.includes('daily-avg')) {
    const avg = Math.round(expenses.reduce((s,e)=>s+e.amount,0)/daysElapsed);
    results.push({ id: 'daily-avg', text: `Rata-rata harian bulan ini: ${fmtRp(avg)}` });
  }
  if (!dismissed.includes('top-frequency') && expenses.length >= 5) {
    const freq = {};
    expenses.forEach(e => { freq[e.category] = (freq[e.category]||0)+1; });
    const [cat, n] = Object.entries(freq).sort((a,b) => b[1]-a[1])[0];
    if (n >= 3) results.push({ id: 'top-frequency', text: `Paling sering: ${catEmoji(cat)} ${cat} (${n}x bulan ini)` });
  }
  return results.slice(0, 3);
}

function dismissInsightD(id) {
  addDismissedD(id);
  document.querySelector(`.insight-card-d[data-insight-id="${id}"]`)?.remove();
}

async function getWeeklyInsightHtml() {
  const weekKey = 'catetu_weekly_insight_week';
  const cardKey = 'catetu_weekly_insight_card';
  const dismissKey = 'catetu_weekly_card_dismissed_week';
  const now = new Date();
  const curWeek = isoWeek(now);
  if (localStorage.getItem(dismissKey) === curWeek) return '';

  if (now.getDay() === 1 && localStorage.getItem(weekKey) !== curWeek) {
    const end = now.toISOString().split('T')[0];
    const startD = new Date(now); startD.setDate(startD.getDate() - 6);
    const start = startD.toISOString().split('T')[0];
    try {
      const r = await api('GET', `/expenses?startDate=${start}&endDate=${end}`);
      if (r && r.ok) {
        const exp = await r.json();
        if (exp.length > 0) {
          const totals = {};
          exp.forEach(e => { totals[e.category] = (totals[e.category]||0)+e.amount; });
          const [cat, amt] = Object.entries(totals).sort((a,b)=>b[1]-a[1])[0];
          localStorage.setItem(cardKey, `Minggu lalu, ${catEmoji(cat)} ${cat} jadi pengeluaran terbesar (${fmtRp(amt)})`);
          localStorage.setItem(weekKey, curWeek);
        }
      }
    } catch (_) {}
  }

  const saved = localStorage.getItem(cardKey);
  if (!saved || localStorage.getItem(weekKey) !== curWeek) return '';
  return `<div class="weekly-card-d">${saved}<button class="weekly-dismiss-d" onclick="dismissWeeklyCardD()">×</button></div>`;
}

function dismissWeeklyCardD() {
  localStorage.setItem('catetu_weekly_card_dismissed_week', isoWeek());
  document.querySelector('.weekly-card-d')?.remove();
}

```

- [ ] **Step 2: Add insight row + weekly card HTML into `loadBeranda()` template**

Find in `src/main/resources/static/desktop.html`:
```javascript
  const monthLabel = now.toLocaleDateString('id-ID', {month:'long', year:'numeric'});
  document.getElementById('beranda-month').textContent = monthLabel;

  const recent = [...thisMonth].sort((a,b) => b.date.localeCompare(a.date)).slice(0, 8);

  view.innerHTML = `
    <div class="view-title">Beranda</div>
    <div class="view-subtitle" id="beranda-month">${monthLabel}</div>
    <div class="stat-grid">
```

Replace with:
```javascript
  const monthLabel = now.toLocaleDateString('id-ID', {month:'long', year:'numeric'});
  document.getElementById('beranda-month').textContent = monthLabel;

  const recent = [...thisMonth].sort((a,b) => b.date.localeCompare(a.date)).slice(0, 8);

  const insightCards = computeInsightsD(thisMonth, totalLast);
  const insightRowHtml = insightCards.length
    ? `<div class="insight-row-d">${insightCards.map(c =>
        `<div class="insight-card-d" data-insight-id="${c.id}">${c.text}<button class="insight-dismiss-d" onclick="dismissInsightD('${c.id}')">×</button></div>`
      ).join('')}</div>`
    : '';

  const weeklyHtml = await getWeeklyInsightHtml();

  view.innerHTML = `
    <div class="view-title">Beranda</div>
    <div class="view-subtitle" id="beranda-month">${monthLabel}</div>
    ${weeklyHtml}
    ${insightRowHtml}
    <div class="stat-grid">
```

- [ ] **Step 3: Manual test**

Open `localhost:8080/desktop.html`, log in. With expenses this month: insight cards appear as a horizontal row of smaller cards below the weekly card area. Click × → dismissed. Refresh → still dismissed.

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/static/desktop.html
git commit -m "feat: add insight cards and weekly insight card to desktop Beranda"
```

---

## Task 10 — Desktop: Monthly Summary + Ramadan Mode

**Files:**
- Modify: `src/main/resources/static/desktop.html`

- [ ] **Step 1: Add monthly summary modal HTML (permanent overlay outside views)**

Find in `src/main/resources/static/desktop.html`:
```html
</div>
<script>
```

(This is the `</div>` closing `#app-shell` and the opening `<script>` tag.)

Insert between them:
```html
<div id="monthly-modal-d" style="display:none" class="monthly-overlay-d">
  <div class="monthly-card-d">
    <h2 id="monthly-title-d">Ringkasan Bulan Lalu</h2>
    <div class="m-sub" id="monthly-sub-d"></div>
    <div class="m-total" id="monthly-total-d"></div>
    <div id="monthly-rows-d"></div>
    <button class="m-close" onclick="closeMonthlySummaryD()">Tutup</button>
  </div>
</div>
```

- [ ] **Step 2: Add monthly summary + Ramadan JS**

Find in `src/main/resources/static/desktop.html`:
```javascript
async function loadBeranda() {
```

Insert before it:
```javascript
// ── Phase 5: Monthly Summary (desktop) ──
async function checkMonthlySummaryD() {
  const now = new Date();
  if (now.getDate() > 3) return;
  const curMonth = `${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,'0')}`;
  if (localStorage.getItem('catetu_last_summary_month') === curMonth) return;

  const firstPrev = `${now.getFullYear()}-${String(now.getMonth()).padStart(2,'0')}-01`;
  const lastPrev = new Date(now.getFullYear(), now.getMonth(), 0).toISOString().split('T')[0];
  const firstPP = `${now.getFullYear()}-${String(now.getMonth()-1 < 1 ? 12 : now.getMonth()-1).padStart(2,'0')}-01`;
  const ppYear = now.getMonth() < 2 ? now.getFullYear()-1 : now.getFullYear();
  const lastPP = new Date(ppYear, now.getMonth()-1, 0).toISOString().split('T')[0];

  try {
    const [r1, r2] = await Promise.all([
      api('GET', `/expenses?startDate=${firstPrev}&endDate=${lastPrev}`),
      api('GET', `/expenses?startDate=${firstPP}&endDate=${lastPP}`),
    ]);
    if (!r1 || !r2) return;
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
    const mLabel = prevDate.toLocaleDateString('id-ID', {month:'long', year:'numeric'});

    document.getElementById('monthly-title-d').textContent = `Ringkasan ${mLabel}`;
    document.getElementById('monthly-sub-d').textContent = pctLabel;
    document.getElementById('monthly-total-d').textContent = fmtRp(total);
    document.getElementById('monthly-rows-d').innerHTML =
      top3.map(([cat, amt]) =>
        `<div class="m-row"><span>${catEmoji(cat)} ${cat}</span><span>${fmtRp(amt)}</span></div>`
      ).join('') +
      `<div class="m-row"><span style="color:var(--text-muted)">${prev.length} transaksi</span><span></span></div>`;

    document.getElementById('monthly-modal-d').style.display = 'flex';
  } catch (_) {}
}

function closeMonthlySummaryD() {
  const now = new Date();
  localStorage.setItem('catetu_last_summary_month',
    `${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,'0')}`);
  const modal = document.getElementById('monthly-modal-d');
  if (modal) modal.style.display = 'none';
}

// ── Phase 5: Ramadan Mode (desktop) ──
const RAMADAN_RANGES_D = [
  { year: 2026, start: new Date('2026-02-18T00:00:00'), end: new Date('2026-03-19T23:59:59') },
  { year: 2027, start: new Date('2027-02-07T00:00:00'), end: new Date('2027-03-08T23:59:59') },
  { year: 2028, start: new Date('2028-01-27T00:00:00'), end: new Date('2028-02-25T23:59:59') },
];

function getRamadanWindowD(now = new Date()) {
  const lead = 14 * 86400000;
  return RAMADAN_RANGES_D.find(r => now >= new Date(r.start.getTime()-lead) && now <= r.end) || null;
}

function isRamadanActiveD() { return localStorage.getItem('catetu_ramadan_mode') === 'true'; }

function getDisplayLabelD(label) {
  if (!isRamadanActiveD()) return label;
  const map = { 'Makan & Minum': 'Sahur & Berbuka', 'Sedekah & Sosial': 'Zakat & Infaq' };
  return map[label] || label;
}

function getRamadanBannerHtml() {
  if (!isRamadanActiveD()) return '';
  const now = new Date();
  const range = RAMADAN_RANGES_D.find(r => now >= r.start && now <= r.end);
  if (!range) return '';
  return `<div class="ramadan-banner-d">
    <span>🌙 Mode Ramadan aktif — kategori ditampilkan dalam konteks Ramadan</span>
    <button onclick="toggleRamadanD(false)" style="font-size:12px;color:var(--text-muted);background:none;border:1px solid var(--border);border-radius:6px;padding:4px 10px;cursor:pointer">Nonaktifkan</button>
  </div>`;
}

function toggleRamadanD(on) {
  localStorage.setItem('catetu_ramadan_mode', String(on));
  if (currentView === 'beranda') loadBeranda();
  if (currentView === 'catatan') loadCatatan();
}

```

- [ ] **Step 3: Add Ramadan banner to `loadBeranda()` template and call monthly summary check**

Find in `src/main/resources/static/desktop.html`:
```javascript
  view.innerHTML = `
    <div class="view-title">Beranda</div>
    <div class="view-subtitle" id="beranda-month">${monthLabel}</div>
    ${weeklyHtml}
    ${insightRowHtml}
    <div class="stat-grid">
```

Replace with:
```javascript
  view.innerHTML = `
    <div class="view-title">Beranda</div>
    <div class="view-subtitle" id="beranda-month">${monthLabel}</div>
    ${getRamadanBannerHtml()}
    ${weeklyHtml}
    ${insightRowHtml}
    <div class="stat-grid">
```

Then find the end of `loadBeranda()`:
```javascript
    <div class="expense-list">
      ${recent.length === 0
        ? '<div class="empty-state">Belum ada catatan bulan ini.</div>'
        : recent.map(e => expenseRow(e)).join('')}
    </div>`;
}
```

Replace with:
```javascript
    <div class="expense-list">
      ${recent.length === 0
        ? '<div class="empty-state">Belum ada catatan bulan ini.</div>'
        : recent.map(e => expenseRow(e)).join('')}
    </div>`;

  checkMonthlySummaryD();
}
```

- [ ] **Step 4: Apply Ramadan label override in `buildCatPills()`**

Find in `src/main/resources/static/desktop.html`:
```javascript
function buildCatPills(scope, containerId) {
  const container = document.getElementById(containerId);
  container.innerHTML = '';
  CATEGORIES.forEach(c => {
    const pill = document.createElement('button');
    pill.textContent = c.emoji + ' ' + c.label;
```

Replace with:
```javascript
function buildCatPills(scope, containerId) {
  const container = document.getElementById(containerId);
  container.innerHTML = '';
  CATEGORIES.forEach(c => {
    const pill = document.createElement('button');
    pill.textContent = c.emoji + ' ' + getDisplayLabelD(c.label);
```

- [ ] **Step 5: Add Ramadan toggle to Profil view in `loadProfil()`**

Find in `src/main/resources/static/desktop.html`:
```javascript
    <div class="profil-section">
      <h3>Sesi</h3>
      <button class="logout-btn" onclick="if(confirm('Keluar dari akun?'))logout()">Keluar</button>
    </div>`;
```

Replace with:
```javascript
    <div class="profil-section">
      <h3>Preferensi</h3>
      <div style="display:flex;align-items:center;justify-content:space-between;padding:8px 0">
        <span style="font-size:14px">Mode Ramadan</span>
        <label style="display:flex;align-items:center;gap:8px;cursor:pointer">
          <input type="checkbox" id="ramadan-toggle-d" ${isRamadanActiveD() ? 'checked' : ''}
            onchange="toggleRamadanD(this.checked)"
            style="width:16px;height:16px;accent-color:var(--accent);cursor:pointer">
          <span style="font-size:13px;color:var(--text-muted)">Ganti label kategori untuk Ramadan</span>
        </label>
      </div>
    </div>
    <div class="profil-section">
      <h3>Sesi</h3>
      <button class="logout-btn" onclick="if(confirm('Keluar dari akun?'))logout()">Keluar</button>
    </div>`;
```

- [ ] **Step 6: Manual test**

Open `localhost:8080/desktop.html`. Log in. Go to Profil → toggle Mode Ramadan on → return to Beranda. Ramadan banner appears. Open Quick-Add → category pills show "Sahur & Berbuka". Category data saved still stores original label. Monthly summary: in DevTools set `localStorage.setItem('catetu_last_summary_month', '2020-01')`, reload on day 1–3 → modal appears.

- [ ] **Step 7: Commit**

```bash
git add src/main/resources/static/desktop.html
git commit -m "feat: add monthly summary modal and Ramadan mode to desktop"
```

---

## Self-Review

**Spec coverage check:**
- ✅ Rotating insight cards (5 templates) — Task 2 (mobile) + Task 9 (desktop)
- ✅ Quiet streak display (mobile only) — Task 3
- ✅ Weekly insight card — Task 4 (mobile) + Task 9 (desktop)
- ✅ In-app nudge + Notification API — Task 5
- ✅ Monthly summary modal — Task 6 (mobile) + Task 10 (desktop)
- ✅ Payday banner (mobile only) — Task 7
- ✅ Ramadan mode — Task 8 (mobile) + Task 10 (desktop)
- ✅ Profil settings (nudge time, payday date, Ramadan toggle) — Task 5 (mobile) + Task 10 (desktop)
- ✅ All `catetu_*` localStorage keys from spec's Key Reference table — covered across tasks

**No placeholders.** All code blocks are complete and runnable.

**Type/name consistency:**
- `isoWeek()` — defined in Task 2 (mobile), defined again in Task 9 (desktop, separate scope)
- `getDismissed()` / `getDismissedD()` — separate names avoid desktop/mobile collision since both files are independent
- `RAMADAN_RANGES` / `RAMADAN_RANGES_D` — separate constants per file
- `computeInsights()` / `computeInsightsD()` — separate per file
- `dismissInsight()` / `dismissInsightD()` — separate per file
- All function calls in `loadBeranda()` reference functions defined earlier in the same task or a prior task
