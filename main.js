const { app, BrowserWindow, protocol, session, ipcMain } = require('electron');
const path = require('path');
const fs = require('fs');
const { fileURLToPath } = require('url');
const { buildFrontend, DIST_DIR } = require('./scripts/build-frontend');

const BACKEND_URL = (
  process.env.BACKEND_URL ||
  process.env.APP_URL ||
  `http://localhost:${process.env.SERVER_PORT || 8080}`
).replace(/\/+$/, '');

const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.mjs': 'application/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.gif': 'image/gif',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
  '.ttf': 'font/ttf',
  '.eot': 'application/vnd.ms-fontobject'
};

function getMimeType(filePath) {
  const ext = path.extname(filePath).toLowerCase();
  return MIME_TYPES[ext] || 'application/octet-stream';
}

function ensureProductionBuild() {
  if (app.isPackaged) {
    return;
  }
  const entryHtml = path.join(DIST_DIR, 'index.html');
  if (!fs.existsSync(entryHtml)) {
    try {
      buildFrontend();
    } catch (err) {
      console.error('Failed to generate frontend production build:', err);
    }
  }
}

function resolveDistFilePath(rawFilePath) {
  if (fs.existsSync(rawFilePath) && fs.statSync(rawFilePath).isFile()) {
    return rawFilePath;
  }

  const parsedRoot = path.parse(rawFilePath).root;
  const relFromRoot = path.relative(parsedRoot, rawFilePath);

  if (relFromRoot && !relFromRoot.startsWith('..')) {
    const candidateInDist = path.join(DIST_DIR, relFromRoot);
    if (fs.existsSync(candidateInDist) && fs.statSync(candidateInDist).isFile()) {
      return candidateInDist;
    }

    const candidateInStatic = path.join(__dirname, 'src', 'main', 'resources', 'static', relFromRoot);
    if (fs.existsSync(candidateInStatic) && fs.statSync(candidateInStatic).isFile()) {
      return candidateInStatic;
    }

    const candidateInTemplates = path.join(__dirname, 'src', 'main', 'resources', 'templates', relFromRoot);
    if (fs.existsSync(candidateInTemplates) && fs.statSync(candidateInTemplates).isFile()) {
      return candidateInTemplates;
    }
  }

  return null;
}

function setupFileProtocolHandler() {
  protocol.handle('file', async (request) => {
    try {
      const parsedUrl = new URL(request.url);
      const rawPath = fileURLToPath(parsedUrl);
      const resolvedPath = resolveDistFilePath(rawPath);

      if (!resolvedPath) {
        return new Response('Not Found', {
          status: 404,
          headers: { 'Content-Type': 'text/plain; charset=utf-8' }
        });
      }

      const data = await fs.promises.readFile(resolvedPath);
      return new Response(data, {
        status: 200,
        headers: {
          'Content-Type': getMimeType(resolvedPath),
          'Access-Control-Allow-Origin': '*'
        }
      });
    } catch (err) {
      return new Response(String(err), {
        status: 500,
        headers: { 'Content-Type': 'text/plain; charset=utf-8' }
      });
    }
  });
}

function setupBackendInterceptors() {
  ipcMain.handle('escolaweb:fetch', async (_event, urlPath, options = {}) => {
    const targetUrl = urlPath.startsWith('http')
      ? urlPath
      : `${BACKEND_URL}${urlPath.startsWith('/') ? '' : '/'}${urlPath}`;

    const headers = {
      ...(options.headers || {}),
      Origin: BACKEND_URL,
      Referer: `${BACKEND_URL}/`
    };

    const response = await fetch(targetUrl, {
      method: options.method || 'GET',
      headers,
      body: options.body !== undefined ? options.body : undefined
    });

    const arrayBuffer = await response.arrayBuffer();
    const bodyBuffer = new Uint8Array(arrayBuffer);
    const bodyText = Buffer.from(arrayBuffer).toString('utf8');
    const responseHeaders = {};
    response.headers.forEach((value, key) => {
      responseHeaders[key] = value;
    });

    return {
      status: response.status,
      statusText: response.statusText,
      headers: responseHeaders,
      bodyBuffer,
      bodyText
    };
  });

  const wsUrl = BACKEND_URL.replace(/^http/, 'ws');
  const filter = {
    urls: [`${BACKEND_URL}/*`, `${wsUrl}/*`, 'http://localhost:*/*', 'ws://localhost:*/*']
  };

  session.defaultSession.webRequest.onBeforeSendHeaders(filter, (details, callback) => {
    const requestHeaders = { ...details.requestHeaders };
    requestHeaders['Origin'] = BACKEND_URL;
    requestHeaders['Referer'] = `${BACKEND_URL}/`;
    callback({ requestHeaders });
  });

  session.defaultSession.webRequest.onHeadersReceived(filter, (details, callback) => {
    const responseHeaders = { ...details.responseHeaders };
    responseHeaders['Access-Control-Allow-Origin'] = ['*'];
    responseHeaders['Access-Control-Allow-Methods'] = ['GET, POST, PUT, PATCH, DELETE, OPTIONS'];
    responseHeaders['Access-Control-Allow-Headers'] = ['*'];
    callback({ responseHeaders });
  });
}

function createWindow() {
  ensureProductionBuild();

  const iconCandidates = [
    path.join(DIST_DIR, 'favicon.ico'),
    path.join(__dirname, 'src', 'main', 'resources', 'static', 'favicon.ico')
  ];
  const iconPath = iconCandidates.find((p) => fs.existsSync(p));

  const mainWindow = new BrowserWindow({
    width: 1280,
    height: 800,
    minWidth: 800,
    minHeight: 600,
    title: 'EscolaWEB SGE',
    icon: iconPath,
    autoHideMenuBar: true,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false
    }
  });

  mainWindow.webContents.on('will-navigate', (event, navigationUrl) => {
    try {
      const parsed = new URL(navigationUrl);
      if (parsed.protocol === 'file:') {
        const rawTarget = fileURLToPath(parsed);
        const normalizedDist = path.resolve(DIST_DIR).toLowerCase();
        const normalizedTarget = path.resolve(rawTarget).toLowerCase();

        if (!normalizedTarget.startsWith(normalizedDist)) {
          const resolved = resolveDistFilePath(rawTarget);
          if (resolved) {
            event.preventDefault();
            mainWindow.loadFile(resolved, {
              search: parsed.search ? parsed.search.slice(1) : undefined,
              hash: parsed.hash ? parsed.hash.slice(1) : undefined
            });
          }
        }
      }
    } catch (_err) {
      // Ignore malformed navigation URLs
    }
  });

  const prodEntryFile = path.join(__dirname, 'dist', 'index.html');
  mainWindow.loadFile(prodEntryFile);

  return mainWindow;
}

app.whenReady().then(() => {
  setupFileProtocolHandler();
  setupBackendInterceptors();
  createWindow();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow();
    }
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});

module.exports = { createWindow, ensureProductionBuild };
