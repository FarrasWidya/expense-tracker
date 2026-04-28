// ─── Rumah ───────────────────────────────────────────────────────────────────

let currentRumah = null;
let currentUserId = null;
let rumahMemberMap = {};
let rumahPollInterval = null;

async function loadRumahTab() {
  try {
    if (!currentUserId) {
      const meRes = await authFetch('/auth/me');
      if (meRes.ok) { const me = await meRes.json(); currentUserId = me.id; }
    }
    const res = await authFetch('/rumah/me');
    const data = res.ok ? await res.json() : null;
    currentRumah = data && data.id ? data : null;
  } catch (e) { currentRumah = null; }

  document.getElementById('rumah-empty').style.display = currentRumah ? 'none' : 'block';
  document.getElementById('rumah-active').style.display = currentRumah ? 'block' : 'none';
  updateBarengToggle();

  if (currentRumah) {
    renderRumahHeader(currentRumah);
    loadRumahFeed();
    startRumahPolling();
  }
}

function renderRumahHeader(rumah) {
  const isAdmin = currentUserId && rumah.adminId === currentUserId;
  rumahMemberMap = {};
  const memberList = (rumah.members || []).map(m => {
    rumahMemberMap[m.userId] = m;
    const initial = ((m.name || '?')[0]).toUpperCase();
    const isThisAdmin = m.userId === rumah.adminId;
    const isSelf = m.userId === currentUserId;
    const badge = isThisAdmin
      ? `<span style="font-size:10px;background:${rumah.color||'var(--accent)'};color:#fff;border-radius:4px;padding:1px 5px;margin-left:5px;flex-shrink:0">Admin</span>`
      : '';
    const kickBtn = (isAdmin && !isSelf)
      ? `<button onclick="doKickMember('${rumah.id}',${m.userId})" style="font-size:11px;color:#e53935;background:none;border:1px solid #e53935;border-radius:6px;padding:2px 7px;cursor:pointer;flex-shrink:0">Keluarkan</button>`
      : '';
    const transferBtn = (isAdmin && !isSelf && !isThisAdmin)
      ? `<button onclick="doTransferAdmin('${rumah.id}',${m.userId})" style="font-size:11px;color:var(--accent);background:none;border:1px solid var(--accent);border-radius:6px;padding:2px 7px;cursor:pointer;flex-shrink:0">Jadikan Admin</button>`
      : '';
    return `<div style="display:flex;align-items:center;gap:8px;padding:6px 0;border-bottom:1px solid var(--divider)">
      <span style="width:26px;height:26px;border-radius:50%;background:${rumah.color||'var(--accent)'};color:#fff;font-size:11px;font-weight:700;display:inline-flex;align-items:center;justify-content:center;flex-shrink:0">${initial}</span>
      <span style="flex:1;font-size:13px;font-weight:600;min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">${m.name || '?'}${badge}</span>
      ${kickBtn}${transferBtn}
    </div>`;
  }).join('');

  document.getElementById('rumah-header-card').innerHTML = `
    <div style="display:flex;align-items:center;gap:12px;margin-bottom:12px">
      <span style="font-size:28px">${rumah.emoji || '🏠'}</span>
      <div style="flex:1">
        <div style="font-weight:700;font-size:16px;color:var(--text-primary)">${rumah.name}</div>
        <div style="font-size:12px;color:var(--text-secondary)">${(rumah.members||[]).length} anggota</div>
      </div>
      <button onclick="shareRumahLink('${rumah.inviteToken}')" style="background:none;border:none;cursor:pointer;font-size:20px;padding:4px" title="Bagikan link undangan">🔗</button>
    </div>
    <div style="margin-bottom:12px">${memberList}</div>
    <div id="rumah-contribution-bar" style="font-size:13px;color:var(--text-secondary)">Memuat kontribusi...</div>
  `;
  loadContribution(rumah.id);
}

