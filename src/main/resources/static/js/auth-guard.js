// ==============================================================================
// Auth Guard - Proteção de Rotas no Frontend (SPA / MPA)
// Valida a presença e expiração do token JWT antes da renderização da página
// ==============================================================================

let token          = localStorage.getItem("jwt_token");
const refreshToken = localStorage.getItem("refresh_token");

function clearTokensAndRedirect() {
    document.documentElement.style.display = 'none';
    const rt = localStorage.getItem("refresh_token");
    fetch('/auth/logout', {
        method: 'POST',
        keepalive: true,
        ...(rt ? {
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ refreshToken: rt })
        } : {})
    }).catch(() => {}).finally(() => {
        const themeKey = (window.EscolaTheme && window.EscolaTheme.STORAGE_KEY) || 'escolaweb-theme';
        const savedTheme = localStorage.getItem(themeKey);
        localStorage.clear();
        if (savedTheme) localStorage.setItem(themeKey, savedTheme);
        window.location.href = "/entrar";
    });
}

function decodeJwtPayload(jwtStr) {
    const payloadBase64 = jwtStr.split('.')[1];
    let base64 = payloadBase64.replace(/-/g, '+').replace(/_/g, '/');
    while (base64.length % 4) base64 += '=';
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
            } else if (isStudentPath && role !== "ROLE_STUDENT" && role !== "ROLE_PRINCIPAL") {
                handleUnauthorized("Acesso Negado! Área exclusiva do Aluno.");
            } else if (isTeacherPath && role !== "ROLE_TEACHER" && role !== "ROLE_PRINCIPAL") {
                handleUnauthorized("Acesso Negado! Área exclusiva do Professor.");
            }
            // Se nenhuma condição disparou, o usuário está autorizado e a página carrega normalmente.
        }

        function handleUnauthorized(msg) {
            document.documentElement.style.display = 'none';
            document.addEventListener("DOMContentLoaded", async () => {
                document.documentElement.style.display = '';
                const lw = document.querySelector('.layout-wrapper');
                if (lw) lw.style.visibility = 'hidden';
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