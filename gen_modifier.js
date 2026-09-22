const fs = require('fs');
const file = 'src/main/resources/static/secretary/student-performance.html';
let content = fs.readFileSync(file, 'utf8');

// 1. Replace HTML of navs-faltas
const htmlTarget = `<div class="tab-pane fade" id="navs-faltas" role="tabpanel">
            <div class="table-responsive">
                <table class="table table-hover">
                    <thead>
                        <tr>
                            <th>Disciplina</th>
                            <th>Data</th>
                            <th>Status Atual</th>
                            <th>Ação</th>
                        </tr>
                    </thead>
                    <tbody id="editAttendancesBody">
                        <tr><td colspan="3">Carregando faltas...</td></tr>
                    </tbody>
                </table>
            </div>
        </div>`;

const htmlReplacement = `<div class="tab-pane fade" id="navs-faltas" role="tabpanel">
            <!-- Filtro de Tipo -->
            <div class="card mb-4 mt-3">
                <div class="card-body">
                    <div class="row justify-content-center text-center">
                        <div class="col-md-6 col-lg-4">
                            <label for="typeSelect" class="form-label text-uppercase fw-bold text-muted small mb-3">O que deseja visualizar?</label>
                            <select id="typeSelect" class="form-select form-select-lg">
                                <option value="PRESENT" selected>Presenças</option>
                                <option value="ABSENT">Faltas Injustificadas</option>
                                <option value="JUSTIFIED_ABSENCE">Faltas Justificadas</option>
                            </select>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Matérias -->
            <div id="subjectsContainer" class="row mb-4 d-none justify-content-center text-center">
                <div class="col-12"><h5 class="mb-3 text-muted fw-bold">Disciplinas e Contagem</h5></div>
                <div id="subjectsList" class="d-flex flex-column gap-3 col-12 col-md-8 col-lg-6 mx-auto"></div>
            </div>

            <!-- Tabela de Faltas Oculta -->
            <div id="attendanceTableContainer" class="card d-none">
                <div class="card-header d-flex justify-content-between align-items-center flex-wrap">
                    <h5 class="mb-0">Histórico de <span id="currentSubjectName" class="text-primary"></span></h5>
                </div>
                <div class="table-responsive">
                    <table class="table table-hover">
                        <thead>
                            <tr>
                                <th>Data da Aula</th>
                                <th>Período</th>
                                <th>Qtd. de Aulas</th>
                                <th>Ação</th>
                            </tr>
                        </thead>
                        <tbody id="attendanceTableBody">
                        </tbody>
                    </table>
                </div>
            </div>
        </div>`;

content = content.replace(htmlTarget, htmlReplacement);

// 2. Replace Javascript logic
const jsTargetRegex = /\/\/ Busca as faltas detalhadas[\s\S]*?\} catch\(e\) \{\s*console\.error\("Erro ao carregar lista de faltas", e\);\s*\}/;

const jsReplacement = `
                // Busca as faltas detalhadas
                try {
                    const absRes = await fetch(\`/api/attendances/student/\${studentId}\`, { headers: {"Authorization": \`Bearer \${jwtToken}\`} });
                    if (absRes.ok) {
                        allAttendances = (await absRes.json()).sort((a, b) => new Date(b.lessonDate) - new Date(a.lessonDate));
                        setupDropdown();
                    }
                } catch(e) {
                    console.error("Erro ao carregar lista de faltas", e);
                }
`;

content = content.replace(jsTargetRegex, jsReplacement);

// We need to add the helper functions setupDropdown, showSubjectsForType, showAttendanceForSubject
// I'll place them just above loadReportCard(); call at the bottom.

