// ==============================================================================
// Auth Guard - Proteção de Rotas no Frontend (SPA / MPA)
// Valida a presença e expiração do token JWT antes da renderização da página
// ==============================================================================

let token          = localStorage.getItem("jwt_token");
const refreshToken = localStorage.getItem("refresh_token");

function clearTokensAndRedirect() {
    document.documentElement.style.display = 'none';
    localStorage.removeItem("jwt_token");
    localStorage.removeItem("refresh_token");
    document.cookie = "jwt_token=; path=/; expires=Thu, 01 Jan 1970 00:00:00 UTC; SameSite=Strict";
    window.location.href = "/entrar";
}

function decodeJwtPayload(jwtStr) {
    const payloadBase64 = jwtStr.split('.')[1];
    const base64 = payloadBase64.replace(/-/g, '+').replace(/_/g, '/');
    return JSON.parse(atob(base64));
}

// Redireciona imediatamente para o login caso o usuário não possua token de autenticação
if (!token) {
    clearTokensAndRedirect();
} else {
    try {
        let decodedPayload = decodeJwtPayload(token);
        const isExpired = decodedPayload.exp * 1000 < Date.now();

        if (isExpired) {
            let renewed = false;
            if (refreshToken) {
                try {
                    const xhr = new XMLHttpRequest();
                    xhr.open("POST", "/auth/refresh", false);
                    xhr.setRequestHeader("Content-Type", "application/json");
                    xhr.send(JSON.stringify({ refreshToken: refreshToken }));
                    if (xhr.status === 200) {
                        const data = JSON.parse(xhr.responseText);
                        if (data && data.accessToken) {
                            token = data.accessToken;
                            localStorage.setItem("jwt_token", data.accessToken);
                            if (data.refreshToken) {
                                localStorage.setItem("refresh_token", data.refreshToken);
                            }
                            document.cookie = `jwt_token=${encodeURIComponent(data.accessToken)}; path=/; SameSite=Strict`;
                            decodedPayload = decodeJwtPayload(token);
                            renewed = true;
                        }
                    }
                } catch (err) {
                    console.error("Erro na renovação síncrona do token:", err);
                }
            }

            if (!renewed) {
                clearTokensAndRedirect();
            }
        }

        if (token && decodedPayload && decodedPayload.exp * 1000 >= Date.now()) {
            // Validação de Perfil de Acesso (RBAC) com base no caminho da URL
            const rawRole = (decodedPayload.role || '').toUpperCase();
            const role = rawRole.startsWith("ROLE_") ? rawRole : "ROLE_" + rawRole;
            const currentPath   = window.location.pathname;

            const isSecretaryPath = currentPath.includes("/secretaria/") || currentPath.includes("/secretary/");
            const isPrincipalPath = currentPath.includes("/direcao/") || currentPath.includes("/principal/") || currentPath.includes("/admin/");
            const isTeacherPath   = currentPath.includes("/professor/") || currentPath.includes("/teacher/");
            const isStudentPath   = currentPath.includes("/aluno/") || currentPath.includes("/student/");

            if (isSecretaryPath && role !== "ROLE_SECRETARY" && role !== "ROLE_PRINCIPAL") {
                handleUnauthorized("Acesso Negado! Área restrita.");
            } else if (isPrincipalPath && role !== "ROLE_PRINCIPAL") {
                handleUnauthorized("Acesso Negado! Área exclusiva da Direção.");
            } else if (isStudentPath && role !== "ROLE_STUDENT") {
                handleUnauthorized("Acesso Negado! Área exclusiva do Aluno.");
            } else if (isTeacherPath && role !== "ROLE_TEACHER") {
                handleUnauthorized("Acesso Negado! Área exclusiva do Professor.");
            }
            // Se nenhuma condição disparou, o usuário está autorizado e a página carrega normalmente.
        }

        function handleUnauthorized(msg) {
            document.documentElement.style.display = 'none';
            document.addEventListener("DOMContentLoaded", async () => {
                document.documentElement.style.display = '';
                document.body.style.display = 'none';
                if (window.showAlertModal) {
                    await window.showAlertModal("Acesso Negado", msg);
                }
                window.location.href = "/entrar";
            });
        }
    } catch (error) {
        // Token corrompido ou malformado — limpa os dois tokens e expulsa
        console.error("Token inválido ou corrompido:", error);
        clearTokensAndRedirect();
    }
}