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

        const isExpired = decodedPayload.exp * 1000 < Date.now();

        if (isExpired) {
            localStorage.removeItem("jwt_token");
            localStorage.removeItem("refresh_token");
            window.location.href = "/login.html";
        } else {
            // Serializa o payload em texto maiúsculo para checar a role
            const payloadString = JSON.stringify(decodedPayload).toUpperCase();
            const currentPath   = window.location.pathname;

            if (currentPath.includes("/secretary/") && !payloadString.includes("SECRETARY") && !payloadString.includes("PRINCIPAL")) {
                if(window.alert) window.alert("Acesso Negado! Área restrita.");
                setTimeout(() => window.location.href = "/login.html", 1500);
            } else if ((currentPath.includes("/admin/") || currentPath.includes("/principal/")) && !payloadString.includes("PRINCIPAL")) {
                if(window.alert) window.alert("Acesso Negado! Área exclusiva da Direção.");
                setTimeout(() => window.location.href = "/login.html", 1500);
            } else if (currentPath.includes("/student/") && !payloadString.includes("STUDENT")) {
                if(window.alert) window.alert("Acesso Negado! Área exclusiva do Aluno.");
                setTimeout(() => window.location.href = "/login.html", 1500);
            } else if (currentPath.includes("/teacher/") && !payloadString.includes("TEACHER")) {
                if(window.alert) window.alert("Acesso Negado! Área exclusiva do Professor.");
                setTimeout(() => window.location.href = "/login.html", 1500);
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