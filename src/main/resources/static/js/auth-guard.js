// auth-guard.js

const token       = localStorage.getItem("jwt_token");
const refreshToken = localStorage.getItem("refresh_token");

if (!token) {
    window.location.href = "/login.html";
} else {
    try {
        const payloadBase64  = token.split('.')[1];
        const base64 = payloadBase64.replace(/-/g, '+').replace(/_/g, '/');
        const decodedPayload = JSON.parse(atob(base64));

        // exp no JWT é em segundos; Date.now() é em milissegundos.
        const isExpired = decodedPayload.exp * 1000 < Date.now();

        if (isExpired) {
            localStorage.removeItem("jwt_token");
            localStorage.removeItem("refresh_token");
            window.location.href = "/login.html";
        } else {
            // Serializa o payload em texto maiúsculo para checar a role
            // independentemente de capitalização (ex: "ROLE_STUDENT" → inclui "STUDENT")
            const payloadString = JSON.stringify(decodedPayload).toUpperCase();
            const currentPath   = window.location.pathname;

            if (currentPath.includes("/secretary/") && !payloadString.includes("SECRETARY")) {
                alert("Acesso Negado! Área exclusiva da Secretaria.");
                window.location.href = "/login.html";
            } else if (currentPath.includes("/admin/") && !payloadString.includes("PRINCIPAL")) {
                alert("Acesso Negado! Área exclusiva da Direção.");
                window.location.href = "/login.html";
            } else if (currentPath.includes("/student/") && !payloadString.includes("STUDENT")) {
                alert("Acesso Negado! Área exclusiva do Aluno.");
                window.location.href = "/login.html";
            } else if (currentPath.includes("/teacher/") && !payloadString.includes("TEACHER")) {
                alert("Acesso Negado! Área exclusiva do Professor.");
                window.location.href = "/login.html";
            }
            // Se nenhuma condição disparou, o usuário está autorizado e a página carrega normalmente.
        }

    } catch (error) {
        // Token corrompido ou malformado — limpa os dois tokens e expulsa
        console.error("Token inválido ou corrompido:", error);
        localStorage.removeItem("jwt_token");
        localStorage.removeItem("refresh_token");
        window.location.href = "/login.html";
    }
}