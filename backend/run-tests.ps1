$ErrorActionPreference = "Stop"

function Find-JavaHome {
    $candidates = @(
        $env:JAVA_HOME,
        "C:\Users\$env:USERNAME\.jdks",
        "C:\Program Files\Java",
        "C:\Program Files\Microsoft",
        "C:\Program Files\JetBrains"
    ) | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }

    foreach ($candidate in $candidates) {
        if (Test-Path $candidate) {
            $javaExe = Get-ChildItem -Path $candidate -Filter java.exe -Recurse -File -ErrorAction SilentlyContinue | Select-Object -First 1
            if ($javaExe) {
                return Split-Path -Parent (Split-Path -Parent $javaExe.FullName)
            }
        }
    }

    return $null
}

$javaHome = Find-JavaHome

if (-not $javaHome) {
    throw "No JDK installation was found. Install a JDK and set JAVA_HOME, or add one under C:\Users\$env:USERNAME\.jdks."
}

$env:JAVA_HOME = $javaHome
$env:Path = "$javaHome\bin;$env:Path"

Write-Host "Using JAVA_HOME=$javaHome"

$argsToPass = @()
if ($args.Count -gt 0) {
    $argsToPass = $args
} else {
    $argsToPass = @("test")
}

& "$PSScriptRoot\mvnw.cmd" @argsToPass
exit $LASTEXITCODE
