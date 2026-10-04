// E2E check for dark mode via Chrome DevTools Protocol (headless Edge). Usage: node scratch/dark-mode-e2e.mjs
import { spawn } from 'node:child_process';
import { mkdtempSync, writeFileSync, mkdirSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

const BASE = 'http://localhost:8080';
const OUT = join(process.cwd(), 'scratch', 'shots');
mkdirSync(OUT, { recursive: true });
const EDGE = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe';
const PORT = 9333;
const profile = mkdtempSync(join(tmpdir(), 'ew-edge-'));
const browser = spawn(EDGE, ['--headless=new', `--remote-debugging-port=${PORT}`, `--user-data-dir=${profile}`,
  '--window-size=1440,900', '--no-first-run', '--disable-extensions', 'about:blank'], { stdio: 'ignore' });

const sleep = ms => new Promise(r => setTimeout(r, ms));
let failures = 0;
const check = (cond, msg) => { console.log(`${cond ? 'PASS' : 'FAIL'}  ${msg}`); if (!cond) failures++; };

let ws, id = 0; const pending = new Map(); const listeners = [];
const errors = [];
async function connect() {
  for (let i = 0; i < 50; i++) {
    try {
      const list = await (await fetch(`http://127.0.0.1:${PORT}/json/list`)).json();
      const page = list.find(t => t.type === 'page');
      if (page) { ws = new WebSocket(page.webSocketDebuggerUrl); break; }
    } catch { }
    await sleep(200);
  }
  await new Promise((res, rej) => { ws.onopen = res; ws.onerror = rej; });
  ws.onmessage = ev => {
    const msg = JSON.parse(ev.data);
    if (msg.id && pending.has(msg.id)) { const { res, rej } = pending.get(msg.id); pending.delete(msg.id); msg.error ? rej(new Error(msg.error.message)) : res(msg.result); }
    else if (msg.method) {
      if (msg.method === 'Runtime.exceptionThrown') errors.push(msg.params.exceptionDetails.exception?.description || msg.params.exceptionDetails.text);
      listeners.slice().forEach(l => l(msg));
    }
  };
}
const send = (method, params = {}) => new Promise((res, rej) => { const i = ++id; pending.set(i, { res, rej }); ws.send(JSON.stringify({ id: i, method, params })); });
const once = (method, timeout = 15000) => new Promise((res, rej) => {
  const t = setTimeout(() => { listeners.splice(listeners.indexOf(l), 1); rej(new Error('timeout ' + method)); }, timeout);
  const l = msg => { if (msg.method === method) { clearTimeout(t); listeners.splice(listeners.indexOf(l), 1); res(msg.params); } };
  listeners.push(l);
});
async function ev(expr) {
  const r = await send('Runtime.evaluate', { expression: expr, awaitPromise: true, returnByValue: true });
  if (r.exceptionDetails) throw new Error('eval: ' + (r.exceptionDetails.exception?.description || r.exceptionDetails.text));
  return r.result.value;
}
async function go(path) { const p = once('Page.loadEventFired'); await send('Page.navigate', { url: BASE + path }); await p; await sleep(400); }
async function waitFor(expr, timeout = 10000) { const end = Date.now() + timeout; while (Date.now() < end) { if (await ev(expr)) return true; await sleep(150); } return false; }
async function shot(name) { const { data } = await send('Page.captureScreenshot', { format: 'png' }); writeFileSync(join(OUT, name + '.png'), Buffer.from(data, 'base64')); }
async function clickSel(sel) {
  const r = await ev(`(() => { const e = document.querySelector(${JSON.stringify(sel)}); if (!e) return null; e.scrollIntoView({block:'center'}); const b = e.getBoundingClientRect(); return {x: b.x + b.width/2, y: b.y + b.height/2}; })()`);
  if (!r) throw new Error('not found ' + sel);
  await sleep(150);
  for (const type of ['mousePressed', 'mouseReleased']) await send('Input.dispatchMouseEvent', { type, x: r.x, y: r.y, button: 'left', clickCount: 1 });
  await sleep(450);
}
const isDark = () => ev(`document.documentElement.classList.contains('dark-style')`);
// Visible elements still painted pure white while in dark mode (coverage audit)
const whiteAudit = () => ev(`(() => { const out = []; for (const el of document.querySelectorAll('body *')) { const r = el.getBoundingClientRect(); if (r.width < 8 || r.height < 8) continue; const cs = getComputedStyle(el); if (cs.visibility === 'hidden' || cs.display === 'none') continue; if (cs.backgroundColor === 'rgb(255, 255, 255)') out.push(el.tagName.toLowerCase() + (el.id ? '#' + el.id : '') + (el.className && typeof el.className === 'string' ? '.' + el.className.trim().split(/\\s+/).slice(0,3).join('.') : '')); } return out.slice(0, 15); })()`);

async function login(email) {
  await go('/login.html');
  await ev(`document.getElementById('email').value = ${JSON.stringify(email)}; document.getElementById('password').value = 'senha123'; true`);
  const p = once('Page.loadEventFired');
  await clickSel('#formAuthentication button[type=submit]');
  await p; await sleep(600);
}

try {
  await connect();
  await send('Page.enable'); await send('Runtime.enable');
  await send('Emulation.setDeviceMetricsOverride', { width: 1440, height: 900, deviceScaleFactor: 1, mobile: false });

  // ---------- Login page
  await go('/login.html');
  check(!(await isDark()), 'login: default theme is light (no stored pref)');
  const pos = await ev(`(() => { const t = document.querySelector('.auth-theme-toggle'); const s = document.querySelector('#formAuthentication button[type=submit]'); if (!t || !s) return null; return { below: t.getBoundingClientRect().top >= s.getBoundingClientRect().bottom, gap: Math.round(t.getBoundingClientRect().top - s.getBoundingClientRect().bottom), sameWidth: Math.abs(t.offsetWidth - s.offsetWidth) <= 1, inForm: !!t.closest('#formAuthentication'), type: t.type }; })()`);
  check(pos && pos.below && pos.inForm, `login: toggle sits below "Entrar" inside the form (gap=${pos?.gap}px, sameWidth=${pos?.sameWidth}, type=${pos?.type})`);
  await shot('01_login_light');
  await clickSel('.auth-theme-toggle');
  check(await isDark(), 'login: clicking toggle enables dark');
  check((await ev(`localStorage.getItem('escolaweb-theme')`)) === 'dark', 'login: preference persisted to localStorage');
  check((await ev(`location.pathname`)) === '/login.html', 'login: toggle does not submit the form');
  check((await ev(`getComputedStyle(document.body).backgroundColor`)) === 'rgb(35, 35, 51)', 'login: body background is dark');
  check((await ev(`document.querySelector('.auth-theme-toggle').getAttribute('aria-pressed')`)) === 'true', 'login: aria-pressed=true in dark');
  await sleep(400); await shot('02_login_dark');
  await go('/login.html');
  check(await isDark(), 'login: dark persists after reload');
  // keyboard: Space on focused button toggles (native)
  await ev(`document.querySelector('.auth-theme-toggle').focus(); true`);
  await send('Input.dispatchKeyEvent', { type: 'keyDown', key: ' ', code: 'Space', windowsVirtualKeyCode: 32, text: ' ' });
  await send('Input.dispatchKeyEvent', { type: 'keyUp', key: ' ', code: 'Space', windowsVirtualKeyCode: 32 });
  await sleep(300);
  check(!(await isDark()), 'login: Space on focused toggle switches to light (exactly once)');
  await clickSel('.auth-theme-toggle');
  check(await isDark(), 'login: back to dark');

  for (const p of ['/forgot-password.html', '/reset-password.html']) { await go(p); check(await isDark(), `${p}: dark applied`); }

  // ---------- Principal
  await login('diretor@escola.com');
  check((await ev('location.pathname')) === '/principal/dashboard.html', 'principal: logged in -> dashboard');
  check(await isDark(), 'principal dashboard: dark applied');
  check(await waitFor(`!!document.querySelector('#layout-menu [data-theme-toggle]')`), 'sidebar: toggle injected');
  check((await ev(`document.querySelector('#layout-menu [data-theme-label]').textContent`)) === 'Modo Claro', 'sidebar: label reads "Modo Claro" in dark');
  check(await ev(`document.querySelector('#layout-menu [data-theme-icon]').classList.contains('bx-sun')`), 'sidebar: icon is sun in dark');
  check(!(await ev(`document.querySelector('#layout-menu .theme-toggle-link').closest('.menu-item').classList.contains('active')`)), 'sidebar: toggle item not marked active');
  await sleep(1500); await shot('03_principal_dashboard_dark');
  console.log('   white bg audit:', JSON.stringify(await whiteAudit()));
  await clickSel('#layout-menu .theme-toggle-link');
  check(!(await isDark()), 'sidebar: click switches to light');
  check((await ev(`localStorage.getItem('escolaweb-theme')`)) === 'light', 'sidebar: light persisted');
  check((await ev(`document.querySelector('#layout-menu [data-theme-label]').textContent`)) === 'Modo Escuro', 'sidebar: label reads "Modo Escuro" in light');
  check((await ev('location.pathname')) === '/principal/dashboard.html', 'sidebar: no navigation on toggle');
  await shot('04_principal_dashboard_light');
  await clickSel('#layout-menu .theme-toggle-link');
  check(await isDark(), 'sidebar: click switches back to dark');
  await shot('05_principal_sidebar_toggle_dark');

  const pages = {
    principal: ['students', 'teachers', 'classrooms', 'subjects', 'academic-periods', 'academic-years', 'announcements', 'my-announcements', 'chat', 'school-settings', 'year-conclusion', 'year-conclusion-results', 'create/announcement', 'create/academic-year'],
  };
  for (const p of pages.principal) {
    await go(`/principal/${p}.html`);
    await waitFor(`!!document.querySelector('#layout-menu [data-theme-toggle]')`, 5000);
    const ok = await isDark();
    const white = await whiteAudit();
    check(ok, `principal/${p}: dark applied; white-bg elements: ${JSON.stringify(white)}`);
    await shot(`p_${p.replace('/', '_')}`);
  }
  // Modals in dark mode
  for (const mp of ['/principal/academic-years.html', '/principal/year-conclusion.html', '/principal/my-announcements.html']) {
    await go(mp); await sleep(1500);
    const modalShown = await ev(`(() => { if (typeof showConfirmModal === 'function') { showConfirmModal('Teste', 'Mensagem de teste', () => {}); return 'showConfirmModal'; } const m = document.querySelector('.modal'); if (!m || !window.bootstrap) return false; new bootstrap.Modal(m).show(); return m.id || true; })()`);
    await sleep(800);
    if (modalShown) {
      const bg = await ev(`(() => { const c = document.querySelector('.modal.show .modal-content'); return c ? getComputedStyle(c).backgroundColor : null; })()`);
      const white = await ev(`(() => { const out = []; for (const el of document.querySelectorAll('.modal.show *')) { const cs = getComputedStyle(el); if (el.getBoundingClientRect().width > 8 && cs.backgroundColor === 'rgb(255, 255, 255)') out.push(el.tagName + '.' + el.className); } return out.slice(0, 8); })()`);
      check(bg && bg !== 'rgb(255, 255, 255)', `modal ${mp}#${modalShown}: content bg=${bg}; white inside: ${JSON.stringify(white)}`);
      await shot('06_modal_' + mp.split('/').pop().replace('.html', ''));
    } else console.log(`   (no modal found on ${mp})`);
  }
  await go('/ai-chat.html'); check(await isDark(), 'ai-chat: dark applied'); await sleep(800); await shot('07_ai_chat_dark');

  // Logout keeps preference
  await go('/principal/dashboard.html');
  await waitFor(`!!document.querySelector('#btnLogout')`);
  const lp = once('Page.loadEventFired'); await clickSel('#btnLogout'); await lp; await sleep(500);
  check((await ev('location.pathname')) === '/login.html', 'logout: back on login');
  check(await isDark(), 'logout: dark preference survives logout (localStorage.clear)');
  check((await ev(`localStorage.getItem('jwt_token')`)) === null, 'logout: session data still cleared');

  // Other roles
  for (const [email, role] of [['secretaria@escola.com', 'secretary'], ['prof.joao@escola.com', 'teacher'], ['aluno1@escola.com', 'student']]) {
    await login(email);
    check((await ev('location.pathname')) === `/${role}/dashboard.html`, `${role}: logged in`);
    check(await isDark(), `${role} dashboard: dark applied`);
    check(await waitFor(`!!document.querySelector('#layout-menu [data-theme-toggle]')`), `${role}: sidebar toggle present`);
    await sleep(1500);
    console.log(`   ${role} white bg audit:`, JSON.stringify(await whiteAudit()));
    await shot(`08_${role}_dashboard_dark`);
    for (const p of ({ secretary: ['students', 'create/student', 'classrooms', 'manage-classroom', 'chat'], teacher: ['lessons', 'assessments', 'my-students', 'create/lesson', 'chat'], student: ['grades', 'attendances', 'report-card', 'chat', 'subjects'] })[role]) {
      await go(`/${role}/${p}.html`); await sleep(800);
      check(await isDark(), `${role}/${p}: dark applied; white-bg: ${JSON.stringify(await whiteAudit())}`);
      await shot(`${role}_${p.replace('/', '_')}`);
    }
    await go(`/${role}/dashboard.html`); await waitFor(`!!document.querySelector('#btnLogout')`);
    const l2 = once('Page.loadEventFired'); await clickSel('#btnLogout'); await l2; await sleep(400);
  }

  // Mobile: sidebar toggle reachable in the off-canvas menu
  await send('Emulation.setDeviceMetricsOverride', { width: 390, height: 844, deviceScaleFactor: 1, mobile: true });
  await login('diretor@escola.com');
  await waitFor(`!!document.querySelector('#layout-menu [data-theme-toggle]')`);
  await ev(`window.Helpers && Helpers.setCollapsed(false); true`); await sleep(600);
  await clickSel('#layout-menu .theme-toggle-link');
  check(!(await isDark()), 'mobile: sidebar toggle works');
  await shot('09_mobile_menu_light');
  await clickSel('#layout-menu .theme-toggle-link');
  await shot('10_mobile_menu_dark');

  check(errors.length === 0, `no uncaught JS exceptions (${errors.length}) ${errors.slice(0, 5).join(' | ')}`);
} catch (e) {
  console.log('FAIL  script error: ' + e.stack); failures++;
} finally {
  try { ws?.close(); } catch { }
  browser.kill();
  console.log(`\n${failures === 0 ? 'ALL PASSED' : failures + ' FAILURE(S)'}  screenshots: ${OUT}`);
  process.exit(failures ? 1 : 0);
}
