@echo off
chcp 65001 >nul

:: Auto-elevação para Administrador via UAC
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo [INFO] Solicitando permissão de Administrador ao Windows...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Start-Process -FilePath '%~f0' -Verb RunAs"
    exit /b
)

echo ========================================================
echo     Configuração do Domínio Local: escolaweb
echo ========================================================
echo.

set HOSTS_FILE=%WINDIR%\System32\drivers\etc\hosts

:: Adiciona escolaweb e escolaweb.local no arquivo hosts caso não existam
findstr /i /c:"escolaweb" "%HOSTS_FILE%" >nul 2>&1
if %errorLevel% equ 0 (
    echo [OK] O domínio 'escolaweb' já estava no arquivo hosts.
) else (
    echo Adicionando 'escolaweb' e 'escolaweb.local' em %HOSTS_FILE%...
    echo. >> "%HOSTS_FILE%"
    echo # Dominio local EscolaWeb >> "%HOSTS_FILE%"
    echo 127.0.0.1 escolaweb >> "%HOSTS_FILE%"
    echo 127.0.0.1 escolaweb.local >> "%HOSTS_FILE%"
    echo ::1 escolaweb >> "%HOSTS_FILE%"
    echo ::1 escolaweb.local >> "%HOSTS_FILE%"
    echo [SUCESSO] Domínios adicionados ao arquivo hosts!
)

:: Limpa o cache DNS do Windows
echo.
echo Atualizando cache DNS do Windows...
ipconfig /flushdns >nul
echo [OK] Cache DNS atualizado com sucesso.

:: Configura o redirecionamento da porta 80 para 8080
echo.
echo Configurando redirecionamento de porta (Porta 80 -> Porta 8080)...
netsh interface portproxy delete v4tov4 listenport=80 listenaddress=127.0.0.1 >nul 2>&1
netsh interface portproxy delete v4tov4 listenport=80 listenaddress=0.0.0.0 >nul 2>&1
netsh interface portproxy add v4tov4 listenport=80 listenaddress=127.0.0.1 connectport=8080 connectaddress=127.0.0.1 >nul 2>&1
netsh interface portproxy add v4tov4 listenport=80 listenaddress=0.0.0.0 connectport=8080 connectaddress=127.0.0.1 >nul 2>&1

if %errorLevel% equ 0 (
    echo [SUCESSO] Redirecionamento da porta 80 para 8080 ativado com sucesso!
) else (
    echo [AVISO] Não foi possível ativar o portproxy. Você também pode acessar via http://escolaweb:8080
)

echo.
echo ========================================================
echo  TUDO PRONTO!
echo.
echo  No seu navegador, digite:
echo      http://escolaweb
echo  ou:
echo      http://escolaweb.local
echo.
echo  (Dica: certifique-se de digitar "http://" antes)
echo ========================================================
echo.
pause
