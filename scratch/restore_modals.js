const fs = require("fs");
let content = fs.readFileSync("c:\\escolaweb\\src\\main\\resources\\static\\secretary\\student-performance.html", "utf-8");

// First, fix the layout divs that got broken by fuzzy replace!
// Find script tags
const scriptIdx = content.indexOf("<script src=\"/assets/vendor/libs/jquery/jquery.js\"></script>");
const contentBeforeScripts = content.substring(0, scriptIdx);
const contentAfterScripts = content.substring(scriptIdx);

// In contentBeforeScripts, check if it has the closing layout divs:
if (!contentBeforeScripts.includes("content-backdrop")) {
    // Add them back
    content = contentBeforeScripts + `
                    </div>
                    <div class="content-backdrop fade"></div>
                </div>
            </div>
        </div>
        <div class="layout-overlay layout-menu-toggle"></div>
    </div>\n
` + contentAfterScripts;
}

// Add modals before closing content-wrapper
const modalsHtml = `
<!-- Modal para Editar Nota -->
<div class="modal fade" id="modalEditGrade" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog modal-dialog-centered" role="document">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title">Editar Nota</h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
      </div>
      <div class="modal-body">
        <input type="hidden" id="editGradeId">
        <div class="mb-3">
          <label class="form-label">Avaliação</label>
          <input type="text" class="form-control" id="editGradeTitle" readonly>
        </div>
        <div class="mb-3">
          <label class="form-label">Nova Nota</label>
          <input type="number" class="form-control" id="editGradeValue" step="0.1" min="0" max="10">
        </div>
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Cancelar</button>
        <button type="button" class="btn btn-primary" onclick="saveGrade()">Salvar Nota</button>
      </div>
    </div>
  </div>
</div>

<!-- Modal para Editar Falta -->
<div class="modal fade" id="modalEditAttendance" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog modal-dialog-centered" role="document">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title">Editar Frequência</h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
      </div>
      <div class="modal-body">
        <input type="hidden" id="editAttendanceId">
        <div class="mb-3">
          <label class="form-label">Data da Aula</label>
          <input type="text" class="form-control" id="editAttendanceDate" readonly>
        </div>
        <div class="mb-3">
          <label class="form-label">Novo Status</label>
          <select class="form-select" id="editAttendanceStatus">
            <option value="PRESENT">Presente</option>
            <option value="ABSENT">Falta</option>
            <option value="EXCUSED">Falta Justificada</option>
          </select>
        </div>
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Cancelar</button>
        <button type="button" class="btn btn-primary" onclick="saveAttendance()">Salvar Frequência</button>
      </div>
    </div>
  </div>
</div>
`;

if (!content.includes("modalEditGrade")) {
    content = content.replace("</div>\n                    <div class=\"content-backdrop fade\"></div>", modalsHtml + "\n</div>\n                    <div class=\"content-backdrop fade\"></div>");
}

// Add JS functions
const jsFunctions = `
        let gradeModal, attendanceModal;
        document.addEventListener("DOMContentLoaded", () => {
            gradeModal = new bootstrap.Modal(document.getElementById("modalEditGrade"));
            attendanceModal = new bootstrap.Modal(document.getElementById("modalEditAttendance"));
        });

        function openGradeModal(id, title, value) {
            document.getElementById("editGradeId").value = id;
            document.getElementById("editGradeTitle").value = title || "";
            document.getElementById("editGradeValue").value = value !== null ? value : "";
            gradeModal.show();
        }

        async function saveGrade() {
            const id = document.getElementById("editGradeId").value;
            const value = document.getElementById("editGradeValue").value;
            if(!value) return alert("Insira uma nota");
            try {
                const res = await fetch(\`/api/grades/\${id}\`, {
                    method: "PUT",
                    headers: {
                        "Authorization": \`Bearer \${jwtToken}\`,
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({ value: parseFloat(value) })
                });
                if(res.ok) {
                    gradeModal.hide();
                    loadReportCard();
                } else {
                    alert("Erro ao salvar nota");
                }
            } catch(e) { console.error(e); }
        }

        function openAttendanceModal(id, date, status) {
            document.getElementById("editAttendanceId").value = id;
            document.getElementById("editAttendanceDate").value = date || "";
            document.getElementById("editAttendanceStatus").value = status || "PRESENT";
            attendanceModal.show();
        }

        async function saveAttendance() {
            const id = document.getElementById("editAttendanceId").value;
            const status = document.getElementById("editAttendanceStatus").value;
            try {
                const res = await fetch(\`/api/attendances/\${id}\`, {
                    method: "PUT",
                    headers: {
                        "Authorization": \`Bearer \${jwtToken}\`,
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({ status })
                });
                if(res.ok) {
                    attendanceModal.hide();
                    loadReportCard();
                } else {
                    alert("Erro ao salvar frequência");
                }
            } catch(e) { console.error(e); }
        }
`;

if (!content.includes("saveGrade()")) {
    content = content.replace("} catch (e) {", jsFunctions + "\n            } catch (e) {");
}

fs.writeFileSync("c:\\escolaweb\\src\\main\\resources\\static\\secretary\\student-performance.html", content, "utf-8");
console.log("Restored modals and edit logic for Secretary.");
