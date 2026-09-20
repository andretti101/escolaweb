(function() {
    const token = localStorage.getItem("jwt_token");
    const userIdStr = localStorage.getItem("user_id");

    if (!token || !userIdStr) return;
    const userId = parseInt(userIdStr, 10);

    if (window.appStompClient) return; // Evita múltiplas conexões

    const socket = new SockJS('/ws-chat');
    const stompClient = Stomp.over(socket);
    stompClient.debug = null; // Desativa logs no console para não poluir

    window.appStompClient = stompClient;

    stompClient.connect({ 'Authorization': 'Bearer ' + token }, function(frame) {
        // Se inscreve no canal exclusivo do usuário para receber notificações em tempo real
        stompClient.subscribe('/topic/user/' + userId + '/notifications', function(message) {
            if (message.body && message.body.includes("NEW_CHAT_MESSAGE")) {
                // Notificação recebida em tempo real!
                
                // Se o usuário não está na tela de chat
                if (!window.location.pathname.includes('chat.html')) {
                    const chatDiv = document.querySelector('[data-i18n="Chat da Turma"]');
                    if (chatDiv) {
                        // Injeta o selo azul apenas se já não existir
                        if (!chatDiv.innerHTML.includes('background-color:#696cff')) {
                            chatDiv.innerHTML += ' <span style="display:inline-block; width:8px; height:8px; background-color:#696cff; border-radius:50%; margin-left:8px; vertical-align:middle;"></span>';
                        }
                    }
                }
            }
        });
    }, function(error) {
        console.warn("Global Notification WebSocket Error:", error);
    });
})();
