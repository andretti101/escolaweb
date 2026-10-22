// ==============================================================================
// Script de Build do Frontend para o Electron (Desktop)
// Transforma os templates Thymeleaf e assets estáticos em um pacote estático (dist)
// compatível com execução local off-line e protocolo file://
// ==============================================================================

const fs = require('fs');
const path = require('path');
const { ROUTE_MAP } = require('./routes-map');

const ROOT_DIR = path.resolve(__dirname, '..');
const DIST_DIR = path.join(ROOT_DIR, 'dist');

// Locais candidatos para os assets estáticos (código-fonte ou compilação Maven)
const STATIC_CANDIDATES = [
  path.join(ROOT_DIR, 'src', 'main', 'resources', 'static'),
  path.join(ROOT_DIR, 'target', 'classes', 'static')
];

// Locais candidatos para os templates HTML
const TEMPLATES_CANDIDATES = [
  path.join(ROOT_DIR, 'src', 'main', 'resources', 'templates'),
  path.join(ROOT_DIR, 'target', 'classes', 'templates')
];

// Identifica o diretório existente dentre os caminhos candidatos
function findExistingDir(candidates) {
  for (const candidate of candidates) {
    if (fs.existsSync(candidate)) {
      return candidate;
    }
  }
  return null;
}

// Copia recursivamente pastas e arquivos para o diretório de distribuição
function copyDirectoryRecursive(srcDir, destDir) {
  if (!fs.existsSync(srcDir)) return;
  fs.mkdirSync(destDir, { recursive: true });

  const entries = fs.readdirSync(srcDir, { withFileTypes: true });
  for (const entry of entries) {
    const srcPath = path.join(srcDir, entry.name);
    const destPath = path.join(destDir, entry.name);

    if (entry.isDirectory()) {
      copyDirectoryRecursive(srcPath, destPath);
    } else if (entry.isFile()) {
      fs.copyFileSync(srcPath, destPath);
    }
  }
}

