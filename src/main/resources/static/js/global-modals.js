// ==============================================================================
// Gerenciador de Modais Globais Assíncronos (Alert / Confirm)
// Substitui os alert() e confirm() nativos do navegador por diálogos estilizados baseados em Promises
// ==============================================================================

(function (window) {
    'use strict';

    let modalCounter = 0;

    // Modal de confirmação assíncrono (retorna Promise<boolean>)
    window.showConfirmModal = function (title, message, onConfirm) {
        return new Promise((resolve) => {
            modalCounter++;
            const modalId = 'customGlobalConfirmModal_' + modalCounter;
            const html = `
            <div class="modal fade" id="${modalId}" tabindex="-1" aria-hidden="true" data-bs-backdrop="static">
                <div class="modal-dialog modal-sm modal-dialog-centered" role="document">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title"></h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                        </div>
                        <div class="modal-body">
                            <p class="modal-message" style="white-space: pre-wrap;"></p>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-outline-secondary btn-cancel" data-bs-dismiss="modal">Cancelar</button>
                            <button type="button" class="btn btn-primary btn-confirm">Confirmar</button>
                        </div>
                    </div>
                </div>
            </div>`;
            document.body.insertAdjacentHTML('beforeend', html);
            const modalEl = document.getElementById(modalId);
            
            const cleanTitle = (title && String(title).trim()) ? String(title).trim() : 'Atenção';
            let cleanMsg = (message != null && String(message).trim()) ? String(message).trim() : 'Deseja prosseguir com esta ação?';
            cleanMsg = cleanMsg.replace(/<br\s*[\/]?>/gi, '\n');
            modalEl.querySelector('.modal-title').textContent = cleanTitle;
            modalEl.querySelector('.modal-message').textContent = cleanMsg;

            const confirmBtn = modalEl.querySelector('.btn-confirm');
            const cancelBtn = modalEl.querySelector('.btn-cancel');
            const closeBtn = modalEl.querySelector('.btn-close');

            const modal = new bootstrap.Modal(modalEl);

            let resolved = false;

            const executeConfirm = () => {
                if (!resolved) {
                    resolved = true;
                    modal.hide();
                    resolve(true);
                    if (typeof onConfirm === 'function') {
                        onConfirm();
                    }
                }
            };

            const executeCancel = () => {
                if (!resolved) {
                    resolved = true;
                    modal.hide();
                    resolve(false);
                }
            };

            confirmBtn.addEventListener('click', executeConfirm);
            cancelBtn.addEventListener('click', executeCancel);
            closeBtn.addEventListener('click', executeCancel);

            modalEl.addEventListener('hidden.bs.modal', function () {
                if (!resolved) {
                    resolved = true;
                    resolve(false);
                }
                modalEl.remove();
            });

            modal.show();
        });
    };

    window.showAlertModal = function (title, message) {
        return new Promise((resolve) => {
            modalCounter++;
            const modalId = 'customGlobalAlertModal_' + modalCounter;
            const html = `
            <div class="modal fade" id="${modalId}" tabindex="-1" aria-hidden="true" data-bs-backdrop="static">
                <div class="modal-dialog modal-sm modal-dialog-centered" role="document">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title"></h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                        </div>
                        <div class="modal-body">
                            <p class="modal-message" style="white-space: pre-wrap;"></p>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-primary btn-ok">OK</button>
                        </div>
                    </div>
                </div>
            </div>`;
            document.body.insertAdjacentHTML('beforeend', html);
            const modalEl = document.getElementById(modalId);
            
            const cleanTitle = (title && String(title).trim()) ? String(title).trim() : 'Aviso';
            let cleanMsg = (message != null && String(message).trim()) ? String(message).trim() : 'Ocorreu um erro ao processar a solicitação.';
            cleanMsg = cleanMsg.replace(/<br\s*[\/]?>/gi, '\n');
            modalEl.querySelector('.modal-title').textContent = cleanTitle;
            modalEl.querySelector('.modal-message').textContent = cleanMsg;

            const okBtn = modalEl.querySelector('.btn-ok');
            const closeBtn = modalEl.querySelector('.btn-close');

            const modal = new bootstrap.Modal(modalEl);

            let resolved = false;

            const onClose = () => {
                if (!resolved) {
                    resolved = true;
                    modal.hide();
                    resolve(true);
                }
            };

            okBtn.addEventListener('click', onClose);
            closeBtn.addEventListener('click', onClose);

            modalEl.addEventListener('hidden.bs.modal', function () {
                if (!resolved) {
                    resolved = true;
                    resolve(true);
                }
                modalEl.remove();
            });

            modal.show();
        });
    };

    window.showPromptModal = function (title, message, defaultValue = '') {
        return new Promise((resolve) => {
            modalCounter++;
            const modalId = 'customGlobalPromptModal_' + modalCounter;
            const html = `
            <div class="modal fade" id="${modalId}" tabindex="-1" aria-hidden="true" data-bs-backdrop="static">
                <div class="modal-dialog modal-sm modal-dialog-centered" role="document">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title"></h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                        </div>
                        <div class="modal-body">
                            <p class="modal-message" style="white-space: pre-wrap; margin-bottom: 0.5rem;"></p>
                            <input type="text" class="form-control modal-input" />
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-outline-secondary btn-cancel" data-bs-dismiss="modal">Cancelar</button>
                            <button type="button" class="btn btn-primary btn-confirm">Confirmar</button>
                        </div>
                    </div>
                </div>
            </div>`;
            document.body.insertAdjacentHTML('beforeend', html);
            const modalEl = document.getElementById(modalId);
            
            modalEl.querySelector('.modal-title').textContent = title;
            modalEl.querySelector('.modal-message').textContent = message;
            const inputEl = modalEl.querySelector('.modal-input');
            inputEl.value = defaultValue;

            const confirmBtn = modalEl.querySelector('.btn-confirm');
            const cancelBtn = modalEl.querySelector('.btn-cancel');
            const closeBtn = modalEl.querySelector('.btn-close');

            const modal = new bootstrap.Modal(modalEl);

            let resolved = false;

            const onConfirm = () => {
                if (!resolved) {
                    resolved = true;
                    modal.hide();
                    resolve(inputEl.value);
                }
            };

            const onCancel = () => {
                if (!resolved) {
                    resolved = true;
                    modal.hide();
                    resolve(null);
                }
            };

            confirmBtn.addEventListener('click', onConfirm);
            cancelBtn.addEventListener('click', onCancel);
            closeBtn.addEventListener('click', onCancel);

            modalEl.addEventListener('hidden.bs.modal', function () {
                if (!resolved) {
                    resolved = true;
                    resolve(null);
                }
                modalEl.remove();
            });

            modalEl.addEventListener('shown.bs.modal', function () {
                inputEl.focus();
            });

            modal.show();
        });
    };

    function isTechnicalJargon(str) {
        if (!str || typeof str !== 'string') return false;
        const lower = str.trim().toLowerCase();
        return (
            lower === 'bad request' ||
            lower === 'unauthorized' ||
            lower === 'forbidden' ||
            lower === 'not found' ||
            lower === 'method not allowed' ||
            lower === 'not acceptable' ||
            lower === 'conflict' ||
            lower === 'unsupported media type' ||
            lower === 'unprocessable entity' ||
            lower === 'internal server error' ||
            lower === 'bad gateway' ||
            lower === 'service unavailable' ||
            lower === 'gateway timeout' ||
            lower === 'failed to fetch' ||
            lower.includes('networkerror') ||
            lower.startsWith('http ') ||
            lower.startsWith('status ') ||
            lower.includes('json parse error') ||
            lower.includes('cannot deserialize') ||
            lower.includes('could not execute statement') ||
            lower.includes('data integrity violation') ||
            lower.includes('constraint violation') ||
            lower.includes('entitynotfoundexception') ||
            lower.includes('illegalstateexception') ||
            lower.includes('nullpointerexception') ||
            lower.includes('org.springframework') ||
            lower.includes('jakarta.persistence') ||
            lower.includes('com.andretti101')
        );
    }

    window.extractErrorMessage = function (errorData, fallbackMsg = 'Ocorreu um erro ao processar a solicitação.') {
        const safeFallback = (fallbackMsg && String(fallbackMsg).trim())
            ? String(fallbackMsg).trim()
            : 'Ocorreu um erro ao processar a solicitação. Tente novamente.';

        if (!errorData) return safeFallback;

        if (typeof errorData === 'string') {
            const trimmed = errorData.trim();
            if (trimmed && !isTechnicalJargon(trimmed)) return trimmed;
            return safeFallback;
        }

        if (errorData instanceof Error && errorData.message) {
            const trimmed = errorData.message.trim();
            if (trimmed && !isTechnicalJargon(trimmed)) return trimmed;
            return safeFallback;
        }

        // Se houver lista de erros (validações Spring / Bean Validation)
        if (Array.isArray(errorData.errors) && errorData.errors.length > 0) {
            const list = errorData.errors
                .map(err => {
                    if (typeof err === 'string' && err.trim()) return err.trim();
                    if (err && typeof err === 'object') {
                        return err.defaultMessage || err.message || err.detail || null;
                    }
                    return null;
                })
                .filter(msg => typeof msg === 'string' && msg.trim().length > 0 && !isTechnicalJargon(msg));

            if (list.length > 0) {
                return list.join('<br>');
            }
        } else if (errorData.errors && typeof errorData.errors === 'object' && !Array.isArray(errorData.errors)) {
            const vals = Object.values(errorData.errors).flat()
                .map(err => {
                    if (typeof err === 'string' && err.trim()) return err.trim();
                    if (err && typeof err === 'object') {
                        return err.defaultMessage || err.message || err.detail || null;
                    }
                    return null;
                })
                .filter(msg => typeof msg === 'string' && msg.trim().length > 0 && !isTechnicalJargon(msg));
            if (vals.length > 0) {
                return vals.join('<br>');
            }
        }

        // Se houver message, detail ou error
        if (typeof errorData.message === 'string' && errorData.message.trim()) {
            const trimmed = errorData.message.trim();
            if (!isTechnicalJargon(trimmed)) return trimmed;
        }
        if (typeof errorData.detail === 'string' && errorData.detail.trim()) {
            const trimmed = errorData.detail.trim();
            if (!isTechnicalJargon(trimmed)) return trimmed;
        }
        if (typeof errorData.error === 'string' && errorData.error.trim()) {
            const trimmed = errorData.error.trim();
            if (!isTechnicalJargon(trimmed)) return trimmed;
        }

        return safeFallback;
    };

})(window);
