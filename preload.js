// ==============================================================================
// Electron Preload Script - EscolaWeb Desktop
// Expõe APIs seguras para a janela do navegador via contextBridge (sem expor Node.js)
// ==============================================================================

const { contextBridge, ipcRenderer } = require('electron');

// Determina a URL do backend através de variáveis de ambiente ou IPC síncrono com o processo principal
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

// Expõe métodos seguros no objeto global window.escolawebDesktop para o frontend
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
  // Chamada de API intermediada pelo IPC do Electron para evitar problemas de CORS em desenvolvimento
  fetchBackend: (urlPath, options) => ipcRenderer.invoke('escolaweb:fetch', urlPath, options)
});
