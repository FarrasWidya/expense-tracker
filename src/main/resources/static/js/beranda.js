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

async function loadBeranda() {
  // Greeting
  const greetName = document.getElementById('beranda-greeting-name');
  if (greetName) greetName.textContent = getNickname() || 'Kamu';
  const greetDate = document.getElementById('beranda-greeting-date');
  if (greetDate) {
    const now = new Date();
    greetDate.textContent = now.toLocaleDateString('id-ID', { weekday:'long', day:'numeric', month:'long' });
  }

  const { from, to } = monthRange();
  const { from: lf, to: lt } = monthRange(-1);
  const [r1, r2] = await Promise.all([
    authFetch(`/expenses?startDate=${from}&endDate=${to}`),
    authFetch(`/expenses?startDate=${lf}&endDate=${lt}`),
  ]);
  const expenses  = await r1.json();
  const lastMonth = await r2.json();

  const total     = expenses.reduce((s, e) => s + e.amount, 0);
  const lastTotal = lastMonth.reduce((s, e) => s + e.amount, 0);

  document.getElementById('beranda-total').textContent = formatRp(total);

  // Avg per day
  const avgEl = document.getElementById('beranda-avg');
  if (avgEl) {
    const dayOfMonth = new Date().getDate();
    const avg = dayOfMonth > 0 ? total / dayOfMonth : 0;
    avgEl.textContent = total > 0 ? formatRp(avg) : '—';
  }

  // Category breakdown — top 3
  const breakdownEl = document.getElementById('beranda-cat-breakdown');
  if (breakdownEl && expenses.length > 0) {
    const catTotals = {};
    expenses.forEach(e => { catTotals[e.category] = (catTotals[e.category] || 0) + e.amount; });
    const sorted = Object.entries(catTotals).sort((a, b) => b[1] - a[1]).slice(0, 3);
    const max = sorted[0]?.[1] || 1;
    breakdownEl.innerHTML = sorted.map(([cat, amt]) => {
      const pct = Math.round((amt / max) * 100);
      return `<div class="hero-cat-row">
        <div class="hero-cat-label">${CATS[cat] || '📦'} <span style="font-size:12px;color:inherit">${escHtml(cat)}</span></div>
        <div class="hero-cat-bar-wrap"><div class="hero-cat-bar" style="width:${pct}%"></div></div>
        <div class="hero-cat-amount">${formatRp(amt)}</div>
      </div>`;
    }).join('');
  } else if (breakdownEl) {
    breakdownEl.innerHTML = '';
  }

  const cmp = document.getElementById('beranda-compare');
  if (lastTotal > 0) {
    cmp.textContent = `${Math.round((total / lastTotal) * 100)}% dari bulan lalu`;
  } else {
    cmp.textContent = '—';
  }

  const lastMonthTotal = lastMonth.reduce((s, e) => s + e.amount, 0);
  renderDonutChart(expenses);
  renderInsightCards(expenses, lastMonthTotal);
  renderStreak(expenses);
  checkWeeklyInsight();
  const todayStr = today();
  const todayCount = expenses.filter(e => (e.date||'').substring(0,10) === todayStr).length;
  scheduleNudge(todayCount);
  checkMonthlySummary();
  checkPaydayBanner();
  checkRamadanSuggestion();

  if (expenses.length === 0) {
    document.getElementById('beranda-empty').style.display = 'none';
    const placeholders = [
      { emoji: '🍽️', name: 'Makan siang', cat: 'Makan & Minum', amount: 35000 },
      { emoji: '🛵', name: 'Bensin motor', cat: 'Transportasi', amount: 50000 },
      { emoji: '🛍️', name: 'Belanja bulanan', cat: 'Belanja', amount: 120000 },
    ];
    document.getElementById('beranda-list').innerHTML =
      `<div class="placeholder-badge">Contoh catatan</div>` +
      `<div class="day-group placeholder-row">` +
      placeholders.map(p => `
        <div class="expense-row">
          <div class="expense-icon">${p.emoji}</div>
          <div class="expense-row-body">
            <div class="expense-row-name">${p.name}</div>
            <div class="expense-row-cat">${p.cat}</div>
          </div>
          <div class="expense-row-amount">${formatRp(p.amount)}</div>
        </div>`).join('') +
      `</div>` +
      `<p style="text-align:center;padding:16px 20px 8px;color:var(--text-2);font-size:13px">Belum ada catatan. Ketuk <strong>+</strong> untuk mulai!</p>`;
  } else {
    renderDayGroups(expenses.slice(0, 20), 'beranda-list', 'beranda-empty');
  }
}

function renderDayGroups(expenses, listId, emptyId) {
  const list  = document.getElementById(listId);
  const empty = document.getElementById(emptyId);
  if (!expenses.length) { list.innerHTML = ''; empty.style.display = 'block'; return; }
  empty.style.display = 'none';

  const groups = {};
  expenses.forEach(e => { (groups[e.date || today()] = groups[e.date || today()] || []).push(e); });

  const todayStr = today();
  const yest = new Date(Date.now() - 86400000).toISOString().split('T')[0];

  list.innerHTML = Object.entries(groups)
    .sort(([a], [b]) => b.localeCompare(a))
    .map(([date, items]) => {
      const label = date === todayStr ? 'Hari ini' : date === yest ? 'Kemarin'
        : new Date(date + 'T00:00:00').toLocaleDateString('id-ID', { day:'numeric', month:'long' });
      return `
        <div class="day-group-header">${label}</div>
        <div class="day-group">
          ${items.map(e => `
            <div class="expense-row">
              <div class="expense-icon">${CATS[e.category] || '📦'}</div>
              <div class="expense-row-body">
                <div class="expense-row-name">${escHtml(e.note || e.category)}</div>
                <div class="expense-row-cat">${escHtml(e.category)}</div>
              </div>
              <div class="expense-row-amount">${formatRp(e.amount)}</div>
            </div>`).join('')}
        </div>`;
    }).join('');
}
