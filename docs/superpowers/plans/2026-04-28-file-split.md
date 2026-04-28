# CatetU File Split Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Split `index.html` (1700-line monolith) into focused files so future sessions read ~1–3k tokens per file instead of 23k.

**Architecture:** Extract the inline `<style>` block to `css/main.css` and the inline `<script>` block to 9 focused JS files. All files are loaded globally (no ES modules) so load order matters. No behavior changes — pure refactor.

**Tech Stack:** Vanilla HTML/CSS/JS. Spring Boot serves all files from `src/main/resources/static/` with no config change needed.

**Load order (must match `<script>` tag order in index.html):**
`config.js` → `utils.js` → `auth.js` → `beranda.js` → `sheet.js` → `catatan.js` → `anggaran.js` → `profil.js` → `rumah.js` → `app.js`

---

## File Map

| New File | Extracted From (index.html lines) | Contains |
|----------|----------------------------------|----------|
| `css/main.css` | Lines 11–291 (inside `<style>`) | All CSS |
| `js/config.js` | Lines 767–780 | `CATS`, `CATEGORY_LIST` |
| `js/utils.js` | Lines 660–672, 782–808 | Token helpers, `authFetch`, `formatRp`, `today`, `showToast`, `escHtml`, `monthRange` |
| `js/auth.js` | Lines 674–738 | All auth/login/logout functions |
| `js/beranda.js` | Lines 810–1091 | Insight engine, `loadBeranda`, `renderDayGroups`, `renderStreak` |
| `js/sheet.js` | Lines 1092–1167 | Quick-add sheet, numpad, `renderCatPills` |
| `js/catatan.js` | Lines 1168–1298 | Catatan screen, filter drawer, swipe delete |
| `js/anggaran.js` | Lines 1299–1360 | Anggaran screen |
| `js/profil.js` | Lines 1361–1580 | Profil, nudge, Ramadan, payday banner, monthly summary |
| `js/rumah.js` | Lines 1581–1821 | Rumah screen + bareng toggle |
| `js/app.js` | Lines 740–765 | `showScreen`, `onAppReady`, tab/FAB wiring, URL token pickup, init |

---

## Task 1: Create directory structure

**Files:**
- Create: `expense-tracker/src/main/resources/static/css/` (directory)
- Create: `expense-tracker/src/main/resources/static/js/` (directory)

- [ ] **Step 1: Create directories**
```bash
mkdir -p expense-tracker/src/main/resources/static/css
mkdir -p expense-tracker/src/main/resources/static/js
```

- [ ] **Step 2: Verify**
```bash
ls expense-tracker/src/main/resources/static/
```
Expected: `css/  desktop.html  index.html  js/`

---

## Task 2: Extract CSS to `css/main.css`

**Files:**
- Create: `expense-tracker/src/main/resources/static/css/main.css`
- Reference: `index.html` lines 11–291

- [ ] **Step 1: Create `css/main.css`**

Copy everything inside the `<style>` tag (lines 11–291, NOT the `<style>` tags themselves) into `css/main.css`. The file should start with `:root {` and end with `}` (closing the last rule before `</style>`).

- [ ] **Step 2: Verify file exists and has content**
```bash
wc -l expense-tracker/src/main/resources/static/css/main.css
```
Expected: ~282 lines

- [ ] **Step 3: Commit**
```bash
git add expense-tracker/src/main/resources/static/css/main.css
git commit -m "refactor: extract CSS to css/main.css"
```

---

## Task 3: Create `js/config.js`

**Files:**
- Create: `expense-tracker/src/main/resources/static/js/config.js`
- Reference: `index.html` lines 767–780

- [ ] **Step 1: Create `js/config.js`** with this exact content (copy from index.html lines 767–780):