// Gera o script injetado nas páginas para interceptar fetch, rotas e redirecionamentos
function createElectronBridgeScript(distDir) {
  const jsDir = path.join(distDir, 'js');
  fs.mkdirSync(jsDir, { recursive: true });

  const bridgeCode = `(function () {
  if (window.__ESCOLAWEB_ELECTRON_BRIDGE__) return;
  window.__ESCOLAWEB_ELECTRON_BRIDGE__ = true;

  // URL padrão do backend Spring Boot caso não informada
  var DEFAULT_BACKEND = 'http://localhost';
  function getBackendUrl() {
    if (window.escolawebDesktop && typeof window.escolawebDesktop.getBackendUrl === 'function') {
      try {
        var detected = window.escolawebDesktop.getBackendUrl();
        if (detected) return detected.replace(/\\/+$/, '');
      } catch (_e) {}
    }
    return (
      window.__ESCOLAWEB_BACKEND_URL__ ||
      (window.escolawebDesktop && window.escolawebDesktop.backendUrl) ||
      localStorage.getItem('escolaweb_backend_url') ||
      DEFAULT_BACKEND
    ).replace(/\\/+$/, '');
  }

  var backendUrl = getBackendUrl();
  window.__ESCOLAWEB_BACKEND_URL__ = backendUrl;

  var bridgeScriptSrc = (
    document.currentScript &&
    document.currentScript.src &&
    document.currentScript.src.indexOf('file:') === 0
  )
    ? document.currentScript.src.split('?')[0].split('#')[0].replace(/\\/js\\/electron-bridge\\.js$/i, '')
    : '';

  function getDistRootUrl() {
    if (bridgeScriptSrc) {
      return bridgeScriptSrc;
    }
    var href = window.location.href.split('?')[0].split('#')[0];
    var markers = ['/principal/create/', '/principal/edit/', '/principal/', '/secretary/create/', '/secretary/edit/', '/secretary/', '/teacher/create/', '/teacher/edit/', '/teacher/', '/student/', '/components/'];
    for (var i = 0; i < markers.length; i++) {
      var idx = href.lastIndexOf(markers[i]);
      if (idx !== -1) {
        return href.substring(0, idx);
      }
    }
    var lastSlash = href.lastIndexOf('/');
    return lastSlash !== -1 ? href.substring(0, lastSlash) : href;
  }

  if (window.location.protocol === 'file:') {
    window.__ESCOLAWEB_DIST_ROOT__ = getDistRootUrl();
  }

  function isBackendPath(urlStr) {
    if (typeof urlStr !== 'string') return false;
    var testPath = urlStr;
    try {
      if (urlStr.indexOf('://') !== -1) {
        var parsed = new URL(urlStr);
        testPath = parsed.pathname;
      }
    } catch (_e) {}
    return (
      testPath.indexOf('/auth/') === 0 ||
      testPath === '/auth' ||
      testPath.indexOf('/api/') === 0 ||
      testPath === '/api' ||
      testPath.indexOf('/ws-chat') === 0 ||
      testPath.indexOf('/v3/api-docs') === 0 ||
      testPath.indexOf('/swagger-ui') === 0
    );
  }

  var ROUTE_MAP = ${JSON.stringify(ROUTE_MAP)};

  function mapRoute(urlStr) {
    if (!urlStr) return urlStr;
    var clean = urlStr.split('?')[0].split('#')[0].replace(/\\/+$/, '');
    var suffix = (urlStr.indexOf('?') !== -1 ? urlStr.substring(urlStr.indexOf('?')) : '') ||
                 (urlStr.indexOf('#') !== -1 ? urlStr.substring(urlStr.indexOf('#')) : '');
    if (ROUTE_MAP[clean]) {
      return ROUTE_MAP[clean] + suffix;
    }
    return urlStr;
  }

  function resolveRequestUrl(urlStr) {
    if (typeof urlStr !== 'string') return urlStr;
    if (isBackendPath(urlStr)) {
      return getBackendUrl() + urlStr;
    }
    if (window.location.protocol === 'file:' && urlStr.charAt(0) === '/' && urlStr.charAt(1) !== '/') {
      return getDistRootUrl() + mapRoute(urlStr);
    }
    return urlStr;
  }

  // 1. Wrap window.fetch
  if (typeof window.fetch === 'function') {
    var originalFetch = window.fetch.bind(window);
    window.fetch = function (input, init) {
      var rawUrl = typeof input === 'string'
        ? input
        : (typeof URL !== 'undefined' && input instanceof URL)
          ? input.toString()
          : (typeof Request !== 'undefined' && input instanceof Request)
            ? input.url
            : '';

      if (isBackendPath(rawUrl) && window.escolawebDesktop && typeof window.escolawebDesktop.fetchBackend === 'function') {
        var reqInit = init || {};
        var headersObj = {};
        if (reqInit.headers) {
          if (typeof Headers !== 'undefined' && reqInit.headers instanceof Headers) {
            reqInit.headers.forEach(function (v, k) { headersObj[k] = v; });
          } else if (Array.isArray(reqInit.headers)) {
            reqInit.headers.forEach(function (pair) { headersObj[pair[0]] = pair[1]; });
          } else {
            Object.keys(reqInit.headers).forEach(function (k) { headersObj[k] = reqInit.headers[k]; });
          }
        }
        var hasAuthHeader = false;
        Object.keys(headersObj).forEach(function (k) {
          if (k.toLowerCase() === 'authorization') hasAuthHeader = true;
        });
        if (!hasAuthHeader) {
          var token = localStorage.getItem('jwt_token');
          if (token) {
            headersObj['Authorization'] = 'Bearer ' + token;
          }
        }
        return window.escolawebDesktop.fetchBackend(rawUrl, {
          method: reqInit.method || 'GET',
          headers: headersObj,
          body: reqInit.body !== undefined ? String(reqInit.body) : undefined
        }).then(function (res) {
          var nullBodyStatuses = [101, 204, 205, 304];
          var payload = res.bodyBuffer !== undefined ? res.bodyBuffer : res.bodyText;
          var body = nullBodyStatuses.indexOf(res.status) !== -1 ? null : payload;
          return new Response(body, {
            status: res.status,
            statusText: res.statusText,
            headers: res.headers || {}
          });
        });
      }

      if (typeof input === 'string') {
        input = resolveRequestUrl(input);
      } else if (typeof URL !== 'undefined' && input instanceof URL) {
        input = resolveRequestUrl(input.toString());
      } else if (typeof Request !== 'undefined' && input instanceof Request) {
        var resolved = resolveRequestUrl(input.url);
        if (resolved !== input.url) {
          input = new Request(resolved, input);
        }
      }
      return originalFetch(input, init);
    };
  }

  // 2. Wrap XMLHttpRequest.prototype.open
  if (typeof XMLHttpRequest !== 'undefined' && XMLHttpRequest.prototype) {
    var originalXhrOpen = XMLHttpRequest.prototype.open;
    XMLHttpRequest.prototype.open = function (method, url) {
      var args = Array.prototype.slice.call(arguments);
      if (typeof url === 'string') {
        args[1] = resolveRequestUrl(url);
      }
      return originalXhrOpen.apply(this, args);
    };
  }

  // 3. Wrap SockJS when available
  function wrapSockJS(SockJSConstructor) {
    if (!SockJSConstructor || SockJSConstructor.__escolawebWrapped) return SockJSConstructor;
    var WrappedSockJS = function (url, _reserved, options) {
      var resolvedUrl = isBackendPath(url) ? getBackendUrl() + url : resolveRequestUrl(url);
      return new SockJSConstructor(resolvedUrl, _reserved, options);
    };
    WrappedSockJS.prototype = SockJSConstructor.prototype;
    Object.keys(SockJSConstructor).forEach(function (key) {
      WrappedSockJS[key] = SockJSConstructor[key];
    });
    WrappedSockJS.__escolawebWrapped = true;
    return WrappedSockJS;
  }

  var _sockJS = window.SockJS ? wrapSockJS(window.SockJS) : undefined;
  try {
    Object.defineProperty(window, 'SockJS', {
      configurable: true,
      enumerable: true,
      get: function () {
        return _sockJS;
      },
      set: function (val) {
        _sockJS = wrapSockJS(val);
      }
    });
  } catch (_e) {
    if (window.SockJS) {
      window.SockJS = wrapSockJS(window.SockJS);
    }
  }

  // 4. Intercept root-relative anchor clicks when running under file://
  if (window.location.protocol === 'file:') {
    document.addEventListener(
      'click',
      function (event) {
        if (event.defaultPrevented) return;
        var anchor = event.target && event.target.closest ? event.target.closest('a[href]') : null;
        if (!anchor) return;
        var rawHref = anchor.getAttribute('href');
        if (!rawHref || rawHref.charAt(0) !== '/' || rawHref.charAt(1) === '/') return;
        if (anchor.target && anchor.target !== '_self') return;
        event.preventDefault();
        window.location.href = getDistRootUrl() + mapRoute(rawHref);
      },
      true
    );
  }
})();
`;

  fs.writeFileSync(path.join(jsDir, 'electron-bridge.js'), bridgeCode, 'utf8');
}

