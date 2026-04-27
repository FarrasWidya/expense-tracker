# Design: Phase 5 — Depth & Delight

> **Status:** Approved · April 27, 2026
> **Scope:** UI delight features + in-app nudges. No OS push, no Service Worker, no home screen widget (those are Phase 6).

---

## North Star

Every Phase 5 feature checks against: *"Does this make the app feel warmer and smarter — or does it add anxiety and noise?"* Insight cards inform, never judge. Streaks encourage, never shame. Nudges are gentle, never insistent.

---

## Architecture

**Approach:** Inline JS only — all features added as self-contained script blocks inside the existing HTML files. No new backend endpoints, no new static `.js` files.

**Files changed:**
- `src/main/resources/static/index.html` — all 7 features
- `src/main/resources/static/desktop.html` — features 1, 3, 5, 7 only (insights, weekly card, monthly summary, Ramadan mode)

**State storage:** `localStorage` under `catetu_*` namespace. All keys are safe to clear — features degrade gracefully to their default/empty state.

**Data source:** Existing `/expenses?startDate=&endDate=` API. Features reuse data already fetched for Beranda; no extra API calls unless the feature explicitly needs a separate range (weekly insight, monthly summary).

**No backend changes in Phase 5.**

---

## Features

### 1 — Rotating Insight Cards

**Where:** Beranda screen (mobile + desktop), between the month total and the recent list.

**What:** A horizontally scrollable row of 1–3 insight cards. Each card is dismissible (× button). Dismissed card IDs stored in `catetu_dismissed_insights` (array). All dismissed → row hidden.

**Templates (5):**

| ID | Template | Data needed |
|---|---|---|
| `top-category` | "Pengeluaran terbesar bulan ini: {emoji} {category} — Rp {amount}" | Current month expenses |
| `savings-vs-last` | "Kamu udah hemat {X}% dibanding bulan lalu" (only shown if X > 0) | Current + previous month total |
| `busiest-day` | "Hari tersibuk: {day} — {N} transaksi" | Current month expenses |
| `daily-avg` | "Rata-rata harian bulan ini: Rp {amount}" | Current month total ÷ days elapsed |
| `top-frequency` | "Paling sering: {emoji} {category} ({N}x bulan ini)" | Current month expenses grouped by category |

**Logic:** On Beranda load, compute which templates have enough data to render (e.g. `savings-vs-last` requires last month data — skip if unavailable). Show up to 3. Cards that were dismissed are excluded.

**Dismiss persistence:** `localStorage.setItem('catetu_dismissed_insights', JSON.stringify([...ids]))`. Reset automatically on the 1st of each month (stale dismissals cleared).

---

### 2 — Quiet Streak Display

**Where:** Beranda header area, mobile only. Shown as a small badge: `📅 7 hari berturut-turut`.

**What:** Count of consecutive days (ending today) where the user logged at least one expense.

**Logic:**
1. From the current month's expense list (already fetched), extract unique `date` values.
2. Walk backward from today: increment streak counter while the date exists, stop on first gap.
3. If streak < 1, render nothing.
4. Streak breaks restart silently — no "streak broken" message, ever.

**State:** Computed on every load from live data — no localStorage key needed.

---

### 3 — Weekly Insight Card

**Where:** Beranda (mobile + desktop), shown above the rotating insight row. One card, dismissible.

**What:** A single summary card generated every Monday covering the previous 7 days. Picked from the same 5 insight templates but scoped to the last 7 days rather than current month.

**Logic:**
1. On app load, read `catetu_weekly_insight_week` (ISO week string, e.g. `"2026-W18"`).
2. If today is Monday AND stored week ≠ current week:
   a. Fetch expenses for the last 7 days.
   b. Pick the template with the most interesting data (highest absolute value).
   c. Store rendered text + current week string to `catetu_weekly_insight_card` and `catetu_weekly_insight_week`.
3. Show card if `catetu_weekly_insight_card` exists and not dismissed (`catetu_weekly_card_dismissed_week` ≠ current week).
4. Dismiss: set `catetu_weekly_card_dismissed_week` to current week.

---

### 4 — In-App Nudge

**Where:** Mobile only. Bottom toast banner.

**What:** If the user hasn't logged any expense today and the app is open past their set nudge time, show a toast: *"Udah catat pengeluaran hari ini?"* with a "Catat Sekarang" button (opens Quick-Add sheet).

**Logic:**
1. On app open, read `catetu_nudge_time` (default `"21:00"`).
2. Compute milliseconds until that clock time today. If already past, do not fire (avoid repeat nudges).
3. `setTimeout` → check today's expense count from already-fetched data → if 0, show toast.
4. Toast auto-dismisses after 8 seconds or on tap.
5. On first fire, call `Notification.requestPermission()`. If granted, also send a browser `Notification`. If denied or unavailable, silent fallback (in-app toast still shows).

**User setting:** Nudge time configurable in Profil → Pengaturan. Toggle to disable entirely (`catetu_nudge_enabled = false`).

---

### 5 — Monthly Summary Modal

**Where:** Mobile + desktop. Full-screen modal (mobile) / centered dialog (desktop).

