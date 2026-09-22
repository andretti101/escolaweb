const fs = require("fs");
const filePaths = [
    "c:\\escolaweb\\src\\main\\resources\\static\\principal\\students.html"
];

for (const path of filePaths) {
    if (!fs.existsSync(path)) continue;
    let content = fs.readFileSync(path, "utf-8");

    // Add class filter dropdown in UI
    if (!content.includes('id="classFilter"')) {
        content = content.replace(
            /<!-- Filtros de status -->\s*<div class="btn-group" role="group">/,
            `<div class="d-flex align-items-center gap-3">
                                <select id="classFilter" class="form-select form-select-sm" style="width: auto;">
                                    <option value="all">Todas as Turmas</option>
                                </select>
                                <div class="btn-group" role="group">`
        );
        content = content.replace(
            /<label class="btn btn-outline-primary btn-sm" for="filterAll">Todos<\/label>\s*<\/div>/,
            `<label class="btn btn-outline-primary btn-sm" for="filterAll">Todos</label>\n                                </div>\n                            </div>`
        );
    }

    // Add Turma column header
    if (!content.includes('<th class="text-center">Turma</th>')) {
        content = content.replace(
            /<th class="text-center">Ações?<\/th>/,
            '<th class="text-center">Turma</th>\n                                    <th class="text-center">Ação</th>'
        );
    }

    // Add Turma data cell
    if (!content.includes('${escapeHtml(student.className) || \'-\'}</td>')) {
        content = content.replace(
            /<td class="text-center">\s*<div class="dropdown">/,
            '<td class="text-center">${escapeHtml(student.className) || \'-\'}</td>\n            <td class="text-center">\n              <div class="dropdown">'
        );
    }

    // Update filter logic
    if (!content.includes('let classFilter = \'all\';')) {
        content = content.replace(
            /function applyFilters\(\) {/,
            `let classFilter = 'all';\n\n    function applyFilters() {`
        );
        content = content.replace(
            /if \(statusFilter === 'inactive'\) {[^}]*}\n/,
            `if (statusFilter === 'inactive') {\n            result = result.filter(s => !s.active);\n        }\n\n        if (classFilter !== 'all') {\n            result = result.filter(s => s.className === classFilter);\n        }\n`
        );
        content = content.replace(
            /\(s\.registrationNumber \|\| ''\)\.toLowerCase\(\)\.includes\(term\)/,
            `(s.registrationNumber || '').toLowerCase().includes(term) ||\n                (s.className || '').toLowerCase().includes(term)`
        );
    }

    // Add populateClassDropdown function
    if (!content.includes('function populateClassDropdown()')) {
        const populateFunc = `
    function populateClassDropdown() {
        const select = document.getElementById('classFilter');
        if (!select) return;
        const currentVal = select.value;
        select.innerHTML = '<option value="all">Todas as Turmas</option>';
        const classes = [...new Set(allStudents.map(s => s.className).filter(c => c && c !== '-'))].sort();
        classes.forEach(c => {
            const opt = document.createElement('option');
            opt.value = c;
            opt.textContent = c;
            select.appendChild(opt);
        });
        if (classes.includes(currentVal)) {
            select.value = currentVal;
            classFilter = currentVal;
        }
    }
`;
        content = content.replace(/async function loadStudents\(\) {/, populateFunc + "\n    async function loadStudents() {");
    }

    // Call populateClassDropdown
    if (!content.includes('populateClassDropdown();\n            applyFilters();')) {
        content = content.replace(/applyFilters\(\);/, 'populateClassDropdown();\n            applyFilters();');
    }

    // Add event listener for dropdown
    if (!content.includes('classFilterSelect.addEventListener')) {
        const listener = `
        const classFilterSelect = document.getElementById('classFilter');
        if (classFilterSelect) {
            classFilterSelect.addEventListener('change', e => {
                classFilter = e.target.value;
                applyFilters();
            });
        }
`;
        content = content.replace(/document\.getElementById\('globalSearch'\)\.addEventListener/, listener + "\n        document.getElementById('globalSearch').addEventListener");
    }

    fs.writeFileSync(path, content, "utf-8");
}
console.log("Updated principal/students.html");