// Ajusta links e caminhos estáticos dos arquivos HTML para funcionamento sob file://
function processHtmlFiles(dirPath, rootDistDir) {
  const entries = fs.readdirSync(dirPath, { withFileTypes: true });
  for (const entry of entries) {
    const fullPath = path.join(dirPath, entry.name);
    if (entry.isDirectory()) {
      if (entry.name === 'electron') continue;
      processHtmlFiles(fullPath, rootDistDir);
    } else if (entry.isFile() && entry.name.endsWith('.html')) {
      const relDir = path.relative(path.dirname(fullPath), rootDistDir).replace(/\\/g, '/');
      const prefix = relDir === '' ? '.' : relDir;
      const isComponent = path.relative(rootDistDir, fullPath).replace(/\\/g, '/').startsWith('components/');

      let content = fs.readFileSync(fullPath, 'utf8');

      // Normaliza checagens de pathname para compatibilidade entre HTTP e file://
      content = content.replace(
        /window\.location\.pathname\.startsWith\((['"])\/secretary\1\)/g,
        "window.location.pathname.includes('/secretary')"
      );

      if (!isComponent) {
        // Converte assets absolutos (/assets/...) em relativos para o protocolo file://
        content = content.replace(
          /\b(href|src)=(["'])\/(assets|js|favicon\.ico)/g,
          `$1=$2${prefix}/$3`
        );
        content = content.replace(
          /\bdata-assets-path=(["'])\/assets\/(["'])/g,
          `data-assets-path=$1${prefix}/assets/$2`
        );

        // Injeta o electron-bridge.js logo após a tag <head>
        const bridgeTag = `<script src="${prefix}/js/electron-bridge.js"></script>`;
        if (!content.includes('electron-bridge.js')) {
          content = content.replace(/<head(\s[^>]*)?>/i, (match) => `${match}\n    ${bridgeTag}`);
        }
      }

      fs.writeFileSync(fullPath, content, 'utf8');
    }
  }
}

// Orquestra a geração completa do frontend na pasta dist/
function buildFrontend() {
  const staticDir = findExistingDir(STATIC_CANDIDATES);
  const templatesDir = findExistingDir(TEMPLATES_CANDIDATES);

  if (!staticDir && !templatesDir) {
    if (fs.existsSync(path.join(DIST_DIR, 'index.html'))) {
      return DIST_DIR;
    }
    throw new Error('Could not find frontend source directories (static/templates).');
  }

  fs.mkdirSync(DIST_DIR, { recursive: true });

  if (staticDir) {
    copyDirectoryRecursive(staticDir, DIST_DIR);
  }

  if (templatesDir) {
    copyDirectoryRecursive(templatesDir, DIST_DIR);
  }

  // Ensure boxicons font files also exist directly in dist/assets/vendor/fonts/
  // to satisfy both @font-face declarations in boxicons.css
  const boxiconsSubdir = path.join(DIST_DIR, 'assets', 'vendor', 'fonts', 'boxicons');
  const fontsDir = path.join(DIST_DIR, 'assets', 'vendor', 'fonts');
  if (fs.existsSync(boxiconsSubdir)) {
    copyDirectoryRecursive(boxiconsSubdir, fontsDir);
  }

  createElectronBridgeScript(DIST_DIR);

  // Create index.html from login.html so standard entry point dist/index.html exists
  const loginHtmlPath = path.join(DIST_DIR, 'login.html');
  const indexHtmlPath = path.join(DIST_DIR, 'index.html');
  if (fs.existsSync(loginHtmlPath)) {
    fs.copyFileSync(loginHtmlPath, indexHtmlPath);
  }

  processHtmlFiles(DIST_DIR, DIST_DIR);

  const loadSidebarPath = path.join(DIST_DIR, 'js', 'load-sidebar.js');
  if (fs.existsSync(loadSidebarPath)) {
    let sidebarJs = fs.readFileSync(loadSidebarPath, 'utf8');
    sidebarJs = sidebarJs.replace(
      'sidebarContainer.outerHTML = html;',
      `if (window.__ESCOLAWEB_DIST_ROOT__) {\n                html = html.replace(/\\b(src|href)=(["'])\\/(assets\\/|favicon\\.ico)/g, '$1=$2' + window.__ESCOLAWEB_DIST_ROOT__ + '/$3');\n            }\n            sidebarContainer.outerHTML = html;`
    );
    sidebarJs = sidebarJs.replace(
      /globalNotifScript\.src\s*=\s*["']\/js\/global-notifications\.js["'];/g,
      `globalNotifScript.src = (window.__ESCOLAWEB_DIST_ROOT__ || '') + "/js/global-notifications.js";`
    );
    sidebarJs = sidebarJs.replace(
      /stompScript\.onload\s*=\s*\(\)\s*=>\s*\{\s*document\.body\.appendChild\(globalNotifScript\);\s*\};/,
      `let loadedCount = 0;\n                    const onBothLoaded = () => { if (++loadedCount === 2) document.body.appendChild(globalNotifScript); };\n                    sockjsScript.onload = onBothLoaded;\n                    stompScript.onload = onBothLoaded;`
    );
    fs.writeFileSync(loadSidebarPath, sidebarJs, 'utf8');
  }

  return DIST_DIR;
}

if (require.main === module) {
  const out = buildFrontend();
  console.log(`Frontend production build generated at: ${out}`);
}

module.exports = { buildFrontend, DIST_DIR };
