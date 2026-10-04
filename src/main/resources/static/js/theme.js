/**
 * theme.js — Gerenciador global de tema (Claro / Escuro) do EscolaWEB.
 *
 * - Deve ser carregado de forma SÍNCRONA dentro do <head> (antes do <body>),
 *   para aplicar a classe do tema antes da primeira pintura e evitar o
 *   "flash" do tema claro ao navegar/recarregar.
 * - Persiste a preferência no localStorage (chave: "escolaweb-theme").
 * - Qualquer elemento com o atributo [data-theme-toggle] vira um botão de
 *   alternância (delegação de eventos — funciona também na sidebar, que é
 *   injetada dinamicamente via fetch).
 * - Elementos internos opcionais de um toggle:
 *     [data-theme-icon]  -> recebe "bx-moon" (tema claro) ou "bx-sun" (tema escuro)
 *     [data-theme-label] -> recebe o texto da ação ("Modo Escuro" / "Modo Claro")
 * - Sincroniza entre abas abertas através do evento "storage".
 * - Expõe window.EscolaTheme = { get, set, toggle, STORAGE_KEY }.
 * - Dispara o evento "escolaweb:themechange" em document ao trocar de tema.
 */
(function () {
    'use strict';

    var STORAGE_KEY = 'escolaweb-theme';
    var DARK = 'dark';
    var LIGHT = 'light';
    var root = document.documentElement;

    function readStored() {
        try {
            var value = window.localStorage.getItem(STORAGE_KEY);
            return value === DARK || value === LIGHT ? value : null;
        } catch (e) {
            return null; // localStorage indisponível (modo privado restrito, etc.)
        }
    }

    function writeStored(theme) {
        try {
            window.localStorage.setItem(STORAGE_KEY, theme);
        } catch (e) { /* ignora: o tema ainda é aplicado na página atual */ }
    }

    function current() {
        return root.classList.contains('dark-style') ? DARK : LIGHT;
    }

    function applyClasses(theme) {
        var isDark = theme === DARK;
        root.classList.toggle('dark-style', isDark);
        root.classList.toggle('light-style', !isDark);
        root.setAttribute('data-bs-theme', theme);
        root.style.colorScheme = isDark ? 'dark' : 'light';
    }

    function syncUI() {
        var isDark = current() === DARK;

        var toggles = document.querySelectorAll('[data-theme-toggle]');
        for (var i = 0; i < toggles.length; i++) {
            var t = toggles[i];
            t.setAttribute('aria-pressed', isDark ? 'true' : 'false');
            t.setAttribute('title', isDark ? 'Ativar modo claro' : 'Ativar modo escuro');

            var icon = t.querySelector('[data-theme-icon]');
            if (icon) {
                icon.classList.toggle('bx-moon', !isDark);
                icon.classList.toggle('bx-sun', isDark);
            }
            var label = t.querySelector('[data-theme-label]');
            if (label) {
                label.textContent = isDark ? 'Modo Claro' : 'Modo Escuro';
            }
        }

        // emoji-picker-element usa as classes "light" / "dark" para o próprio tema
        var pickers = document.querySelectorAll('emoji-picker');
        for (var j = 0; j < pickers.length; j++) {
            pickers[j].classList.toggle('dark', isDark);
            pickers[j].classList.toggle('light', !isDark);
        }
    }

    function set(theme, options) {
        theme = theme === DARK ? DARK : LIGHT;
        var animate = !options || options.animate !== false;
        var persist = !options || options.persist !== false;

        if (animate && document.body) {
            // Transição suave apenas durante a troca (evita animar cada hover da página)
            root.classList.add('theme-switching');
            window.clearTimeout(set._timer);
            set._timer = window.setTimeout(function () {
                root.classList.remove('theme-switching');
            }, 350);
        }

        applyClasses(theme);
        if (persist) writeStored(theme);
        syncUI();

        try {
            document.dispatchEvent(new CustomEvent('escolaweb:themechange', { detail: { theme: theme } }));
        } catch (e) { /* navegadores muito antigos */ }
        return theme;
    }

    function toggle() {
        return set(current() === DARK ? LIGHT : DARK);
    }

    // 1) Aplica imediatamente (ainda no <head>) para não haver flash do tema claro
    applyClasses(readStored() || LIGHT);

    // 2) Delegação de clique/teclado para qualquer [data-theme-toggle], inclusive os injetados depois
    document.addEventListener('click', function (event) {
        var target = event.target && event.target.closest ? event.target.closest('[data-theme-toggle]') : null;
        if (!target) return;
        event.preventDefault();
        toggle();
    });

    document.addEventListener('keydown', function (event) {
        if (event.key !== ' ' && event.key !== 'Spacebar') return;
        var target = event.target && event.target.closest ? event.target.closest('[data-theme-toggle]') : null;
        // <button> já trata espaço nativamente; links (<a>) não
        if (!target || target.tagName === 'BUTTON') return;
        event.preventDefault();
        toggle();
    });

    // 3) Sincroniza ícones/rótulos quando o DOM ficar pronto e quando a sidebar for injetada
    document.addEventListener('DOMContentLoaded', function () {
        syncUI();
        if (window.MutationObserver && document.body) {
            var pending = false;
            new MutationObserver(function (mutations) {
                if (pending) return;
                for (var i = 0; i < mutations.length; i++) {
                    var added = mutations[i].addedNodes;
                    for (var k = 0; k < added.length; k++) {
                        var node = added[k];
                        if (node.nodeType !== 1) continue;
                        if ((node.matches && (node.matches('[data-theme-toggle], emoji-picker'))) ||
                            (node.querySelector && node.querySelector('[data-theme-toggle], emoji-picker'))) {
                            pending = true;
                            window.requestAnimationFrame(function () { pending = false; syncUI(); });
                            return;
                        }
                    }
                }
            }).observe(document.body, { childList: true, subtree: true });
        }
    });

    // 4) Mantém várias abas sincronizadas
    window.addEventListener('storage', function (event) {
        if (event.key !== STORAGE_KEY) return;
        var theme = event.newValue === DARK ? DARK : LIGHT;
        if (theme !== current()) set(theme, { persist: false });
    });

    // 5) Restaura a classe correta se a página vier do bfcache (botão "voltar")
    window.addEventListener('pageshow', function (event) {
        if (!event.persisted) return;
        var stored = readStored() || LIGHT;
        if (stored !== current()) set(stored, { animate: false, persist: false });
    });

    window.EscolaTheme = {
        STORAGE_KEY: STORAGE_KEY,
        get: current,
        set: set,
        toggle: toggle
    };
})();
