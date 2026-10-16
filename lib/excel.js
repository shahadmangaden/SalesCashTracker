// Excel import / export for the Sales & Cash Tracker workbook format.
// Import understands the original "SAHAD M SALES AND CASH TRACKER" layout:
//   monthly sheets named like "OCT 2026", bookings in A:Z, cash-paid log in AB:AG.
const ExcelJS = require('exceljs');
const XLSX = require('xlsx');
const { randomUUID } = require('crypto');

const MONTHS = ['JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC'];

function pad(n) { return String(n).padStart(2, '0'); }
function toISO(v) {
  if (v === null || v === undefined || v === '') return '';
  if (v instanceof Date) {
    // SheetJS returns local-time dates; round to the nearest day to absorb timezone offsets
    const t = new Date(v.getTime() + 12 * 3600000 - v.getTimezoneOffset() * 60000);
    return `${t.getUTCFullYear()}-${pad(t.getUTCMonth() + 1)}-${pad(t.getUTCDate())}`;
  }
  if (typeof v === 'number') { // Excel serial
    const d = new Date(Math.round((v - 25569) * 86400000));
    return `${d.getUTCFullYear()}-${pad(d.getUTCMonth() + 1)}-${pad(d.getUTCDate())}`;
  }
  const s = String(v).trim();
  const m = s.match(/^(\d{4})-(\d{1,2})-(\d{1,2})/);
  if (m) return `${m[1]}-${pad(m[2])}-${pad(m[3])}`;
  const m2 = s.match(/^(\d{1,2})[\/.-](\d{1,2})[\/.-](\d{4})$/); // dd/mm/yyyy
  if (m2) return `${m2[3]}-${pad(m2[2])}-${pad(m2[1])}`;
  return '';
}
function str(v) { return v === null || v === undefined ? '' : String(v).trim(); }
function num(v) {
  if (v === null || v === undefined || v === '') return null;
  const n = typeof v === 'number' ? v : parseFloat(String(v).replace(/,/g, ''));
  return isNaN(n) ? null : n;
}
function monthSheetFirstDay(name) {
  const m = name.trim().toUpperCase().match(/^([A-Z]{3})\w*\s+(\d{4})$/);
  if (!m) return null;
  const idx = MONTHS.indexOf(m[1]);
  if (idx < 0) return null;
  return `${m[2]}-${pad(idx + 1)}-01`;
}

function importWorkbook(filePath) {
  const wb = XLSX.readFile(filePath, { cellDates: true, cellFormula: false });
  const bookings = [];
  const cash = [];
  const agents = new Set();
  const docTypes = [];

  for (const name of wb.SheetNames) {
    const ws = wb.Sheets[name];
    const g = (r, c) => { // 1-based row/col
      const cell = ws[XLSX.utils.encode_cell({ r: r - 1, c: c - 1 })];
      return cell ? cell.v : null;
    };
    const range = ws['!ref'] ? XLSX.utils.decode_range(ws['!ref']) : { e: { r: 0 } };
    const lastRow = range.e.r + 1;

    if (name.trim().toUpperCase() === 'LISTS') {
      // Find columns by their header text
      const col = {};
      for (let c = 1; c <= range.e.c + 1; c++) {
        const h = str(g(1, c)).toUpperCase();
        if (h.includes('VISA') || h.includes('DOC TYPE')) col.doc = c;
        else if (h.includes('VALID DAYS')) col.days = c;
        else if (h.includes('AGENT')) col.agent = c;
      }
      for (let r = 2; r <= lastRow; r++) {
        const dt = col.doc ? str(g(r, col.doc)) : '';
        if (dt) docTypes.push({ name: dt, days: col.days ? num(g(r, col.days)) : null });
        const ag = col.agent ? str(g(r, col.agent)) : '';
        if (ag) agents.add(ag);
      }
      continue;
    }
    const firstDay = monthSheetFirstDay(name);
    if (!firstDay) continue;
    if (!str(g(1, 3)).toUpperCase().includes('CUSTOMER')) continue;

    for (let r = 2; r <= lastRow; r++) {
      const customer = str(g(r, 3));
      if (customer && customer.toUpperCase() !== 'CASH PAID') {
        bookings.push({
          id: randomUUID(),
          date: toISO(g(r, 2)) || firstDay,
          customer,
          customerType: str(g(r, 4)),
          agent: str(g(r, 5)),
          category: str(g(r, 6)),
          detail: str(g(r, 7)),
          contact: str(g(r, 8)),
          sector: str(g(r, 9)),
          ref: str(g(r, 10)),
          pnr: str(g(r, 11)),
          netCost: num(g(r, 12)),
          sellingPrice: num(g(r, 13)),
          paymentStatus: str(g(r, 16)),
          paymentMode: str(g(r, 17)),
          amountPaid: num(g(r, 18)),
          serviceDate: toISO(g(r, 20)),
          bookingStatus: str(g(r, 21)),
          entryDate: toISO(g(r, 22)),
          docType: str(g(r, 25)),
          pax: num(g(r, 26)),
        });
      }
      const cDate = toISO(g(r, 29));
      const cAmt = num(g(r, 32));
      if (cDate || cAmt !== null) {
        cash.push({
          id: randomUUID(),
          date: cDate || firstDay,
          agent: str(g(r, 30)),
          mode: str(g(r, 31)),
          amount: cAmt || 0,
          remarks: str(g(r, 33)),
        });
      }
    }
  }
  return { bookings, cash, agents: [...agents], docTypes };
}

