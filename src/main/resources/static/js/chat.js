document.addEventListener("DOMContentLoaded", () => {
    const chatHistory = document.getElementById("chatHistory");
    const messageInput = document.getElementById("messageInput");
    const btnSend = document.getElementById("btnSend");
    
    const chatTitle = document.getElementById("chatTitle");
    const classroomSelect = document.getElementById("classroomSelect");
    const replyPreviewContainer = document.getElementById("replyPreviewContainer");
    const replyPreviewName = document.getElementById("replyPreviewName");
    const replyPreviewText = document.getElementById("replyPreviewText");
    const replyPreviewClose = document.getElementById("replyPreviewClose");
    
    const token = localStorage.getItem("jwt_token");
    const btnEmoji = document.getElementById("btnEmoji");
    const emojiPickerContainer = document.getElementById("emojiPickerContainer");
    const emojiPicker = document.querySelector('emoji-picker');

    if (btnEmoji && emojiPickerContainer) {
        btnEmoji.addEventListener("click", () => {
            emojiPickerContainer.style.display = emojiPickerContainer.style.display === "none" || emojiPickerContainer.style.display === "" ? "block" : "none";
        });
    }
    if (emojiPicker) {
        emojiPicker.addEventListener('emoji-click', event => {
            if (messageInput) {
                messageInput.value += event.detail.unicode;
                messageInput.focus();
            }
        });
    }
    document.addEventListener("click", (e) => {
        if (btnEmoji && emojiPickerContainer && !btnEmoji.contains(e.target) && !emojiPickerContainer.contains(e.target)) {
            emojiPickerContainer.style.display = "none";
        }
    });
    const userId = parseInt(localStorage.getItem("user_id"), 10);
    const userName = localStorage.getItem("user_name");

    if (!token) {
        window.location.href = "/login.html";
        return;
    }

    const payloadBase64 = token.split('.')[1];
    const decodedPayload = JSON.parse(atob(payloadBase64.replace(/-/g, '+').replace(/_/g, '/')));
    
    const payloadString = JSON.stringify(decodedPayload).toUpperCase();
    const isStudent = payloadString.includes("STUDENT");
    const isSecretary = payloadString.includes("SECRETARY") || payloadString.includes("PRINCIPAL");
    const isAdmin = isSecretary;

    let stompClient = null;
    let currentClassroomId = null;
    let currentSubscription = null;
    
    let currentReplyId = null;
    let currentEditId = null;

    if (replyPreviewClose) {
        replyPreviewClose.addEventListener("click", () => {
            clearActionState();
        });
    }

    function clearActionState() {
        currentReplyId = null;
        currentEditId = null;
        if (replyPreviewContainer) replyPreviewContainer.style.display = "none";
        if (messageInput) messageInput.value = "";
        if (btnSend) btnSend.innerHTML = "<i class='bx bxs-send'></i>";
    }

    const isTeacher = payloadString.includes("TEACHER");

    let isGlobalTeacherView = window.location.pathname.includes('/teacher/chat.html');

    if (isStudent) {
        fetch('/api/enrollments/student/' + userId, { headers: { 'Authorization': 'Bearer ' + token } })
        .then(res => { if (!res.ok) throw new Error("N&atilde;o autorizado"); return res.json(); })
        .then(enrollments => {
            const active = enrollments.find(e => e.active);
            if (active && active.classRoomId) {
                currentClassroomId = active.classRoomId;
                if (chatTitle) chatTitle.textContent = active.classRoomName || "Turma " + currentClassroomId;
                connectAndSubscribe();
            } else {
                showError("Acesso negado: Voc&ecirc; n&atilde;o possui matr&iacute;cula ativa.");
            }
        })
        .catch(err => showError("Erro ao buscar matr&iacute;cula."));
    } else if (isTeacher && isGlobalTeacherView) {
        currentClassroomId = 'TEACHERS';
        if (chatTitle) chatTitle.textContent = "Sala dos Professores";
        connectAndSubscribe();
    } else if (isSecretary) {
        fetch('/api/classrooms', { headers: { 'Authorization': 'Bearer ' + token } })
        .then(res => res.json())
        .then(classes => {
            if (classroomSelect) {
                classes.sort((a, b) => {
                    const nameA = a.name || "Turma " + a.id;
                    const nameB = b.name || "Turma " + b.id;
                    return nameA.localeCompare(nameB, undefined, {numeric: true, sensitivity: 'base'});
                }).forEach(c => {
                    const opt = document.createElement("option");
                    opt.value = c.id;
                    opt.textContent = c.name || "Turma " + c.id;
                    classroomSelect.appendChild(opt);
                });
                connect(false); 
                classroomSelect.addEventListener("change", (e) => {
                    const newId = e.target.value;
                    clearActionState();
                    if (newId) switchRoom(newId);
                    else {
                        if (currentSubscription) currentSubscription.unsubscribe();
                        currentClassroomId = null;
                        if(chatHistory) chatHistory.innerHTML = "<div class='text-center mt-4 text-muted'>Selecione uma turma para monitorar.</div>";
                    }
                });
            }
        }).catch(err => showError("Erro ao carregar turmas."));
    }

    function connect(autoSubscribe = true) {
        if (window.appStompClient && window.appStompClient.connected) {
            stompClient = window.appStompClient;
            setupSubscriptions(autoSubscribe);
            return;
        }

        const socket = new SockJS('/ws-chat');
        stompClient = Stomp.over(socket);
        stompClient.debug = () => {}; 
        window.appStompClient = stompClient;

        stompClient.connect({ 'Authorization': 'Bearer ' + token }, function(frame) {
            setupSubscriptions(autoSubscribe);
        }, function(error) {
            showError("Conex&atilde;o com o servidor de chat recusada ou perdida.");
        });
    }

    function setupSubscriptions(autoSubscribe) {
        if (autoSubscribe && currentClassroomId) subscribeToRoom(currentClassroomId);
        
        stompClient.subscribe('/user/queue/errors', function(messageOutput) {
            showBlockModal(messageOutput.body);
        });
    }

    function connectAndSubscribe() { connect(true); }

    function switchRoom(newClassroomId) {
        if (currentSubscription) currentSubscription.unsubscribe();
        currentClassroomId = newClassroomId;
        if(chatHistory) chatHistory.innerHTML = "<div class='text-center mt-4 text-muted'>Carregando mensagens...</div>";
        subscribeToRoom(currentClassroomId);
    }

    function subscribeToRoom(classroomId) {
        const topic = classroomId === 'TEACHERS' ? '/topic/teachers' : '/topic/classroom/' + classroomId;
        currentSubscription = stompClient.subscribe(topic, function(messageOutput) {
            const message = JSON.parse(messageOutput.body);
            handleIncomingMessage(message);
        }, { 'Authorization': 'Bearer ' + token });
        loadHistory();
    }

    function loadHistory() {
        if (!currentClassroomId) return;
        const historyUrl = currentClassroomId === 'TEACHERS' 
            ? '/api/chat/teachers/history' 
            : '/api/chat/classroom/' + currentClassroomId + '/history';
            
        fetch(historyUrl, { headers: { 'Authorization': 'Bearer ' + token } })
        .then(response => { if (!response.ok) throw new Error(); return response.json(); })
        .then(data => {
            if(!chatHistory) return;
            chatHistory.innerHTML = "";
            let messages = data.messages || [];
            let lastReadId = data.lastReadMessageId || 0;
            
            let insertedDivider = false;
            let dividerElement = null;

            messages.forEach(msg => {
                if (!insertedDivider && msg.id > lastReadId && lastReadId > 0) {
                    const divider = document.createElement("div");
                    divider.className = "unread-divider";
                    divider.innerHTML = "<span>Novas Mensagens</span>";
                    chatHistory.appendChild(divider);
                    dividerElement = divider;
                    insertedDivider = true;
                }
                appendMessage(msg, false, null, true);
            });
            
            if (dividerElement) {
                dividerElement.scrollIntoView({ behavior: 'smooth', block: 'center' });
            } else {
                scrollToBottom();
            }

            if (messages.length > 0) {
                const latestMsgId = Math.max(...messages.map(m => m.id));
                if (latestMsgId > lastReadId) {
                    const readUrl = currentClassroomId === 'TEACHERS'
                        ? '/api/chat/teachers/read?messageId=' + latestMsgId
                        : '/api/chat/read/' + currentClassroomId + '?messageId=' + latestMsgId;
                    fetch(readUrl, {
                        method: 'POST',
                        headers: { 'Authorization': 'Bearer ' + token }
                    });
                }
            }
        }).catch(() => {
            if(chatHistory) chatHistory.innerHTML = "<div class='text-center text-muted'>Erro ao carregar hist&oacute;rico.</div>";
        });
    }

    function handleIncomingMessage(message) {
        const existingDiv = document.getElementById('msg-' + message.id);
        if (existingDiv) {
            updateMessageDOM(existingDiv, message);
        } else {
            appendMessage(message);
            scrollToBottom();
        }
    }

    function appendMessage(message, isPending = false, tempId = null, isHistoryLoad = false) {
        if(!chatHistory) return;
        const isMine = message.senderId === userId;
        const msgDiv = document.createElement("div");
        msgDiv.className = "chat-message " + (isMine ? "sent" : "received");
        msgDiv.id = tempId ? tempId : 'msg-' + message.id;

        msgDiv.innerHTML = generateMessageInnerHtml(message, isMine, isPending);
        chatHistory.appendChild(msgDiv);

        if (!isPending) {
            attachMessageEvents(msgDiv, message, isMine);
        }
    }

    function updateMessageDOM(msgDiv, message) {
        const isMine = message.senderId === userId;
        msgDiv.innerHTML = generateMessageInnerHtml(message, isMine, false);
        attachMessageEvents(msgDiv, message, isMine);
    }

    function generateMessageInnerHtml(message, isMine, isPending) {
        const isMsgDeleted = message.isDeleted || message.deleted;
        const timeString = new Date(message.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
        
        let innerContent = message.content;
        if (isMsgDeleted) {
            if (message.deletedByAdmin) {
                innerContent = '<span class="text-danger" style="opacity: 0.8;"><i class="bx bx-block"></i> Esta mensagem foi apagada pela Dire&ccedil;&atilde;o/Secretaria.</span>';            } else {
                innerContent = '<i class="bx bx-block"></i> Esta mensagem foi apagada.';
            }
        }

        let badge = "";
        if (message.senderRole === "PRINCIPAL") badge = " <span class='badge bg-danger ms-1'>Dire&ccedil;&atilde;o</span>";
        else if (message.senderRole === "SECRETARY") badge = " <span class='badge bg-warning ms-1'>Secr.</span>";

        let statusHtml = "";
        if (isMine) {
            statusHtml = isPending ? "<i class='bx bx-time-five ms-1'></i>" : "<i class='bx bx-check ms-1'></i>";
        }

        const isMsgEdited = message.isEdited || message.edited;
        let editedText = isMsgEdited && !isMsgDeleted ? "<small class='me-1' style='opacity: 0.8;'>Editada</small>" : "";

        let replyHtml = "";
        if (message.repliedToId && !isMsgDeleted) {
            const authorColor = isMine ? "#ffffff" : "#696cff";
            replyHtml = '<div class="replied-box"><strong style="color: ' + authorColor + ';">' + message.repliedToSenderName + '</strong><div style="opacity: 0.9; font-size: 12px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 250px;">' + message.repliedToContent + '</div></div>';
        }

        let actionsHtml = "";
        if (!isPending) {
            actionsHtml = '<div class="message-actions">';
            if (!isMsgDeleted) {
                actionsHtml += '<button class="action-btn btn-reply" title="Responder"><i class="bx bx-reply"></i></button>';
                if (isMine) {
                    actionsHtml += '<button class="action-btn btn-edit" title="Editar"><i class="bx bx-edit"></i></button>';
                }
                if (isMine || isAdmin) {
                    actionsHtml += '<button class="action-btn btn-delete" title="Apagar para Todos"><i class="bx bx-trash"></i></button>';
                }
            }
            actionsHtml += '<button class="action-btn btn-hide" title="Apagar para mim"><i class="bx bx-hide"></i></button></div>';
        }

        return actionsHtml + '<span class="author">' + message.senderName + badge + '</span><div class="msg-content-wrapper">' + replyHtml + '<span style="padding-right: 8px; word-break: break-word;">' + innerContent + '</span><div class="time-container">' + editedText + '<span class="time-text">' + timeString + '</span>' + statusHtml + '</div></div>';
    }

    function attachMessageEvents(msgDiv, message, isMine) {
        const isMsgDeleted = message.isDeleted || message.deleted;
        const btnHide = msgDiv.querySelector('.btn-hide');
        if (btnHide) {
            btnHide.addEventListener('click', () => {
                fetch('/api/chat/messages/' + message.id + '/hide', {
                    method: 'POST',
                    headers: { 'Authorization': 'Bearer ' + token }
                }).then(() => {
                    msgDiv.remove();
                });
            });
        }

        if (isMsgDeleted) return;

        const btnReply = msgDiv.querySelector('.btn-reply');
        const btnEdit = msgDiv.querySelector('.btn-edit');
        const btnDelete = msgDiv.querySelector('.btn-delete');

        if (btnReply) {
            btnReply.addEventListener('click', () => {
                currentEditId = null;
                currentReplyId = message.id;
                if (replyPreviewContainer) {
                    replyPreviewContainer.style.display = "flex";
                    if(replyPreviewName) replyPreviewName.textContent = "Respondendo a " + message.senderName;
                    if(replyPreviewText) replyPreviewText.textContent = message.content;
                }
                if (messageInput) {
                    messageInput.focus();
                    if(btnSend) btnSend.innerHTML = "<i class='bx bxs-send'></i>";
                }
            });
        }

        if (btnEdit) {
            btnEdit.addEventListener('click', () => {
                currentReplyId = null;
                currentEditId = message.id;
                if (replyPreviewContainer) {
                    replyPreviewContainer.style.display = "flex";
                    if(replyPreviewName) replyPreviewName.textContent = "Editando mensagem";
                    if(replyPreviewText) replyPreviewText.textContent = message.content;
                }
                if (messageInput) {
                    messageInput.value = message.content;
                    messageInput.focus();
                    if(btnSend) btnSend.innerHTML = "<i class='bx bx-check'></i>";
                }
            });
        }

        if (btnDelete) {
            btnDelete.addEventListener('click', () => {
                showDeleteModal(message.id);
            });
        }
    }

    function submitMessageAction() {
        if (!currentClassroomId || !stompClient) return;
        
        const content = messageInput.value.trim();
        if (!content) return;

        if (currentEditId) {
            const editRequest = { content: content };
            const editDest = currentClassroomId === 'TEACHERS'
                ? '/app/teachers/edit/' + currentEditId
                : '/app/classroom/' + currentClassroomId + '/edit/' + currentEditId;
            stompClient.send(editDest, {}, JSON.stringify(editRequest));
        } else {
            const chatMessageRequest = { 
                content: content,
                repliedToId: currentReplyId
            };
            const sendDest = currentClassroomId === 'TEACHERS'
                ? '/app/teachers/send'
                : '/app/classroom/' + currentClassroomId + '/send';
            stompClient.send(sendDest, {}, JSON.stringify(chatMessageRequest));
        }

        messageInput.value = "";
        clearActionState();
        if (btnSend) btnSend.disabled = true;
        setTimeout(() => { if (btnSend) btnSend.disabled = false; }, 500);
    }

    window.loadMembers = function() {
        if (!currentClassroomId) return;
        const membersList = document.getElementById('membersList');
        if(membersList) membersList.innerHTML = '<li class="list-group-item text-center text-muted">Carregando...</li>';

        const offcanvasLabel = document.getElementById('offcanvasMembersLabel');
        if (offcanvasLabel) {
            offcanvasLabel.textContent = currentClassroomId === 'TEACHERS' ? 'Membros do Corpo Docente' : 'Membros da Turma';
        }

        const membersUrl = currentClassroomId === 'TEACHERS' 
            ? '/api/chat/teachers/members' 
            : '/api/chat/classrooms/' + currentClassroomId + '/members';

        fetch(membersUrl, {
            headers: { 'Authorization': 'Bearer ' + token }
        })
        .then(response => response.json())
        .then(members => {
            if(!membersList) return;
            membersList.innerHTML = "";
            members.forEach(member => {
                const li = document.createElement("li");
                li.className = "list-group-item d-flex justify-content-between align-items-center";
                
                let infoHtml = '<div><strong>' + member.name + '</strong>';
                if (currentClassroomId === 'TEACHERS' && member.email) {
                    infoHtml += '<br><small class="text-muted">' + member.email + '</small>';
                } else if (member.registrationNumber) {
                    infoHtml += '<br><small class="text-muted">Matr&iacute;cula: ' + member.registrationNumber + '</small>';
                }
                infoHtml += '</div>';

                let actionHtml = "";
                if (currentClassroomId !== 'TEACHERS' && isAdmin && member.registrationNumber) {
                    const btnClass = member.isBlocked ? "btn-success" : "btn-warning";
                    const btnIcon = member.isBlocked ? "bx-check-circle" : "bx-block";
                    const btnTitle = member.isBlocked ? "Desbloquear Chat" : "Bloquear Chat";
                    
                    actionHtml = '<button class="btn btn-sm ' + btnClass + '" onclick="toggleBlock(' + member.id + ')" title="' + btnTitle + '"><i class="bx ' + btnIcon + '"></i></button>';
                }

                li.innerHTML = infoHtml + actionHtml;
                membersList.appendChild(li);
            });
        }).catch(err => {
            if(membersList) membersList.innerHTML = '<li class="list-group-item text-center text-danger">Erro ao carregar membros.</li>';
        });
    };

    window.toggleBlock = function(studentId) {
        fetch('/api/chat/users/' + studentId + '/block', {
            method: 'PUT',
            headers: { 'Authorization': 'Bearer ' + token }
        }).then(response => {
            if (response.ok) {
                window.loadMembers();
            }
        });
    };

    function scrollToBottom() {
        setTimeout(() => {
            if(chatHistory) chatHistory.scrollTop = chatHistory.scrollHeight;
        }, 100);
    }

    function showError(msg) {
        if(chatHistory) chatHistory.innerHTML = "<div class='text-center text-danger mt-4'>" + msg + "</div>";
    }

    if (btnSend) {
        btnSend.addEventListener("click", submitMessageAction);
    }
    if (messageInput) {
        messageInput.addEventListener("keypress", (e) => {
            if (e.key === 'Enter') submitMessageAction();
        });
    }

    function showBlockModal(msg) {
        let modalEl = document.getElementById('blockWarningModal');
        if (!modalEl) {
            const html = '<div class="modal fade" id="blockWarningModal" tabindex="-1" aria-hidden="true">' +
                '<div class="modal-dialog modal-dialog-centered">' +
                    '<div class="modal-content">' +
                        '<div class="modal-header bg-warning">' +
                            '<h5 class="modal-title text-white"><i class="bx bx-error-circle"></i> A&ccedil;&atilde;o Bloqueada</h5>' +
                            '<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>' +
                        '</div>' +
                        '<div class="modal-body text-center py-4">' +
                            '<i class="bx bx-lock-alt text-warning" style="font-size: 4rem; margin-bottom: 15px;"></i>' +
                            '<h5 class="mb-2">Acesso Restrito</h5>' +
                            '<p class="text-muted mb-0" id="blockWarningText"></p>' +
                        '</div>' +
                        '<div class="modal-footer justify-content-center">' +
                            '<button type="button" class="btn btn-warning" data-bs-dismiss="modal">Entendi</button>' +
                        '</div>' +
                    '</div>' +
                '</div>' +
            '</div>';
            document.body.insertAdjacentHTML('beforeend', html);
            modalEl = document.getElementById('blockWarningModal');
        }
        document.getElementById('blockWarningText').textContent = msg;
        const bsModal = new bootstrap.Modal(modalEl);
        bsModal.show();
    }

    function showDeleteModal(msgId) {
        let modalEl = document.getElementById('deleteWarningModal');
        if (!modalEl) {
            const html = '<div class="modal fade" id="deleteWarningModal" tabindex="-1" aria-hidden="true">' +
                '<div class="modal-dialog modal-dialog-centered">' +
                    '<div class="modal-content">' +
                        '<div class="modal-header bg-danger">' +
                            '<h5 class="modal-title text-white"><i class="bx bx-trash"></i> Apagar Mensagem</h5>' +
                            '<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>' +
                        '</div>' +
                        '<div class="modal-body text-center py-4">' +
                            '<h5 class="mb-2">Tem certeza?</h5>' +
                            '<p class="text-muted mb-0">Esta mensagem ser&aacute; apagada para todos.</p>' +
                        '</div>' +
                        '<div class="modal-footer justify-content-center">' +
                            '<button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>' +
                            '<button type="button" class="btn btn-danger" id="btnConfirmDelete">Apagar</button>' +
                        '</div>' +
                    '</div>' +
                '</div>' +
            '</div>';
            document.body.insertAdjacentHTML('beforeend', html);
            modalEl = document.getElementById('deleteWarningModal');
            document.getElementById('btnConfirmDelete').addEventListener('click', () => {
                if (messageToDeleteId && stompClient && currentClassroomId) {
                    const deleteDest = currentClassroomId === 'TEACHERS'
                        ? '/app/teachers/delete/' + messageToDeleteId
                        : '/app/classroom/' + currentClassroomId + '/delete/' + messageToDeleteId;
                    stompClient.send(deleteDest, {}, "");
                }
                const bsModal = bootstrap.Modal.getInstance(modalEl);
                bsModal.hide();
            });
        }
        messageToDeleteId = msgId;
        const bsModal = new bootstrap.Modal(modalEl);
        bsModal.show();
    }
});

