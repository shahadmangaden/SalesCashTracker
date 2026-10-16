// Node (desktop) wrapper around the shared Excel core.
const fs = require('fs');
const { importWorkbookData, buildWorkbook, toISO } = require('./excel-core');

function importWorkbook(filePath) {
  return importWorkbookData(fs.readFileSync(filePath), 'buffer');
}

async function exportWorkbook(filePath, data) {
  const buf = await buildWorkbook(data);
  fs.writeFileSync(filePath, Buffer.from(buf));
}

module.exports = { importWorkbook, exportWorkbook, toISO };