const helpers = `
        function escapeHtml(unsafe) {
            if (!unsafe) return '';
            return unsafe.toString().replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;").replace(/'/g, "&#039;");
        }

        function formatDate(dateStr) {
            if (!dateStr) return '';
            const [y, m, d] = dateStr.split('-');
            return \`\${d}/\${m}/\${y}\`;
        }

        let selectedType = null;

        function setupDropdown() {
            const select = document.getElementById('typeSelect');
            if(!select) return; // Prevent error if tab not rendered
            
            select.value = 'PRESENT';
            selectedType = 'PRESENT';
            showSubjectsForType();
            
            select.addEventListener('change', (e) => {
                selectedType = e.target.value;
                document.getElementById('attendanceTableContainer').classList.add('d-none');
                
                if (selectedType) {
                    showSubjectsForType();
                } else {
                    document.getElementById('subjectsContainer').classList.add('d-none');
                }
            });
        }

        function showSubjectsForType() {
            const container = document.getElementById('subjectsContainer');
            const list = document.getElementById('subjectsList');
            
            const filtered = allAttendances.filter(a => a.status === selectedType);
            
            const counts = {};
            filtered.forEach(a => {
                if(!counts[a.subjectName]) counts[a.subjectName] = 0;
                counts[a.subjectName] += (a.lessonCount || 1);
            });

            const subjects = Object.keys(counts).sort();

            if (subjects.length === 0) {
                list.innerHTML = \`<p class="text-muted w-100">Nenhum registro encontrado para esse tipo.</p>\`;
            } else {
                let badgeClass = 'bg-primary';
                if(selectedType === 'ABSENT') badgeClass = 'bg-danger';
                if(selectedType === 'JUSTIFIED_ABSENCE') badgeClass = 'bg-warning text-dark';
                if(selectedType === 'PRESENT') badgeClass = 'bg-success';

                list.innerHTML = subjects.map(subj => \`
                    <button type="button" class="btn btn-outline-primary btn-lg w-100 d-flex justify-content-between align-items-center" onclick="showAttendanceForSubject('\${escapeHtml(subj)}')">
                        \${escapeHtml(subj)}
                        <span class="badge \${badgeClass} rounded-pill fs-6">\${counts[subj]}</span>
                    </button>
                \`).join('');
            }

            container.classList.remove('d-none');
        }

        function showAttendanceForSubject(subject) {
            document.getElementById('currentSubjectName').textContent = subject;
            
            const tbody = document.getElementById('attendanceTableBody');
            const records = allAttendances.filter(a => a.status === selectedType && a.subjectName === subject);

            if (records.length === 0) {
                tbody.innerHTML = '<tr><td colspan="4" class="text-center">Nenhum registro encontrado.</td></tr>';
            } else {
                tbody.innerHTML = records.map((a, index) => {
                    const rowId = \`details_\${index}\`;
                    const periodDisplay = a.periodName && a.periodName !== '-' ? a.periodName : 'Ano Vigente';
                    
                    return \`
                    <tr class="cursor-pointer" data-bs-toggle="collapse" data-bs-target="#\${rowId}" aria-expanded="false" aria-controls="\${rowId}">
                        <td><strong>\${formatDate(a.lessonDate)}</strong> <i class="bx bx-chevron-down text-muted ms-1"></i></td>
                        <td>\${escapeHtml(periodDisplay)}</td>
                        <td><span class="badge bg-label-secondary">\${a.lessonCount || 1} aula(s)</span></td>
                        <td>
                            <button class="btn btn-sm btn-outline-primary" onclick="openAttendanceModal(\${a.id}, '\${a.lessonDate}', '\${a.status}'); event.stopPropagation();"><i class="bx bx-edit-alt"></i></button>
                        </td>
                    </tr>
                    <tr>
                        <td colspan="4" class="p-0 border-0">
                            <div class="collapse" id="\${rowId}">
                                <div class="p-3 bg-light rounded m-2">
                                    <div class="mb-2">
                                        <strong><i class="bx bx-book-content me-1"></i> Conteúdo da Aula:</strong>
                                        <p class="mb-1 ms-4 text-muted">\${escapeHtml(a.lessonContent || 'Sem registro de conteúdo')}</p>
                                    </div>
                                    <div class="mb-2">
                                        <strong><i class="bx bx-message-alt-detail me-1"></i> Observações da Aula:</strong>
                                        <p class="mb-1 ms-4 text-muted">\${escapeHtml(a.lessonNotes || 'Sem observações gerais')}</p>
                                    </div>
                                </div>
                            </div>
                        </td>
                    </tr>
                \`}).join('');
            }

            document.getElementById('attendanceTableContainer').classList.remove('d-none');
        }
`;

content = content.replace("loadReportCard();", helpers + "\n        loadReportCard();");

// Fix openAttendanceModal arguments inside Javascript so it correctly passes lessonId as expected by API 
// Wait, looking at the previous openAttendanceModal definition:
//         function openAttendanceModal(id, date, status) {
// API wants body: JSON.stringify({ status }) - Wait, my previous saveAttendance only sent status!
// Let's verify what AttendanceController requires.
// @Valid @RequestBody AttendanceRequestDTO dto
// AttendanceRequestDTO requires studentId, lessonId, status.
// But the UI in `secretary/student-performance.html` only had id, date, status!
// Let me check my previous saveAttendance implementation in `secretary/student-performance.html`
`;

fs.writeFileSync('modify.js', content);
console.log("Script generation complete.");
