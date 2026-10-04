document.addEventListener("DOMContentLoaded", () => {
    const chatHistory = document.getElementById("chatHistory");
    const messageInput = document.getElementById("messageInput");
    const btnSend = document.getElementById("btnSend");
    
    const token = localStorage.getItem("jwt_token");
    if (!token) {
        window.location.href = "/login.html";
        return;
    }

    let stompClient = null;
    let userId = localStorage.getItem("user_id") || "anonymous";
    let conversationIdKey = "chat_conversation_id_" + userId;
    let conversationId = localStorage.getItem(conversationIdKey);
    if (!conversationId) {
        conversationId = "conv-" + userId + "-" + Date.now();
        localStorage.setItem(conversationIdKey, conversationId);
    }
    let pendingRequests = 0;

    function connect() {
        const socket = new SockJS('/ws-chat');
        stompClient = Stomp.over(socket);
        stompClient.debug = () => {}; 

        stompClient.connect({ 'Authorization': 'Bearer ' + token }, function(frame) {
            stompClient.subscribe('/user/queue/ai/reply', function(messageOutput) {
                const response = JSON.parse(messageOutput.body);
                if (response.conversationId && response.conversationId !== conversationId) {
                    return;
                }
                removeTypingIndicator();
                appendMessage(response.reply, false);
            }, { 'Authorization': 'Bearer ' + token });
            
            // Welcome message moved to history loading
        }, function(error) {
            forceRemoveTypingIndicator();
            showError("Conexão com o servidor de chat recusada ou perdida.");
        });
    }

    let messageIndexCounter = 0;

    async function loadHistoryAndConnect() {
        chatHistory.innerHTML = "";
        messageIndexCounter = 0;
        try {
            const response = await fetch(`/api/chat/history?conversationId=${conversationId}`, {
                headers: {
                    'Authorization': 'Bearer ' + token
                }
            });
            if (response.ok) {
                const history = await response.json();
                if (history.length > 0) {
                    history.forEach(msg => {
                        appendMessage(msg.content, msg.role === 'USER', true);
                    });
                } else {
                    appendMessage("Olá! Sou o Assistente Virtual EscolaIA. Como posso ajudar você hoje?", false, false);
                }
            } else {
                appendMessage("Olá! Sou o Assistente Virtual EscolaIA. Como posso ajudar você hoje?", false, false);
            }
        } catch (e) {
            console.error("Error fetching history:", e);
            appendMessage("Olá! Sou o Assistente Virtual EscolaIA. Como posso ajudar você hoje?", false, false);
        }
        
        connect();
    }

    function forceRemoveTypingIndicator() {
        pendingRequests = 0;
        const indicator = document.getElementById("typingIndicator");
        if (indicator) {
            indicator.remove();
        }
    }

    function removeTypingIndicator() {
        if (pendingRequests > 0) {
            pendingRequests--;
        }
        if (pendingRequests === 0) {
            const indicator = document.getElementById("typingIndicator");
            if (indicator) {
                indicator.remove();
            }
        }
    }

    function showTypingIndicator() {
        if (!chatHistory) return;
        
        pendingRequests++;
        if (pendingRequests === 1) {
            const indicatorDiv = document.createElement("div");
            indicatorDiv.id = "typingIndicator";
            indicatorDiv.className = "typing-indicator-wrapper";
            indicatorDiv.innerHTML = '<div class="dot"></div><div class="dot"></div><div class="dot"></div>';
            
            chatHistory.appendChild(indicatorDiv);
            scrollToBottom();
        } else {
            const indicatorDiv = document.getElementById("typingIndicator");
            if (indicatorDiv) {
                chatHistory.appendChild(indicatorDiv);
                scrollToBottom();
            }
        }
    }

    function appendMessage(text, isMine, isRealMessage = true) {
        if (!chatHistory) return;
        
        const msgDiv = document.createElement("div");
        msgDiv.className = "chat-message " + (isMine ? "sent" : "received");
        
        let msgIndex = -1;
        if (isRealMessage) {
            msgIndex = messageIndexCounter++;
            msgDiv.setAttribute("data-index", msgIndex);
        }
        
        const timeString = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
        
        let authorHtml = isMine ? "" : '<span class="author">EscolaIA <span class="badge bg-primary ms-1">IA</span></span>';
        
        let formattedText = text
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;");
        
        msgDiv.innerHTML = authorHtml + 
            '<div class="msg-content-wrapper">' + 
                '<span class="msg-text" style="padding-right: 8px; word-break: break-word; white-space: pre-wrap;">' + formattedText + '</span>' + 
                '<div class="time-container">' + 
                    '<span class="time-text">' + timeString + '</span>' + 
                '</div>' + 
            '</div>';

        if (isMine && isRealMessage) {
            const editBtn = document.createElement("div");
            editBtn.className = "edit-btn";
            editBtn.innerHTML = "<i class='bx bx-pencil'></i>";
            editBtn.onclick = () => enableEditMode(msgDiv, text, msgIndex);
            msgDiv.appendChild(editBtn);
        }
            
        chatHistory.appendChild(msgDiv);

        if (window.renderMathInElement) {
            renderMathInElement(msgDiv, {
                delimiters: [
                    {left: '$$', right: '$$', display: true},
                    {left: '$', right: '$', display: false},
                    {left: '\\(', right: '\\)', display: false},
                    {left: '\\[', right: '\\]', display: true}
                ],
                throwOnError: false
            });
        }

        if (!isMine) {
            const msgTextSpan = msgDiv.querySelector('.msg-text');
            if (msgTextSpan) {
                applyMarkdownToTextNodes(msgTextSpan);
            }
        }

        scrollToBottom();
    }

    function applyMarkdownToTextNodes(node) {
        if (node.nodeType === Node.ELEMENT_NODE) {
            if (node.classList.contains('katex') || node.classList.contains('katex-display')) {
                return;
            }
            Array.from(node.childNodes).forEach(applyMarkdownToTextNodes);
        } else if (node.nodeType === Node.TEXT_NODE) {
            let originalText = node.nodeValue;
            let newText = originalText
                .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
                .replace(/\*(.*?)\*/g, '<em>$1</em>');
                
            if (newText !== originalText) {
                let wrapper = document.createElement('span');
                wrapper.innerHTML = newText;
                while (wrapper.firstChild) {
                    node.parentNode.insertBefore(wrapper.firstChild, node);
                }
                node.parentNode.removeChild(node);
            }
        }
    }

    function enableEditMode(msgDiv, originalText, msgIndex) {
        const wrapper = msgDiv.querySelector('.msg-content-wrapper');
        const oldHtml = wrapper.innerHTML;
        
        const editBtn = msgDiv.querySelector('.edit-btn');
        if (editBtn) editBtn.style.display = 'none';
        
        wrapper.innerHTML = `
            <textarea class="edit-area" rows="3"></textarea>
            <div class="edit-actions">
                <button class="btn-cancel-edit">Cancelar</button>
                <button class="btn-save-edit">Salvar & Enviar</button>
            </div>
        `;
        wrapper.querySelector('.edit-area').value = originalText;
        
        msgDiv.querySelector('.btn-cancel-edit').onclick = () => {
            wrapper.innerHTML = oldHtml;
            if (editBtn) editBtn.style.display = '';
        };
        
        msgDiv.querySelector('.btn-save-edit').onclick = async () => {
            const newText = msgDiv.querySelector('.edit-area').value.trim();
            if (!newText) return;
            
            try {
                const response = await fetch(`/api/chat/history/${conversationId}/after/${msgIndex}`, {
                    method: 'DELETE',
                    headers: { 'Authorization': 'Bearer ' + token }
                });
                
                if (response.ok) {
                    const allMessages = Array.from(chatHistory.querySelectorAll('.chat-message'));
                    allMessages.forEach(msg => {
                        const idxAttr = msg.getAttribute('data-index');
                        if (idxAttr !== null) {
                            const idx = parseInt(idxAttr, 10);
                            if (idx >= msgIndex) {
                                msg.remove();
                            }
                        }
                    });
                    
                    messageIndexCounter = msgIndex;
                    
                    messageInput.value = newText;
                    sendMessage();
                } else {
                    showError("Erro ao editar a mensagem.");
                }
            } catch (e) {
                console.error(e);
                showError("Erro ao editar a mensagem.");
            }
        };
    }

    function sendMessage() {
        const content = messageInput.value.trim();
        if (!content || !stompClient) return;

        // Display my message
        appendMessage(content, true);

        // Send to backend
        const request = { 
            message: content,
            conversationId: conversationId
        };
        stompClient.send('/app/ai/chat', {}, JSON.stringify(request));

        messageInput.value = "";
        
        showTypingIndicator();
        
        if (btnSend) {
            btnSend.disabled = true;
            setTimeout(() => { btnSend.disabled = false; }, 500);
        }
    }

    function scrollToBottom() {
        setTimeout(() => {
            if (chatHistory) chatHistory.scrollTop = chatHistory.scrollHeight;
        }, 100);
    }

    function showError(msg) {
        if (chatHistory) {
            const errorDiv = document.createElement("div");
            errorDiv.className = "text-center text-danger mt-4";
            errorDiv.textContent = msg;
            chatHistory.appendChild(errorDiv);
        }
    }

    const btnNovoChat = document.getElementById("btnNovoChat");
    if (btnNovoChat) {
        btnNovoChat.addEventListener("click", () => {
            conversationId = "conv-" + userId + "-" + Date.now();
            localStorage.setItem(conversationIdKey, conversationId);
            
            chatHistory.innerHTML = "";
            messageIndexCounter = 0;
            pendingRequests = 0;
            appendMessage("Olá! Sou o Assistente Virtual EscolaIA. Como posso ajudar você hoje?", false, false);
            
            if (stompClient) {
                stompClient.disconnect(() => {
                    connect();
                });
            }
        });
    }

    if (btnSend) {
        btnSend.addEventListener("click", sendMessage);
    }
    if (messageInput) {
        messageInput.addEventListener("keypress", (e) => {
            if (e.key === 'Enter') sendMessage();
        });
    }

    // Initialize connection
    loadHistoryAndConnect();
});
