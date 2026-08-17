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

            // 2. Habilitar o toggle (abrir/fechar) dos submenus
            const toggles = document.querySelectorAll('#layout-menu .menu-toggle');
            toggles.forEach(toggle => {
                toggle.addEventListener('click', (e) => {
                    e.preventDefault();
                    const parent = toggle.closest('.menu-item');
                    if (parent) {
                        parent.classList.toggle('open');
                    }
                });
            });
        })
        .catch(err => console.error('Erro ao carregar sidebar:', err));
});