// ---------- Export ----------
const BOOKING_COLS = [
  ['S.NO', 6], ['T. DATE', 12], ['CUSTOMER NAME', 30], ['CUSTOMER TYPE', 16], ['AGENCY / SUB-AGENT NAME', 24],
  ['PRODUCT CATEGORY', 20], ['PRODUCT / SERVICE DETAIL', 28], ['CONTACT', 14], ['SECTOR / DESTINATION', 14],
  ['FLIGHTS / REF', 14], ['PNR', 10], ['NET COST', 11], ['SELLING PRICE', 12], ['PROFIT', 10], ['PROFIT %', 9],
  ['PAYMENT STATUS', 12], ['PAYMENT MODE', 14], ['AMOUNT PAID', 12], ['BALANCE DUE', 12], ['SERVICE / TRVL DATE', 13],
  ['BOOKING STATUS', 13], ['ENTRY DATE', 12], ['EXPIRE DATE', 12], ['DAYS TO EXPIRY', 10], ['VISA / DOC TYPE', 16], ['NO. OF PAX', 8],
];
const CASH_COLS = [['S.NO', 6], ['CASH PAID DATE', 13], ['AGENT / CUSTOMER', 24], ['PAYMENT MODE', 14], ['CASH PAID AMOUNT', 14], ['REMARKS / RECEIPT REF', 30]];

function isoToDate(s) {
  if (!s) return null;
  const [y, m, d] = s.split('-').map(Number);
  return new Date(Date.UTC(y, m - 1, d));
}

function styleHeader(row, color) {
  row.eachCell(c => {
    c.font = { bold: true, color: { argb: 'FFFFFFFF' } };
    c.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: color } };
    c.alignment = { vertical: 'middle', horizontal: 'center', wrapText: true };
  });
  row.height = 30;
}

