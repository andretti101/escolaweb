process.env['ELECTRON_DISABLE_SECURITY_WARNINGS'] = 'true';
const { app, BrowserWindow, protocol, session, ipcMain } = require('electron');
const path = require('path');
const fs = require('fs');
const { fileURLToPath } = require('url');
const { buildFrontend, DIST_DIR } = require('./scripts/build-frontend');
const { ROUTE_MAP } = require('./scripts/routes-map');

const configuredBackendUrl = (
  process.env.BACKEND_URL ||
  process.env.APP_URL ||
  (process.env.SERVER_PORT
    ? (process.env.SERVER_PORT === '80' ? 'http://localhost' : `http://localhost:${process.env.SERVER_PORT}`)
    : null)
);

let activeBackendUrl = configuredBackendUrl
  ? configuredBackendUrl.replace(/\/+$/, '')
  : null;

function getCandidateBackendUrls() {
  if (configuredBackendUrl) {
    return [configuredBackendUrl.replace(/\/+$/, '')];
  }
  const defaultCandidates = ['http://escolaweb', 'http://localhost', 'http://localhost:8080', 'http://127.0.0.1'];
  if (activeBackendUrl && defaultCandidates.includes(activeBackendUrl)) {
    return [activeBackendUrl, ...defaultCandidates.filter((u) => u !== activeBackendUrl)];
  }
  return defaultCandidates;
}

async function detectActiveBackendPort() {
  if (configuredBackendUrl) {
    activeBackendUrl = configuredBackendUrl.replace(/\/+$/, '');
    return;
  }
  const candidates = ['http://escolaweb', 'http://localhost', 'http://localhost:8080', 'http://127.0.0.1'];
  for (const candidate of candidates) {
    try {
      const controller = new AbortController();
      const timeout = setTimeout(() => controller.abort(), 800);
      const res = await fetch(`${candidate}/login.html`, {
        method: 'HEAD',
        signal: controller.signal
      });
      clearTimeout(timeout);
      if (res.status === 200 || res.status === 302 || res.status === 401) {
        activeBackendUrl = candidate;
        return;
      }
    } catch (_e) {
      // Continue to next candidate
    }
  }
  activeBackendUrl = 'http://localhost';
}

function getRawPathFromUrl(parsedUrl) {
  try {
    return fileURLToPath(parsedUrl);
  } catch (_err) {
    return path.normalize(decodeURIComponent(parsedUrl.pathname));
  }
}

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
    const normalized = '/' + relFromRoot.replace(/\\/g, '/').replace(/\/+$/, '');
    if (ROUTE_MAP[normalized]) {
      const mappedRel = ROUTE_MAP[normalized].replace(/^\//, '');
      const candidateMapped = path.join(DIST_DIR, mappedRel);
      if (fs.existsSync(candidateMapped) && fs.statSync(candidateMapped).isFile()) {
        return candidateMapped;
      }
    }

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
      const rawPath = getRawPathFromUrl(parsedUrl);
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
  ipcMain.on('escolaweb:get-backend-url-sync', (event) => {
    event.returnValue = activeBackendUrl || getCandidateBackendUrls()[0];
  });

  ipcMain.handle('escolaweb:fetch', async (_event, urlPath, options = {}) => {
    const candidates = getCandidateBackendUrls();
    let response = null;
    let lastError = null;

    const method = (options.method || 'GET').toUpperCase();
    const hasBody = method !== 'GET' && method !== 'HEAD' && options.body !== undefined && options.body !== null;

    for (let i = 0; i < candidates.length; i++) {
      const baseUrl = candidates[i];
      let targetUrl = urlPath;
      if (urlPath.startsWith('http')) {
        try {
          const parsed = new URL(urlPath);
          if (parsed.hostname === 'localhost' || parsed.hostname === '127.0.0.1') {
            targetUrl = `${baseUrl}${parsed.pathname}${parsed.search}${parsed.hash}`;
          }
        } catch (_e) {}
      } else {
        targetUrl = `${baseUrl}${urlPath.startsWith('/') ? '' : '/'}${urlPath}`;
      }

      const headers = {
        ...(options.headers || {}),
        Origin: baseUrl,
        Referer: `${baseUrl}/`
      };

      try {
        response = await fetch(targetUrl, {
          method,
          headers,
          body: hasBody ? options.body : undefined
        });
        activeBackendUrl = baseUrl;
        lastError = null;
        break;
      } catch (err) {
        lastError = err;
        if (i < candidates.length - 1 && (!urlPath.startsWith('http') || urlPath.includes('localhost') || urlPath.includes('127.0.0.1'))) {
          continue;
        }
        throw err;
      }
    }

    if (!response && lastError) {
      throw lastError;
    }

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

  const filter = {
    urls: [
      'http://escolaweb/*',
      'ws://escolaweb/*',
      'http://escolaweb:*/*',
      'ws://escolaweb:*/*',
      'http://localhost:*/*',
      'ws://localhost:*/*',
      'http://localhost/*',
      'ws://localhost/*',
      'http://127.0.0.1:*/*',
      'ws://127.0.0.1:*/*',
      'http://127.0.0.1/*',
      'ws://127.0.0.1/*'
    ]
  };

  session.defaultSession.webRequest.onBeforeSendHeaders(filter, (details, callback) => {
    const requestHeaders = { ...details.requestHeaders };
    let origin = activeBackendUrl || 'http://localhost';
    try {
      const parsed = new URL(details.url);
      origin = `${parsed.protocol.replace('ws', 'http')}//${parsed.host}`;
    } catch (_e) {
      // fallback
    }
    requestHeaders['Origin'] = origin;
    requestHeaders['Referer'] = `${origin}/`;
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
        const rawTarget = getRawPathFromUrl(parsed);
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

app.whenReady().then(async () => {
  await detectActiveBackendPort();
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
