(function (window) {
    'use strict';

    let modalCounter = 0;

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
            
            modalEl.querySelector('.modal-title').textContent = title;
            modalEl.querySelector('.modal-message').textContent = message;

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
            
            modalEl.querySelector('.modal-title').textContent = title || 'Aviso';
            modalEl.querySelector('.modal-message').textContent = message;

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

})(window);
