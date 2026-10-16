/* Sales & Cash Tracker — renderer */
'use strict';

// ---------- Storage bridge (Electron preload, or browser fallback for preview) ----------
let bridge = window.api || (() => {
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
  agents: ['DIRECT / WALK-IN'],
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

// Sub-products suggested for each product (same list as the Android app); saved as the sale's detail
const SUB_PRODUCTS = {
  'FLIGHT TICKET': ['ONE WAY', 'RETURN', 'MULTI CITY', 'DATE CHANGE', 'REISSUE', 'REFUND'],
  'VISA SERVICE': ['30 DAYS TOURIST', '60 DAYS TOURIST', '90 DAYS TOURIST', 'VISA EXTENSION', 'STATUS CHANGE', 'VISA CANCELLATION'],
  'VISA CHANGE BY BUS': ['OMAN EXIT', 'OMAN RETURN', 'BUS + 30 DAYS VISA', 'BUS + 60 DAYS VISA'],
  'KSA MULTIPPLE ENTRY': ['1 YEAR MULTIPLE', 'SINGLE ENTRY', 'UMRAH VISA', 'TRANSIT VISA'],
  'TYPING CENTER / DOCUMENT SERVICE': ['EMIRATES ID', 'MEDICAL', 'ATTESTATION', 'TRANSLATION', 'TYPING', 'LABOUR / MOHRE'],
  'HOTEL': ['1 NIGHT', '2 NIGHTS', '3 NIGHTS', 'WEEK STAY', 'MAKKAH', 'MADINAH'],
  'TOUR PACKAGE': ['DESERT SAFARI', 'CITY TOUR', 'DHOW CRUISE', 'INTERNATIONAL PACKAGE', 'THEME PARK'],
  'GLOBAL VISA': ['SCHENGEN', 'UK', 'USA', 'CANADA', 'TURKEY', 'GEORGIA', 'AZERBAIJAN', 'THAILAND'],
  'UMRAH PACKAGE': ['UMRAH BY BUS', 'UMRAH BY FLIGHT', 'VISA ONLY', 'VISA + HOTEL', 'FULL PACKAGE'],
  'HAJJ PACKAGE': ['ECONOMY', 'STANDARD', 'PREMIUM'],
  'CAR RENTAL': ['DAILY', 'WEEKLY', 'MONTHLY', 'WITH DRIVER'],
  'INSURANCE': ['TRAVEL INSURANCE', 'HEALTH INSURANCE', 'VISA INSURANCE', 'CAR INSURANCE'],
  'OTHER': ['SERVICE CHARGE', 'COURIER', 'MISC'],
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
const TITLES = { dashboard: 'Dashboard', bookings: 'Sales / Bookings', cash: 'Cash Paid Log', ledger: 'Agent Ledger', expiry: 'Visa Expiry Tracker', settings: 'Settings' };
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
    <p class="muted" style="font-size:12px;margin-top:14px">Your records are saved in this browser on this device. Save a backup file now and then from <b>Settings</b>.<br>
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

// Cash paid by an agent isn't tied to one sale: apply each agent's cash to their sales, oldest first,
// so a sale shows as paid once the agent has paid for it. Returns Map(saleId -> amount still due).
function dueAfterCash() {
  const pool = new Map();
  data.cash.forEach(c => { if (c.agent) pool.set(up(c.agent), (pool.get(up(c.agent)) || 0) + n0(c.amount)); });
  const out = new Map();
  [...data.bookings].sort((a, b) => String(a.date).localeCompare(String(b.date)) || String(a.createdAt || '').localeCompare(String(b.createdAt || '')))
    .forEach(b => {
      let due = Math.max(0, n0(calc(b).balance));
      const k = up(b.agent), avail = pool.get(k) || 0;
      if (b.agent && due > 0 && avail > 0) { const use = Math.min(due, avail); due -= use; pool.set(k, avail - use); }
      out.set(b.id, due);
    });
  return out;
}
function statusWith(b, due) {
  const st = up(b.paymentStatus), sell = n0(b.sellingPrice);
  if (st === 'REFUNDED' || sell <= 0) return st;
  if (due <= 0.5) return 'PAID';
  if (due < sell - 0.5) return 'PARTIAL';
  return (st === 'PAID' || st === 'PARTIAL') ? 'PENDING' : (st || 'PENDING');
}
let dueMap = new Map();
const dueOf = b => dueMap.has(b.id) ? dueMap.get(b.id) : n0(calc(b).balance);
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
  { key: 'paymentStatus', label: 'Payment', fmt: b => { const st = statusWith(b, dueOf(b)); return st ? `<span class="pill ${esc(st)}">${esc(st)}</span>` : ''; } },
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
  dueMap = dueAfterCash();
  const f = ui.bookingFilter;
  let rows = periodBookings().filter(b =>
    matchesSearch(b, ['customer', 'agent', 'pnr', 'ref', 'detail', 'contact', 'sector', 'category']) &&
    (!f.agent || b.agent === f.agent) && (!f.category || b.category === f.category) &&
    (!f.status || (f.status === 'DUE' ? dueOf(b) > 0.5 : statusWith(b, dueOf(b)) === f.status)));
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
  const details = [...new Set([...data.bookings.map(x => x.detail).filter(Boolean), ...Object.values(SUB_PRODUCTS).flat()])].sort();
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
    <div class="spacer"></div><span class="muted">Expiry = Entry Date + visa duration</span></div>
    <div class="table-wrap"><table>
      <thead><tr><th>Customer</th><th>Agent</th><th>Product</th><th>Visa / Doc Type</th><th>Entry Date</th><th>Expire Date</th><th class="num">Days Left</th><th>Contact</th><th>Status</th></tr></thead>
      <tbody>${rows.map(({ b, c }) => `<tr data-id="${b.id}"><td class="clickable">${esc(b.customer)}</td><td>${esc(b.agent)}</td><td>${esc(b.category)}</td><td>${esc(b.docType)}</td><td>${fdate(b.entryDate)}</td><td>${fdate(c.expire)}</td>
        <td class="num"><span class="pill ${c.left < 0 ? '' : c.left <= 7 ? 'PENDING' : c.left <= 15 ? 'PARTIAL' : 'PAID'}">${c.left < 0 ? 'expired ' + (-c.left) + 'd ago' : c.left + ' days'}</span></td><td>${esc(b.contact)}</td><td>${esc(b.bookingStatus)}</td></tr>`).join('') || '<tr><td colspan="9" class="empty">No visas with an entry date and a timed visa type yet. Add the Entry Date and a Visa / Doc Type like "30 DAYS" on a booking to track it here.</td></tr>'}</tbody>
    </table></div>`;
  $$('[data-f]', v).forEach(el => el.onclick = () => { ui.expiryFilter = el.dataset.f; render(); });
  $$('tr[data-id] .clickable', v).forEach(el => el.onclick = () => openBookingForm(data.bookings.find(b => b.id === el.closest('tr').dataset.id)));
}

// ---------- Settings / lists / data ----------
async function renderSettings(v) {
  const info = await bridge.info();
  v.innerHTML = `
    <div class="card" style="margin-bottom:14px" id="syncCard"></div>
    <div class="card" style="margin-bottom:14px"><h2>Business</h2>
      <div class="form-grid" style="grid-template-columns:repeat(3,1fr)">
        <label class="field"><span>Company</span><input type="text" id="setCompany" value="${esc(data.settings.company)}"></label>
        <label class="field"><span>Your name</span><input type="text" id="setUser" value="${esc(data.settings.user)}"></label>
        <label class="field"><span>Currency</span><input type="text" id="setCur" value="${esc(data.settings.currency)}"></label>
      </div>
    </div>
    <div class="card"><h2>Data</h2>
      <div class="data-actions">
        <button class="btn green" id="sExport">Export to Excel</button>
        <button class="btn" id="sBackup">Save backup file…</button>
        <button class="btn" id="sRestore">Restore from backup…</button>
        <button class="btn" id="sImport">Import Excel tracker</button>
      </div>
      <p class="muted" style="font-size:12px;margin-bottom:0">Version ${esc(info.version)} · ${data.bookings.length} sales · ${data.cash.length} cash entries</p>
    </div>`;

  renderSyncCard();
  $('#sImport').onclick = doImport;
  $('#sExport').onclick = doExport;
  $('#sBackup').onclick = async () => { const p = await bridge.saveBackup(data); if (p) toast('Backup saved'); };
  $('#sRestore').onclick = doRestore;
  const bindSet = (id, key) => { $(id).onchange = e => { data.settings[key] = e.target.value.trim(); persist(); render(); }; };
  bindSet('#setCompany', 'company'); bindSet('#setUser', 'user'); bindSet('#setCur', 'currency');
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

// ---------- Online sync ----------
const SYNC_TEXT = {
  off: 'Data saves automatically',
  checking: '…',
  'signed-out': 'Not syncing · saved on this device',
  syncing: '⟳ Syncing…',
  synced: '☁ Synced',
  error: '⚠ Sync problem',
};
let syncInfo = { status: 'off', email: '', error: '' };
function setupSync() {
  const s = bridge.sync;
  if (!s || !s.available) return;
  s.onRemoteChange(d => {
    data = normalize(d);
    if (ui.view !== 'settings') render(); else { renderMonthFilter(); updateExpiryBadge(); }
  });
  s.onStatus(info => {
    syncInfo = info;
    const el = $('#syncState');
    if (el) {
      el.hidden = false;
      el.className = 'sync-state ' + info.status;
      el.onclick = () => (info.status === 'signed-out' ? showAuth() : go('settings'));
      let t = SYNC_TEXT[info.status] || '';
      if (info.status === 'synced' && !navigator.onLine) t = '☁ Offline · will sync';
      el.textContent = t;
      el.title = info.error || info.email || '';
    }
    if (ui.view === 'settings' && $('#syncCard')) renderSyncCard();
    renderAccountBox();
    if (info.status === 'signed-out' && !authSkipped) showAuth();
    else if (info.status !== 'signed-out' && info.status !== 'checking') hideAuth();
  });
  window.addEventListener('online', () => s.flush());
}

// ---------- Sign-in page ----------
let authSkipped = false;     // "Continue without signing in" for this session only
let authMode = 'in';         // 'in' | 'up' | 'reset'
function renderAccountBox() {
  const box = $('#accountBox');
  if (!box) return;
  const st = syncInfo.status;
  if (st === 'off' || st === 'checking') { box.hidden = true; return; }
  box.hidden = false;
  if (st === 'signed-out') {
    box.innerHTML = `<button class="btn block ghost-light" id="accSignIn">👤 Sign in to sync</button>`;
    $('#accSignIn').onclick = showAuth;
  } else {
    box.innerHTML = `<div class="acc-email" title="${esc(syncInfo.email)}">👤 ${esc(syncInfo.email)}</div><button class="acc-out" id="accOut">Sign out</button>`;
    $('#accOut').onclick = async () => {
      if (await confirmBox('Sign out?', 'Records stay on this device. Sign in again to keep syncing.', 'Sign out')) { authSkipped = false; await bridge.sync.signOut(); }
    };
  }
}
function showAuth() {
  const el = $('#authScreen');
  if (!el || !bridge.sync || !bridge.sync.available) return;
  const s = bridge.sync;
  const title = { in: 'Sign in', up: 'Create your account', reset: 'Reset password' }[authMode];
  const sub = {
    in: 'Sign in to see the same sales and cash records on your phone, PC and the Windows app.',
    up: 'One account for all your devices. Use an email you can open — it\'s needed if you forget the password.',
    reset: 'Enter your email and we\'ll send you a link to choose a new password.',
  }[authMode];
  el.innerHTML = `<div class="auth-card">
    <div class="auth-brand"><div class="logo">S</div><div><div class="auth-company">${esc(data.settings.company || 'Sales & Cash Tracker')}</div><div class="muted" style="font-size:12px">Sales &amp; Cash Tracker</div></div></div>
    <h2>${title}</h2>
    <p class="muted">${sub}</p>
    <form id="authForm" autocomplete="on">
      <label class="field"><span>Email</span><input type="email" name="email" autocomplete="username" required></label>
      ${authMode !== 'reset' ? `<label class="field"><span>Password</span><input type="password" name="password" autocomplete="${authMode === 'up' ? 'new-password' : 'current-password'}" required minlength="6"></label>` : ''}
      ${authMode === 'up' ? `<label class="field"><span>Confirm password</span><input type="password" name="password2" autocomplete="new-password" required minlength="6"></label>` : ''}
      <div class="auth-msg" id="authMsg"></div>
      <button class="btn primary block auth-go" type="submit">${{ in: 'Sign in', up: 'Create account', reset: 'Send reset link' }[authMode]}</button>
    </form>
    <div class="auth-links">
      ${authMode === 'in' ? `<a href="#" data-mode="reset">Forgot password?</a><span>New here? <a href="#" data-mode="up">Create an account</a></span>` : `<a href="#" data-mode="in">← Back to sign in</a>`}
    </div>
    <button class="auth-skip" id="authSkip">Continue without signing in</button>
  </div>`;
  el.hidden = false;
  $$('[data-mode]', el).forEach(a => a.onclick = e => { e.preventDefault(); authMode = a.dataset.mode; showAuth(); });
  $('#authSkip').onclick = () => { authSkipped = true; hideAuth(); };
  const f = $('#authForm');
  const msg = (t, ok) => { const m = $('#authMsg'); m.textContent = t || ''; m.className = 'auth-msg' + (ok ? ' ok' : ''); };
  f.onsubmit = async e => {
    e.preventDefault();
    const email = f.email.value.trim();
    const btn = $('.auth-go', f);
    msg('');
    if (authMode === 'up' && f.password.value !== f.password2.value) { msg('The two passwords don\'t match.'); return; }
    btn.disabled = true; const label = btn.textContent; btn.textContent = 'Please wait…';
    try {
      if (authMode === 'reset') { await s.resetPassword(email); msg('Reset link sent. Check your email (and spam folder).', true); }
      else if (authMode === 'up') { await s.signUp(email, f.password.value); toast('Account created — syncing'); }
      else { await s.signIn(email, f.password.value); toast('Signed in — syncing'); }
    } catch (err) { msg(friendlyError(err)); }
    btn.disabled = false; btn.textContent = label;
  };
  setTimeout(() => f.email.focus(), 30);
}
function hideAuth() { const el = $('#authScreen'); if (el) { el.hidden = true; el.innerHTML = ''; } authMode = 'in'; }
function friendlyError(e) {
  const code = (e && (e.code || e.message)) || String(e);
  const map = {
    'auth/invalid-credential': 'Wrong email or password.',
    'auth/wrong-password': 'Wrong email or password.',
    'auth/user-not-found': 'No account with that email. Use “Create account” first.',
    'auth/invalid-email': 'That email address doesn\'t look right.',
    'auth/email-already-in-use': 'An account with this email already exists. Use “Sign in”.',
    'auth/weak-password': 'Password must be at least 6 characters.',
    'auth/missing-password': 'Enter a password.',
    'auth/network-request-failed': 'No internet connection. Try again when online.',
    'auth/too-many-requests': 'Too many attempts. Wait a few minutes and try again.',
    'auth/operation-not-allowed': 'Email/Password sign-in is not switched on in Firebase (Authentication → Sign-in method).',
    'permission-denied': 'Firebase refused access. Check the Firestore rules were published.',
  };
  for (const k of Object.keys(map)) if (code.includes(k)) return map[k];
  return code.replace(/^Firebase:\s*/, '');
}
function renderSyncCard() {
  const card = $('#syncCard');
  if (!card) return;
  const s = bridge.sync;
  if (!s || !s.available) {
    card.innerHTML = `<h2>Online sync</h2><p class="muted" style="margin:0">Online sync isn't set up in this version.</p>`;
    return;
  }
  const st = syncInfo.status;
  if (st === 'signed-out') {
    card.innerHTML = `<h2>Online sync <span class="hint">share records between your phone and PC</span></h2>
      <p class="muted" style="margin-top:0">Sign in with the same email on every device. Records already on this device are kept and added to your account.</p>
      <form id="syncForm" class="form-grid" style="grid-template-columns:1fr 1fr auto;align-items:end">
        <label class="field"><span>Email</span><input type="email" name="email" autocomplete="username" required></label>
        <label class="field"><span>Password</span><input type="password" name="password" autocomplete="current-password" required minlength="6"></label>
        <div style="display:flex;gap:6px;flex-wrap:wrap"><button class="btn primary" type="submit" data-act="in">Sign in</button><button class="btn" type="button" data-act="up">Create account</button></div>
      </form>
      <p style="margin:8px 0 0;font-size:12px"><a href="#" id="syncForgot">Forgot password?</a> <span id="syncMsg" class="due" style="margin-left:8px"></span></p>`;
    const f = $('#syncForm');
    const msg = t => { $('#syncMsg').textContent = t || ''; };
    const run = async (fn, label) => {
      const email = f.email.value.trim(), pw = f.password.value;
      if (!email || !pw) { msg('Enter your email and password.'); return; }
      $$('button', f).forEach(b => b.disabled = true); msg('');
      try { await fn(email, pw); toast(label); }
      catch (e) { msg(friendlyError(e)); $$('button', f).forEach(b => b.disabled = false); }
    };
    f.onsubmit = e => { e.preventDefault(); run(s.signIn, 'Signed in — syncing'); };
    $('[data-act=up]', f).onclick = () => run(s.signUp, 'Account created — syncing');
    $('#syncForgot').onclick = async e => {
      e.preventDefault();
      const email = f.email.value.trim();
      if (!email) { msg('Type your email first, then click “Forgot password?”.'); return; }
      try { await s.resetPassword(email); msg(''); toast('Password reset email sent'); } catch (err) { msg(friendlyError(err)); }
    };
    return;
  }
  const label = { syncing: 'Syncing…', synced: navigator.onLine ? 'All records synced' : 'Offline — changes will sync when you\'re back online', error: 'Sync problem' }[st] || st;
  card.innerHTML = `<h2>Online sync <span class="pill ${st === 'synced' ? 'PAID' : st === 'error' ? 'PENDING' : 'SUBMITTED'}">${esc(label)}</span></h2>
    <p style="margin-top:0">Signed in as <b>${esc(syncInfo.email)}</b>. New and edited records go to your account automatically and appear on your other devices within seconds.</p>
    ${syncInfo.error ? `<p class="due" style="font-size:12.5px">${esc(friendlyError(syncInfo.error))}</p>` : ''}
    <div class="data-actions"><button class="btn" id="syncNow">Sync now</button><button class="btn" id="syncOut">Sign out</button></div>`;
  $('#syncNow').onclick = () => { s.flush(); toast('Syncing…'); };
  $('#syncOut').onclick = async () => {
    if (await confirmBox('Sign out of online sync?', 'Records stay on this device, but new changes will no longer sync until you sign in again.', 'Sign out')) {
      await s.signOut(); toast('Signed out');
    }
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
  const out = { ...base, ...d, settings: { ...base.settings, ...(d.settings || {}) }, lists: { ...base.lists } };
  // Keep built-in choices when a synced list is missing or empty
  Object.entries(d.lists || {}).forEach(([k, v]) => { if (!Array.isArray(v) || v.length) out.lists[k] = v; });
  out.bookings = Array.isArray(d.bookings) ? d.bookings : [];
  out.cash = Array.isArray(d.cash) ? d.cash : [];
  out.bookings.forEach(b => { if (!b.id) b.id = uid(); });
  out.cash.forEach(c => { if (!c.id) c.id = uid(); });
  return out;
}

async function boot() {
  // Online sync (if set up) wraps the storage bridge; wait for it before loading
  if (window.apiReady) { try { bridge = (await window.apiReady) || bridge; } catch (e) { console.error('Sync unavailable', e); } }
  data = normalize(await bridge.load());
  const info = await bridge.info();
  $('#sideFoot').innerHTML = `${IS_WEB ? 'Web' : 'Desktop'} v${esc(info.version)} · Ctrl+N new sale<br>Data saves automatically`;
  setupSync();
  $$('#nav button').forEach(b => b.onclick = () => go(b.dataset.view));
  $('#monthFilter').onchange = e => { ui.month = e.target.value; render(); };
  $('#search').oninput = e => { ui.search = e.target.value; render(); $('#search').focus(); };
  $('#sideAddBooking').onclick = () => openBookingForm();
  $('#sideAddCash').onclick = () => openCashForm();
  $('#fab').onclick = () => (ui.view === 'cash' ? openCashForm() : openBookingForm());
  go('dashboard');
}
boot();
