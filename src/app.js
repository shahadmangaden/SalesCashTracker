/* Sales & Cash Tracker — renderer */
'use strict';

// ---------- Storage bridge (Electron preload, or browser fallback for preview) ----------
const bridge = window.api || (() => {
  const KEY = 'sct-data';
  return {
    load: async () => { try { return JSON.parse(localStorage.getItem(KEY)); } catch (_) { return null; } },
    save: async d => { try { localStorage.setItem(KEY, JSON.stringify(d)); } catch (_) { } return true; },
    info: async () => ({ dataFile: '(browser preview)', backupDir: '(browser preview)', version: 'dev' }),
    openDataFolder: async () => { },
    importExcel: async () => { toast('Excel import works in the installed app.', true); return null; },
    exportExcel: async () => { toast('Excel export works in the installed app.', true); return null; },
    saveBackup: async () => null,
    restoreBackup: async () => null,
    confirm: async m => window.confirm(m),
  };
})();

// ---------- Defaults (from the original workbook) ----------
const DEFAULT_LISTS = {
  agents: ['KHASRUL', 'IJAZ TYPING', 'RAHEEB', 'JALEEL', 'OUD AL ANFAL', 'AMMOLITE JEWELLERY', 'SHAFEER KUNHI PARAMBATH', 'AFR ELECTRONICS', 'MOHAMMED KABIR', 'DIRECT / WALK-IN', 'OFFICE DEPOSIT / OTHER'],
  customerTypes: ['AGENT', 'SUB-AGENT', 'DIRECT CUSTOMER', 'BUSINESS', 'CORPORATE'],
  categories: ['FLIGHT TICKET', 'VISA SERVICE', 'VISA CHANGE BY BUS', 'KSA MULTIPPLE ENTRY', 'TYPING CENTER / DOCUMENT SERVICE', 'HOTEL', 'TOUR PACKAGE', 'GLOBAL VISA', 'UMRAH PACKAGE', 'HAJJ PACKAGE', 'CAR RENTAL', 'INSURANCE', 'OTHER'],
  paymentStatuses: ['PAID', 'PARTIAL', 'PENDING', 'REFUNDED'],
  paymentModes: ['CASH', 'BANK TRANSFER', 'CDM DEPOSIT', 'CREDIT CARD', 'DEBIT CARD', 'ONLINE PAYMENT LINK', 'WALLET PAYMENT', 'CHEQUE'],
  bookingStatuses: ['SUBMITTED', 'ENQUIRY', 'HOLD', 'OPEN', 'ACTIVE', 'USED', 'COMPLETED', 'CANCELLED', 'REFUND'],
  docTypes: [
    { name: '14 DAYS', days: 14 }, { name: '30 DAYS', days: 30 }, { name: '60 DAYS', days: 60 }, { name: '90 DAYS', days: 90 },
    { name: '120 DAYS', days: 120 }, { name: '180 DAYS', days: 180 }, { name: '1 YEAR / 365 DAYS', days: 365 },
    { name: '2 YEARS / 730 DAYS', days: 730 }, { name: 'TRANSIT 48 HOURS', days: 2 }, { name: 'TRANSIT 96 HOURS', days: 4 },
    { name: 'SINGLE ENTRY', days: null }, { name: 'MULTIPLE ENTRY', days: null }, { name: 'TOURIST VISA', days: null },
    { name: 'VISIT VISA', days: null }, { name: 'BUSINESS VISA', days: null }, { name: 'EMPLOYMENT VISA', days: null },
    { name: 'RESIDENCE VISA', days: null }, { name: 'STUDENT VISA', days: null }, { name: 'FAMILY VISA', days: null },
    { name: 'MEDICAL VISA', days: null }, { name: 'E-VISA', days: null }, { name: 'VISA ON ARRIVAL', days: null },
    { name: 'GLOBAL VISA', days: null }, { name: 'UMRAH VISA', days: null }, { name: 'HAJJ VISA', days: null },
    { name: 'DOCUMENT SERVICE', days: null }, { name: 'PASSPORT SERVICE', days: null }, { name: 'EMIRATES ID', days: null }, { name: 'N/A', days: null },
  ],
  countries: ['UAE', 'IND', 'PAK', 'BGD', 'NPL', 'LKA', 'PHL', 'KSA', 'OMN', 'QAT', 'BHR', 'KWT', 'EGY', 'JOR', 'TUR', 'GEO', 'ARM', 'AZE', 'THA', 'MYS', 'IDN', 'SGP', 'GBR', 'USA', 'CAN', 'AUS', 'FRA', 'DEU', 'ITA', 'ESP', 'CHE', 'NLD', 'RUS', 'KAZ', 'UZB', 'KEN', 'ETH', 'NGA', 'ZAF', 'CHN', 'JPN', 'KOR', 'MDV', 'AFG', 'IRN', 'IRQ', 'LBN', 'SYR', 'YEM', 'SDN'],
};

function emptyData() {
  return {
    version: 1,
    settings: { company: 'BAKHTAR TRAVELS', user: 'SAHAD M', currency: 'AED' },
    lists: JSON.parse(JSON.stringify(DEFAULT_LISTS)),
    bookings: [],
    cash: [],
  };
}

// ---------- State ----------
let data = emptyData();
const ui = {
  view: 'dashboard',
  month: 'all',
  search: '',
  sort: { bookings: { key: 'date', dir: -1 }, cash: { key: 'date', dir: -1 } },
  bookingFilter: { agent: '', category: '', status: '' },
  expiryFilter: 'upcoming',
  checkDate: todayISO(),
  checkAgent: '',
};

