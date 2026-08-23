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

            // 3. Re-vincular o botão de fechar sidebar no mobile (já que a sidebar foi injetada agora)
            const menuTogglers = document.querySelectorAll('#layout-menu .layout-menu-toggle');
            menuTogglers.forEach(item => {
                item.addEventListener('click', event => {
                    event.preventDefault();
                    if (window.Helpers) {
                        window.Helpers.toggleCollapsed();
                    }
                });
            });
            // 4. Configurar funcionalidade de Sair (Logout)
            const btnLogout = document.getElementById('btnLogout');
            if (btnLogout) {
                btnLogout.addEventListener('click', async (e) => {
                    e.preventDefault();
                    
                    const token = localStorage.getItem('jwt_token');
                    const refreshToken = localStorage.getItem('refresh_token');
                    
                    // Avisa a API para invalidar o refresh token (se existir)
                    if (refreshToken) {
                        try {
                            await fetch('/auth/logout', {
                                method: 'POST',
                                headers: {
                                    'Content-Type': 'application/json',
                                    'Authorization': token ? `Bearer ${token}` : ''
                                },
                                body: JSON.stringify({ refreshToken: refreshToken })
                            });
                        } catch (err) {
                            console.error('Erro ao fazer logout na API', err);
                        }
                    }
                    
                    // Limpa os tokens locais e redireciona para o login
                    localStorage.removeItem('jwt_token');
                    localStorage.removeItem('refresh_token');
                    window.location.href = '/login.html';
                });
            }
        })
        .catch(err => console.error('Erro ao carregar sidebar:', err));
});