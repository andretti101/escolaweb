const { contextBridge, ipcRenderer } = require('electron');

let backendUrl = process.env.BACKEND_URL || process.env.APP_URL;
if (!backendUrl && process.env.SERVER_PORT) {
  backendUrl = process.env.SERVER_PORT === '80' ? 'http://localhost' : `http://localhost:${process.env.SERVER_PORT}`;
}
if (!backendUrl) {
  try {
    backendUrl = ipcRenderer.sendSync('escolaweb:get-backend-url-sync');
  } catch (_e) {
    backendUrl = 'http://localhost';
  }
}

contextBridge.exposeInMainWorld('escolawebDesktop', {
  isElectron: true,
  platform: process.platform,
  backendUrl: backendUrl || 'http://localhost',
  getBackendUrl: () => {
    try {
      return ipcRenderer.sendSync('escolaweb:get-backend-url-sync');
    } catch (_e) {
      return backendUrl || 'http://localhost';
    }
  },
  fetchBackend: (urlPath, options) => ipcRenderer.invoke('escolaweb:fetch', urlPath, options)
});