```js
const CATS = {
  'Makan & Minum':'🍽️','Transportasi':'🛵','Kos & Rumah':'🏠',
  'Pulsa & Internet':'📱','Belanja':'🛍️','Hiburan':'🎉',
  'Kesehatan':'💊','Pendidikan':'📚','Sedekah & Sosial':'🙏',
  'Perawatan Diri':'✂️','Transfer & Hutang':'💸','Lainnya':'📦',
};
const CATEGORY_LIST = [
  {emoji:'🍽️',label:'Makan & Minum'},{emoji:'🛵',label:'Transportasi'},
  {emoji:'🏠',label:'Kos & Rumah'},{emoji:'📱',label:'Pulsa & Internet'},
  {emoji:'🛍️',label:'Belanja'},{emoji:'🎉',label:'Hiburan'},
  {emoji:'💊',label:'Kesehatan'},{emoji:'📚',label:'Pendidikan'},
  {emoji:'🙏',label:'Sedekah & Sosial'},{emoji:'✂️',label:'Perawatan Diri'},
  {emoji:'💸',label:'Transfer & Hutang'},{emoji:'📦',label:'Lainnya'},
];
```

- [ ] **Step 2: Commit**
```bash
git add expense-tracker/src/main/resources/static/js/config.js
git commit -m "refactor: extract CATS/CATEGORY_LIST to js/config.js"
```

---

## Task 4: Create `js/utils.js`

**Files:**
- Create: `expense-tracker/src/main/resources/static/js/utils.js`
- Reference: `index.html` lines 660–672 (token helpers + authFetch) and 782–808 (formatRp, today, showToast, escHtml, monthRange)

- [ ] **Step 1: Create `js/utils.js`**

Copy from index.html: the `// TOKEN HELPERS` block (lines 660–672) followed by the utility functions (lines 782–808). File should contain these functions in order:
- `const getToken`, `setToken`, `clearToken`
- `function authFetch`
- `function formatRp`
- `function today`
- `function showToast` (includes the `let _toastTimer` variable)
- `function escHtml`
- `function monthRange`

- [ ] **Step 2: Verify**
```bash
grep -c "^    function\|^    const\|^    let\|^    async" expense-tracker/src/main/resources/static/js/utils.js
```
Expected: 8 or more matches

- [ ] **Step 3: Commit**
```bash
git add expense-tracker/src/main/resources/static/js/utils.js
git commit -m "refactor: extract utils to js/utils.js"
```

---

## Task 5: Create `js/auth.js`

**Files:**
- Create: `expense-tracker/src/main/resources/static/js/auth.js`
- Reference: `index.html` lines 674–738

- [ ] **Step 1: Create `js/auth.js`**

Copy from index.html lines 674–738. Contains (in order):
- `function showAuthOverlay`
- `function hideAuthOverlay`
- `function showLanding`
- `function openAuthModal`
- `function showTab`
- `async function submitAuth`
- `function showAuthError`
- `function cancelAuth`
- `function logout`
- `function confirmLogout`
- `function cancelLogout`

- [ ] **Step 2: Commit**
```bash
git add expense-tracker/src/main/resources/static/js/auth.js
git commit -m "refactor: extract auth functions to js/auth.js"
```

---

## Task 6: Create `js/beranda.js`

**Files:**
- Create: `expense-tracker/src/main/resources/static/js/beranda.js`
- Reference: `index.html` lines 810–1091

- [ ] **Step 1: Create `js/beranda.js`**

Copy from index.html lines 810–1091. Contains (in order):
- `// ── Phase 5: Insight Engine ──` comment
- `function isoWeek`
- `function getDismissed`
- `function addDismissed`
- `function maybeClearMonthlyDismissals`
- `function computeInsights`
- `function renderInsightCards`
- `function dismissInsight`
- `async function checkWeeklyInsight`
- `function dismissWeeklyCard`
- `function computeStreak`
- `function renderStreak`
- `async function loadBeranda`
- `function renderDayGroups`

- [ ] **Step 2: Commit**
```bash
git add expense-tracker/src/main/resources/static/js/beranda.js
git commit -m "refactor: extract beranda logic to js/beranda.js"
```

---

## Task 7: Create `js/sheet.js`

**Files:**
- Create: `expense-tracker/src/main/resources/static/js/sheet.js`
- Reference: `index.html` lines 1092–1167

- [ ] **Step 1: Create `js/sheet.js`**

Copy from index.html lines 1092–1167. Contains:
- `// QUICK-ADD SHEET` comment
- `let sheetAmount` and `let sheetCat` variables
- `function renderCatPills`
- `function updateAmountDisplay`
- `function openSheet`
- `function closeSheet`
- `document.getElementById('sheet-overlay').addEventListener(...)` event listener
- `document.getElementById('sheet-numpad').addEventListener(...)` event listener
- `document.getElementById('sheet-simpan').addEventListener(...)` event listener

