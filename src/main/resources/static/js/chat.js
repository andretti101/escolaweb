document.addEventListener("DOMContentLoaded", () => {
    const chatHistory = document.getElementById("chatHistory");
    const messageInput = document.getElementById("messageInput");
    const btnSend = document.getElementById("btnSend");
    
    // Elementos condicionais (presentes apenas na view específica)
    const chatTitle = document.getElementById("chatTitle"); // Aluno
    const classroomSelect = document.getElementById("classroomSelect"); // Secretaria

    // Dados de autenticação do localStorage
    const token = localStorage.getItem("jwt_token");
    const userId = parseInt(localStorage.getItem("user_id"), 10);
    const userName = localStorage.getItem("user_name");

    if (!token) {
        window.location.href = "/login.html";
        return;
    }

    // Identifica a Role através do Payload do JWT
    const payloadBase64 = token.split('.')[1];
    const decodedPayload = JSON.parse(atob(payloadBase64.replace(/-/g, '+').replace(/_/g, '/')));
    const role = String(decodedPayload.role || "").toUpperCase();
    
    const isStudent = role.includes("STUDENT");
    const isSecretary = role.includes("SECRETARY") || role.includes("PRINCIPAL");

    let stompClient = null;
    let currentSubscription = null;
    let currentClassroomId = null;

    // ----- LÓGICA DE INICIALIZAÇÃO BASEADA NO PERFIL ----- //

    if (isStudent) {
        // Fluxo Aluno: Encontrar a turma ativa do estudante automaticamente
        fetch(`/api/enrollments/student/${userId}`, {
            headers: { 'Authorization': 'Bearer ' + token }
        })
        .then(res => {
            if (!res.ok) throw new Error("Não autorizado");
            return res.json();
        })
        .then(enrollments => {
            const active = enrollments.find(e => e.active);
            if (active && active.classRoomId) {
                currentClassroomId = active.classRoomId;
                if (chatTitle) chatTitle.textContent = active.classRoomName || `Turma ${currentClassroomId}`;
                connectAndSubscribe(); // Conecta direto
            } else {
                showError("Acesso negado: Você não possui matrícula ativa em nenhuma turma.");
            }
        })
        .catch(err => showError("Erro ao buscar matrícula do aluno."));

    } else if (isSecretary) {
        // Fluxo Secretaria: Preencher a lista de turmas no dropdown
        fetch(`/api/classrooms`, {
            headers: { 'Authorization': 'Bearer ' + token }
        })
        .then(res => res.json())
        .then(classes => {
            if (classroomSelect) {
                classes.forEach(c => {
                    const opt = document.createElement("option");
                    opt.value = c.id;
                    opt.textContent = c.name || `Turma ${c.id}`;
                    classroomSelect.appendChild(opt);
                });

                // Conecta o STOMP assim que carregar, mas não assina nada ainda
                connect(false); 

                // Event listener para quando a secretaria trocar de turma no dropdown
                classroomSelect.addEventListener("change", (e) => {
                    const newId = e.target.value;
                    if (newId) {
                        switchRoom(newId);
                    } else {
                        // Desconectou ou voltou para a opção default vazia
                        if (currentSubscription) currentSubscription.unsubscribe();
                        currentClassroomId = null;
                        chatHistory.innerHTML = "<div class='text-center mt-4 text-muted'>Selecione uma turma para monitorar.</div>";
                    }
                });
            }
        })
        .catch(err => showError("Erro ao carregar lista de turmas."));
    } else {
        showError("Acesso não autorizado. Apenas Alunos ou Secretaria/Diretoria.");
    }

    // ----- FUNÇÕES DE CONEXÃO E COMUNICAÇÃO STOMP ----- //

    function connect(autoSubscribe = true) {
        const socket = new SockJS('/ws-chat');
        stompClient = Stomp.over(socket);
        stompClient.debug = () => {}; // Desliga log excessivo

        const headers = { 'Authorization': 'Bearer ' + token };

        stompClient.connect(headers, function(frame) {
            console.log('Conectado ao STOMP: ' + frame);
            if (autoSubscribe && currentClassroomId) {
                subscribeToRoom(currentClassroomId);
            }
        }, function(error) {
            console.error(error);
            showError("Conexão com o servidor de chat recusada ou perdida.");
        });
    }

    function connectAndSubscribe() {
        connect(true);
    }

    // Usado exclusivamente pela Diretoria/Secretaria para pular de chat em chat
    function switchRoom(newClassroomId) {
        if (currentSubscription) {
            currentSubscription.unsubscribe();
        }
        currentClassroomId = newClassroomId;
        chatHistory.innerHTML = "<div class='text-center mt-4 text-muted'>Carregando mensagens...</div>";
        subscribeToRoom(currentClassroomId);
    }

    // Inscreve no tópico desejado e dispara carregamento do histórico
    function subscribeToRoom(classroomId) {
        const headers = { 'Authorization': 'Bearer ' + token };
        
        currentSubscription = stompClient.subscribe(`/topic/classroom/${classroomId}`, function(messageOutput) {
            const message = JSON.parse(messageOutput.body);
            appendMessage(message);
        }, headers);

        loadHistory();
    }

    // Traz o histórico do REST e desenha na tela
    function loadHistory() {
        if (!currentClassroomId) return;
        
        fetch(`/api/chat/classroom/${currentClassroomId}/history`, {
            headers: { 'Authorization': 'Bearer ' + token }
        })
        .then(response => {
            if (!response.ok) throw new Error("Erro de acesso");
            return response.json();
        })
        .then(messages => {
            chatHistory.innerHTML = "";
            messages.forEach(msg => appendMessage(msg));
            scrollToBottom();
        })
        .catch(err => {
            chatHistory.innerHTML = "<div class='text-center text-muted'>Não foi possível carregar o histórico de mensagens desta turma.</div>";
        });
    }

    // Função de envio
    function sendMessage() {
        if (!currentClassroomId || !stompClient) return;
        
        const content = messageInput.value.trim();
        if (content) {
            const chatMessageRequest = { content: content };
            stompClient.send(`/app/classroom/${currentClassroomId}/send`, {}, JSON.stringify(chatMessageRequest));
            messageInput.value = '';
        }
    }

    // Renderiza cada balão
    function appendMessage(message) {
        const isMine = message.senderId === userId;
        const msgDiv = document.createElement("div");
        msgDiv.classList.add("chat-message");
        msgDiv.classList.add(isMine ? "sent" : "received");

        const date = new Date(message.timestamp);
        const timeString = date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

        msgDiv.innerHTML = `
            <span class="author">${message.senderName}</span>
            ${message.content}
            <span class="time">${timeString}</span>
        `;
        
        chatHistory.appendChild(msgDiv);
        scrollToBottom();
    }

    // ----- UTILITÁRIOS ----- //

    function scrollToBottom() {
        chatHistory.scrollTop = chatHistory.scrollHeight;
    }

    function showError(msg) {
        chatHistory.innerHTML = `<div class='text-center text-danger mt-4'>${msg}</div>`;
        if (messageInput) messageInput.disabled = true;
        if (btnSend) btnSend.disabled = true;
    }

    // Escuta eventos no input de enviar
    if (btnSend) {
        btnSend.addEventListener("click", sendMessage);
    }
    if (messageInput) {
        messageInput.addEventListener("keypress", (e) => {
            if (e.key === 'Enter') sendMessage();
        });
    }
});
