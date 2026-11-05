param(
    [ValidateSet('Server','Client','Check')][string]$Role = 'Client',
    [string]$ServerHost = '127.0.0.1',
    [ValidateRange(1,65535)][int]$Port = 8888,
    [switch]$Mock
)
$ErrorActionPreference = 'Stop'
$projectDirectory = $PSScriptRoot
if (-not (Get-Command java -ErrorAction SilentlyContinue) -or -not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    throw 'Install JDK 25 and Maven, then add java and mvn to PATH.'
}
Push-Location -LiteralPath $projectDirectory
try {
    switch ($Role) {
        'Check' {
            & mvn -B verify
            if ($LASTEXITCODE -ne 0) { throw 'Tests failed. See the output above.' }
        }
        'Server' {
            & mvn -B -f auction-server/pom.xml package -DskipTests
            if ($LASTEXITCODE -ne 0) { throw 'Server build failed. See the output above.' }
            Push-Location -LiteralPath (Join-Path $projectDirectory 'auction-server')
            try { & java "-Dauction.host=$ServerHost" "-Dauction.port=$Port" '-Dauction.demo=true' -jar target/auction-server-1.0.0.jar }
            finally { Pop-Location }
        }
        'Client' {
            $clientMode = if ($Mock) { 'mock' } else { 'server' }
            & mvn -B -f auction-client-ui/pom.xml javafx:run "-Dauction.mode=$clientMode" "-Dauction.host=$ServerHost" "-Dauction.port=$Port"
            if ($LASTEXITCODE -ne 0) { throw 'Client launch failed. See the output above.' }
        }
    }
} finally { Pop-Location }
