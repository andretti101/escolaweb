const fs = require("fs");
const path = "c:\\escolaweb\\src\\main\\resources\\static\\principal\\students.html";
let content = fs.readFileSync(path, "utf-8");

// Fix headers
content = content.replace(
    /<th>Status<\/th>[\s\n\r]*<\/tr>/,
    `<th>Status</th>
                                    <th class="text-center">Turma</th>
                                    <th class="text-center">Ação</th>
                                </tr>`
);

// Fix filter dropdown UI
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

// Fix body cells
content = content.replace(
    /<\/td>\s*<td class="text-center">\s*<a href="\/principal\/student-performance\.html\?studentId=\$\{student\.id\}"/,
    `</td>
            <td class="text-center">\${escapeHtml(student.className) || '-'}</td>
            <td class="text-center">
              <a href="/principal/student-performance.html?studentId=\${student.id}"`
);

// Update JS logic
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

content = content.replace(/applyFilters\(\);/, 'populateClassDropdown();\n            applyFilters();');

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

fs.writeFileSync(path, content, "utf-8");
console.log("Updated principal/students.html completely");
