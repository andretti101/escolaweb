#requires -version 5
# Move o <link> de dark-mode.css para logo antes de </head>, para que ele carregue DEPOIS de
# demo.css, page-auth.css e dos <style> inline de cada pagina (e assim vença empates de cascata).
# Byte-preserving (Latin1 1:1). Idempotente.
$ErrorActionPreference = 'Stop'
$root = Join-Path $PSScriptRoot '..\src\main\resources\static'
$latin1 = [System.Text.Encoding]::GetEncoding(28591)
$linkRx = '(?m)^[ \t]*<link rel="stylesheet" href="/assets/css/dark-mode\.css" />[ \t]*\r?\n'
$moved = 0; $bad = @()
Get-ChildItem -Recurse -Path $root -Filter *.html | Where-Object { $_.FullName -notmatch '\\components\\' } | ForEach-Object {
    $text = $latin1.GetString([System.IO.File]::ReadAllBytes($_.FullName))
    $heads = [regex]::Matches($text, '(?im)^([ \t]*)</head>')
    if ($heads.Count -ne 1) { $bad += "$($_.FullName) heads=$($heads.Count)"; return }
    $m = [regex]::Match($text, $linkRx)
    if (-not $m.Success) { $bad += "$($_.FullName) nolink"; return }
    $text = $text.Remove($m.Index, $m.Length)
    $h = [regex]::Match($text, '(?im)^([ \t]*)</head>')
    $nl = if ($text -match "\r\n") { "`r`n" } else { "`n" }
    $text = $text.Insert($h.Index, '    <link rel="stylesheet" href="/assets/css/dark-mode.css" />' + $nl)
    [System.IO.File]::WriteAllBytes($_.FullName, $latin1.GetBytes($text))
    $moved++
}
"moved=$moved bad=$($bad.Count)"
$bad