async function doKickMember(rumahId, targetId) {
  const name = (rumahMemberMap[targetId]?.name || 'anggota ini');
  if (!confirm(`Keluarkan ${name} dari Rumah?`)) return;
  const res = await authFetch(`/rumah/${rumahId}/members/${targetId}`, { method: 'DELETE' });
  if (res.ok) { showToast(`${name} dikeluarkan`); loadRumahTab(); }
  else showToast('Gagal mengeluarkan anggota');
}

async function doTransferAdmin(rumahId, newAdminId) {
  const name = (rumahMemberMap[newAdminId]?.name || 'anggota ini');
  if (!confirm(`Jadikan ${name} admin Rumah?`)) return;
  const res = await authFetch(`/rumah/${rumahId}/admin/${newAdminId}`, { method: 'PUT' });
  if (res.ok) { showToast(`${name} sekarang admin`); loadRumahTab(); }
  else showToast('Gagal transfer admin');
}

async function loadContribution(rumahId) {
  try {
    const res = await authFetch('/rumah/' + rumahId + '/contribution');
    if (!res.ok) return;
    const members = await res.json();
    const el = document.getElementById('rumah-contribution-bar');
    if (!el) return;
    if (!members || members.length === 0) {
      el.textContent = 'Belum ada catatan bareng bulan ini';
      return;
    }
    el.innerHTML = members
      .map(m => `<span style="margin-right:12px"><b>${(m.name || '?').split(' ')[0]}</b> ${m.pct}%</span>`)
      .join('');
  } catch (e) { /* silent */ }
}

async function loadRumahFeed() {
  if (!currentRumah) return;
  try {
    const res = await authFetch('/rumah/' + currentRumah.id + '/feed?page=0&size=30');
    if (!res.ok) return;
    const page = await res.json();
    const list = document.getElementById('rumah-feed-list');
    if (!list) return;
    if (!page.content || page.content.length === 0) {
      list.innerHTML = '<div style="text-align:center;color:var(--text-secondary);padding:32px;font-size:14px">Belum ada pengeluaran bersama</div>';
      return;
    }
    const accentColor = currentRumah?.color || 'var(--accent)';
    list.innerHTML = page.content.map(exp => `
      <div style="display:flex;align-items:center;gap:12px;padding:14px 0 14px 10px;border-bottom:1px solid var(--divider);border-left:3px solid ${accentColor}">
        <span style="width:40px;height:40px;border-radius:50%;background:var(--surface-raised);display:flex;align-items:center;justify-content:center;font-size:20px;flex-shrink:0">${CATS[exp.category] || '📦'}</span>
        <div style="flex:1;min-width:0">
          <div style="font-weight:600;font-size:14px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">${exp.note || exp.category}</div>
          <div style="font-size:12px;color:var(--text-secondary)">${exp.category} · ${((exp.createdBy && exp.createdBy.name) || '?').split(' ')[0]}</div>
        </div>
        <div style="font-family:'JetBrains Mono',monospace;font-size:14px;font-weight:600;flex-shrink:0">${formatRp(exp.amount)}</div>
      </div>
    `).join('');
  } catch (e) { /* silent */ }
}

function startRumahPolling() {
  stopRumahPolling();
  rumahPollInterval = setInterval(() => {
    if (!document.hidden && currentRumah) loadRumahFeed();
  }, 30000);
}

function stopRumahPolling() {
  if (rumahPollInterval) { clearInterval(rumahPollInterval); rumahPollInterval = null; }
}

function openBuatRumahModal() { document.getElementById('modal-buat-rumah').style.display = 'flex'; }
function closeBuatRumahModal() { document.getElementById('modal-buat-rumah').style.display = 'none'; }
function openGabungModal() { document.getElementById('modal-gabung').style.display = 'flex'; }
function closeGabungModal() { document.getElementById('modal-gabung').style.display = 'none'; }

