document.addEventListener('DOMContentLoaded', () => {
    const initMenuToggles = () => {
        let overlay = document.querySelector(".layout-overlay");
        if (!overlay) {
            overlay = document.createElement("div");
            overlay.className = "layout-overlay layout-menu-toggle";
            const wrapper = document.querySelector(".layout-wrapper");
            if (wrapper) wrapper.appendChild(overlay);
        }

        const closeMenu = () => {
            if (window.Helpers && typeof window.Helpers.setCollapsed === "function") {
                window.Helpers.setCollapsed(true, false);
            } else {
                document.documentElement.classList.remove("layout-menu-expanded");
            }
        };

        const toggleMenu = (event) => {
            if (event) event.preventDefault();
            if (window.Helpers && typeof window.Helpers.toggleCollapsed === "function") {
                window.Helpers.toggleCollapsed();
            } else {
                document.documentElement.classList.toggle("layout-menu-expanded");
            }
        };

        // 1. Overlay click SEMPRE fecha o menu de forma determinística
        if (overlay && !overlay.dataset.overlayBound) {
            overlay.dataset.overlayBound = "true";
            overlay.addEventListener("click", (event) => {
                event.preventDefault();
                event.stopImmediatePropagation();
                closeMenu();
            }, true);
        }

        // 2. Botão de fechar dentro da sidebar dinâmica (#layout-menu)
        document.querySelectorAll("#layout-menu .layout-menu-toggle").forEach(item => {
            if (!item.dataset.menuToggleBound) {
                item.dataset.menuToggleBound = "true";
                item.addEventListener("click", (event) => {
                    event.preventDefault();
                    event.stopImmediatePropagation();
                    closeMenu();
                });
            }
        });

        // 3. Fallback para toggler da navbar caso main.js não tenha vinculado
        document.querySelectorAll(".layout-navbar .layout-menu-toggle").forEach(item => {
            if (!item.dataset.menuToggleBound) {
                item.dataset.menuToggleBound = "true";
                if (!window.Helpers || typeof window.Helpers.toggleCollapsed !== "function") {
                    item.addEventListener("click", toggleMenu);
                }
            }
        });

        // 4. No mobile (< 1200px), fechar menu ao clicar num link de navegação direta
        document.querySelectorAll("#layout-menu .menu-link:not(.menu-toggle)").forEach(link => {
            if (!link.dataset.menuLinkBound) {
                link.dataset.menuLinkBound = "true";
                link.addEventListener("click", () => {
                    if (window.innerWidth < 1200) {
                        closeMenu();
                    }
                });
            }
        });
    };

    initMenuToggles();

    const sidebarContainer = document.getElementById('sidebar-container');
    if (!sidebarContainer) return;

    // Obtém o arquivo definido no atributo data-sidebar
    const sidebarPath = sidebarContainer.getAttribute('data-sidebar') || '/components/secretary-sidebar.html';

    fetch(sidebarPath)
        .then(response => {
            if (!response.ok) throw new Error(`Não foi possível carregar a sidebar: ${sidebarPath}`);
            return response.text();
        })
        .then(html => {
            // Insere o HTML da sidebar no lugar do container
            sidebarContainer.outerHTML = html;

            // 1. Destacar a página atual no menu
            const currentPath = window.location.pathname;
            const menuLinks = document.querySelectorAll('#layout-menu .menu-link');

            menuLinks.forEach(link => {
                const href = link.getAttribute('href');
                let isActive = currentPath.endsWith(href);
                if (currentPath.includes('student-performance.html') && href.includes('students.html')) {
                    isActive = true;
                }
                
                if (href && href !== 'javascript:void(0);' && isActive) {
                    const menuItem = link.closest('.menu-item');
                    if (menuItem) menuItem.classList.add('active');

                    // Se for submenu, abre o menu pai
                    const parentSub = link.closest('.menu-sub');
                    if (parentSub) {
                        const parentItem = parentSub.closest('.menu-item');
                        if (parentItem) parentItem.classList.add('active', 'open');
                    }
                }
            });

            // 2. Inicializar o Menu nativo do Sneat para ter as animações fluidas
            const layoutMenu = document.querySelector('#layout-menu');
            if (layoutMenu && typeof Menu !== 'undefined') {
                const menu = new Menu(layoutMenu, {
                    orientation: 'vertical',
                    closeChildren: false
                });
                if (window.Helpers) {
                    window.Helpers.scrollToActive(false);
                    window.Helpers.mainMenu = menu;
                }
            }

            // 3. Re-vincular overlay e botoes apos injecao da sidebar
            initMenuToggles();

            // Funcao auxiliar para adicionar o selo (badge) de notificacao sem quebrar o layout/html
            const addNotificationBadge = (elem) => {
                if (!elem) return;
                
                // Limpar qualquer bolinha injetada incorretamente (antiga) que esteja presa dentro do elem
                const oldBadges = elem.querySelectorAll('span');
                oldBadges.forEach(b => b.remove());

                if (!elem.parentElement.querySelector('.notif-badge')) {
                    const badge = document.createElement('span');
                    badge.className = 'notif-badge';
                    // SNEAT usa ms-auto (margin-left: auto) para empurrar pro lado direito
                    badge.style.cssText = 'display:inline-block; width:8px; height:8px; background-color:#696cff; border-radius:50%; margin-left:auto; margin-right:8px; vertical-align:middle;';
                    elem.parentElement.appendChild(badge);
                }
            };

            // 4. Check for unread announcements
            const token = localStorage.getItem("jwt_token");
            if (token) {
                fetch('/api/announcements/has-unread', {
                    headers: { 'Authorization': 'Bearer ' + token }
                })
                .then(r => r.json())
                .then(hasUnread => {
                    if (hasUnread && !window.location.pathname.includes('announcements.html')) {
                        addNotificationBadge(document.querySelector('[data-i18n="Mural de Recados"]'));
                    }
                })
                .catch(e => console.error('Erro ao verificar avisos:', e));

                // 4.1. Check for unread chat messages (Inicial)
                fetch('/api/chat/unread-status', {
                    headers: { 'Authorization': 'Bearer ' + token }
                })
                .then(r => r.json())
                .then(status => {
                    if (status.hasUnreadMessages && !window.location.pathname.includes('chat.html')) {
                        addNotificationBadge(document.querySelector('[data-i18n="Chat da Turma"]'));
                    }
                })
                .catch(e => console.error('Erro ao verificar chat não lido:', e));

                // 4.2. Injetar Conexão WebSocket Global (Tempo Real)
                if (typeof SockJS === 'undefined') {
                    const sockjsScript = document.createElement('script');
                    sockjsScript.src = "https://cdnjs.cloudflare.com/ajax/libs/sockjs-client/1.6.1/sockjs.min.js";
                    document.body.appendChild(sockjsScript);
                    
                    const stompScript = document.createElement('script');
                    stompScript.src = "https://cdnjs.cloudflare.com/ajax/libs/stomp.js/2.3.3/stomp.min.js";
                    document.body.appendChild(stompScript);
                    
                    const globalNotifScript = document.createElement('script');
                    globalNotifScript.src = "/js/global-notifications.js";
                    
                    // Espera os scripts principais carregarem
                    stompScript.onload = () => {
                        document.body.appendChild(globalNotifScript);
                    };
                } else {
                    const globalNotifScript = document.createElement('script');
                    globalNotifScript.src = "/js/global-notifications.js";
                    document.body.appendChild(globalNotifScript);
                }
            }

            // 5. Sessão e Logout movidos para DENTRO do then (remove race condition)
            const userName = localStorage.getItem('user_name') || 'Usuário';
            const initial = userName.charAt(0).toUpperCase();
            
            const avatarInitial = document.getElementById('dropdownAvatarInitial');
            const avatarInitialInner = document.getElementById('dropdownAvatarInitialInner');
            if (avatarInitial) avatarInitial.textContent = initial;
            if (avatarInitialInner) avatarInitialInner.textContent = initial;
            
            const nameElem = document.getElementById('dropdownUserName');
            if (nameElem) nameElem.textContent = userName;

            // Vinculando botões de Logout
            const logoutBtns = document.querySelectorAll('#btnLogout, #btnLogoutNavbar');
            logoutBtns.forEach(btn => {
                btn.addEventListener('click', async (e) => {
                    e.preventDefault();
                    try {
                        const rt = localStorage.getItem('refresh_token');
                        if(rt) {
                            await fetch('/auth/logout', {
                                method: 'POST',
                                headers: {'Content-Type': 'application/json'},
                                body: JSON.stringify({refreshToken: rt})
                            });
                        }
                    } finally {
                        // Preserva a preferência de tema (não é dado de sessão)
                        const themeKey = (window.EscolaTheme && window.EscolaTheme.STORAGE_KEY) || 'escolaweb-theme';
                        const savedTheme = localStorage.getItem(themeKey);
                        localStorage.clear();
                        if (savedTheme) localStorage.setItem(themeKey, savedTheme);
                        window.location.href = '/login.html';
                    }
                });
            });
        })
        .catch(err => console.error('Erro ao carregar sidebar:', err));
});

