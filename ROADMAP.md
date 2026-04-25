# Expense Tracker — Roadmap V2

## Goal / USP

> **"The expense tracker for Indonesian families and small groups — no bank linking, no subscription, native Rupiah, categories you recognize."**

Target audience: 10–100 people. Couples, families, housemates, friend groups.
The Indonesian market is underserved — good apps are English-first, USD-focused, and $13–15/month.

---

## Competitive Landscape (researched April 2026)

| App | Strength | Weakness |
|---|---|---|
| YNAB | Zero-based budgeting, credit card handling | $14.99/mo, steep learning curve |
| Copilot | Best design in category | iOS only, $13/mo |
| Fortune City | Gamified (expenses build a city) | Gimmicky, subscription for full features |
| Finny | Tap-to-Track, AI input, cheapest at $1.99/mo | Requires Apple Pay |
| Monarch Money | Full household dashboard | Overkill, requires bank credentials |

**What they all get wrong for our audience:**
- Expensive (most are $13–15/month)
- Require bank account linking (privacy/trust concern in Indonesia)
- Not Rupiah-native (poor handling of large IDR numbers)
- Generic categories — no kos/kontrakan, pulsa & data, GoPay, OVO
- English-first, not built for Indonesian context
- Feature bloat — young Indonesians find them "too confusing"

---

## Roadmap

### Phase 1 — Foundation ✅ (done)
- CRUD (create, read, update, delete)
- Categories, dates, notes
- Filter by category and date range
- Spending chart (doughnut by category)
- Deployed on Railway (auto-deploy from GitHub)

### Phase 2 — UI Polish ✅ (done — April 2026)
- Mobile-responsive layout — form panel hidden on mobile, full-width list
- Quick-add — FAB opens bottom sheet (amount + category only, date defaults today)
- Default view — "This month" on every load; filter button resets to current month
- Monthly summary bar — This month vs Last month with +/–% delta
- Makefile — `make run`, `make test`, `make fmt`, `make stop`, etc.

### Phase 3 — Auth + Multi-user
- Email + password login
- Google OAuth login
- Per-user data isolation (expenses scoped to logged-in user)

### Phase 4 — Product Value
- Budget targets per category with visual progress bars
- Monthly trend view (Jan / Feb / Mar side-by-side)
- Indonesian default categories: Makan, Kos/Kontrakan, Pulsa & Data, Transportasi, GoPay/OVO
- Export to CSV

### Phase 5 — Social / Differentiator
- Shared groups (family, couple, housemates, friend group)
- Split expenses between group members
- This is where the 10–100 person use case becomes real

---

## Current App Weaknesses (honest)

1. **No auth** — single shared data store; anyone with the URL can see everything
2. **Indonesian categories** — still using generic English labels (Food, Transport…) not IDR-native ones
3. **No budget targets** — can see spending but can't set limits per category