async function onGabungInput() {
  const raw = document.getElementById('gabung-token').value;
  const match = raw.match(/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/i);
  const preview = document.getElementById('gabung-preview');
  if (!match) { preview.style.display = 'none'; return; }
  try {
    const res = await fetch('/rumah/join/' + match[0]);
    if (!res.ok) { preview.style.display = 'none'; return; }
    const data = await res.json();
    document.getElementById('gabung-preview-name').textContent = (data.emoji || '🏠') + ' ' + data.name;
    document.getElementById('gabung-preview-count').textContent = data.memberCount + ' anggota';
    preview.style.display = 'block';
  } catch (e) { preview.style.display = 'none'; }
}

async function submitBuatRumah() {
  const name = document.getElementById('buat-nama').value.trim();
  const emoji = document.getElementById('buat-emoji').value.trim() || '🏠';
  const color = document.getElementById('buat-color').value || '#00897B';
  if (!name) { showToast('Isi nama Rumah dulu'); return; }
  const res = await authFetch('/rumah', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, emoji, color })
  });
  if (res.ok) { closeBuatRumahModal(); loadRumahTab(); showToast('Rumah dibuat! 🏠'); }
  else showToast('Gagal membuat Rumah, coba lagi');
}

async function submitGabung() {
  const raw = document.getElementById('gabung-token').value.trim();
  const match = raw.match(/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/i);
  if (!match) { showToast('Link tidak valid'); return; }
  const res = await authFetch('/rumah/join/' + match[0], { method: 'POST' });
  if (res.ok) { closeGabungModal(); loadRumahTab(); showToast('Berhasil bergabung!'); }
  else if (res.status === 409) showToast('Kamu sudah punya Rumah atau Rumah sudah penuh');
  else showToast('Link tidak valid');
}

function shareRumahLink(inviteToken) {
  const link = window.location.origin + '/join/' + inviteToken;
  if (navigator.share) {
    navigator.share({ title: 'Gabung Rumahku di CatetU', url: link });
  } else {
    navigator.clipboard.writeText(link).then(() => showToast('Link undangan disalin! 🔗'));
  }
}

// ─── Bareng toggle ───────────────────────────────────────────────────────────

function updateBarengToggle() {
  const row = document.getElementById('bareng-toggle-row');
  if (!row) return;
  if (currentRumah) {
    row.style.display = 'flex';
    const memberNames = (currentRumah.members || [])
      .map(m => (m.name ? m.name.split(' ')[0] : null))
      .filter(Boolean);
    const preview = memberNames.length
      ? memberNames.join(', ')
      : currentRumah.name || 'Rumah';
    document.getElementById('bareng-members-preview').textContent = preview;
  } else {
    row.style.display = 'none';
  }
  const cb = document.getElementById('bareng-checkbox');
  if (cb) cb.checked = false;
  updateBarengSwitchStyle();
}

function onBarengToggle() {
  updateBarengSwitchStyle();
  const cb = document.getElementById('bareng-checkbox');
  const btn = document.getElementById('sheet-simpan');
  if (!btn) return;
  if (cb?.checked) {
    btn.textContent = 'Simpan ke Rumah 🏠';
    btn.style.background = '#00695C';
  } else {
    btn.textContent = 'Simpan';
    btn.style.background = '';
  }
}

function updateBarengSwitchStyle() {
  const cb = document.getElementById('bareng-checkbox');
  const sw = document.getElementById('bareng-switch');
  if (!cb || !sw) return;
  sw.style.background = cb.checked ? 'var(--accent)' : 'var(--divider)';
  const thumb = document.getElementById('bareng-thumb');
  if (thumb) thumb.style.left = cb.checked ? '22px' : '2px';
}

// Wire Rumah tab data load and polling lifecycle
document.querySelector('.tab[data-screen="rumah"]')?.addEventListener('click', loadRumahTab);
document.querySelectorAll('.tab:not([data-screen="rumah"])').forEach(btn =>
  btn.addEventListener('click', stopRumahPolling)
);
