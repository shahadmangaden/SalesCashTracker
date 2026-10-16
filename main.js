const { app, BrowserWindow, ipcMain, dialog, shell, Menu } = require('electron');
const path = require('path');
const fs = require('fs');
const { importWorkbook, exportWorkbook } = require('./lib/excel');

const DATA_DIR = app.getPath('userData');
const DATA_FILE = path.join(DATA_DIR, 'tracker-data.json');
const BACKUP_DIR = path.join(DATA_DIR, 'backups');
const KEEP_BACKUPS = 60;

let win;

function ensureDirs() {
  fs.mkdirSync(DATA_DIR, { recursive: true });
  fs.mkdirSync(BACKUP_DIR, { recursive: true });
}

function readData() {
  try {
    return JSON.parse(fs.readFileSync(DATA_FILE, 'utf8'));
  } catch (e) {
    if (e.code !== 'ENOENT') {
      // Corrupt file: keep a copy so nothing is lost
      try { fs.copyFileSync(DATA_FILE, DATA_FILE + '.corrupt-' + Date.now()); } catch (_) { }
    }
    return null;
  }
}

function writeData(data) {
  ensureDirs();
  const tmp = DATA_FILE + '.tmp';
  fs.writeFileSync(tmp, JSON.stringify(data, null, 1), 'utf8');
  fs.renameSync(tmp, DATA_FILE);
  dailyBackup();
  return true;
}

function dailyBackup() {
  try {
    const stamp = new Date().toISOString().slice(0, 10);
    const target = path.join(BACKUP_DIR, `tracker-${stamp}.json`);
    fs.copyFileSync(DATA_FILE, target); // overwrite today's backup with latest
    const files = fs.readdirSync(BACKUP_DIR).filter(f => f.startsWith('tracker-') && f.endsWith('.json')).sort();
    while (files.length > KEEP_BACKUPS) fs.unlinkSync(path.join(BACKUP_DIR, files.shift()));
  } catch (_) { /* backups are best-effort */ }
}

function createWindow() {
  win = new BrowserWindow({
    width: 1400,
    height: 880,
    minWidth: 1000,
    minHeight: 640,
    title: 'Sales & Cash Tracker',
    icon: path.join(__dirname, '..', 'build', 'icon.png'),
    backgroundColor: '#f4f6fa',
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true,
    },
  });
  win.maximize();
  win.loadFile(path.join(__dirname, '..', 'src', 'index.html'));
  win.webContents.setWindowOpenHandler(({ url }) => { shell.openExternal(url); return { action: 'deny' }; });
}

Menu.setApplicationMenu(Menu.buildFromTemplate([
  { label: 'File', submenu: [{ role: 'reload', label: 'Reload' }, { type: 'separator' }, { role: 'quit', label: 'Exit' }] },
  { label: 'Edit', submenu: [{ role: 'undo' }, { role: 'redo' }, { type: 'separator' }, { role: 'cut' }, { role: 'copy' }, { role: 'paste' }, { role: 'selectAll' }] },
  { label: 'View', submenu: [{ role: 'zoomIn' }, { role: 'zoomOut' }, { role: 'resetZoom' }, { type: 'separator' }, { role: 'togglefullscreen' }, { role: 'toggleDevTools' }] },
]));

ipcMain.handle('data:load', () => readData());
ipcMain.handle('data:save', (_e, data) => writeData(data));
ipcMain.handle('data:info', () => ({ dataFile: DATA_FILE, backupDir: BACKUP_DIR, version: app.getVersion() }));
ipcMain.handle('data:openFolder', () => shell.openPath(DATA_DIR));

ipcMain.handle('excel:import', async () => {
  const r = await dialog.showOpenDialog(win, {
    title: 'Import Excel tracker',
    filters: [{ name: 'Excel', extensions: ['xlsx', 'xlsm', 'xls'] }],
    properties: ['openFile'],
  });
  if (r.canceled || !r.filePaths[0]) return null;
  return importWorkbook(r.filePaths[0]);
});

ipcMain.handle('excel:export', async (_e, data, suggestedName) => {
  const r = await dialog.showSaveDialog(win, {
    title: 'Export to Excel',
    defaultPath: path.join(app.getPath('documents'), suggestedName || 'Sales-Cash-Tracker.xlsx'),
    filters: [{ name: 'Excel', extensions: ['xlsx'] }],
  });
  if (r.canceled || !r.filePath) return null;
  await exportWorkbook(r.filePath, data);
  shell.showItemInFolder(r.filePath);
  return r.filePath;
});

ipcMain.handle('backup:save', async (_e, data) => {
  const r = await dialog.showSaveDialog(win, {
    title: 'Save backup',
    defaultPath: path.join(app.getPath('documents'), `tracker-backup-${new Date().toISOString().slice(0, 10)}.json`),
    filters: [{ name: 'Backup', extensions: ['json'] }],
  });
  if (r.canceled || !r.filePath) return null;
  fs.writeFileSync(r.filePath, JSON.stringify(data, null, 1), 'utf8');
  return r.filePath;
});

ipcMain.handle('backup:restore', async () => {
  const r = await dialog.showOpenDialog(win, {
    title: 'Restore backup',
    defaultPath: BACKUP_DIR,
    filters: [{ name: 'Backup', extensions: ['json'] }],
    properties: ['openFile'],
  });
  if (r.canceled || !r.filePaths[0]) return null;
  const d = JSON.parse(fs.readFileSync(r.filePaths[0], 'utf8'));
  if (!d || !Array.isArray(d.bookings)) throw new Error('This file is not a tracker backup.');
  return d;
});

ipcMain.handle('app:confirm', async (_e, message, detail) => {
  const r = await dialog.showMessageBox(win, { type: 'question', buttons: ['Yes', 'Cancel'], defaultId: 1, cancelId: 1, message, detail });
  return r.response === 0;
});

const gotLock = app.requestSingleInstanceLock();
if (!gotLock) app.quit();
else {
  app.on('second-instance', () => { if (win) { if (win.isMinimized()) win.restore(); win.focus(); } });
  app.whenReady().then(() => { ensureDirs(); createWindow(); });
  app.on('window-all-closed', () => app.quit());
}