// ---------- Helpers ----------
const $ = (s, el = document) => el.querySelector(s);
const $$ = (s, el = document) => [...el.querySelectorAll(s)];
const MONTHS = ['JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC'];
function esc(s) { return String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c])); }
function pad(n) { return String(n).padStart(2, '0'); }
function todayISO() { const d = new Date(); return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`; }
function uid() { return (crypto.randomUUID ? crypto.randomUUID() : Date.now().toString(36) + Math.random().toString(36).slice(2)); }
function n0(v) { return typeof v === 'number' && !isNaN(v) ? v : 0; }
function money(v, blankZero) {
  if (v === null || v === undefined || v === '' || isNaN(v)) return '';
  if (blankZero && !v) return '–';
  return Number(v).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}
function pct(v) { return v === null || v === undefined || isNaN(v) ? '' : (v * 100).toFixed(1) + '%'; }
function fdate(iso) {
  if (!iso) return '';
  const [y, m, d] = iso.split('-');
  return `${d}-${MONTHS[+m - 1].charAt(0)}${MONTHS[+m - 1].slice(1).toLowerCase()}-${y.slice(2)}`;
}
function monthName(key) { const [y, m] = key.split('-'); return `${MONTHS[+m - 1]} ${y}`; }
function addDays(iso, days) {
  const [y, m, d] = iso.split('-').map(Number);
  const t = new Date(Date.UTC(y, m - 1, d + days));
  return `${t.getUTCFullYear()}-${pad(t.getUTCMonth() + 1)}-${pad(t.getUTCDate())}`;
}
function daysBetween(a, b) {
  const p = s => { const [y, m, d] = s.split('-').map(Number); return Date.UTC(y, m - 1, d); };
  return Math.round((p(b) - p(a)) / 86400000);
}
function numOrNull(v) { if (v === '' || v === null || v === undefined) return null; const n = parseFloat(String(v).replace(/,/g, '')); return isNaN(n) ? null : n; }
function up(v) { return String(v || '').trim().toUpperCase(); }

function docDays(name) { const d = data.lists.docTypes.find(x => x.name === name); return d ? d.days : null; }
function calc(b) {
  const sell = b.sellingPrice, cost = b.netCost;
  const profit = sell === null || sell === undefined ? null : sell - n0(cost);
  const margin = sell ? profit / sell : null;
  const balance = sell === null || sell === undefined ? null : sell - n0(b.amountPaid);
  const days = docDays(b.docType);
  const expire = b.entryDate && days ? addDays(b.entryDate, days) : '';
  const left = expire ? daysBetween(todayISO(), expire) : null;
  return { profit, margin, balance, expire, left };
}

function inPeriod(iso) { return ui.month === 'all' || (iso || '').startsWith(ui.month); }
function periodBookings() { return data.bookings.filter(b => inPeriod(b.date)); }
function periodCash() { return data.cash.filter(c => inPeriod(c.date)); }
function periodLabel() { return ui.month === 'all' ? 'All months' : monthName(ui.month); }

function totals(bookings, cash) {
  const t = { count: bookings.length, sales: 0, cost: 0, profit: 0, paid: 0, balance: 0, pax: 0, cash: 0 };
  for (const b of bookings) {
    const c = calc(b);
    t.sales += n0(b.sellingPrice); t.cost += n0(b.netCost); t.profit += n0(c.profit);
    t.paid += n0(b.amountPaid); t.balance += n0(c.balance); t.pax += n0(b.pax);
  }
  for (const c of cash) t.cash += n0(c.amount);
  t.margin = t.sales ? t.profit / t.sales : 0;
  t.remaining = t.balance - t.cash;
  return t;
}

let saveTimer = null;
function persist() {
  clearTimeout(saveTimer);
  saveTimer = setTimeout(async () => {
    try { await bridge.save(data); } catch (e) { toast('Could not save: ' + e.message, true); }
  }, 150);
}

let toastTimer;
function toast(msg, err) {
  const t = $('#toast');
  t.textContent = msg; t.className = 'toast' + (err ? ' err' : ''); t.hidden = false;
  clearTimeout(toastTimer); toastTimer = setTimeout(() => { t.hidden = true; }, 2800);
}

// ---------- Month filter ----------
function monthKeys() {
  const s = new Set();
  data.bookings.forEach(b => b.date && s.add(b.date.slice(0, 7)));
  data.cash.forEach(c => c.date && s.add(c.date.slice(0, 7)));
  s.add(todayISO().slice(0, 7));
  return [...s].sort().reverse();
}
function renderMonthFilter() {
  const sel = $('#monthFilter');
  const keys = monthKeys();
  if (ui.month !== 'all' && !keys.includes(ui.month)) ui.month = 'all';
  sel.innerHTML = `<option value="all">All months</option>` + keys.map(k => `<option value="${k}">${monthName(k)}</option>`).join('');
  sel.value = ui.month;
}

// ---------- Navigation ----------
const TITLES = { dashboard: 'Dashboard', bookings: 'Sales / Bookings', cash: 'Cash Paid Log', ledger: 'Agent Ledger', expiry: 'Visa Expiry Tracker', settings: 'Lists & Data' };
function go(view) {
  ui.view = view;
  $$('#nav button').forEach(b => b.classList.toggle('active', b.dataset.view === view));
  $('#viewTitle').textContent = TITLES[view];
  $('#search').hidden = !['bookings', 'cash'].includes(view);
  render();
}
function render() {
  renderMonthFilter();
  $('#brandCompany').textContent = data.settings.company || 'Sales & Cash Tracker';
  $('#brandUser').textContent = (data.settings.user ? data.settings.user + ' · ' : '') + 'Sales & Cash Tracker';
  updateExpiryBadge();
  const v = $('#view');
  if (!data.bookings.length && !data.cash.length && ui.view === 'dashboard') { v.innerHTML = welcomeHTML(); bindWelcome(); return; }
  ({ dashboard: renderDashboard, bookings: renderBookings, cash: renderCash, ledger: renderLedger, expiry: renderExpiry, settings: renderSettings })[ui.view](v);
}

const IS_WEB = !window.api || window.api.platform === 'web';

function welcomeHTML() {
  if (IS_WEB) {
    return `<div class="welcome card">
    <h2>Welcome to your Sales &amp; Cash Tracker</h2>
    <p>You're starting fresh. Record each sale or booking as it happens, and log cash paid by agents. Totals, profit, balances, the agent ledger and visa expiry alerts all fill in automatically.</p>
    <div><button class="btn primary" id="wNew">+ Add first sale</button>
    <button class="btn" id="wCash">+ Record cash paid</button></div>
    <p class="muted" style="font-size:12px;margin-top:14px">Your records are saved in this browser on this device. Save a backup file now and then from <b>Lists &amp; Data</b>.<br>
    Have an existing Excel tracker? <a href="#" id="wImport">Import it</a> · <a href="#" id="wRestore">Restore a backup</a></p>
  </div>`;
  }
  return `<div class="welcome card">
    <h2>Welcome to your Sales &amp; Cash Tracker</h2>
    <p>Bring in everything from your Excel tracker in one click — bookings, the cash-paid log, agents and visa types. Or start fresh and add your first sale.</p>
    <div><button class="btn primary" id="wImport">Import my Excel tracker</button>
    <button class="btn" id="wNew">+ Add first sale</button>
    <button class="btn" id="wRestore">Restore a backup</button></div>
  </div>`;
}
function bindWelcome() {
  $('#wImport').onclick = e => { e.preventDefault(); doImport(); };
  $('#wNew').onclick = () => openBookingForm();
  $('#wRestore').onclick = e => { e.preventDefault(); doRestore(); };
  if ($('#wCash')) $('#wCash').onclick = () => openCashForm();
}

// ---------- Dashboard ----------
function kpi(label, value, sub, cls) {
  return `<div class="kpi ${cls || ''}"><div class="label">${label}</div><div class="value">${value}</div>${sub ? `<div class="sub">${sub}</div>` : ''}</div>`;
}
function renderDashboard(v) {
  const B = periodBookings(), C = periodCash();
  const t = totals(B, C);
  const cur = data.settings.currency || 'AED';

  // month-by-month
  const keys = monthKeys().slice().reverse();
  const monthRows = keys.map(k => {
    const mt = totals(data.bookings.filter(b => b.date.startsWith(k)), data.cash.filter(c => c.date.startsWith(k)));
    if (!mt.count && !mt.cash) return '';
    return `<tr class="${ui.month === k ? 'sel' : ''}"><td class="clickable" data-month="${k}">${monthName(k)}</td><td class="num">${money(mt.sales)}</td><td class="num">${money(mt.cost)}</td>
      <td class="num pos">${money(mt.profit)}</td><td class="num">${pct(mt.margin)}</td><td class="num">${mt.count}</td>
      <td class="num">${money(mt.paid)}</td><td class="num ${mt.balance > 0 ? 'due' : ''}">${money(mt.balance)}</td><td class="num">${money(mt.cash)}</td></tr>`;
  }).join('');
  const all = totals(data.bookings, data.cash);

  // breakdowns
  const group = (key) => {
    const m = {};
    B.forEach(b => { const k = b[key] || '(blank)'; m[k] = m[k] || { sales: 0, profit: 0, n: 0 }; m[k].sales += n0(b.sellingPrice); m[k].profit += n0(calc(b).profit); m[k].n++; });
    return Object.entries(m).sort((a, b) => b[1].sales - a[1].sales);
  };
  const bars = (rows) => {
    const max = Math.max(1, ...rows.map(r => r[1].sales));
    return rows.length ? rows.map(([k, r]) => `<div class="bar-row"><div class="name">${esc(k)} <span class="muted">· ${r.n} · profit ${money(r.profit)}</span></div><div class="amt">${money(r.sales)}</div><div class="bar"><i style="width:${(r.sales / max * 100).toFixed(1)}%"></i></div></div>`).join('') : '<div class="empty">No sales in this period</div>';
  };

  // recent cash
  const recentCash = data.cash.slice().sort((a, b) => b.date.localeCompare(a.date)).slice(0, 8);

  v.innerHTML = `
    <div class="grid kpis">
      ${kpi('Total Sales', money(t.sales), `${cur} · ${periodLabel()}`)}
      ${kpi('Net Cost', money(t.cost), cur)}
      ${kpi('Profit', money(t.profit), `Margin ${pct(t.margin)}`, 'good')}
      ${kpi('Bookings', t.count, `${t.pax} pax served`)}
      ${kpi('Paid in Booking Rows', money(t.paid), cur)}
      ${kpi('Unpaid Balance', money(t.balance), 'Row-level balance due', t.balance > 0 ? 'warn' : '')}
      ${kpi('Cash Paid (Log)', money(t.cash), `${C.length} entries`, 'good')}
      ${kpi('Net Remaining', money(t.remaining), 'Balance − cash log', t.remaining > 0 ? 'bad' : '')}
    </div>
    <div class="grid three" style="margin-bottom:14px">
      <div class="card"><h2>Month-by-Month <span class="hint">click a month to focus</span></h2>
        <div class="table-wrap"><table>
          <thead><tr><th>Month</th><th class="num">Sales</th><th class="num">Net Cost</th><th class="num">Profit</th><th class="num">Margin</th><th class="num">Bookings</th><th class="num">Paid in Rows</th><th class="num">Balance Due</th><th class="num">Cash Log</th></tr></thead>
          <tbody>${monthRows || '<tr><td colspan="9" class="empty">No data yet</td></tr>'}</tbody>
          <tfoot><tr><td>TOTAL</td><td class="num">${money(all.sales)}</td><td class="num">${money(all.cost)}</td><td class="num">${money(all.profit)}</td><td class="num">${pct(all.margin)}</td><td class="num">${all.count}</td><td class="num">${money(all.paid)}</td><td class="num">${money(all.balance)}</td><td class="num">${money(all.cash)}</td></tr></tfoot>
        </table></div>
      </div>
      <div class="card"><h2>Cash Paid Checker <span class="hint">by date &amp; agent, all months</span></h2>
        <div class="checker">
          <label>Date<input type="date" id="ckDate" value="${ui.checkDate}"></label>
          <label>Agent / Customer<select id="ckAgent"><option value="">— any agent —</option>${agentOptions(ui.checkAgent)}</select></label>
        </div>
        <div class="check-results" id="ckResults"></div>
        <div id="ckAgentTotals" style="margin-top:10px"></div>
      </div>
    </div>
    <div class="grid two" style="margin-bottom:14px">
      <div class="card"><h2>Sales by Product <span class="hint">${periodLabel()}</span></h2>${bars(group('category'))}</div>
      <div class="card"><h2>Sales by Customer Type <span class="hint">${periodLabel()}</span></h2>${bars(group('customerType'))}</div>
    </div>
    <div class="card"><h2>Latest Cash Paid Entries <button class="btn small" style="margin-left:auto" id="dashAddCash">+ Cash Paid</button></h2>
      <div class="table-wrap"><table><thead><tr><th>Date</th><th>Agent / Customer</th><th>Mode</th><th class="num">Amount</th><th>Remarks</th></tr></thead>
      <tbody>${recentCash.map(c => `<tr><td>${fdate(c.date)}</td><td>${esc(c.agent)}</td><td>${esc(c.mode)}</td><td class="num">${money(c.amount)}</td><td class="muted">${esc(c.remarks)}</td></tr>`).join('') || '<tr><td colspan="5" class="empty">No cash paid entries yet</td></tr>'}</tbody></table></div>
    </div>`;

  $$('[data-month]', v).forEach(el => el.onclick = () => { ui.month = el.dataset.month; render(); });
  $('#dashAddCash').onclick = () => openCashForm();
  const upd = () => {
    ui.checkDate = $('#ckDate').value; ui.checkAgent = $('#ckAgent').value;
    const onDate = data.cash.filter(c => c.date === ui.checkDate);
    const byAgent = data.cash.filter(c => ui.checkAgent && c.agent === ui.checkAgent);
    const both = onDate.filter(c => !ui.checkAgent || c.agent === ui.checkAgent);
    $('#ckResults').innerHTML = `
      <div><b>${money(onDate.reduce((a, c) => a + n0(c.amount), 0))}</b><span>Paid on ${fdate(ui.checkDate) || '—'} (${onDate.length})</span></div>
      <div><b>${ui.checkAgent ? money(byAgent.reduce((a, c) => a + n0(c.amount), 0)) : '–'}</b><span>All-time by agent</span></div>
      <div><b>${money(both.reduce((a, c) => a + n0(c.amount), 0))}</b><span>Date + agent</span></div>`;
    if (ui.checkAgent) {
      const ab = data.bookings.filter(b => b.agent === ui.checkAgent);
      const at = totals(ab, byAgent);
      $('#ckAgentTotals').innerHTML = `<div class="muted" style="font-size:12px;line-height:1.7">${esc(ui.checkAgent)} — all time: <b>${at.count}</b> bookings · sales <b>${money(at.sales)}</b> · net cost <b>${money(at.cost)}</b> · balance due <b>${money(at.balance)}</b> · after cash log <b class="${at.remaining > 0 ? 'due' : ''}">${money(at.remaining)}</b></div>`;
    } else $('#ckAgentTotals').innerHTML = '';
  };
  $('#ckDate').onchange = upd; $('#ckAgent').onchange = upd; upd();
}

function agentOptions(selected) {
  const set = new Set(data.lists.agents);
  data.bookings.forEach(b => b.agent && set.add(b.agent));
  data.cash.forEach(c => c.agent && set.add(c.agent));
  return [...set].sort().map(a => `<option ${a === selected ? 'selected' : ''}>${esc(a)}</option>`).join('');
}

// ---------- Bookings ----------
const BOOKING_COLS = [
  { key: 'date', label: 'T. Date', fmt: b => fdate(b.date) },
  { key: 'customer', label: 'Customer Name' },
  { key: 'customerType', label: 'Type' },
  { key: 'agent', label: 'Agency / Sub-Agent' },
  { key: 'category', label: 'Product' },
  { key: 'detail', label: 'Service Detail', cls: 'wrap' },
  { key: 'sector', label: 'Sector' },
  { key: 'pnr', label: 'PNR / Ref', fmt: b => esc([b.pnr, b.ref].filter(Boolean).join(' · ')) },
  { key: 'netCost', label: 'Net Cost', num: true, fmt: b => money(b.netCost) },
  { key: 'sellingPrice', label: 'Selling', num: true, fmt: b => money(b.sellingPrice) },
  { key: 'profit', label: 'Profit', num: true, calc: true, fmt: (b, c) => `<span class="${c.profit < 0 ? 'neg' : 'pos'}">${money(c.profit)}</span>` },
  { key: 'margin', label: '%', num: true, calc: true, fmt: (b, c) => pct(c.margin) },
  { key: 'paymentStatus', label: 'Payment', fmt: b => b.paymentStatus ? `<span class="pill ${esc(b.paymentStatus)}">${esc(b.paymentStatus)}</span>` : '' },
  { key: 'paymentMode', label: 'Mode' },
  { key: 'amountPaid', label: 'Paid', num: true, fmt: b => money(b.amountPaid) },
  { key: 'balance', label: 'Balance', num: true, calc: true, fmt: (b, c) => `<span class="${c.balance > 0 ? 'due' : ''}">${money(c.balance, true)}</span>` },
  { key: 'bookingStatus', label: 'Status', fmt: b => b.bookingStatus ? `<span class="pill ${esc(b.bookingStatus)}">${esc(b.bookingStatus)}</span>` : '' },
  { key: 'docType', label: 'Visa / Doc' },
  { key: 'expire', label: 'Expires', calc: true, fmt: (b, c) => c.expire ? `${fdate(c.expire)} <span class="${c.left < 0 ? 'muted' : c.left <= 7 ? 'due' : 'muted'}">(${c.left}d)</span>` : '' },
  { key: 'pax', label: 'Pax', num: true },
];

function matchesSearch(obj, fields) {
  const q = ui.search.trim().toLowerCase();
  if (!q) return true;
  return fields.some(f => String(obj[f] ?? '').toLowerCase().includes(q));
}
function sortRows(rows, s, getter) {
  return rows.sort((a, b) => {
    const x = getter(a, s.key), y = getter(b, s.key);
    if (typeof x === 'number' || typeof y === 'number') return (n0(x) - n0(y)) * s.dir;
    return String(x ?? '').localeCompare(String(y ?? '')) * s.dir;
  });
}

function renderBookings(v) {
  const f = ui.bookingFilter;
  let rows = periodBookings().filter(b =>
    matchesSearch(b, ['customer', 'agent', 'pnr', 'ref', 'detail', 'contact', 'sector', 'category']) &&
    (!f.agent || b.agent === f.agent) && (!f.category || b.category === f.category) &&
    (!f.status || (f.status === 'DUE' ? n0(calc(b).balance) > 0 : b.paymentStatus === f.status)));
  const s = ui.sort.bookings;
  rows = sortRows(rows, s, (b, k) => { if (['profit', 'margin', 'balance', 'expire'].includes(k)) return calc(b)[k]; return b[k]; });
  const t = totals(rows, []);
  const cats = [...new Set(data.bookings.map(b => b.category).filter(Boolean))].sort();

  v.innerHTML = `
    <div class="toolbar">
      <select id="fAgent"><option value="">All agents</option>${agentOptions(f.agent)}</select>
      <select id="fCat"><option value="">All products</option>${cats.map(c => `<option ${c === f.category ? 'selected' : ''}>${esc(c)}</option>`).join('')}</select>
      <div class="chips">${['', 'PAID', 'PARTIAL', 'PENDING', 'DUE'].map(st => `<span class="chip ${f.status === st ? 'on' : ''}" data-st="${st}">${st === '' ? 'All' : st === 'DUE' ? 'Has balance' : st}</span>`).join('')}</div>
      <div class="spacer"></div>
      <span class="muted">${rows.length} of ${periodBookings().length} · ${periodLabel()}</span>
      <button class="btn primary" id="addB">+ New Sale</button>
    </div>
    <div class="table-wrap" style="max-height:calc(100vh - 190px)">
      <table>
        <thead><tr><th>#</th>${BOOKING_COLS.map(c => `<th class="sortable ${c.num ? 'num' : ''}" data-k="${c.key}">${c.label}${s.key === c.key ? (s.dir > 0 ? ' ▲' : ' ▼') : ''}</th>`).join('')}<th></th></tr></thead>
        <tbody>${rows.map((b, i) => {
          const c = calc(b);
          return `<tr data-id="${b.id}"><td class="muted">${i + 1}</td>${BOOKING_COLS.map(col => `<td class="${col.num ? 'num' : ''} ${col.cls || ''}">${col.fmt ? col.fmt(b, c) : esc(b[col.key] ?? '')}</td>`).join('')}
            <td><div class="row-actions"><button class="btn small" data-act="edit">Edit</button><button class="btn small" data-act="dup" title="Duplicate">⧉</button><button class="btn small danger" data-act="del" title="Delete">✕</button></div></td></tr>`;
        }).join('') || `<tr><td colspan="${BOOKING_COLS.length + 2}" class="empty">No bookings match. Add a new sale or change the filters.</td></tr>`}</tbody>
        <tfoot><tr><td></td><td colspan="8">TOTAL (${t.count} bookings, ${t.pax} pax)</td><td class="num">${money(t.cost)}</td><td class="num">${money(t.sales)}</td><td class="num">${money(t.profit)}</td><td class="num">${pct(t.margin)}</td><td></td><td></td><td class="num">${money(t.paid)}</td><td class="num">${money(t.balance)}</td><td colspan="5"></td></tr></tfoot>
      </table>
    </div>`;

  $('#fAgent').onchange = e => { f.agent = e.target.value; render(); };
  $('#fCat').onchange = e => { f.category = e.target.value; render(); };
  $$('.chip', v).forEach(el => el.onclick = () => { f.status = el.dataset.st; render(); });
  $('#addB').onclick = () => openBookingForm();
  $$('th.sortable', v).forEach(th => th.onclick = () => { const k = th.dataset.k; s.dir = s.key === k ? -s.dir : (k === 'date' ? -1 : 1); s.key = k; render(); });
  $$('tbody tr[data-id]', v).forEach(tr => {
    const b = data.bookings.find(x => x.id === tr.dataset.id);
    tr.ondblclick = () => openBookingForm(b);
    $$('[data-act]', tr).forEach(btn => btn.onclick = async e => {
      e.stopPropagation();
      const act = btn.dataset.act;
      if (act === 'edit') openBookingForm(b);
      if (act === 'dup') { const copy = { ...b, id: undefined, customer: '', pnr: '', contact: '' }; openBookingForm(copy, true); }
      if (act === 'del' && await confirmBox('Delete this booking?', `${b.customer} · ${fdate(b.date)} · ${money(b.sellingPrice)}`)) {
        data.bookings = data.bookings.filter(x => x.id !== b.id); persist(); render(); toast('Booking deleted');
      }
    });
  });
}

function selectHTML(name, list, value, allowBlank = true) {
  const opts = [...list];
  if (value && !opts.includes(value)) opts.unshift(value);
  return `<select name="${name}">${allowBlank ? '<option value=""></option>' : ''}${opts.map(o => `<option ${o === value ? 'selected' : ''}>${esc(o)}</option>`).join('')}</select>`;
}
function datalistInput(name, list, value, id) {
  return `<input type="text" name="${name}" list="${id}" value="${esc(value || '')}" autocomplete="off"><datalist id="${id}">${list.map(o => `<option value="${esc(o)}">`).join('')}</datalist>`;
}

function openBookingForm(b, isCopy) {
  const isNew = !b || !b.id;
  b = b || {};
  const L = data.lists;
  const agents = [...new Set([...L.agents, ...data.bookings.map(x => x.agent).filter(Boolean)])].sort();
  const details = [...new Set(data.bookings.map(x => x.detail).filter(Boolean))].sort();
  const f = (label, html, cls = '', req) => `<label class="field ${cls}"><span>${label}${req ? ' <i class="req">*</i>' : ''}</span>${html}</label>`;
  const val = k => esc(b[k] ?? '');
  openModal(`
    <div class="modal-head"><h3>${isNew ? (isCopy ? 'New Sale (copied)' : 'New Sale / Booking') : 'Edit Booking'}</h3><button class="x" data-close>×</button></div>
    <form class="modal-body" id="bForm" autocomplete="off">
      <div class="form-grid">
        <div class="section-label">Customer &amp; Product</div>
        ${f('T. Date', `<input type="date" name="date" value="${b.date || todayISO()}" required>`, '', 1)}
        ${f('Customer Name', `<input type="text" name="customer" value="${val('customer')}" required>`, 'span2', 1)}
        ${f('Customer Type', selectHTML('customerType', L.customerTypes, b.customerType))}
        ${f('Agency / Sub-Agent Name', datalistInput('agent', agents, b.agent, 'dlAgents'), 'span2')}
        ${f('Product Category', selectHTML('category', L.categories, b.category))}
        ${f('Contact', `<input type="text" name="contact" value="${val('contact')}">`)}
        ${f('Product / Service Detail', datalistInput('detail', details, b.detail, 'dlDetails'), 'span2')}
        ${f('Sector / Destination', datalistInput('sector', L.countries, b.sector, 'dlSector'))}
        ${f('No. of Pax', `<input type="number" name="pax" min="0" step="1" value="${b.pax ?? 1}">`)}
        ${f('Flights / Ref', `<input type="text" name="ref" value="${val('ref')}">`)}
        ${f('PNR', `<input type="text" name="pnr" value="${val('pnr')}">`)}
        ${f('Service / Travel Date', `<input type="date" name="serviceDate" value="${b.serviceDate || ''}">`)}
        ${f('Booking Status', selectHTML('bookingStatus', L.bookingStatuses, b.bookingStatus))}

        <div class="section-label">Price &amp; Payment</div>
        ${f('Net Cost', `<input type="number" name="netCost" step="0.01" value="${b.netCost ?? ''}">`)}
        ${f('Selling Price', `<input type="number" name="sellingPrice" step="0.01" value="${b.sellingPrice ?? ''}">`)}
        ${f('Profit', `<div class="calc" id="cProfit"></div>`)}
        ${f('Profit %', `<div class="calc" id="cMargin"></div>`)}
        ${f('Amount Paid', `<input type="number" name="amountPaid" step="0.01" value="${b.amountPaid ?? ''}">`)}
        ${f('Payment Status', selectHTML('paymentStatus', L.paymentStatuses, b.paymentStatus))}
        ${f('Payment Mode', selectHTML('paymentMode', L.paymentModes, b.paymentMode))}
        ${f('Balance Due', `<div class="calc" id="cBal"></div>`)}
        <div class="span4"><button type="button" class="btn small" id="fullPaid">Mark fully paid</button> <span class="muted" style="font-size:12px">Payment status updates automatically from the amount paid unless you choose one.</span></div>

        <div class="section-label">Visa / Document</div>
        ${f('Visa / Doc Type', selectHTML('docType', L.docTypes.map(d => d.name), b.docType))}
        ${f('Entry Date', `<input type="date" name="entryDate" value="${b.entryDate || ''}">`)}
        ${f('Expire Date', `<div class="calc" id="cExp"></div>`)}
        ${f('Days to Expiry', `<div class="calc" id="cLeft"></div>`)}
      </div>
    </form>
    <div class="modal-foot">
      ${!isNew ? '<button class="btn danger" id="bDel" style="margin-right:auto">Delete</button>' : ''}
      <button class="btn" data-close>Cancel</button>
      ${isNew ? '<button class="btn" id="bSaveNew">Save &amp; add another</button>' : ''}
      <button class="btn primary" id="bSave">Save</button>
    </div>`);

  const form = $('#bForm');
  let statusTouched = !!b.paymentStatus && !isNew;
  form.paymentStatus.addEventListener('change', () => { statusTouched = true; });
  const read = () => {
    const fd = Object.fromEntries(new FormData(form).entries());
    return {
      date: fd.date, customer: up(fd.customer), customerType: fd.customerType, agent: up(fd.agent), category: fd.category,
      detail: up(fd.detail), contact: fd.contact.trim(), sector: up(fd.sector), ref: up(fd.ref), pnr: up(fd.pnr),
      netCost: numOrNull(fd.netCost), sellingPrice: numOrNull(fd.sellingPrice), paymentStatus: fd.paymentStatus,
      paymentMode: fd.paymentMode, amountPaid: numOrNull(fd.amountPaid), serviceDate: fd.serviceDate,
      bookingStatus: fd.bookingStatus, entryDate: fd.entryDate, docType: fd.docType, pax: numOrNull(fd.pax),
    };
  };
  const live = () => {
    const r = read(); const c = calc(r);
    $('#cProfit').textContent = money(c.profit) || '—';
    $('#cProfit').className = 'calc ' + (c.profit < 0 ? 'neg' : 'pos');
    $('#cMargin').textContent = pct(c.margin) || '—';
    $('#cBal').textContent = money(c.balance) || '—';
    $('#cBal').className = 'calc ' + (c.balance > 0 ? 'due' : '');
    $('#cExp').textContent = c.expire ? fdate(c.expire) : '—';
    $('#cLeft').textContent = c.left === null ? '—' : c.left + ' days';
    if (!statusTouched && r.sellingPrice !== null) {
      const paid = n0(r.amountPaid);
      form.paymentStatus.value = paid <= 0 ? 'PENDING' : paid >= r.sellingPrice ? 'PAID' : 'PARTIAL';
    }
  };
  form.addEventListener('input', live); form.addEventListener('change', live); live();
  $('#fullPaid').onclick = () => { form.amountPaid.value = form.sellingPrice.value; form.paymentStatus.value = 'PAID'; statusTouched = true; live(); };

  const save = (again) => {
    const r = read();
    if (!r.date || !r.customer) { toast('Date and customer name are required', true); (r.date ? form.customer : form.date).focus(); return; }
    if (isNew) { data.bookings.push({ id: uid(), ...r, createdAt: new Date().toISOString() }); }
    else Object.assign(data.bookings.find(x => x.id === b.id), r, { updatedAt: new Date().toISOString() });
    if (r.agent && !data.lists.agents.includes(r.agent)) data.lists.agents.push(r.agent);
    persist(); toast(isNew ? 'Sale saved' : 'Booking updated');
    if (again) {
      // keep date / agent / type / product for fast entry
      openBookingForm({ date: r.date, agent: r.agent, customerType: r.customerType, category: r.category, paymentMode: r.paymentMode, pax: 1 });
      render(); setTimeout(() => $('#bForm').customer.focus(), 30);
    } else { closeModal(); render(); }
  };
  $('#bSave').onclick = () => save(false);
  if ($('#bSaveNew')) $('#bSaveNew').onclick = () => save(true);
  form.onsubmit = e => { e.preventDefault(); save(false); };
  if ($('#bDel')) $('#bDel').onclick = async () => {
    if (await confirmBox('Delete this booking?', `${b.customer} · ${fdate(b.date)}`)) {
      data.bookings = data.bookings.filter(x => x.id !== b.id); persist(); closeModal(); render(); toast('Booking deleted');
    }
  };
  setTimeout(() => (isNew ? form.customer : form.date).focus(), 30);
}

// ---------- Cash Paid ----------
function renderCash(v) {
  let rows = periodCash().filter(c => matchesSearch(c, ['agent', 'mode', 'remarks']));
  const s = ui.sort.cash;
  rows = sortRows(rows, s, (c, k) => c[k]);
  const total = rows.reduce((a, c) => a + n0(c.amount), 0);
  // daily summary
  const byDay = {};
  rows.forEach(c => { byDay[c.date] = byDay[c.date] || { n: 0, amt: 0 }; byDay[c.date].n++; byDay[c.date].amt += n0(c.amount); });
  const byAgent = {};
  rows.forEach(c => { byAgent[c.agent || '(blank)'] = (byAgent[c.agent || '(blank)'] || 0) + n0(c.amount); });
  const cols = [['date', 'Cash Paid Date'], ['agent', 'Agent / Customer'], ['mode', 'Payment Mode'], ['amount', 'Amount', 1], ['remarks', 'Remarks / Receipt Ref']];
  v.innerHTML = `
    <div class="toolbar"><span class="muted">${rows.length} entries · ${periodLabel()}</span><div class="spacer"></div><button class="btn primary" id="addC">+ Cash Paid</button></div>
    <div class="grid three">
      <div class="table-wrap" style="max-height:calc(100vh - 190px)"><table>
        <thead><tr><th>#</th>${cols.map(([k, l, num]) => `<th class="sortable ${num ? 'num' : ''}" data-k="${k}">${l}${s.key === k ? (s.dir > 0 ? ' ▲' : ' ▼') : ''}</th>`).join('')}<th></th></tr></thead>
        <tbody>${rows.map((c, i) => `<tr data-id="${c.id}"><td class="muted">${i + 1}</td><td>${fdate(c.date)}</td><td>${esc(c.agent)}</td><td>${esc(c.mode)}</td><td class="num">${money(c.amount)}</td><td class="wrap muted">${esc(c.remarks)}</td>
          <td><div class="row-actions"><button class="btn small" data-act="edit">Edit</button><button class="btn small danger" data-act="del">✕</button></div></td></tr>`).join('') || '<tr><td colspan="7" class="empty">No cash paid entries in this period.</td></tr>'}</tbody>
        <tfoot><tr><td></td><td colspan="3">TOTAL CASH PAID</td><td class="num">${money(total)}</td><td colspan="2"></td></tr></tfoot>
      </table></div>
      <div>
        <div class="card" style="margin-bottom:14px"><h2>By Agent</h2><table><tbody>${Object.entries(byAgent).sort((a, b) => b[1] - a[1]).map(([a, amt]) => `<tr><td>${esc(a)}</td><td class="num">${money(amt)}</td></tr>`).join('') || '<tr><td class="empty">—</td></tr>'}</tbody></table></div>
        <div class="card"><h2>Daily Summary</h2><table><thead><tr><th>Date</th><th class="num">Entries</th><th class="num">Total</th></tr></thead><tbody>${Object.entries(byDay).sort((a, b) => b[0].localeCompare(a[0])).map(([d, r]) => `<tr><td>${fdate(d)}</td><td class="num">${r.n}</td><td class="num">${money(r.amt)}</td></tr>`).join('') || '<tr><td colspan="3" class="empty">—</td></tr>'}</tbody></table></div>
      </div>
    </div>`;
  $('#addC').onclick = () => openCashForm();
  $$('th.sortable', v).forEach(th => th.onclick = () => { const k = th.dataset.k; s.dir = s.key === k ? -s.dir : (k === 'date' ? -1 : 1); s.key = k; render(); });
  $$('tbody tr[data-id]', v).forEach(tr => {
    const c = data.cash.find(x => x.id === tr.dataset.id);
    tr.ondblclick = () => openCashForm(c);
    $$('[data-act]', tr).forEach(btn => btn.onclick = async e => {
      e.stopPropagation();
      if (btn.dataset.act === 'edit') openCashForm(c);
      else if (await confirmBox('Delete this cash paid entry?', `${c.agent} · ${fdate(c.date)} · ${money(c.amount)}`)) {
        data.cash = data.cash.filter(x => x.id !== c.id); persist(); render(); toast('Entry deleted');
      }
    });
  });
}

function openCashForm(c) {
  const isNew = !c;
  c = c || { date: todayISO(), mode: 'CASH' };
  const agents = [...new Set([...data.lists.agents, ...data.cash.map(x => x.agent).filter(Boolean)])].sort();
  openModal(`
    <div class="modal-head"><h3>${isNew ? 'Record Cash Paid' : 'Edit Cash Paid'}</h3><button class="x" data-close>×</button></div>
    <form class="modal-body" id="cForm" autocomplete="off">
      <div class="form-grid" style="grid-template-columns:1fr 1fr">
        <label class="field"><span>Cash Paid Date <i class="req">*</i></span><input type="date" name="date" value="${c.date || ''}" required></label>
        <label class="field"><span>Amount <i class="req">*</i></span><input type="number" step="0.01" name="amount" value="${c.amount ?? ''}" required></label>
        <label class="field"><span>Agent / Customer</span>${datalistInput('agent', agents, c.agent, 'dlCashAgents')}</label>
        <label class="field"><span>Payment Mode</span>${selectHTML('mode', data.lists.paymentModes, c.mode)}</label>
        <label class="field span4"><span>Remarks / Receipt Ref</span><input type="text" name="remarks" value="${esc(c.remarks || '')}"></label>
      </div>
    </form>
    <div class="modal-foot"><button class="btn" data-close>Cancel</button>${isNew ? '<button class="btn" id="cSaveNew">Save &amp; add another</button>' : ''}<button class="btn primary" id="cSave">Save</button></div>`, true);
  const form = $('#cForm');
  const save = again => {
    const fd = Object.fromEntries(new FormData(form).entries());
    const r = { date: fd.date, agent: up(fd.agent), mode: fd.mode, amount: numOrNull(fd.amount), remarks: fd.remarks.trim() };
    if (!r.date || r.amount === null) { toast('Date and amount are required', true); return; }
    if (isNew) data.cash.push({ id: uid(), ...r, createdAt: new Date().toISOString() });
    else Object.assign(data.cash.find(x => x.id === c.id), r);
    if (r.agent && !data.lists.agents.includes(r.agent)) data.lists.agents.push(r.agent);
    persist(); toast('Cash paid saved');
    if (again) { openCashForm(); $('#cForm').date.value = r.date; $('#cForm').agent.value = r.agent; render(); setTimeout(() => $('#cForm').amount.focus(), 30); }
    else { closeModal(); render(); }
  };
  $('#cSave').onclick = () => save(false);
  if ($('#cSaveNew')) $('#cSaveNew').onclick = () => save(true);
  form.onsubmit = e => { e.preventDefault(); save(false); };
  setTimeout(() => form.amount.focus(), 30);
}

// ---------- Agent ledger ----------
function renderLedger(v) {
  const B = periodBookings(), C = periodCash();
  const names = new Set([...B.map(b => b.agent || '(no agent)'), ...C.map(c => c.agent || '(no agent)')]);
  const rows = [...names].map(a => {
    const t = totals(B.filter(b => (b.agent || '(no agent)') === a), C.filter(c => (c.agent || '(no agent)') === a));
    return { agent: a, ...t };
  }).sort((a, b) => b.sales - a.sales);
  const T = totals(B, C);
  v.innerHTML = `
    <div class="toolbar"><span class="muted">Agent &amp; customer account ledger · ${periodLabel()} · click a name to see their bookings</span></div>
    <div class="table-wrap"><table>
      <thead><tr><th>Agent / Customer</th><th class="num">Bookings</th><th class="num">Pax</th><th class="num">Net Cost</th><th class="num">Selling Price</th><th class="num">Profit</th><th class="num">Paid in Rows</th><th class="num">Balance Due</th><th class="num">Cash Paid (Log)</th><th class="num">Net Remaining</th></tr></thead>
      <tbody>${rows.map(r => `<tr><td class="clickable" data-agent="${esc(r.agent)}">${esc(r.agent)}</td><td class="num">${r.count}</td><td class="num">${r.pax}</td><td class="num">${money(r.cost)}</td><td class="num">${money(r.sales)}</td><td class="num pos">${money(r.profit)}</td><td class="num">${money(r.paid)}</td><td class="num">${money(r.balance, true)}</td><td class="num">${money(r.cash, true)}</td><td class="num ${r.remaining > 0 ? 'due' : r.remaining < 0 ? 'pos' : ''}">${money(r.remaining, true)}</td></tr>`).join('') || '<tr><td colspan="10" class="empty">No activity in this period</td></tr>'}</tbody>
      <tfoot><tr><td>TOTAL</td><td class="num">${T.count}</td><td class="num">${T.pax}</td><td class="num">${money(T.cost)}</td><td class="num">${money(T.sales)}</td><td class="num">${money(T.profit)}</td><td class="num">${money(T.paid)}</td><td class="num">${money(T.balance)}</td><td class="num">${money(T.cash)}</td><td class="num">${money(T.remaining)}</td></tr></tfoot>
    </table></div>
    <p class="muted" style="font-size:12px;margin-top:10px">Net Remaining = Balance Due (from booking rows) − Cash Paid (from the cash log), the same as the "Net Remaining After Cash Log" line in the Excel tracker. A negative figure means the agent has paid ahead.</p>`;
  $$('[data-agent]', v).forEach(el => el.onclick = () => {
    ui.bookingFilter = { agent: el.dataset.agent === '(no agent)' ? '' : el.dataset.agent, category: '', status: '' };
    go('bookings');
  });
}

// ---------- Visa expiry ----------
function expiryRows() {
  return data.bookings.map(b => ({ b, c: calc(b) })).filter(r => r.c.expire);
}
function updateExpiryBadge() {
  const n = expiryRows().filter(r => r.c.left >= 0 && r.c.left <= 7 && !['CANCELLED', 'REFUND'].includes(r.b.bookingStatus)).length;
  const el = $('#expiryBadge'); el.hidden = !n; el.textContent = n;
}
function renderExpiry(v) {
  const f = ui.expiryFilter;
  let rows = expiryRows().filter(r => f === 'all' || (f === 'upcoming' ? r.c.left >= 0 : r.c.left < 0));
  rows.sort((a, b) => f === 'expired' ? b.c.left - a.c.left : a.c.left - b.c.left);
  v.innerHTML = `
    <div class="toolbar"><div class="chips">${[['upcoming', 'Active / upcoming'], ['expired', 'Expired'], ['all', 'All']].map(([k, l]) => `<span class="chip ${f === k ? 'on' : ''}" data-f="${k}">${l}</span>`).join('')}</div>
    <div class="spacer"></div><span class="muted">Expiry = Entry Date + visa duration (set durations under Lists &amp; Data)</span></div>
    <div class="table-wrap"><table>
      <thead><tr><th>Customer</th><th>Agent</th><th>Product</th><th>Visa / Doc Type</th><th>Entry Date</th><th>Expire Date</th><th class="num">Days Left</th><th>Contact</th><th>Status</th></tr></thead>
      <tbody>${rows.map(({ b, c }) => `<tr data-id="${b.id}"><td class="clickable">${esc(b.customer)}</td><td>${esc(b.agent)}</td><td>${esc(b.category)}</td><td>${esc(b.docType)}</td><td>${fdate(b.entryDate)}</td><td>${fdate(c.expire)}</td>
        <td class="num"><span class="pill ${c.left < 0 ? '' : c.left <= 7 ? 'PENDING' : c.left <= 15 ? 'PARTIAL' : 'PAID'}">${c.left < 0 ? 'expired ' + (-c.left) + 'd ago' : c.left + ' days'}</span></td><td>${esc(b.contact)}</td><td>${esc(b.bookingStatus)}</td></tr>`).join('') || '<tr><td colspan="9" class="empty">No visas with an entry date and a timed visa type yet. Add the Entry Date and a Visa / Doc Type like "30 DAYS" on a booking to track it here.</td></tr>'}</tbody>
    </table></div>`;
  $$('[data-f]', v).forEach(el => el.onclick = () => { ui.expiryFilter = el.dataset.f; render(); });
  $$('tr[data-id] .clickable', v).forEach(el => el.onclick = () => openBookingForm(data.bookings.find(b => b.id === el.closest('tr').dataset.id)));
}

// ---------- Settings / lists / data ----------
const LIST_DEFS = [
  ['agents', 'Agents / Sub-Agents'], ['categories', 'Product Categories'], ['customerTypes', 'Customer Types'],
  ['paymentModes', 'Payment Modes'], ['paymentStatuses', 'Payment Statuses'], ['bookingStatuses', 'Booking Statuses'], ['countries', 'Sectors / Destinations'],
];
async function renderSettings(v) {
  const info = await bridge.info();
  v.innerHTML = `
    <div class="card" style="margin-bottom:14px"><h2>Data</h2>
      <p class="muted" style="margin-top:0">${IS_WEB
        ? 'Everything is saved automatically in this browser on this device, with a snapshot kept each day (last 30 days). Clearing browser data deletes it, so save a backup file regularly. To move to another device, save a backup here and restore it there.'
        : 'Everything is saved automatically on this PC. A dated backup copy is kept every day (last 60 days).'}</p>
      <div class="data-actions">
        <button class="btn primary" id="sImport">Import Excel tracker</button>
        <button class="btn green" id="sExport">Export to Excel</button>
        <button class="btn" id="sBackup">Save backup file…</button>
        <button class="btn" id="sRestore">Restore from backup…</button>
        ${IS_WEB ? '<button class="btn" id="sSnap">Restore daily snapshot…</button>' : '<button class="btn" id="sFolder">Open data folder</button>'}
        <button class="btn danger" id="sWipe" style="margin-left:auto">Delete all records…</button>
      </div>
      <p class="muted" style="font-size:12px;margin-bottom:0">Data file: <span class="path">${esc(info.dataFile)}</span> · App version ${esc(info.version)} · ${data.bookings.length} bookings, ${data.cash.length} cash entries</p>
    </div>
    <div class="card" style="margin-bottom:14px"><h2>Business</h2>
      <div class="form-grid" style="grid-template-columns:repeat(3,1fr)">
        <label class="field"><span>Company</span><input type="text" id="setCompany" value="${esc(data.settings.company)}"></label>
        <label class="field"><span>Sales person</span><input type="text" id="setUser" value="${esc(data.settings.user)}"></label>
        <label class="field"><span>Currency</span><input type="text" id="setCur" value="${esc(data.settings.currency)}"></label>
      </div>
    </div>
    <div class="lists-grid">
      ${LIST_DEFS.map(([k, label]) => `<div class="card list-edit" data-list="${k}"><h2>${label} <span class="hint">${data.lists[k].length}</span></h2>
        <ul>${data.lists[k].map((item, i) => `<li><span>${esc(item)}</span><button data-i="${i}" title="Remove">×</button></li>`).join('')}</ul>
        <div class="add"><input type="text" placeholder="Add new…"><button class="btn small">Add</button></div></div>`).join('')}
      <div class="card list-edit" data-list="docTypes"><h2>Visa / Doc Types <span class="hint">days → expiry</span></h2>
        <ul>${data.lists.docTypes.map((d, i) => `<li><span>${esc(d.name)}</span><span style="display:flex;gap:6px;align-items:center"><input type="number" min="0" style="width:70px" data-days="${i}" value="${d.days ?? ''}" placeholder="days"><button data-i="${i}" title="Remove">×</button></span></li>`).join('')}</ul>
        <div class="add"><input type="text" placeholder="Add new type…"><button class="btn small">Add</button></div></div>
    </div>`;

  $('#sImport').onclick = doImport;
  $('#sExport').onclick = doExport;
  $('#sBackup').onclick = async () => { const p = await bridge.saveBackup(data); if (p) toast('Backup saved'); };
  $('#sRestore').onclick = doRestore;
  if ($('#sFolder')) $('#sFolder').onclick = () => bridge.openDataFolder();
  if ($('#sSnap')) $('#sSnap').onclick = doRestoreSnapshot;
  $('#sWipe').onclick = async () => {
    if (await confirmBox('Delete ALL bookings and cash entries?', 'Lists and settings are kept. Tip: save a backup first. Today\'s automatic backup also stays in the data folder.', 'Delete everything')) {
      data.bookings = []; data.cash = []; persist(); render(); toast('All records deleted');
    }
  };
  const bindSet = (id, key) => { $(id).onchange = e => { data.settings[key] = e.target.value.trim(); persist(); render(); }; };
  bindSet('#setCompany', 'company'); bindSet('#setUser', 'user'); bindSet('#setCur', 'currency');

  $$('.list-edit', v).forEach(card => {
    const key = card.dataset.list;
    const input = $('.add input', card);
    const add = () => {
      const val = up(input.value);
      if (!val) return;
      const list = data.lists[key];
      if (key === 'docTypes') { if (!list.some(d => d.name === val)) list.push({ name: val, days: null }); }
      else if (!list.includes(val)) list.push(val);
      persist(); render();
    };
    $('.add button', card).onclick = add;
    input.onkeydown = e => { if (e.key === 'Enter') add(); };
    $$('li button', card).forEach(btn => btn.onclick = () => { data.lists[key].splice(+btn.dataset.i, 1); persist(); render(); });
    $$('[data-days]', card).forEach(inp => inp.onchange = () => { data.lists.docTypes[+inp.dataset.days].days = numOrNull(inp.value); persist(); });
  });
}

function signature(b) { return [b.date, up(b.customer), n0(b.sellingPrice), up(b.agent), up(b.detail)].join('|'); }
function cashSig(c) { return [c.date, up(c.agent), n0(c.amount)].join('|'); }

async function doImport() {
  let res;
  try { res = await bridge.importExcel(); } catch (e) { toast('Import failed: ' + e.message, true); return; }
  if (!res) return;
  if (!res.bookings.length && !res.cash.length) { toast('No bookings found in that file. Is it the tracker workbook?', true); return; }
  const summary = `${res.bookings.length} bookings and ${res.cash.length} cash entries found.`;
  let mode = 'replace';
  if (data.bookings.length || data.cash.length) {
    mode = await choiceBox('Import Excel tracker', `${summary}<br><br>You already have ${data.bookings.length} bookings and ${data.cash.length} cash entries in the app.`,
      [['add', 'Add new only (skip duplicates)', 'primary'], ['replace', 'Replace everything', 'danger solid'], ['', 'Cancel', '']]);
    if (!mode) return;
  }
  let addedB = 0, addedC = 0;
  if (mode === 'replace') { data.bookings = res.bookings; data.cash = res.cash; addedB = res.bookings.length; addedC = res.cash.length; }
  else {
    const sb = new Set(data.bookings.map(signature)), sc = new Set(data.cash.map(cashSig));
    res.bookings.forEach(b => { if (!sb.has(signature(b))) { data.bookings.push(b); addedB++; } });
    res.cash.forEach(c => { if (!sc.has(cashSig(c))) { data.cash.push(c); addedC++; } });
  }
  res.agents.forEach(a => { if (!data.lists.agents.includes(a)) data.lists.agents.push(a); });
  res.docTypes.forEach(d => { const ex = data.lists.docTypes.find(x => x.name === d.name); if (!ex) data.lists.docTypes.push(d); else if (d.days && !ex.days) ex.days = d.days; });
  // Pick up any categories / statuses used in the sheet so dropdowns include them
  const addAll = (key, vals) => vals.forEach(v => { if (v && !data.lists[key].includes(v)) data.lists[key].push(v); });
  addAll('categories', res.bookings.map(b => b.category)); addAll('customerTypes', res.bookings.map(b => b.customerType));
  addAll('paymentModes', [...res.bookings.map(b => b.paymentMode), ...res.cash.map(c => c.mode)]);
  addAll('bookingStatuses', res.bookings.map(b => b.bookingStatus)); addAll('countries', res.bookings.map(b => b.sector));
  persist(); ui.month = 'all'; go('dashboard');
  toast(`Imported ${addedB} bookings and ${addedC} cash entries`);
}
async function doExport() {
  const stamp = todayISO();
  const p = await bridge.exportExcel(data, `Sales-Cash-Tracker-${stamp}.xlsx`);
  if (p) toast('Exported to Excel');
}
async function doRestore() {
  let d;
  try { d = await bridge.restoreBackup(); } catch (e) { toast(e.message, true); return; }
  if (!d) return;
  if (!(await confirmBox('Restore this backup?', `It has ${d.bookings.length} bookings and ${(d.cash || []).length} cash entries. Your current records will be replaced.`, 'Restore'))) return;
  data = normalize(d); persist(); go('dashboard'); toast('Backup restored');
}

async function doRestoreSnapshot() {
  const dates = bridge.listSnapshots ? await bridge.listSnapshots() : [];
  if (!dates.length) { toast('No daily snapshots yet', true); return; }
  openModal(`<div class="modal-head"><h3>Restore daily snapshot</h3><button class="x" data-close>×</button></div>
    <div class="modal-body"><p class="muted" style="margin-top:0">Each day's last saved state is kept. Pick a day to go back to; your current records will be replaced.</p>
    <select id="snapPick" style="width:100%">${dates.map(d => `<option value="${d}">${fdate(d)}</option>`).join('')}</select></div>
    <div class="modal-foot"><button class="btn" data-close>Cancel</button><button class="btn primary" id="snapGo">Restore</button></div>`, true);
  $('#snapGo').onclick = async () => {
    const d = await bridge.getSnapshot($('#snapPick').value);
    closeModal();
    if (!d) return;
    if (!(await confirmBox('Restore this snapshot?', `It has ${d.bookings.length} bookings and ${(d.cash || []).length} cash entries. Current records will be replaced.`, 'Restore'))) return;
    data = normalize(d); persist(); go('dashboard'); toast('Snapshot restored');
  };
}

// ---------- Modal helpers ----------
function openModal(html, narrow) {
  const m = $('#modal');
  m.className = 'modal' + (narrow ? ' narrow' : '');
  m.innerHTML = html; $('#modalBack').hidden = false;
  $$('[data-close]', m).forEach(b => b.onclick = closeModal);
}
function closeModal() { $('#modalBack').hidden = true; $('#modal').innerHTML = ''; }
function choiceBox(title, bodyHTML, buttons) {
  return new Promise(resolve => {
    openModal(`<div class="modal-head"><h3>${esc(title)}</h3></div><div class="modal-body">${bodyHTML}</div>
      <div class="modal-foot">${buttons.slice().reverse().map(([v, l, cls]) => `<button class="btn ${cls}" data-v="${v}">${esc(l)}</button>`).join('')}</div>`, true);
    $$('[data-v]', $('#modal')).forEach(b => b.onclick = () => { closeModal(); resolve(b.dataset.v); });
  });
}
async function confirmBox(title, detail, okLabel = 'Delete') {
  const v = await choiceBox(title, `<p style="margin:0">${esc(detail || '')}</p>`, [['yes', okLabel, 'danger solid'], ['', 'Cancel', '']]);
  return v === 'yes';
}
document.addEventListener('keydown', e => {
  if (e.key === 'Escape' && !$('#modalBack').hidden) closeModal();
  if (e.ctrlKey && e.key.toLowerCase() === 'n' && $('#modalBack').hidden) { e.preventDefault(); openBookingForm(); }
});

// ---------- Boot ----------
function normalize(d) {
  const base = emptyData();
  d = d || {};
  const out = { ...base, ...d, settings: { ...base.settings, ...(d.settings || {}) }, lists: { ...base.lists, ...(d.lists || {}) } };
  out.bookings = Array.isArray(d.bookings) ? d.bookings : [];
  out.cash = Array.isArray(d.cash) ? d.cash : [];
  out.bookings.forEach(b => { if (!b.id) b.id = uid(); });
  out.cash.forEach(c => { if (!c.id) c.id = uid(); });
  return out;
}

async function boot() {
  data = normalize(await bridge.load());
  const info = await bridge.info();
  $('#sideFoot').innerHTML = `${IS_WEB ? 'Web' : 'Desktop'} v${esc(info.version)} · Ctrl+N new sale<br>Data saves automatically`;
  $$('#nav button').forEach(b => b.onclick = () => go(b.dataset.view));
  $('#monthFilter').onchange = e => { ui.month = e.target.value; render(); };
  $('#search').oninput = e => { ui.search = e.target.value; render(); $('#search').focus(); };
  $('#sideAddBooking').onclick = () => openBookingForm();
  $('#sideAddCash').onclick = () => openCashForm();
  $('#fab').onclick = () => (ui.view === 'cash' ? openCashForm() : openBookingForm());
  go('dashboard');
}
boot();
