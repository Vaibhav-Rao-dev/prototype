# run-tests.ps1
# Helper script to run mvn test, package, and run the shaded jar while capturing logs to target\

Set-StrictMode -Version Latest

if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    Write-Error "mvn not found in PATH. Please install Maven or use the Maven wrapper."
    exit 2
}

# Ensure JAVA_HOME is set correctly for Maven; if missing, try to infer from java.exe
if (-not $env:JAVA_HOME -or -not (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
    $javaCmd = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCmd) {
        $javaPath = $javaCmd.Source
        # javaPath may point to java.exe inside bin; set JAVA_HOME to parent folder
        try {
            $javaFolder = Split-Path $javaPath -Parent
            $candidate = Split-Path $javaFolder -Parent
            if (Test-Path (Join-Path $candidate 'bin\java.exe')) {
                Write-Output "Setting JAVA_HOME to $candidate for this script run"
                $env:JAVA_HOME = $candidate
            }
        } catch { }
    }
}

if (-not $env:JAVA_HOME -or -not (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
    Write-Warning "JAVA_HOME is not set or doesn't point to a valid Java installation. Maven may fail."
}

mkdir -Force target | Out-Null

Write-Output "Running mvn test -> target\mvn-test.log"
mvn -B -DskipTests=false test > target\mvn-test.log 2>&1
if ($LASTEXITCODE -ne 0) { Write-Output "mvn test failed (exit $LASTEXITCODE). Check target\mvn-test.log" }

Write-Output "Running mvn package (skip tests) -> target\mvn-package.log"
mvn -B package -DskipTests=true > target\mvn-package.log 2>&1
if ($LASTEXITCODE -ne 0) { Write-Output "mvn package failed (exit $LASTEXITCODE). Check target\mvn-package.log" }

$jar = Get-ChildItem -Path target -Filter *-shaded.jar -Recurse | Select-Object -First 1
if (-not $jar) { Write-Output "Jar not found in target. Aborting run."; exit 3 }

Write-Output "Running jar $($jar.FullName) -> target\server.log"
# Invoke java directly and redirect both stdout and stderr to target\server.log using PowerShell redirection
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Error "java not found in PATH. Please install Java or set JAVA_HOME. Skipping runtime run."
} else {
    & java -jar "$($jar.FullName)" *> target\server.log
}

Write-Output "Done. Logs: target\mvn-test.log, target\mvn-package.log, target\server.log"
