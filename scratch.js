const token = localStorage.getItem('jwt_token');
let classroomsData = [];
let deleteId = null;

// Verificar se é SECRETARY para mostrar botão de "Nova Turma"
if (token) {
    try {
        const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
        const payloadString = JSON.stringify(payload).toUpperCase();
        if (payloadString.includes("SECRETARY")) {
           document.getElementById('btnNovaTurma').classList.remove('d-none');
           window.isSecretary = true;
        }
    } catch(e) {}
}

function escapeHtml(unsafe) {
    if (!unsafe) return '';
    return unsafe.toString()
         .replace(/&/g, "&amp;")
         .replace(/</g, "&lt;")
         .replace(/>/g, "&gt;")
         .replace(/"/g, "&quot;")
         .replace(/'/g, "&#039;");
}

const shiftMap = {
    'MORNING': 'Matutino',
    'AFTERNOON': 'Vespertino',
    'EVENING': 'Noturno',
    'FULL_TIME': 'Integral'
};

function showAlert(message, type) {
    const container = document.getElementById('alertContainer');
    container.innerHTML = `<div class="alert alert-${type} alert-dismissible" role="alert">${message}<button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button></div>`;
    setTimeout(() => { container.innerHTML = ''; }, 5000);
}

async function loadYears() {
    try {
        const res = await fetch('/api/academic-years', {
            headers: { 'Authorization': `Bearer ${token || ''}` }
        });
        if (res.ok) {
            const years = await res.json();
            const filter = document.getElementById('yearFilter');
            years.sort((a,b) => b.year - a.year).forEach(y => {
                const opt = document.createElement('option');
                opt.value = y.id;
                opt.textContent = y.year + (y.active ? ' (Ativo)' : '');
                filter.appendChild(opt);
            });
        }
    } catch (e) {
        console.error("Erro ao carregar anos:", e);
    }
}

async function loadClassrooms() {
    try {
        const response = await fetch('/api/classrooms', {
            headers: { 'Authorization': `Bearer ${token || ''}` }
        });
        if (response.ok) {
            classroomsData = await response.json();
            renderTable();
        } else {
            document.getElementById('tableBody').innerHTML = '<tr><td colspan="5" class="text-center text-danger">Erro ao carregar dados.</td></tr>';
        }
    } catch (error) {
        document.getElementById('tableBody').innerHTML = '<tr><td colspan="5" class="text-center text-danger">Falha de conexão.</td></tr>';
    }
}

function renderTable() {
    const tbody = document.getElementById('tableBody');
    const searchTerm = document.getElementById('searchInput').value.toLowerCase();
    const statusFilter = document.getElementById('statusFilter').value;
    const yearFilter = document.getElementById('yearFilter').value;

    let filtered = classroomsData.filter(c => {
        const matchSearch = c.name.toLowerCase().includes(searchTerm) || (shiftMap[c.shift] || '').toLowerCase().includes(searchTerm);
        const matchStatus = statusFilter === 'all' ? true : (statusFilter === 'active' ? c.active : !c.active);
        const matchYear = yearFilter === 'all' ? true : (c.academicYearId == yearFilter);
        return matchSearch && matchStatus && matchYear;
    });

    if (filtered.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" class="text-center text-muted">Nenhuma turma encontrada.</td></tr>';
        return;
    }

    tbody.innerHTML = filtered.map(c => {
        const shiftLabel = shiftMap[c.shift] || c.shift;
        const badgeClass = c.active ? 'bg-label-success' : 'bg-label-secondary';
        const badgeText = c.active ? 'Ativa' : 'Inativa';
        const actionIcon = c.active ? 'bx-block' : 'bx-check-circle';
        const actionText = c.active ? 'Desativar' : 'Ativar';
        const actionColor = c.active ? 'text-warning' : 'text-success';
        
        // Só exibe botões se for Secretaria
        let actionsHtml = '';
        if (window.isSecretary) {
            let editBtn = '';
            if (c.active) {
                editBtn = `<a class="dropdown-item" href="/secretary/edit/classroom.html?id=${c.id}"><i class="bx bx-edit-alt me-1"></i> Editar</a>`;
            }

            actionsHtml = `
                <div class="dropdown">
                    <button type="button" class="btn p-0 dropdown-toggle hide-arrow" data-bs-toggle="dropdown">
                        <i class="bx bx-dots-vertical-rounded"></i>
                    </button>
                    <div class="dropdown-menu">
                        ${editBtn}
                        <a class="dropdown-item ${actionColor}" href="javascript:void(0);" onclick="toggleStatus(${c.id}, ${!c.active})">
                            <i class="bx ${actionIcon} me-1"></i> ${actionText}
                        </a>
                        <div class="dropdown-divider"></div>
                        <a class="dropdown-item text-danger" href="javascript:void(0);" onclick="confirmDelete(${c.id}, '${escapeHtml(c.name)}')">
                            <i class="bx bx-trash me-1"></i> Excluir
                        </a>
                    </div>
                </div>
            `;
        } else {
            actionsHtml = `<span class="text-muted"><i class="bx bx-lock-alt"></i> Restrito</span>`;
        }

        return `
            <tr class="${c.active ? '' : 'inactive-row'}">
                <td><strong>${escapeHtml(c.name)}</strong></td>
                <td>${c.academicYearLabel || c.academicYearId}</td>
                <td><span class="badge bg-label-info">${shiftLabel}</span></td>
                <td><span class="badge ${badgeClass}">${badgeText}</span></td>
                <td>
                    ${actionsHtml}
                </td>
            </tr>
        `;
    }).join('');
}

async function toggleStatus(id, activate) {
    const endpoint = `/api/classrooms/${id}/${activate ? 'activate' : 'deactivate'}`;
    try {
        const res = await fetch(endpoint, {
            method: 'PUT',
            headers: { 'Authorization': `Bearer ${token || ''}` }
        });
        if (res.ok) {
            showAlert(`Turma ${activate ? 'ativada' : 'desativada'} com sucesso.`, 'success');
            loadClassrooms();
        } else {
            if(res.status === 403) showAlert("Acesso negado. Apenas a Secretaria pode alterar o status de turmas.", "danger");
            else showAlert("Erro ao alterar status.", "danger");
        }
    } catch (e) {
        showAlert("Erro de conexão.", "danger");
    }
}

function confirmDelete(id, name) {
    deleteId = id;
    document.getElementById('deleteTargetName').textContent = name;
    new bootstrap.Modal(document.getElementById('deleteModal')).show();
}

document.getElementById('btnConfirmDelete').addEventListener('click', async () => {
    if (!deleteId) return;
    const btn = document.getElementById('btnConfirmDelete');
    btn.disabled = true;
    btn.innerText = 'Excluindo...';

    try {
        const response = await fetch(`/api/classrooms/${deleteId}`, {
            method: 'DELETE',
            headers: { 'Authorization': `Bearer ${token || ''}` }
        });
        
        const modal = bootstrap.Modal.getInstance(document.getElementById('deleteModal'));
        modal.hide();

        if (response.ok || response.status === 204) {
            showAlert('Turma excluída com sucesso.', 'success');
            loadClassrooms();
        } else {
            if (response.status === 403) showAlert("Acesso negado. Apenas a Secretaria pode excluir turmas.", "danger");
            else showAlert('Erro ao excluir turma. Verifique se existem dependências.', 'danger');
        }
    } catch (e) {
        showAlert('Erro de conexão com o servidor.', 'danger');
    } finally {
        btn.disabled = false;
        btn.innerText = 'Excluir';
        deleteId = null;
    }
});

document.getElementById('searchInput').addEventListener('input', renderTable);
document.getElementById('statusFilter').addEventListener('change', renderTable);
document.getElementById('yearFilter').addEventListener('change', renderTable);

// Initial load
loadYears().then(loadClassrooms);