**What:** On the 1st–3rd of a new month, show a summary of the previous month when the user opens the app.

**Content:**
- Total spent: "April lalu: Rp 2.340.000"
- Top category with emoji and amount
- Comparison to month before: "↑ 12% dari Maret"
- Close button ("Tutup") stores current month to `catetu_last_summary_month`

**Logic:**
1. On app load, compare current month (`YYYY-MM`) to `catetu_last_summary_month`.
2. If different AND day of month ≤ 3:
   a. Fetch previous month expenses.
   b. Fetch two months ago expenses (for comparison).
   c. Render modal.
3. Modal dismissed → store current month.

---

### 6 — Payday Mode Banner

**Where:** Mobile only. Soft banner below the month total on Beranda.

**What:** During payday week (3 days before, on, 3 days after the set payday date), show a banner: *"Gajian nih — yuk rencanain bulannya"* with "Atur Anggaran →" CTA (navigates to Anggaran tab).

**Logic:**
1. Read `catetu_payday_date` (day of month, 1–31). If not set, don't show.
2. Compute distance from today to payday this month. If |distance| ≤ 3, show banner.
3. Banner is dismissible per occurrence (`catetu_payday_dismissed_month` = `YYYY-MM`).

**User setting:** Payday date set in Profil → Pengaturan. Empty = feature off.

---

### 7 — Ramadan Mode

**Where:** Mobile + desktop. Affects Quick-Add category pills and Catatan category labels.

**Hardcoded Ramadan date ranges:**

| Year | Start | End |
|---|---|---|
| 2026 | Feb 18 | Mar 19 |
| 2027 | Feb 7 | Mar 8 |
| 2028 | Jan 27 | Feb 25 |

**What:**
- 14 days before Ramadan start: show a suggestion card on Beranda (once per year, key: `catetu_ramadan_suggestion_YYYY`). Copy: *"Ramadan sebentar lagi — aktifkan Mode Ramadan?"* with "Aktifkan" and "Nanti saja" buttons.
- When active: rename Quick-Add category labels only (underlying data category unchanged):
  - 🍽️ Makan & Minum → shows as "Sahur / Berbuka" in pill
  - 🙏 Sedekah & Sosial → shows as "Zakat & Infaq" in pill
- Catatan rows: same relabeling on displayed category text.
- Manual toggle in Profil → Pengaturan → "Mode Ramadan".

**State:** `catetu_ramadan_mode` (`true`/`false`). Auto-suggested but never auto-activated.

---

## localStorage Key Reference

| Key | Type | Cleared |
|---|---|---|
| `catetu_dismissed_insights` | `string[]` (insight IDs) | 1st of each month |
| `catetu_weekly_insight_week` | `string` (ISO week) | Never (overwritten) |
| `catetu_weekly_insight_card` | `string` (rendered text) | Never (overwritten) |
| `catetu_weekly_card_dismissed_week` | `string` (ISO week) | Never (overwritten) |
| `catetu_last_summary_month` | `string` (YYYY-MM) | Never (overwritten) |
| `catetu_nudge_time` | `string` (HH:MM) | Never |
| `catetu_nudge_enabled` | `boolean` | Never |
| `catetu_payday_date` | `number` (1–31) | Never |
| `catetu_payday_dismissed_month` | `string` (YYYY-MM) | Never (overwritten) |
| `catetu_ramadan_mode` | `boolean` | Never |
| `catetu_ramadan_suggestion_YYYY` | `boolean` | Never |

---

## Testing Checklist

All manual — no new backend tests.

- [ ] **Insights:** clear `catetu_dismissed_insights`, reload → cards appear. Dismiss one → gone on reload. Day 1 of month → dismissed list clears.
- [ ] **Streak:** seed expenses in localStorage mock for 5 consecutive days → badge shows "5 hari berturut-turut". Gap on day 3 → streak shows 0 → badge hidden.
- [ ] **Weekly insight:** set `catetu_weekly_insight_week` to last week's ISO string, visit on a Monday → new card generated. Dismiss → gone until next Monday.
- [ ] **Nudge:** set `catetu_nudge_time` to 2 minutes from now, ensure no expenses today → toast fires at that time.
- [ ] **Monthly summary:** set `catetu_last_summary_month` = 2 months ago, visit on day 1-3 → modal appears. Dismiss → doesn't reappear same month.
- [ ] **Payday banner:** set `catetu_payday_date` = today's day of month → banner visible. Dismiss → gone for this month.
- [ ] **Ramadan suggestion:** set system date to 14 days before Feb 18 2026, clear `catetu_ramadan_suggestion_2026` → card appears. "Aktifkan" → mode turns on, labels change in Quick-Add.
- [ ] **Ramadan active:** verify Catatan rows show renamed labels. Toggle off in Profil → original labels restored immediately.

---

## Out of Scope (Phase 6)

- OS push notifications (Service Worker + Web Push API)
- Home screen widget (PWA manifest + `display: standalone`)
- Backend persistence of insight state across devices
- Server-side insight computation (`/insights` endpoint)
