const fs = require("fs");
const path = "c:\\escolaweb\\src\\main\\resources\\static\\principal\\students.html";
let content = fs.readFileSync(path, "utf-8");

content = content.replace(
    / : '<span class="badge bg-label-secondary">Inativo<\/span>'}[\s\n\r]*<\/td>[\s\n\r]*<\/tr>/,
    ` : '<span class="badge bg-label-secondary">Inativo</span>'}
            </td>
            <td class="text-center">\${escapeHtml(student.className) || '-'}</td>
            <td class="text-center">
              <a href="/principal/student-performance.html?studentId=\${student.id}" class="btn btn-sm btn-icon btn-outline-primary" title="Ver Desempenho">
                <i class="bx bx-bar-chart-alt-2"></i>
              </a>
            </td>
          </tr>`
);

fs.writeFileSync(path, content, "utf-8");
console.log("Updated cells in principal/students.html");
