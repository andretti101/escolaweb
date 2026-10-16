const { contextBridge, ipcRenderer } = require('electron');

const backendUrl =
  process.env.BACKEND_URL ||
  process.env.APP_URL ||
  `http://localhost:${process.env.SERVER_PORT || 8080}`;

contextBridge.exposeInMainWorld('escolawebDesktop', {
  isElectron: true,
  platform: process.platform,
  backendUrl,
  fetchBackend: (urlPath, options) => ipcRenderer.invoke('escolaweb:fetch', urlPath, options)
});
