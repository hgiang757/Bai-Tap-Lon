param(
    [ValidateSet('Server','Client','Check')][string]$Role = 'Client',
    [string]$ServerHost = '',
    [ValidateRange(1,65535)][int]$Port = 8888
)
$ErrorActionPreference = 'Stop'
$projectDirectory = $PSScriptRoot
if (-not (Get-Command java -ErrorAction SilentlyContinue) -or -not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    throw 'Install JDK 25 or newer and Maven, then add java and mvn to PATH.'
}
Push-Location -LiteralPath $projectDirectory
try {
    switch ($Role) {
        'Check' {
            & mvn -B verify
            if ($LASTEXITCODE -ne 0) { throw 'Tests failed. See the output above.' }
        }
        'Server' {
            if (-not $ServerHost) { $ServerHost = '0.0.0.0' }
            & mvn -B package -DskipTests
            if ($LASTEXITCODE -ne 0) { throw 'Server build failed. See the output above.' }
            & java "-Dauction.bind=$ServerHost" "-Dauction.port=$Port" '-Dauction.demo=false' -jar target/digital-auction-1.0.0.jar
            if ($LASTEXITCODE -ne 0) { throw 'Server stopped with an error. See the output above.' }
        }
        'Client' {
            if (-not $ServerHost) {
                $ServerHost = Read-Host 'Server IPv4 or hostname (Enter for 127.0.0.1)'
            }
            if (-not $ServerHost) { $ServerHost = '127.0.0.1' }
            & mvn -B javafx:run '-Dauction.mode=server' "-Dauction.host=$ServerHost" "-Dauction.port=$Port"
            if ($LASTEXITCODE -ne 0) { throw 'Client launch failed. See the output above.' }
        }
    }
} finally { Pop-Location }
