
import re

files = [
    r"c:\escolaweb\src\main\resources\static\secretary\student-performance.html",
    r"c:\escolaweb\src\main\resources\static\principal\student-performance.html"
]

navbar_html = """            <nav class="layout-navbar container-xxl navbar navbar-expand-xl navbar-detached align-items-center bg-navbar-theme shadow-sm" id="layout-navbar">
                <div class="layout-menu-toggle navbar-nav align-items-xl-center me-3 me-xl-0 d-xl-none">
                    <a class="nav-item nav-link px-0 me-xl-4" href="javascript:void(0)">
                        <i class="bx bx-menu bx-sm"></i>
                    </a>
                </div>
                <div class="navbar-nav-right d-flex align-items-center" id="navbar-collapse">
                    <div class="navbar-nav align-items-center w-100 d-none d-md-flex">
                        <span class="fs-4 fw-bold">
                            <span class="text-primary">Consultar Desempenho</span>
                        </span>
                    </div>
                    <ul class="navbar-nav flex-row align-items-center ms-auto">
                        <li class="nav-item navbar-dropdown dropdown-user dropdown">
                            <a class="nav-link dropdown-toggle hide-arrow" href="javascript:void(0);" data-bs-toggle="dropdown">
                                <div class="avatar avatar-online">
                                    <span class="avatar-initial rounded-circle bg-label-primary" id="dropdownAvatarInitial">A</span>
                                </div>
                            </a>
                            <ul class="dropdown-menu dropdown-menu-end">
                                <li>
                                    <a class="dropdown-item" href="#">
                                        <div class="d-flex">
                                            <div class="flex-shrink-0 me-3">
                                                <div class="avatar avatar-online">
                                                    <span class="avatar-initial rounded-circle bg-label-primary" id="dropdownAvatarInitialInner">A</span>
                                                </div>
                                            </div>
                                            <div class="flex-grow-1">
                                                <span class="fw-semibold d-block" id="dropdownUserName">Usuário</span>
                                                <small class="text-muted">Painel</small>
                                            </div>
                                        </div>
                                    </a>
                                </li>
                                <li><div class="dropdown-divider"></div></li>
                                <li>
                                    <a class="dropdown-item text-danger" href="javascript:void(0);" id="btnLogoutNavbar">
                                        <i class="bx bx-power-off me-2"></i>
                                        <span class="align-middle">Sair</span>
                                    </a>
                                </li>
                            </ul>
                        </li>
                    </ul>
                </div>
            </nav>"""

tabs_html = """<div class="nav-align-top mb-4">
    <ul class="nav nav-tabs" role="tablist">
        <li class="nav-item">
            <button type="button" class="nav-link active" role="tab" data-bs-toggle="tab" data-bs-target="#navs-boletim" aria-controls="navs-boletim" aria-selected="true">Boletim Consolidado</button>
        </li>
        <li class="nav-item">
            <button type="button" class="nav-link" role="tab" data-bs-toggle="tab" data-bs-target="#navs-notas" aria-controls="navs-notas" aria-selected="false">Consultar Notas</button>
        </li>
        <li class="nav-item">
            <button type="button" class="nav-link" role="tab" data-bs-toggle="tab" data-bs-target="#navs-faltas" aria-controls="navs-faltas" aria-selected="false">Consultar Faltas</button>
        </li>
    </ul>
    <div class="tab-content">
        <div class="tab-pane fade show active" id="navs-boletim" role="tabpanel">
            <div class="table-responsive">
                <table class="table table-bordered table-striped">
                    <thead id="reportHead">
                        <tr><th>Carregando...</th></tr>
                    </thead>
                    <tbody id="reportBody">
                        <tr><td><span class="spinner-border spinner-border-sm text-primary" role="status" aria-hidden="true"></span> Aguarde...</td></tr>
                    </tbody>
                </table>
            </div>
        </div>
        
        <div class="tab-pane fade" id="navs-notas" role="tabpanel">
            <div class="table-responsive">
                <table class="table table-hover">
                    <thead>
                        <tr>
                            <th>Disciplina</th>
                            <th>Avaliação</th>
                            <th>Período</th>
                            <th>Nota Atual</th>
                        </tr>
                    </thead>
                    <tbody id="editNotesBody">
                        <tr><td colspan="4">Carregando notas...</td></tr>
                    </tbody>
                </table>
            </div>
        </div>

        <div class="tab-pane fade" id="navs-faltas" role="tabpanel">
            <div class="table-responsive">
                <table class="table table-hover">
                    <thead>
                        <tr>
                            <th>Disciplina</th>
                            <th>Data</th>
                            <th>Status Atual</th>
                        </tr>
                    </thead>
                    <tbody id="editAttendancesBody">
                        <tr><td colspan="3">Carregando faltas...</td></tr>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</div>"""