async function exportWorkbook(filePath, data) {
  const { bookings, cash, lists } = data;
  const days = {};
  (lists.docTypes || []).forEach(d => { if (d.days) days[d.name] = d.days; });
  const wb = new ExcelJS.Workbook();
  wb.creator = 'Sales & Cash Tracker';

  // Group by month key YYYY-MM
  const keys = new Set();
  bookings.forEach(b => keys.add(b.date.slice(0, 7)));
  cash.forEach(c => keys.add(c.date.slice(0, 7)));
  const sorted = [...keys].filter(Boolean).sort();

  // Summary sheet
  const sum = wb.addWorksheet('Dashboard');
  sum.columns = [{ width: 14 }, { width: 14 }, { width: 14 }, { width: 12 }, { width: 10 }, { width: 10 }, { width: 14 }, { width: 14 }, { width: 14 }];
  sum.addRow(['MONTH', 'SALES (SELLING)', 'NET COST', 'PROFIT', 'MARGIN %', 'BOOKINGS', 'PAID IN ROWS', 'ROW BALANCE DUE', 'CASH PAID (LOG)']);
  styleHeader(sum.getRow(1), 'FF1F3A5F');

  for (const key of sorted) {
    const [y, m] = key.split('-').map(Number);
    const name = `${MONTHS[m - 1]} ${y}`;
    const ws = wb.addWorksheet(name);
    ws.columns = BOOKING_COLS.map(([, w]) => ({ width: w }))
      .concat([{ width: 3 }], CASH_COLS.map(([, w]) => ({ width: w })));
    ws.addRow(BOOKING_COLS.map(c => c[0]).concat([''], CASH_COLS.map(c => c[0])));
    styleHeader(ws.getRow(1), 'FF1F3A5F');
    for (let i = 28; i <= 33; i++) ws.getRow(1).getCell(i).fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FF2E7D32' } };
    ws.getRow(1).getCell(27).fill = undefined;
    ws.views = [{ state: 'frozen', ySplit: 1 }];

    const mb = bookings.filter(b => b.date.startsWith(key)).sort((a, b) => a.date.localeCompare(b.date));
    const mc = cash.filter(c => c.date.startsWith(key)).sort((a, b) => a.date.localeCompare(b.date));
    const n = Math.max(mb.length, mc.length);
    let tSell = 0, tCost = 0, tPaid = 0, tBal = 0, tCash = 0;
    for (let i = 0; i < n; i++) {
      const b = mb[i];
      const c = mc[i];
      const row = new Array(33).fill(null);
      if (b) {
        const sell = b.sellingPrice, cost = b.netCost;
        const profit = sell === null ? null : sell - (cost || 0);
        const bal = sell === null ? null : sell - (b.amountPaid || 0);
        const exp = b.entryDate && days[b.docType] ? new Date(isoToDate(b.entryDate).getTime() + days[b.docType] * 86400000) : null;
        tSell += sell || 0; tCost += cost || 0; tPaid += b.amountPaid || 0; tBal += bal || 0;
        Object.assign(row, [i + 1, isoToDate(b.date), b.customer, b.customerType, b.agent, b.category, b.detail, b.contact,
          b.sector, b.ref, b.pnr, cost, sell, profit, sell ? profit / sell : null, b.paymentStatus, b.paymentMode,
          b.amountPaid, bal, isoToDate(b.serviceDate), b.bookingStatus, isoToDate(b.entryDate), exp, null, b.docType, b.pax]);
      }
      if (c) {
        tCash += c.amount || 0;
        row[27] = i + 1; row[28] = isoToDate(c.date); row[29] = c.agent; row[30] = c.mode; row[31] = c.amount; row[32] = c.remarks;
      }
      const r = ws.addRow(row);
      if (b && row[22]) r.getCell(24).value = { formula: `IF(W${r.number}="","",W${r.number}-TODAY())` };
    }
    [2, 20, 22, 23, 29].forEach(c => { ws.getColumn(c).numFmt = 'dd-mmm-yy'; });
    [12, 13, 14, 18, 19, 32].forEach(c => { ws.getColumn(c).numFmt = '#,##0.00'; });
    ws.getColumn(15).numFmt = '0.0%';

    sum.addRow([name, tSell, tCost, tSell - tCost, tSell ? (tSell - tCost) / tSell : 0, mb.length, tPaid, tBal, tCash]);
  }
  const last = sum.rowCount;
  if (last > 1) {
    const t = sum.addRow(['TOTAL',
      { formula: `SUM(B2:B${last})` }, { formula: `SUM(C2:C${last})` }, { formula: `SUM(D2:D${last})` },
      { formula: `IF(B${last + 1}=0,0,D${last + 1}/B${last + 1})` }, { formula: `SUM(F2:F${last})` },
      { formula: `SUM(G2:G${last})` }, { formula: `SUM(H2:H${last})` }, { formula: `SUM(I2:I${last})` }]);
    t.font = { bold: true };
  }
  [2, 3, 4, 7, 8, 9].forEach(c => { sum.getColumn(c).numFmt = '#,##0.00'; });
  sum.getColumn(5).numFmt = '0.0%';

  await wb.xlsx.writeFile(filePath);
}

module.exports = { importWorkbook, exportWorkbook, toISO };
