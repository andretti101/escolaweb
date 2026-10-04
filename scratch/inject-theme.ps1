#requires -version 5
# Insere dark-mode.css e theme.js no <head> de todas as páginas HTML (exceto componentes).
# Operação byte-preserving (Latin1 1:1) para não alterar encoding / quebras de linha existentes. Idempotente.
$ErrorActionPreference = 'Stop'
$root = Join-Path $PSScriptRoot '..\src\main\resources\static'
$latin1 = [System.Text.Encoding]::GetEncoding(28591)
$pattern = '(?m)^([ \t]*)(<link[^>]*theme-default\.css[^>]*>)[ \t]*(\r?\n)'
$changed = 0; $skipped = 0; $missing = @()
Get-ChildItem -Recurse -Path $root -Filter *.html | Where-Object { $_.FullName -notmatch '\\components\\' } | ForEach-Object {
    $bytes = [System.IO.File]::ReadAllBytes($_.FullName)
    $text = $latin1.GetString($bytes)
    if ($text -match 'dark-mode\.css') { $skipped++; return }
    $m = [regex]::Match($text, $pattern)
    if (-not $m.Success) { $missing += $_.FullName; return }
    $indent = $m.Groups[1].Value; $nl = $m.Groups[3].Value
    $insert = $m.Value + $indent + '<link rel="stylesheet" href="/assets/css/dark-mode.css" />' + $nl + $indent + '<script src="/js/theme.js"></script>' + $nl
    $text = $text.Substring(0, $m.Index) + $insert + $text.Substring($m.Index + $m.Length)
    [System.IO.File]::WriteAllBytes($_.FullName, $latin1.GetBytes($text))
    $changed++
}
"changed=$changed skipped=$skipped missing=$($missing.Count)"
$missing
