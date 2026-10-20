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

        // 3. Fallback para togglers (navbar ou standalone) caso main.js não tenha vinculado
        document.querySelectorAll(".layout-menu-toggle:not(.layout-overlay):not(#layout-menu .layout-menu-toggle)").forEach(item => {
            if (!item.dataset.menuToggleBound) {
                item.dataset.menuToggleBound = "true";
                if (!window.Helpers || typeof window.Helpers.toggleCollapsed !== "function") {
                    item.addEventListener("click", toggleMenu);
                }
            }
        });

        // 3.1. Fechar menu com a tecla Escape
        if (!document.documentElement.dataset.menuEscapeBound) {
            document.documentElement.dataset.menuEscapeBound = "true";
            document.addEventListener("keydown", (e) => {
                if (e.key === "Escape" && document.documentElement.classList.contains("layout-menu-expanded")) {
                    closeMenu();
                }
            });
        }

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

            const ROUTE_ALIASES = {
                "/direcao/painel": "/principal/dashboard.html",
                "/direcao/periodos-academicos": "/principal/academic-periods.html",
                "/direcao/anos-letivos": "/principal/academic-years.html",
                "/direcao/conclusao-ano": "/principal/year-conclusion.html",
                "/direcao/resultado-conclusao-ano": "/principal/year-conclusion-results.html",
                "/direcao/turmas": "/principal/classrooms.html",
                "/direcao/usuarios": "/principal/users.html",
                "/direcao/professores": "/principal/teachers.html",
                "/direcao/alunos": "/principal/students.html",
                "/direcao/desempenho-aluno": "/principal/student-performance.html",
                "/direcao/disciplinas": "/principal/subjects.html",
                "/direcao/diretores": "/principal/principals.html",
                "/direcao/secretarios": "/principal/secretaries.html",
                "/direcao/avisos": "/principal/announcements.html",
                "/direcao/meus-avisos": "/principal/my-announcements.html",
                "/direcao/avisos-arquivados": "/principal/archived-announcements.html",
                "/direcao/chat": "/principal/chat.html",
                "/direcao/chat-arquivado": "/principal/archived-chat.html",
                "/direcao/historico-alteracoes": "/principal/change-history.html",
                "/direcao/configuracoes-escola": "/principal/school-settings.html",
                "/direcao/cadastrar/periodo": "/principal/create/academic-period.html",
                "/direcao/cadastrar/ano-letivo": "/principal/create/academic-year.html",
                "/direcao/cadastrar/aviso": "/principal/create/announcement.html",
                "/direcao/editar/periodo": "/principal/edit/academic-period.html",
                "/direcao/editar/ano-letivo": "/principal/edit/academic-year.html",
                "/direcao/editar/aviso": "/principal/edit/announcement.html",
                "/direcao/grade-horaria": "/principal/timetable.html",
                "/direcao/cardapio": "/principal/meal-menu.html",

                "/secretaria/painel": "/secretary/dashboard.html",
                "/secretaria/usuarios": "/secretary/users.html",
                "/secretaria/alunos": "/secretary/students.html",
                "/secretaria/desempenho-aluno": "/secretary/student-performance.html",
                "/secretaria/turmas": "/secretary/classrooms.html",
                "/secretaria/gerenciar-turma": "/secretary/manage-classroom.html",
                "/secretaria/professores": "/secretary/teachers.html",
                "/secretaria/disciplinas": "/secretary/subjects.html",
                "/secretaria/periodos-academicos": "/secretary/academic-periods.html",
                "/secretaria/anos-letivos": "/secretary/academic-years.html",
                "/secretaria/resultado-conclusao-ano": "/secretary/year-conclusion-results.html",
                "/secretaria/diretores": "/secretary/principals.html",
                "/secretaria/secretarios": "/secretary/secretaries.html",
                "/secretaria/avisos": "/secretary/announcements.html",
                "/secretaria/meus-avisos": "/secretary/my-announcements.html",
                "/secretaria/avisos-arquivados": "/secretary/archived-announcements.html",
                "/secretaria/chat": "/secretary/chat.html",
                "/secretaria/chat-arquivado": "/secretary/archived-chat.html",
                "/secretaria/historico-alteracoes": "/secretary/change-history.html",
                "/secretaria/configuracoes-escola": "/secretary/school-settings.html",
                "/secretaria/cadastrar/aluno": "/secretary/create/student.html",
                "/secretaria/cadastrar/turma": "/secretary/create/classroom.html",
                "/secretaria/cadastrar/professor": "/secretary/create/teacher.html",
                "/secretaria/cadastrar/disciplina": "/secretary/create/subject.html",
                "/secretaria/cadastrar/diretor": "/secretary/create/principal.html",
                "/secretaria/cadastrar/secretario": "/secretary/create/secretary.html",
                "/secretaria/cadastrar/aviso": "/secretary/create/announcement.html",
                "/secretaria/editar/aluno": "/secretary/edit/student.html",
                "/secretaria/editar/turma": "/secretary/edit/classroom.html",
                "/secretaria/editar/professor": "/secretary/edit/teacher.html",
                "/secretaria/editar/disciplina": "/secretary/edit/subject.html",
                "/secretaria/editar/diretor": "/secretary/edit/principal.html",
                "/secretaria/editar/secretario": "/secretary/edit/secretary.html",
                "/secretaria/editar/aviso": "/secretary/edit/announcement.html",
                "/secretaria/grade-horaria": "/secretary/timetable.html",
                "/secretaria/cardapio": "/secretary/meal-menu.html",

                "/professor/painel": "/teacher/dashboard.html",
                "/professor/aulas": "/teacher/lessons.html",
                "/professor/avaliacoes": "/teacher/assessments.html",
                "/professor/meus-alunos": "/teacher/my-students.html",
                "/professor/colegas": "/teacher/colleagues.html",
                "/professor/avisos": "/teacher/announcements.html",
                "/professor/meus-avisos": "/teacher/my-announcements.html",
                "/professor/avisos-direcionados": "/teacher/targeted-announcements.html",
                "/professor/chat": "/teacher/chat.html",
                "/professor/periodos-academicos": "/teacher/academic-periods.html",
                "/professor/anos-letivos": "/teacher/academic-years.html",
                "/professor/historico-alteracoes": "/teacher/change-history.html",
                "/professor/configuracoes-escola": "/teacher/school-settings.html",
                "/professor/cadastrar/aula": "/teacher/create/lesson.html",
                "/professor/cadastrar/avaliacao": "/teacher/create/assessment.html",
                "/professor/cadastrar/aviso": "/teacher/create/announcement.html",
                "/professor/editar/aula": "/teacher/edit/lesson.html",
                "/professor/editar/avaliacao": "/teacher/edit/assessment.html",
                "/professor/editar/aviso": "/teacher/edit/announcement.html",
                "/professor/grade-horaria": "/teacher/timetable.html",
                "/professor/cardapio": "/teacher/meal-menu.html",

                "/aluno/painel": "/student/dashboard.html",
                "/aluno/boletim": "/student/report-card.html",
                "/aluno/notas": "/student/grades.html",
                "/aluno/frequencias": "/student/attendances.html",
                "/aluno/colegas": "/student/classmates.html",
                "/aluno/disciplinas": "/student/subjects.html",
                "/aluno/avisos": "/student/announcements.html",
                "/aluno/chat": "/student/chat.html",
                "/aluno/periodos-academicos": "/student/academic-periods.html",
                "/aluno/anos-letivos": "/student/academic-years.html",
                "/aluno/configuracoes-escola": "/student/school-settings.html",
                "/aluno/grade-horaria": "/student/timetable.html",
                "/aluno/cardapio": "/student/meal-menu.html",

                "/entrar": "/login.html",
                "/recuperar-senha": "/forgot-password.html",
                "/redefinir-senha": "/reset-password.html",
                "/assistente-ia": "/ai-chat.html"
            };

            const toCanonical = (p) => {
                if (!p) return "";
                const clean = p.replace(/\/$/, "");
                if (ROUTE_ALIASES[clean]) return ROUTE_ALIASES[clean];
                for (const [pt, en] of Object.entries(ROUTE_ALIASES)) {
                    if (en === clean || clean.endsWith(en)) return en;
                }
                return clean;
            };

            const normCurrent = toCanonical(currentPath);
            const menuLinks = document.querySelectorAll('#layout-menu .menu-link');

            menuLinks.forEach(link => {
                const href = link.getAttribute('href');
                if (!href || href === 'javascript:void(0);') return;

                const normHref = toCanonical(href);
                let isActive = (currentPath === href) || currentPath.endsWith(href) || (normCurrent === normHref) || normCurrent.endsWith(normHref);

                if ((normCurrent.includes('/create/student.html') || normCurrent.includes('/edit/student.html') || normCurrent.includes('student-performance.html') || normCurrent.includes('students.html') || normCurrent.includes('teachers.html') || normCurrent.includes('principals.html') || normCurrent.includes('secretaries.html') || normCurrent.includes('/create/teacher.html') || normCurrent.includes('/create/principal.html') || normCurrent.includes('/create/secretary.html') || normCurrent.includes('/edit/teacher.html') || normCurrent.includes('/edit/principal.html') || normCurrent.includes('/edit/secretary.html')) && normHref.includes('users.html')) {
                    isActive = true;
                }
                if ((normCurrent.includes('/create/student.html') || normCurrent.includes('/edit/student.html') || normCurrent.includes('student-performance.html')) && normHref.includes('students.html')) {
                    isActive = true;
                }
                if ((normCurrent.includes('/create/teacher.html') || normCurrent.includes('/edit/teacher.html')) && normHref.includes('teachers.html')) {
                    isActive = true;
                }
                if ((normCurrent.includes('/create/classroom.html') || normCurrent.includes('/edit/classroom.html') || normCurrent.includes('manage-classroom.html')) && normHref.includes('classrooms.html')) {
                    isActive = true;
                }
                if ((normCurrent.includes('/create/announcement.html') || normCurrent.includes('/edit/announcement.html') || normCurrent.includes('/my-announcements.html')) && normHref.includes('announcements.html')) {
                    isActive = true;
                }
                if ((normCurrent.includes('/create/academic-period.html') || normCurrent.includes('/edit/academic-period.html')) && normHref.includes('academic-periods.html')) {
                    isActive = true;
                }
                if ((normCurrent.includes('/create/academic-year.html') || normCurrent.includes('/edit/academic-year.html')) && normHref.includes('academic-years.html')) {
                    isActive = true;
                }
                if ((normCurrent.includes('/create/subject.html') || normCurrent.includes('/edit/subject.html')) && normHref.includes('subjects.html')) {
                    isActive = true;
                }
                if ((normCurrent.includes('/create/principal.html') || normCurrent.includes('/edit/principal.html')) && normHref.includes('principals.html')) {
                    isActive = true;
                }
                if ((normCurrent.includes('/create/secretary.html') || normCurrent.includes('/edit/secretary.html')) && normHref.includes('secretaries.html')) {
                    isActive = true;
                }
                if ((normCurrent.includes('/create/assessment.html') || normCurrent.includes('/edit/assessment.html')) && normHref.includes('assessments.html')) {
                    isActive = true;
                }
                if ((normCurrent.includes('/create/lesson.html') || normCurrent.includes('/edit/lesson.html')) && normHref.includes('lessons.html')) {
                    isActive = true;
                }
                
                if (isActive) {
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
                    if (hasUnread && !normCurrent.includes('announcements.html') && !currentPath.includes('/avisos')) {
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
                    if (status.hasUnreadMessages && !normCurrent.includes('chat.html') && !currentPath.includes('/chat')) {
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
                    
                    // Espera ambos os scripts principais carregarem
                    let loadedCount = 0;
                    const onBothLoaded = () => {
                        if (++loadedCount === 2) {
                            document.body.appendChild(globalNotifScript);
                        }
                    };
                    sockjsScript.onload = onBothLoaded;
                    stompScript.onload = onBothLoaded;
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
                        document.cookie = "jwt_token=; path=/; expires=Thu, 01 Jan 1970 00:00:00 UTC; SameSite=Strict";
                        if (savedTheme) localStorage.setItem(themeKey, savedTheme);
                        window.location.href = '/entrar';
                    }
                });
            });
        })
        .catch(err => console.error('Erro ao carregar sidebar:', err));
});
