/**
 * change-history.js
 * Gerencia a consulta de histórico de alterações de notas e frequência.
 * Compatível com perfis: SECRETARY, PRINCIPAL e TEACHER.
 */

(function () {
    'use strict';

    const token = localStorage.getItem('jwt_token');
    const role = (document.body.getAttribute('data-role') || 'SECRETARY').toUpperCase();
    const isTeacher = role === 'TEACHER';

    let currentGradesPage = 0;
    let currentAttendancePage = 0;
    const pageSize = 15;

    let teacherClassSubjects = [];
    let allClassRooms = [];
    let allPeriods = [];

    // Elementos DOM
    const filterYear = document.getElementById('filterYear');
    const filterPeriod = document.getElementById('filterPeriod');
    const filterClassRoom = document.getElementById('filterClassRoom');
    const filterTeacher = document.getElementById('filterTeacher');
    const teacherFilterCol = document.getElementById('teacherFilterCol');
    const filterStudent = document.getElementById('filterStudent');
    const filterFrom = document.getElementById('filterFrom');
    const filterTo = document.getElementById('filterTo');
    const btnFilter = document.getElementById('btnFilter');
    const btnClear = document.getElementById('btnClear');

    const gradesTableBody = document.getElementById('gradesTableBody');
    const attendanceTableBody = document.getElementById('attendanceTableBody');
    const gradesBadgeCount = document.getElementById('gradesBadgeCount');
    const attendanceBadgeCount = document.getElementById('attendanceBadgeCount');
    const gradesInfo = document.getElementById('gradesInfo');
    const attendanceInfo = document.getElementById('attendanceInfo');
    const gradesPaginationControls = document.getElementById('gradesPaginationControls');
    const attendancePaginationControls = document.getElementById('attendancePaginationControls');

    // Modal de Linha do Tempo
    const timelineModalEl = document.getElementById('timelineModal');
    let timelineModal = null;
    const timelineModalTitle = document.getElementById('timelineModalTitle');
    const timelineModalBody = document.getElementById('timelineModalBody');

    // ── Utilitários ──

    function escapeHtml(text) {
        if (text === null || text === undefined) return '';
        return String(text)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }

    function formatDateTime(isoString) {
        if (!isoString) return '—';
        const d = new Date(isoString);
        return isNaN(d.getTime()) ? isoString : d.toLocaleString('pt-BR');
    }

    function formatDate(isoString) {
        if (!isoString) return '—';
        if (typeof isoString === 'string' && isoString.includes('-')) {
            const parts = isoString.split('T')[0].split('-');
            if (parts.length === 3) return `${parts[2]}/${parts[1]}/${parts[0]}`;
        }
        const d = new Date(isoString);
        return isNaN(d.getTime()) ? isoString : d.toLocaleDateString('pt-BR');
    }

    function renderGradeBadge(grade) {
        if (grade === null || grade === undefined) {
            return '<span class="badge bg-label-secondary">—</span>';
        }
        const val = typeof grade === 'number' ? grade.toFixed(1) : parseFloat(grade).toFixed(1);
        return `<span class="badge bg-label-primary fw-bold">${val}</span>`;
    }

    function renderAttendanceBadge(status) {
        if (!status) return '<span class="badge bg-label-secondary">—</span>';
        switch (status) {
            case 'PRESENT':
                return '<span class="badge bg-label-success"><i class="bx bx-check me-1"></i>Presente</span>';
            case 'ABSENT':
                return '<span class="badge bg-label-danger"><i class="bx bx-x me-1"></i>Falta</span>';
            case 'JUSTIFIED_ABSENCE':
                return '<span class="badge bg-label-warning"><i class="bx bx-info-circle me-1"></i>Justificada</span>';
            default:
                return `<span class="badge bg-label-secondary">${escapeHtml(status)}</span>`;
        }
    }

    async function apiFetch(url, options = {}) {
        const headers = {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + token,
            ...(options.headers || {})
        };
        const response = await fetch(url, { ...options, headers });
        if (response.status === 401 || response.status === 403) {
            console.warn('Acesso negado ou sessão expirada:', url);
        }
        return response;
    }

    // ── Inicialização dos Filtros ──

    async function initFilters() {
        if (teacherFilterCol && isTeacher) {
            teacherFilterCol.style.display = 'none';
        }

        try {
            // 1. Carrega anos acadêmicos
            const yearsRes = await apiFetch('/api/academic-years');
            if (yearsRes.ok) {
                const years = await yearsRes.json();
                filterYear.innerHTML = '<option value="">Todos os anos</option>';
                years.forEach(y => {
                    const opt = document.createElement('option');
                    opt.value = y.id;
                    opt.textContent = y.year + (y.active ? ' (Atual)' : '');
                    if (y.active) {
                        opt.selected = true;
                    }
                    filterYear.appendChild(opt);
                });
            }
        } catch (err) {
            console.error('Erro ao carregar anos acadêmicos:', err);
        }

        try {
            // 2. Carrega períodos letivos
            const periodsRes = await apiFetch('/api/academic-periods');
            if (periodsRes.ok) {
                allPeriods = await periodsRes.json();
                populatePeriods();
            }
        } catch (err) {
            console.error('Erro ao carregar períodos letivos:', err);
        }

        if (isTeacher) {
            await initTeacherFilters();
        } else {
            await initAdminFilters();
        }

        const triggerAutoFilter = () => {
            currentGradesPage = 0;
            currentAttendancePage = 0;
            loadAll();
        };

        // Eventos dos filtros (carregamento automático sem botão Filtrar)
        if (filterYear) filterYear.addEventListener('change', onYearChange);
        if (filterPeriod) filterPeriod.addEventListener('change', triggerAutoFilter);
        if (filterClassRoom) filterClassRoom.addEventListener('change', onClassRoomChange);
        if (filterTeacher) filterTeacher.addEventListener('change', triggerAutoFilter);
        if (filterStudent) filterStudent.addEventListener('change', triggerAutoFilter);
        if (filterFrom) filterFrom.addEventListener('change', triggerAutoFilter);
        if (filterTo) filterTo.addEventListener('change', triggerAutoFilter);
        if (btnFilter) btnFilter.addEventListener('click', triggerAutoFilter);
        if (btnClear) btnClear.addEventListener('click', onClearFilters);
    }

    function populatePeriods() {
        if (!filterPeriod) return;
        const yearId = filterYear ? filterYear.value : '';
        const previousVal = filterPeriod.value;

        filterPeriod.innerHTML = '<option value="">Todos os períodos</option>';

        const filtered = yearId
            ? allPeriods.filter(p => String(p.academicYearId) === String(yearId))
            : allPeriods;

        filtered.forEach(p => {
            const opt = document.createElement('option');
            opt.value = p.id;
            opt.textContent = yearId ? p.name : `${p.academicYear || ''} - ${p.name}`;
            filterPeriod.appendChild(opt);
        });

        if (previousVal && filtered.some(p => String(p.id) === String(previousVal))) {
            filterPeriod.value = previousVal;
        } else {
            filterPeriod.value = '';
        }
    }

    async function initTeacherFilters() {
        try {
            const res = await apiFetch('/api/teacher-class-subjects');
            if (res.ok) {
                teacherClassSubjects = await res.json();
                populateTeacherClassRooms();
            }
        } catch (err) {
            console.error('Erro ao carregar turmas do professor:', err);
        }
    }

    function populateTeacherClassRooms() {
        const seen = new Set();
        filterClassRoom.innerHTML = '<option value="">Todas as minhas turmas</option>';

        teacherClassSubjects.forEach(tcs => {
            if (!seen.has(tcs.classRoomId)) {
                seen.add(tcs.classRoomId);
                const opt = document.createElement('option');
                opt.value = tcs.classRoomId;
                opt.textContent = tcs.classRoomName;
                filterClassRoom.appendChild(opt);
            }
        });
    }

    async function initAdminFilters() {
        // Carrega professores
        try {
            const teachRes = await apiFetch('/api/teachers');
            if (teachRes.ok && filterTeacher) {
                const teachers = await teachRes.json();
                teachers.sort((a, b) => (a.name || '').localeCompare(b.name || ''));
                filterTeacher.innerHTML = '<option value="">Todos os professores</option>';
                teachers.forEach(t => {
                    const opt = document.createElement('option');
                    opt.value = t.id;
                    opt.textContent = t.name;
                    filterTeacher.appendChild(opt);
                });
            }
        } catch (err) {
            console.error('Erro ao carregar professores:', err);
        }

        // Carrega turmas
        await loadAdminClassRooms();
    }

    async function loadAdminClassRooms() {
        const yearId = filterYear.value;
        const url = yearId ? `/api/classrooms/academic-year/${yearId}` : '/api/classrooms';
        try {
            const res = await apiFetch(url);
            if (res.ok) {
                allClassRooms = await res.json();
                allClassRooms.sort((a, b) => (a.name || '').localeCompare(b.name || ''));
                filterClassRoom.innerHTML = '<option value="">Todas as turmas</option>';
                allClassRooms.forEach(c => {
                    const opt = document.createElement('option');
                    opt.value = c.id;
                    opt.textContent = c.name;
                    filterClassRoom.appendChild(opt);
                });
            }
        } catch (err) {
            console.error('Erro ao carregar turmas:', err);
        }
    }

    async function onYearChange() {
        populatePeriods();
        if (isTeacher) {
            populateTeacherClassRooms();
        } else {
            await loadAdminClassRooms();
        }
        await onClassRoomChange();
    }

    async function onClassRoomChange() {
        const classRoomId = filterClassRoom.value;
        filterStudent.innerHTML = '<option value="">Todos os alunos</option>';

        if (classRoomId) {
            try {
                const res = await apiFetch(`/api/enrollments/classroom/${classRoomId}`);
                if (res.ok) {
                    const enrollments = await res.json();
                    enrollments.sort((a, b) => (a.studentName || '').localeCompare(b.studentName || ''));
                    enrollments.forEach(e => {
                        const opt = document.createElement('option');
                        opt.value = e.studentId;
                        opt.textContent = e.studentName;
                        filterStudent.appendChild(opt);
                    });
                }
            } catch (err) {
                console.error('Erro ao carregar alunos da turma:', err);
            }
        }

        currentGradesPage = 0;
        currentAttendancePage = 0;
        loadAll();
    }

    function onClearFilters() {
        if (filterPeriod) filterPeriod.value = '';
        if (filterClassRoom) filterClassRoom.value = '';
        if (filterStudent) filterStudent.innerHTML = '<option value="">Todos os alunos</option>';
        if (filterTeacher) filterTeacher.value = '';
        if (filterFrom) filterFrom.value = '';
        if (filterTo) filterTo.value = '';

        currentGradesPage = 0;
        currentAttendancePage = 0;
        loadAll();
    }

    // ── Montagem dos Parâmetros ──

    function buildQueryParams(page) {
        const params = new URLSearchParams();
        params.set('page', page);
        params.set('size', pageSize);

        if (filterYear && filterYear.value) params.set('yearId', filterYear.value);
        if (filterPeriod && filterPeriod.value) params.set('periodId', filterPeriod.value);
        if (filterClassRoom && filterClassRoom.value) params.set('classRoomId', filterClassRoom.value);
        if (filterStudent && filterStudent.value) params.set('studentId', filterStudent.value);
        if (!isTeacher && filterTeacher && filterTeacher.value) params.set('teacherId', filterTeacher.value);
        if (filterFrom && filterFrom.value) params.set('from', filterFrom.value);
        if (filterTo && filterTo.value) params.set('to', filterTo.value);

        return params.toString();
    }

    // ── Carregamento de Notas ──

    async function loadGrades(page = 0) {
        currentGradesPage = page;
        gradesTableBody.innerHTML = `
            <tr>
                <td colspan="7" class="text-center py-5">
                    <div class="spinner-border text-primary" role="status"></div>
                    <div class="mt-2 text-muted">Carregando histórico de notas...</div>
                </td>
            </tr>`;

        try {
            const qs = buildQueryParams(page);
            const res = await apiFetch(`/api/grade-history?${qs}`);
            if (!res.ok) {
                gradesTableBody.innerHTML = `
                    <tr>
                        <td colspan="7" class="text-center py-5 text-danger">
                            <i class="bx bx-error-circle fs-3 d-block mb-2"></i>
                            Não foi possível carregar o histórico de notas.
                        </td>
                    </tr>`;
                return;
            }

            const data = await res.json();
            renderGradesTable(data);
        } catch (err) {
            console.error('Erro ao carregar histórico de notas:', err);
            gradesTableBody.innerHTML = `<tr><td colspan="7" class="text-center py-4 text-danger">Erro de conexão ao carregar notas.</td></tr>`;
        }
    }

    function renderGradesTable(pageData) {
        const items = pageData.content || [];
        const totalElements = pageData.totalElements || 0;
        const totalPages = pageData.totalPages || 0;

        if (gradesBadgeCount) gradesBadgeCount.textContent = totalElements;

        if (items.length === 0) {
            gradesTableBody.innerHTML = `
                <tr>
                    <td colspan="7" class="text-center py-5 text-muted">
                        <i class="bx bx-folder-open fs-2 d-block mb-2"></i>
                        Nenhuma alteração de nota encontrada com os filtros selecionados.
                    </td>
                </tr>`;
            if (gradesInfo) gradesInfo.textContent = 'Nenhum registro';
            if (gradesPaginationControls) gradesPaginationControls.innerHTML = '';
            return;
        }

        let html = '';
        items.forEach(h => {
            const studentText = escapeHtml(h.studentName || '—');
            const classRoomText = escapeHtml(h.classRoomName || '—');
            const subjectText = escapeHtml(h.subjectName || '—');
            const assessmentText = escapeHtml(h.assessmentTitle || 'Avaliação');
            const periodText = h.periodName ? ` (${escapeHtml(h.periodName)})` : '';
            const teacherText = escapeHtml(h.teacherName || '—');
            const authorHtml = h.changedByName
                ? `<div class="mt-1"><span class="badge bg-label-info badge-author"><i class="bx bx-user me-1"></i>${escapeHtml(h.changedByName)}</span></div>`
                : `<div class="small text-muted">Autor: —</div>`;
            const notesText = h.notes
                ? `<span title="${escapeHtml(h.notes)}">${escapeHtml(h.notes)}</span>`
                : '<span class="text-muted">—</span>';

            html += `
                <tr>
                    <td class="text-nowrap">
                        <span class="fw-semibold">${formatDateTime(h.changedAt)}</span>
                    </td>
                    <td>
                        <strong>${studentText}</strong>
                        <div class="small text-muted">${classRoomText}</div>
                    </td>
                    <td>
                        <span class="fw-semibold text-primary">${subjectText}</span>
                        <div class="small text-muted">${assessmentText}${periodText}</div>
                    </td>
                    <td class="text-nowrap">
                        <div class="d-flex align-items-center gap-1">
                            ${renderGradeBadge(h.previousGrade)}
                            <i class="bx bx-right-arrow-alt text-muted"></i>
                            ${renderGradeBadge(h.newGrade)}
                        </div>
                    </td>
                    <td>
                        <div class="fw-medium">${teacherText}</div>
                        ${authorHtml}
                    </td>
                    <td class="history-notes-cell">${notesText}</td>
                    <td class="text-end">
                        <button type="button" class="btn btn-sm btn-icon btn-outline-primary" title="Ver linha do tempo da nota" onclick="window.EscolaHistory.showGradeTimeline(${h.gradeId})">
                            <i class="bx bx-history"></i>
                        </button>
                    </td>
                </tr>`;
        });

        gradesTableBody.innerHTML = html;

        // Informações e Paginação
        const start = pageData.number * pageData.size + 1;
        const end = Math.min((pageData.number + 1) * pageData.size, totalElements);
        if (gradesInfo) {
            gradesInfo.textContent = `Mostrando ${start} a ${end} de ${totalElements} registros`;
        }

        renderPagination(gradesPaginationControls, pageData.number, totalPages, (p) => loadGrades(p));
    }

    // ── Carregamento de Frequência ──

    async function loadAttendance(page = 0) {
        currentAttendancePage = page;
        attendanceTableBody.innerHTML = `
            <tr>
                <td colspan="6" class="text-center py-5">
                    <div class="spinner-border text-primary" role="status"></div>
                    <div class="mt-2 text-muted">Carregando histórico de frequência...</div>
                </td>
            </tr>`;

        try {
            const qs = buildQueryParams(page);
            const res = await apiFetch(`/api/attendance-history?${qs}`);
            if (!res.ok) {
                attendanceTableBody.innerHTML = `
                    <tr>
                        <td colspan="6" class="text-center py-5 text-danger">
                            <i class="bx bx-error-circle fs-3 d-block mb-2"></i>
                            Não foi possível carregar o histórico de frequência.
                        </td>
                    </tr>`;
                return;
            }

            const data = await res.json();
            renderAttendanceTable(data);
        } catch (err) {
            console.error('Erro ao carregar histórico de frequência:', err);
            attendanceTableBody.innerHTML = `<tr><td colspan="6" class="text-center py-4 text-danger">Erro de conexão ao carregar frequência.</td></tr>`;
        }
    }

    function renderAttendanceTable(pageData) {
        const items = pageData.content || [];
        const totalElements = pageData.totalElements || 0;
        const totalPages = pageData.totalPages || 0;

        if (attendanceBadgeCount) attendanceBadgeCount.textContent = totalElements;

        if (items.length === 0) {
            attendanceTableBody.innerHTML = `
                <tr>
                    <td colspan="6" class="text-center py-5 text-muted">
                        <i class="bx bx-folder-open fs-2 d-block mb-2"></i>
                        Nenhuma alteração de frequência encontrada com os filtros selecionados.
                    </td>
                </tr>`;
            if (attendanceInfo) attendanceInfo.textContent = 'Nenhum registro';
            if (attendancePaginationControls) attendancePaginationControls.innerHTML = '';
            return;
        }

        let html = '';
        items.forEach(h => {
            const studentText = escapeHtml(h.studentName || '—');
            const classRoomText = escapeHtml(h.classRoomName || '—');
            const subjectText = escapeHtml(h.subjectName || '—');
            const lessonDateText = formatDate(h.lessonDate);
            const teacherText = escapeHtml(h.teacherName || '—');
            const authorHtml = h.changedByName
                ? `<div class="mt-1"><span class="badge bg-label-info badge-author"><i class="bx bx-user me-1"></i>${escapeHtml(h.changedByName)}</span></div>`
                : `<div class="small text-muted">Autor: —</div>`;

            html += `
                <tr>
                    <td class="text-nowrap">
                        <span class="fw-semibold">${formatDateTime(h.changedAt)}</span>
                    </td>
                    <td>
                        <strong>${studentText}</strong>
                        <div class="small text-muted">${classRoomText}</div>
                    </td>
                    <td>
                        <span class="fw-semibold text-primary">${subjectText}</span>
                        <div class="small text-muted"><i class="bx bx-calendar me-1"></i>Aula: ${lessonDateText}</div>
                    </td>
                    <td class="text-nowrap">
                        <div class="d-flex align-items-center gap-1">
                            ${renderAttendanceBadge(h.previousStatus)}
                            <i class="bx bx-right-arrow-alt text-muted"></i>
                            ${renderAttendanceBadge(h.newStatus)}
                        </div>
                    </td>
                    <td>
                        <div class="fw-medium">${teacherText}</div>
                        ${authorHtml}
                    </td>
                    <td class="text-end">
                        <button type="button" class="btn btn-sm btn-icon btn-outline-primary" title="Ver linha do tempo da frequência" onclick="window.EscolaHistory.showAttendanceTimeline(${h.attendanceId})">
                            <i class="bx bx-history"></i>
                        </button>
                    </td>
                </tr>`;
        });

        attendanceTableBody.innerHTML = html;

        // Informações e Paginação
        const start = pageData.number * pageData.size + 1;
        const end = Math.min((pageData.number + 1) * pageData.size, totalElements);
        if (attendanceInfo) {
            attendanceInfo.textContent = `Mostrando ${start} a ${end} de ${totalElements} registros`;
        }

        renderPagination(attendancePaginationControls, pageData.number, totalPages, (p) => loadAttendance(p));
    }

    // ── Renderização da Paginação ──

    function renderPagination(container, currentPage, totalPages, onPageClick) {
        if (!container) return;
        if (totalPages <= 1) {
            container.innerHTML = '';
            return;
        }

        let html = '';

        // Botão Anterior
        html += `
            <li class="page-item prev ${currentPage === 0 ? 'disabled' : ''}">
                <a class="page-link" href="javascript:void(0);" data-page="${currentPage - 1}">
                    <i class="tf-icon bx bx-chevron-left"></i>
                </a>
            </li>`;

        // Janela de páginas
        const maxButtons = 5;
        let startPage = Math.max(0, currentPage - Math.floor(maxButtons / 2));
        let endPage = Math.min(totalPages - 1, startPage + maxButtons - 1);

        if (endPage - startPage + 1 < maxButtons) {
            startPage = Math.max(0, endPage - maxButtons + 1);
        }

        if (startPage > 0) {
            html += `<li class="page-item"><a class="page-link" href="javascript:void(0);" data-page="0">1</a></li>`;
            if (startPage > 1) {
                html += `<li class="page-item disabled"><span class="page-link">...</span></li>`;
            }
        }

        for (let i = startPage; i <= endPage; i++) {
            html += `
                <li class="page-item ${i === currentPage ? 'active' : ''}">
                    <a class="page-link" href="javascript:void(0);" data-page="${i}">${i + 1}</a>
                </li>`;
        }

        if (endPage < totalPages - 1) {
            if (endPage < totalPages - 2) {
                html += `<li class="page-item disabled"><span class="page-link">...</span></li>`;
            }
            html += `<li class="page-item"><a class="page-link" href="javascript:void(0);" data-page="${totalPages - 1}">${totalPages}</a></li>`;
        }

        // Botão Próximo
        html += `
            <li class="page-item next ${currentPage >= totalPages - 1 ? 'disabled' : ''}">
                <a class="page-link" href="javascript:void(0);" data-page="${currentPage + 1}">
                    <i class="tf-icon bx bx-chevron-right"></i>
                </a>
            </li>`;

        container.innerHTML = html;

        container.querySelectorAll('a.page-link').forEach(link => {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                const p = parseInt(link.getAttribute('data-page'));
                if (!isNaN(p) && p >= 0 && p < totalPages && p !== currentPage) {
                    onPageClick(p);
                }
            });
        });
    }

    // ── Linha do Tempo (Modal) ──

    async function showGradeTimeline(gradeId) {
        if (!timelineModal && timelineModalEl) {
            timelineModal = new bootstrap.Modal(timelineModalEl);
        }
        if (timelineModalTitle) timelineModalTitle.textContent = `Linha do Tempo - Nota #${gradeId}`;
        if (timelineModalBody) {
            timelineModalBody.innerHTML = `
                <div class="text-center py-4">
                    <div class="spinner-border text-primary" role="status"></div>
                    <div class="mt-2 text-muted">Carregando histórico do registro...</div>
                </div>`;
        }
        if (timelineModal) timelineModal.show();

        try {
            const res = await apiFetch(`/api/grade-history/grade/${gradeId}`);
            if (!res.ok) {
                timelineModalBody.innerHTML = `<div class="alert alert-danger mb-0">Não foi possível carregar o histórico desta nota.</div>`;
                return;
            }
            const list = await res.json();
            if (list.length === 0) {
                timelineModalBody.innerHTML = `<div class="alert alert-info mb-0">Nenhum evento registrado para esta nota.</div>`;
                return;
            }

            const headerInfo = list[0];
            let bodyHtml = `
                <div class="card border mb-3">
                    <div class="card-body py-2 px-3 small">
                        <div><strong>Aluno:</strong> ${escapeHtml(headerInfo.studentName)}</div>
                        <div><strong>Turma / Matéria:</strong> ${escapeHtml(headerInfo.classRoomName)} — ${escapeHtml(headerInfo.subjectName)}</div>
                        <div><strong>Avaliação:</strong> ${escapeHtml(headerInfo.assessmentTitle)} ${headerInfo.periodName ? `(${escapeHtml(headerInfo.periodName)})` : ''}</div>
                    </div>
                </div>
                <div class="timeline">`;

            list.forEach((item, idx) => {
                const author = item.changedByName ? escapeHtml(item.changedByName) : 'Sistema / Desconhecido';
                bodyHtml += `
                    <div class="d-flex mb-3 pb-2 border-bottom">
                        <div class="me-3 text-center" style="min-width: 60px;">
                            <span class="badge bg-label-secondary small">#${list.length - idx}</span>
                        </div>
                        <div class="flex-grow-1">
                            <div class="d-flex justify-content-between align-items-center">
                                <span class="fw-semibold">${formatDateTime(item.changedAt)}</span>
                                <small class="text-muted"><i class="bx bx-user me-1"></i>${author}</small>
                            </div>
                            <div class="my-1">
                                Alteração: ${renderGradeBadge(item.previousGrade)}
                                <i class="bx bx-right-arrow-alt text-muted mx-1"></i>
                                ${renderGradeBadge(item.newGrade)}
                            </div>
                            ${item.notes ? `<div class="small text-muted mt-1 fst-italic">"${escapeHtml(item.notes)}"</div>` : ''}
                        </div>
                    </div>`;
            });

            bodyHtml += `</div>`;
            timelineModalBody.innerHTML = bodyHtml;
        } catch (err) {
            console.error('Erro ao buscar linha do tempo da nota:', err);
            timelineModalBody.innerHTML = `<div class="alert alert-danger mb-0">Erro de conexão ao carregar linha do tempo.</div>`;
        }
    }

    async function showAttendanceTimeline(attendanceId) {
        if (!timelineModal && timelineModalEl) {
            timelineModal = new bootstrap.Modal(timelineModalEl);
        }
        if (timelineModalTitle) timelineModalTitle.textContent = `Linha do Tempo - Frequência #${attendanceId}`;
        if (timelineModalBody) {
            timelineModalBody.innerHTML = `
                <div class="text-center py-4">
                    <div class="spinner-border text-primary" role="status"></div>
                    <div class="mt-2 text-muted">Carregando histórico do registro...</div>
                </div>`;
        }
        if (timelineModal) timelineModal.show();

        try {
            const res = await apiFetch(`/api/attendance-history/attendance/${attendanceId}`);
            if (!res.ok) {
                timelineModalBody.innerHTML = `<div class="alert alert-danger mb-0">Não foi possível carregar o histórico desta frequência.</div>`;
                return;
            }
            const list = await res.json();
            if (list.length === 0) {
                timelineModalBody.innerHTML = `<div class="alert alert-info mb-0">Nenhum evento registrado para esta frequência.</div>`;
                return;
            }

            const headerInfo = list[0];
            let bodyHtml = `
                <div class="card border mb-3">
                    <div class="card-body py-2 px-3 small">
                        <div><strong>Aluno:</strong> ${escapeHtml(headerInfo.studentName)}</div>
                        <div><strong>Turma / Matéria:</strong> ${escapeHtml(headerInfo.classRoomName)} — ${escapeHtml(headerInfo.subjectName)}</div>
                        <div><strong>Data da Aula:</strong> ${formatDate(headerInfo.lessonDate)}</div>
                    </div>
                </div>
                <div class="timeline">`;

            list.forEach((item, idx) => {
                const author = item.changedByName ? escapeHtml(item.changedByName) : 'Sistema / Desconhecido';
                bodyHtml += `
                    <div class="d-flex mb-3 pb-2 border-bottom">
                        <div class="me-3 text-center" style="min-width: 60px;">
                            <span class="badge bg-label-secondary small">#${list.length - idx}</span>
                        </div>
                        <div class="flex-grow-1">
                            <div class="d-flex justify-content-between align-items-center">
                                <span class="fw-semibold">${formatDateTime(item.changedAt)}</span>
                                <small class="text-muted"><i class="bx bx-user me-1"></i>${author}</small>
                            </div>
                            <div class="my-1">
                                Status: ${renderAttendanceBadge(item.previousStatus)}
                                <i class="bx bx-right-arrow-alt text-muted mx-1"></i>
                                ${renderAttendanceBadge(item.newStatus)}
                            </div>
                        </div>
                    </div>`;
            });

            bodyHtml += `</div>`;
            timelineModalBody.innerHTML = bodyHtml;
        } catch (err) {
            console.error('Erro ao buscar linha do tempo da frequência:', err);
            timelineModalBody.innerHTML = `<div class="alert alert-danger mb-0">Erro de conexão ao carregar linha do tempo.</div>`;
        }
    }

    function loadAll() {
        loadGrades(currentGradesPage);
        loadAttendance(currentAttendancePage);
    }

    // Expondo globalmente para os botões inline
    window.EscolaHistory = {
        showGradeTimeline,
        showAttendanceTimeline
    };

    // Inicialização ao carregar o DOM
    document.addEventListener('DOMContentLoaded', async () => {
        await initFilters();
        loadAll();
    });
})();
