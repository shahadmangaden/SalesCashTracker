const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('api', {
  load: () => ipcRenderer.invoke('data:load'),
  save: data => ipcRenderer.invoke('data:save', data),
  info: () => ipcRenderer.invoke('data:info'),
  openDataFolder: () => ipcRenderer.invoke('data:openFolder'),
  importExcel: () => ipcRenderer.invoke('excel:import'),
  exportExcel: (data, name) => ipcRenderer.invoke('excel:export', data, name),
  saveBackup: data => ipcRenderer.invoke('backup:save', data),
  restoreBackup: () => ipcRenderer.invoke('backup:restore'),
  confirm: (message, detail) => ipcRenderer.invoke('app:confirm', message, detail),
  titleBar: (bg, fg) => ipcRenderer.send('win:theme', { bg, fg }),
  desktop: true,
});