js_addition = """
                // Adiciona o preenchimento das tabs de Notas e Faltas (Consultar)
                const tbodyNotas = document.getElementById("editNotesBody");
                if (grades && grades.length > 0) {
                    let htmlNotas = "";
                    grades.forEach(g => {
                        htmlNotas += `<tr>
                            <td>${g.subjectName || "-"}</td>
                            <td>${g.content || "-"}</td>
                            <td>${g.periodName || "-"}</td>
                            <td>${g.value !== null ? g.value.toFixed(1) : "-"}</td>
                        </tr>`;
                    });
                    if (tbodyNotas) tbodyNotas.innerHTML = htmlNotas;
                } else {
                    if (tbodyNotas) tbodyNotas.innerHTML = `<tr><td colspan="4" class="text-center text-muted">Nenhuma nota encontrada.</td></tr>`;
                }

                // Busca as faltas detalhadas
                try {
                    const absRes = await fetch(`/api/attendances/student/${studentId}`, { headers: {"Authorization": `Bearer ${jwtToken}`} });
                    if (absRes.ok) {
                        const absData = await absRes.json();
                        const tbodyFaltas = document.getElementById("editAttendancesBody");
                        if (absData && absData.length > 0) {
                            let htmlFaltas = "";
                            absData.forEach(a => {
                                let badge = "";
                                if (a.status === "PRESENT") badge = "<span class=\"badge bg-label-success\">Presente</span>";
                                else if (a.status === "ABSENT") badge = "<span class=\"badge bg-label-danger\">Falta</span>";
                                else badge = "<span class=\"badge bg-label-warning\">Falta Justificada</span>";
                                
                                htmlFaltas += `<tr>
                                    <td>${a.subjectName || "-"}</td>
                                    <td>${a.lessonDate || "-"}</td>
                                    <td>${badge}</td>
                                </tr>`;
                            });
                            if (tbodyFaltas) tbodyFaltas.innerHTML = htmlFaltas;
                        } else {
                            if (tbodyFaltas) tbodyFaltas.innerHTML = `<tr><td colspan="3" class="text-center text-muted">Nenhuma falta encontrada.</td></tr>`;
                        }
                    }
                } catch(e) {
                    console.error("Erro ao carregar lista de faltas", e);
                }
"""

for file_path in files:
    with open(file_path, "r", encoding="utf-8") as f:
        content = f.read()

    # Replace navbar
    content = re.sub(r"<nav[^>]*id=\"layout-navbar\"[\s\S]*?</nav>", navbar_html, content)

    # In principal, replace the simple card with the tabs. In secretary, replace the tabs with the fixed tabs
    content = re.sub(r"<div class=\"card\">[\s\S]*?<div class=\"table-responsive\">[\s\S]*?</table>\s*</div>\s*</div>", tabs_html, content)
    content = re.sub(r"<div class=\"nav-align-top mb-4\">[\s\S]*?</div>\s*</div>\s*</div>", tabs_html, content)

    # In secretary, there are modals. We need to strip them.
    content = re.sub(r"<!-- Modal para Editar Nota -->[\s\S]*?<!-- Modal para Editar Falta -->[\s\S]*?</div>\s*</div>", "", content)

    # In JS, inject the code to populate tabs at the end of loadReportCard, just before tbody.innerHTML = html;
    if "const tbodyNotas =" not in content:
        content = content.replace("tbody.innerHTML = html;", "tbody.innerHTML = html;" + js_addition)

    with open(file_path, "w", encoding="utf-8") as f:
        f.write(content)

print("Fixed HTML for both files!")

