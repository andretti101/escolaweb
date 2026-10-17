@echo off
chcp 65001 >nul

echo ========================================================
echo     Remoção da Configuração do Domínio: escolaweb
echo ========================================================
echo.

net session >nul 2>&1
if %errorLevel% neq 0 (
    echo [ERRO] Este script precisa ser executado como ADMINISTRADOR.
    echo.
    echo Por favor:
    echo 1. Clique com o botão direito neste arquivo (remover-dominio.bat)
    echo 2. Selecione "Executar como administrador"
    echo.
    pause
    exit /b 1
)

:: Remove redirecionamento de porta 80 -> 8080
netsh interface portproxy delete v4tov4 listenport=80 listenaddress=127.0.0.1 >nul 2>&1
echo [OK] Redirecionamento de porta removido.

:: Remove a linha do arquivo hosts usando PowerShell
powershell -NoProfile -Command "$content = (Get-Content $env:WINDIR\System32\drivers\etc\hosts) | Where-Object { $_ -notmatch 'escolaweb' }; Set-Content -Path $env:WINDIR\System32\drivers\etc\hosts -Value $content"
echo [OK] Entrada 'escolaweb' removida do arquivo hosts.

ipconfig /flushdns >nul
echo [OK] Cache DNS atualizado.

echo.
echo [SUCESSO] Configuração removida com sucesso.
echo.
pause
