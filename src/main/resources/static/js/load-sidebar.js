document.addEventListener('DOMContentLoaded', () => {
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
                if (href && href !== 'javascript:void(0);' && currentPath.endsWith(href)) {
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

            // 3. Re-vincular o botão de fechar sidebar no mobile
            const menuTogglers = document.querySelectorAll('#layout-menu .layout-menu-toggle');
            menuTogglers.forEach(item => {
                item.addEventListener('click', event => {
                    event.preventDefault();
                    if (window.Helpers) {
                        window.Helpers.toggleCollapsed();
                    }
                });
            });
        })
        .catch(err => console.error('Erro ao carregar sidebar:', err));
});

setTimeout(() => {
    // Populando os avatares da navbar
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
                localStorage.clear();
                window.location.href = '/login.html';
            }
        });
    });
}, 500);