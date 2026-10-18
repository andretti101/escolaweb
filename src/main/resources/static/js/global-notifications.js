(function() {
    const token = localStorage.getItem("jwt_token");
    const userIdStr = localStorage.getItem("user_id");

    if (!token || !userIdStr) return;
    const userId = parseInt(userIdStr, 10);

    if (window.appStompClient) return; // Evita múltiplas conexões

    let initAttempts = 0;
    function initWebSocketNotifications() {
        if (typeof SockJS === 'undefined' || typeof Stomp === 'undefined') {
            if (++initAttempts < 25) {
                setTimeout(initWebSocketNotifications, 150);
            } else {
                console.warn("Global Notifications: SockJS ou Stomp não foram carregados a tempo.");
            }
            return;
        }

        if (window.appStompClient) return;

        const socket = new SockJS('/ws-chat');
        const stompClient = Stomp.over(socket);
        stompClient.debug = null; // Desativa logs no console para não poluir

        window.appStompClient = stompClient;

        function getOrCreateToastContainer() {
            let container = document.getElementById('global-toast-container');
            if (!container) {
                container = document.createElement('div');
                container.id = 'global-toast-container';
                container.className = 'toast-container position-fixed top-0 end-0 p-3';
                container.style.zIndex = '1095';
                document.body.appendChild(container);
            }
            return container;
        }

        function getUserRole() {
            const currentPath = window.location.pathname || '';
            if (currentPath.includes('/aluno') || currentPath.includes('/student')) return 'STUDENT';
            if (currentPath.includes('/professor') || currentPath.includes('/teacher')) return 'TEACHER';
            if (currentPath.includes('/direcao') || currentPath.includes('/principal')) return 'PRINCIPAL';
            if (currentPath.includes('/secretaria') || currentPath.includes('/secretary')) return 'SECRETARY';

            const storedRole = localStorage.getItem('user_role');
            if (storedRole) return storedRole.toUpperCase();

            try {
                const parts = token.split('.');
                if (parts.length === 3) {
                    const payload = JSON.parse(atob(parts[1]));
                    if (payload.role) return String(payload.role).toUpperCase();
                }
            } catch (_e) {}

            return 'STUDENT';
        }

        const ROUTE_FILE_MAP = {
            '/aluno/notas': '/student/grades.html',
            '/aluno/frequencias': '/student/attendances.html',
            '/aluno/boletim': '/student/report-card.html',
            '/aluno/avisos': '/student/announcements.html',
            '/professor/avisos': '/teacher/announcements.html',
            '/professor/avisos-direcionados': '/teacher/targeted-announcements.html',
            '/direcao/avisos': '/principal/announcements.html',
            '/secretaria/avisos': '/secretary/announcements.html'
        };

        function resolveNotificationUrl(rawUrl) {
            if (!rawUrl) return null;
            let url = rawUrl;
            const role = getUserRole();

            if (url === '/avisos' || url === '/avisos/') {
                if (role.includes('TEACHER')) {
                    url = '/professor/avisos';
                } else if (role.includes('PRINCIPAL')) {
                    url = '/direcao/avisos';
                } else if (role.includes('SECRETARY')) {
                    url = '/secretaria/avisos';
                } else {
                    url = '/aluno/avisos';
                }
            }

            const isFileProtocol = window.location.protocol === 'file:';
            const distRoot = window.__ESCOLAWEB_DIST_ROOT__ || (isFileProtocol ? (function() {
                var href = window.location.href.split('?')[0].split('#')[0];
                var markers = ['/principal/', '/secretary/', '/teacher/', '/student/', '/components/'];
                for (var i = 0; i < markers.length; i++) {
                    var idx = href.lastIndexOf(markers[i]);
                    if (idx !== -1) return href.substring(0, idx);
                }
                var lastSlash = href.lastIndexOf('/');
                return lastSlash !== -1 ? href.substring(0, lastSlash) : href;
            })() : null);

            if (distRoot) {
                const mappedFile = ROUTE_FILE_MAP[url] || url;
                const clean = mappedFile.replace(/^\/+/, '');
                return distRoot.replace(/\/+$/, '') + '/' + clean;
            }

            return url;
        }

        function addNotificationBadge(elem) {
            if (!elem) return;
            const parent = elem.closest('.menu-link') || elem.parentElement;
            if (!parent) return;

            const oldBadges = parent.querySelectorAll('span.notif-badge');
            oldBadges.forEach(b => b.remove());

            const badge = document.createElement('span');
            badge.className = 'notif-badge';
            badge.style.cssText = 'display:inline-block; width:8px; height:8px; background-color:#696cff; border-radius:50%; margin-left:auto; margin-right:8px; vertical-align:middle;';
            parent.appendChild(badge);
        }

        function showNotificationToast(title, message, linkUrl, type) {
            const container = getOrCreateToastContainer();
            const toastEl = document.createElement('div');
            toastEl.className = 'toast border-0 shadow-lg';
            toastEl.setAttribute('role', 'alert');
            toastEl.setAttribute('aria-live', 'assertive');
            toastEl.setAttribute('aria-atomic', 'true');
            toastEl.style.minWidth = '300px';
            toastEl.style.maxWidth = '420px';

            let iconClass = 'bx bx-bell text-primary';
            let typeBadge = '<span class="badge bg-label-primary me-1">Aviso</span>';

            if (type === 'GRADE_LAUNCHED') {
                iconClass = 'bx bx-award text-success';
                typeBadge = '<span class="badge bg-label-success me-1">Nota</span>';
            } else if (type === 'ATTENDANCE_RECORDED') {
                iconClass = 'bx bx-calendar-check text-info';
                typeBadge = '<span class="badge bg-label-info me-1">Frequência</span>';
            } else if (type === 'NEW_ANNOUNCEMENT') {
                iconClass = 'bx bx-broadcast text-warning';
                typeBadge = '<span class="badge bg-label-warning me-1">Comunicado</span>';
            }

            const resolvedUrl = resolveNotificationUrl(linkUrl);

            toastEl.innerHTML = `
                <div class="toast-header">
                    <i class="${iconClass} fs-5 me-2"></i>
                    ${typeBadge}
                    <strong class="me-auto text-truncate" style="max-width: 170px;">${title || 'Notificação'}</strong>
                    <small class="text-muted ms-1">Agora</small>
                    <button type="button" class="btn-close ms-2" data-bs-dismiss="toast" aria-label="Close"></button>
                </div>
                <div class="toast-body" style="font-size: 0.9rem;">
                    <div>${message || ''}</div>
                    ${resolvedUrl ? `
                        <div class="mt-2 pt-2 border-top d-flex justify-end justify-content-end">
                            <a href="${resolvedUrl}" class="btn btn-sm btn-outline-primary py-1 px-2" style="font-size: 0.78rem;">
                                Ver detalhes <i class="bx bx-right-arrow-alt"></i>
                            </a>
                        </div>
                    ` : ''}
                </div>
            `;

            // Tratamento explícito de clique para fechar mesmo se bootstrap.js não estiver disponível
            const closeBtn = toastEl.querySelector('.btn-close');
            if (closeBtn) {
                closeBtn.addEventListener('click', function(e) {
                    e.stopPropagation();
                    if (window.bootstrap && window.bootstrap.Toast) {
                        try {
                            const instance = window.bootstrap.Toast.getInstance(toastEl);
                            if (instance) {
                                instance.hide();
                                return;
                            }
                        } catch (_err) {}
                    }
                    toastEl.remove();
                });
            }

            if (resolvedUrl) {
                toastEl.style.cursor = 'pointer';
                toastEl.addEventListener('click', function(e) {
                    if (e.target.closest('.btn-close') || e.target.closest('a')) return;
                    window.location.href = resolvedUrl;
                });
            }

            container.appendChild(toastEl);

            if (window.bootstrap && window.bootstrap.Toast) {
                try {
                    const bsToast = new window.bootstrap.Toast(toastEl, { delay: 7000, autohide: true });
                    bsToast.show();
                    toastEl.addEventListener('hidden.bs.toast', function() {
                        toastEl.remove();
                    });
                } catch (_err) {
                    toastEl.classList.add('show');
                    setTimeout(function() {
                        toastEl.classList.remove('show');
                        setTimeout(function() { toastEl.remove(); }, 400);
                    }, 7000);
                }
            } else {
                toastEl.classList.add('show');
                setTimeout(function() {
                    toastEl.classList.remove('show');
                    setTimeout(function() { toastEl.remove(); }, 400);
                }, 7000);
            }
        }

        function processIncomingMessage(message) {
            if (!message || !message.body) return;

            let parsed = null;
            try {
                parsed = JSON.parse(message.body);
            } catch (e) {
                parsed = null;
            }

            if (parsed && typeof parsed === 'object') {
                const title = parsed.title || 'Nova Notificação';
                const bodyMsg = parsed.message || '';
                const linkUrl = parsed.linkUrl || null;
                const type = parsed.type || 'NOTIFICATION';

                showNotificationToast(title, bodyMsg, linkUrl, type);

                // Atualização dinâmica dos badges de menu para todos os perfis
                if (type === 'NEW_ANNOUNCEMENT') {
                    const announcementElements = [
                        document.querySelector('[data-i18n="Mural de Recados"]'),
                        document.querySelector('[data-i18n="Recados das Minhas Turmas"]'),
                        document.querySelector('[data-i18n="Recados para Professores"]'),
                        document.querySelector('[data-i18n="Listagem"]'),
                        document.querySelector('[data-i18n="Avisos"]'),
                        document.querySelector('[data-i18n="Avisos Direcionados"]')
                    ];
                    announcementElements.forEach(el => { if (el) addNotificationBadge(el); });
                } else if (type === 'GRADE_LAUNCHED') {
                    const gradeElements = [
                        document.querySelector('[data-i18n="Notas"]'),
                        document.querySelector('[data-i18n="Boletim Final"]'),
                        document.querySelector('[data-i18n="Boletim"]'),
                        document.querySelector('[data-i18n="Avaliações"]')
                    ];
                    gradeElements.forEach(el => { if (el) addNotificationBadge(el); });
                } else if (type === 'ATTENDANCE_RECORDED') {
                    const attendanceElements = [
                        document.querySelector('[data-i18n="Minhas Faltas"]'),
                        document.querySelector('[data-i18n="Frequência"]'),
                        document.querySelector('[data-i18n="Frequências"]'),
                        document.querySelector('[data-i18n="Diário de Classe"]')
                    ];
                    attendanceElements.forEach(el => { if (el) addNotificationBadge(el); });
                }
            } else if (typeof message.body === 'string') {
                if (message.body.includes("NEW_CHAT_MESSAGE") || message.body.includes("NEW_GLOBAL_TEACHER_MESSAGE")) {
                    if (!window.location.pathname.includes('chat.html')) {
                        const chatDivs = [
                            document.querySelector('[data-i18n="Chat da Turma"]'),
                            document.querySelector('[data-i18n="Sala dos Professores"]'),
                            document.querySelector('[data-i18n="Monitoramento de Chat"]')
                        ];
                        chatDivs.forEach(chatDiv => {
                            if (chatDiv) addNotificationBadge(chatDiv);
                        });
                    }
                }
            }
        }

        stompClient.connect({ 'Authorization': 'Bearer ' + token }, function(frame) {
            // Canal exclusivo do usuário (notas, frequências, chat, avisos específicos)
            stompClient.subscribe('/topic/user/' + userId + '/notifications', function(message) {
                processIncomingMessage(message);
            });

            // Canal global da instituição (comunicados gerais da direção)
            stompClient.subscribe('/topic/global/notifications', function(message) {
                processIncomingMessage(message);
            });
        }, function(error) {
            console.warn("Global Notification WebSocket Error:", error);
        });
    }

    initWebSocketNotifications();
})();