- [ ] **Step 2: Commit**
```bash
git add expense-tracker/src/main/resources/static/js/sheet.js
git commit -m "refactor: extract quick-add sheet to js/sheet.js"
```

---

## Task 8: Create `js/catatan.js`

**Files:**
- Create: `expense-tracker/src/main/resources/static/js/catatan.js`
- Reference: `index.html` lines 1168–1298

- [ ] **Step 1: Create `js/catatan.js`**

Copy from index.html lines 1168–1298. Contains:
- `// CATATAN` comment
- `let allExpenses`, `let catMonth`, `let catFilterCats`, `let catSearchQ` variables
- `async function loadCatatan`
- `function renderCatatan`
- `function buildMonthChips`
- `function openFilter`
- `function closeFilter`
- All swipe-delete listeners and catatan event wiring

- [ ] **Step 2: Commit**
```bash
git add expense-tracker/src/main/resources/static/js/catatan.js
git commit -m "refactor: extract catatan screen to js/catatan.js"
```

---

## Task 9: Create `js/anggaran.js`

**Files:**
- Create: `expense-tracker/src/main/resources/static/js/anggaran.js`
- Reference: `index.html` lines 1299–1360

- [ ] **Step 1: Create `js/anggaran.js`**

Copy from index.html lines 1299–1360. Contains:
- `// ANGGARAN` comment
- `function getBudgets`
- `function saveBudgets`
- `function openAnggaranModal`
- `function closeAnggaranModal`
- `async function loadAnggaran`
- Anggaran event listeners (overlay click, save button, add button)

- [ ] **Step 2: Commit**
```bash
git add expense-tracker/src/main/resources/static/js/anggaran.js
git commit -m "refactor: extract anggaran screen to js/anggaran.js"
```

---

## Task 10: Create `js/profil.js`

**Files:**
- Create: `expense-tracker/src/main/resources/static/js/profil.js`
- Reference: `index.html` lines 1361–1580

- [ ] **Step 1: Create `js/profil.js`**

Copy from index.html lines 1361–1580. Contains:
- `function scheduleNudge`
- `function showNudgeToast`
- `function closeNudgeToast`
- `function openQuickAdd`
- `function getCurrentRamadanWindow`
- `function isRamadanActive`
- `function getDisplayLabel`
- `function checkRamadanSuggestion`
- `function activateRamadan`
- `function dismissRamadanSuggestion`
- `function checkPaydayBanner`
- `async function checkMonthlySummary`
- `function closeMonthlySummary`
- `function loadProfilSettings`
- `function saveNudgeSetting`
- `function savePaydaySetting`
- `function saveRamadanSetting`
- `async function loadProfil`
- `function applyTheme`
- Theme button event listeners

- [ ] **Step 2: Commit**
```bash
git add expense-tracker/src/main/resources/static/js/profil.js
git commit -m "refactor: extract profil/nudge/ramadan logic to js/profil.js"
```

---

## Task 11: Create `js/rumah.js`

**Files:**
- Create: `expense-tracker/src/main/resources/static/js/rumah.js`
- Reference: `index.html` lines 1581–1821

- [ ] **Step 1: Create `js/rumah.js`**

Copy from index.html lines 1581–1821. Contains:
- `// ─── Rumah ───` comment
- `let currentRumah` variable (look for it — likely defined at start of Rumah section)
- `async function loadRumahTab`
- `function renderRumahHeader`
- `async function doKickMember`
- `async function doTransferAdmin`
- `async function loadContribution`
- `async function loadRumahFeed`
- `function startRumahPolling`
- `function stopRumahPolling`
- `function openBuatRumahModal`, `closeBuatRumahModal`, `openGabungModal`, `closeGabungModal`
- `async function onGabungInput`
- `async function submitBuatRumah`
- `async function submitGabung`
- `function shareRumahLink`
- `// ─── Bareng toggle ───` section
- `function updateBarengToggle`
- `function onBarengToggle`
- `function updateBarengSwitchStyle`
- Rumah tab click/polling lifecycle event listeners (lines 1817–1821)

