// auth-guard.js

const token       = localStorage.getItem("jwt_token");
const refreshToken = localStorage.getItem("refresh_token");

if (!token) {
    document.cookie = "jwt_token=; path=/; expires=Thu, 01 Jan 1970 00:00:00 UTC; SameSite=Strict";
    window.location.href = "/entrar";
} else {
    try {
        const payloadBase64  = token.split('.')[1];
        const base64 = payloadBase64.replace(/-/g, '+').replace(/_/g, '/');
        const decodedPayload = JSON.parse(atob(base64));

        const isExpired = decodedPayload.exp * 1000 < Date.now();

        if (isExpired) {
            localStorage.removeItem("jwt_token");
            localStorage.removeItem("refresh_token");
            document.cookie = "jwt_token=; path=/; expires=Thu, 01 Jan 1970 00:00:00 UTC; SameSite=Strict";
            window.location.href = "/entrar";
        } else {
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
        localStorage.removeItem("jwt_token");
        localStorage.removeItem("refresh_token");
        document.cookie = "jwt_token=; path=/; expires=Thu, 01 Jan 1970 00:00:00 UTC; SameSite=Strict";
        window.location.href = "/entrar";
    }
}