- [ ] **Step 2: Commit**
```bash
git add expense-tracker/src/main/resources/static/js/rumah.js
git commit -m "refactor: extract rumah + bareng toggle to js/rumah.js"
```

---

## Task 12: Create `js/app.js`

**Files:**
- Create: `expense-tracker/src/main/resources/static/js/app.js`
- Reference: `index.html` lines 740–765

- [ ] **Step 1: Create `js/app.js`**

Copy from index.html lines 740–765. Contains:
- `// TAB ROUTER` comment
- `function showScreen`
- Tab click event listener loop (`document.querySelectorAll('.tab[data-screen]')...`)
- FAB listener (`document.getElementById('fab').addEventListener(...)`)
- `// ENTRY POINT` comment
- `function onAppReady`
- `// ?token= pickup` comment + URL token logic
- Init (`if (getToken()) { onAppReady(); } else { showLanding(); }`)

- [ ] **Step 2: Commit**
```bash
git add expense-tracker/src/main/resources/static/js/app.js
git commit -m "refactor: extract app shell/router to js/app.js"
```

---

## Task 13: Update `index.html` — wire external files, remove inline blocks

**Files:**
- Modify: `expense-tracker/src/main/resources/static/index.html`

- [ ] **Step 1: In the `<head>`, replace the `<style>...</style>` block with:**

```html
  <link rel="stylesheet" href="/css/main.css">
```

Place it after the Google Fonts `<link>` tag, before `</head>`.

- [ ] **Step 2: At the bottom of `<body>`, replace the entire `<script>...</script>` block with:**

```html
  <script src="/js/config.js"></script>
  <script src="/js/utils.js"></script>
  <script src="/js/auth.js"></script>
  <script src="/js/beranda.js"></script>
  <script src="/js/sheet.js"></script>
  <script src="/js/catatan.js"></script>
  <script src="/js/anggaran.js"></script>
  <script src="/js/profil.js"></script>
  <script src="/js/rumah.js"></script>
  <script src="/js/app.js"></script>
```

- [ ] **Step 3: Verify index.html has no remaining `<style>` or inline `<script>` content blocks**

```bash
grep -n "<style>" expense-tracker/src/main/resources/static/index.html
```
Expected: no output (zero matches)

```bash
grep -c "function " expense-tracker/src/main/resources/static/index.html
```
Expected: `0`

- [ ] **Step 4: Verify line count shrank**
```bash
wc -l expense-tracker/src/main/resources/static/index.html
```
Expected: ~300 lines (was ~1825)

- [ ] **Step 5: Commit**
```bash
git add expense-tracker/src/main/resources/static/index.html
git commit -m "refactor: index.html now loads external CSS/JS — remove inline style+script blocks"
```

---

## Task 14: Smoke test

No automated tests exist for this frontend. Manual verification required.

- [ ] **Step 1: Start the app**
```bash
cd expense-tracker && ./mvnw spring-boot:run
```

- [ ] **Step 2: Open http://localhost:8080 in a browser**

Verify:
- Landing page renders (no white blank screen)
- Can log in / register
- Beranda loads with expenses
- Tab bar navigates: Catatan, Anggaran, Rumah, Profil all render
- FAB opens quick-add sheet with 3×4 category grid
- Dark mode toggle in Profil works
- No console errors (open DevTools → Console)

- [ ] **Step 3: If console errors appear**

Read the error. It will say something like `ReferenceError: X is not defined`. Find which file `X` should be in, verify it's there, and check the `<script>` load order in index.html.

- [ ] **Step 4: Commit fix if needed, then final commit**
```bash
git add -A
git commit -m "refactor: fix load-order/missing-function issues from file split"
```

---

## Self-Review

**Spec coverage:** All 1700 lines of inline JS/CSS have a destination file. Load order is defined. Smoke test verifies behavior unchanged.

**Placeholder scan:** None.

**Type consistency:** No new types introduced — pure extraction.

---

## Execution Handoff

Plan saved. Two execution options:

**1. Subagent-Driven (recommended)** — fresh subagent per task, review between tasks

**2. Inline Execution** — execute tasks in this session using executing-plans skill

Which approach?
