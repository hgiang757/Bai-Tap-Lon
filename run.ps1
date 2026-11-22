param(
    [ValidateSet('Server','Client','Check')][string]$Role = 'Client',
    [string]$ServerHost = '',
    [ValidateRange(1,65535)][int]$Port = 8888,
    [switch]$ConfigureGemini,
    [switch]$VerifyGemini,
    [switch]$GeminiFromClipboard,
    [switch]$RememberGemini
)
$ErrorActionPreference = 'Stop'
if ($GeminiFromClipboard -and
    ($Role -ne 'Server' -or -not ($ConfigureGemini -or $VerifyGemini -or $RememberGemini))) {
    throw '-GeminiFromClipboard requires a Gemini option in Server mode.'
}
$projectDirectory = $PSScriptRoot
$geminiKeyEntered = $false
$restoreProvider = $false

function Read-SecureKey([Security.SecureString]$secureKey) {
    $keyBuffer = [IntPtr]::Zero
    try {
        $keyBuffer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureKey)
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($keyBuffer)
    } finally {
        if ($keyBuffer -ne [IntPtr]::Zero) {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($keyBuffer)
        }
        $secureKey.Dispose()
    }
}

function Read-GeminiKey {
    if (-not $GeminiFromClipboard) {
        try {
            $entered = Read-SecureKey (Read-Host 'Paste your Gemini key in this masked prompt' -AsSecureString)
        } catch {
            throw 'Cannot read masked input. Try .\Gemini.cmd -ConfigureGemini -GeminiFromClipboard.'
        }
        # Some terminal hosts deliver Ctrl+V as a literal SYN instead of pasting.
        if ($entered -cne [string][char]0x16) { return $entered }
    }
    try {
        return Get-Clipboard -Format Text -Raw -ErrorAction Stop
    } catch {
        throw 'Cannot read the Windows clipboard. Copy the key as text or use the masked prompt.'
    }
}

function Normalize-EnteredGeminiKey([string]$value) {
    if ($value.IndexOf([char]0) -ge 0) {
        throw 'Gemini key input contains U+0000 before Java launch.'
    }
    # Match the edge characters accepted by the Java preflight.
    $value = $value -replace '^[\s\uFEFF]+|[\s\uFEFF]+$', ''
    if (-not $value) { throw 'Gemini key input is empty.' }
    if ($value -match '[\s\p{Cc}\p{Cf}]|^[*\u2022\u25CF]+$') {
        throw 'Gemini key input contains whitespace, control characters, or only masking characters.'
    }
    if ($value -match '[^\u0021-\u00FF]') {
        throw 'Gemini key input contains characters unsupported by the HTTP header.'
    }
    return $value
}

function Read-SavedGeminiKey([string]$path) {
    try {
        $encrypted = Get-Content -LiteralPath $path -Raw
        return Read-SecureKey (ConvertTo-SecureString $encrypted.Trim())
    } catch {
        throw 'Cannot read the saved Gemini key. Run .\Gemini.cmd -ConfigureGemini to replace it.'
    }
}

function Save-GeminiKey([string]$path, [string]$value) {
    $directory = Split-Path -Parent $path
    New-Item -Path $directory -ItemType Directory -Force | Out-Null
    $secureKey = ConvertTo-SecureString $value -AsPlainText -Force
    $temporary = Join-Path $directory ([Guid]::NewGuid().ToString() + '.tmp')
    try {
        $encrypted = ConvertFrom-SecureString $secureKey
        [IO.File]::WriteAllText($temporary, $encrypted, [Text.Encoding]::ASCII)
        if ([IO.File]::Exists($path)) {
            [IO.File]::Replace($temporary, $path, [NullString]::Value)
        } else {
            [IO.File]::Move($temporary, $path)
        }
    } finally {
        $secureKey.Dispose()
        [IO.File]::Delete($temporary)
    }
}

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
            if ($ConfigureGemini -or $VerifyGemini -or $RememberGemini) {
                $previousProvider = $env:AUCTION_AI_PROVIDER
                $restoreProvider = $true
                $env:AUCTION_AI_PROVIDER = 'gemini'
            }
            if (-not $env:AUCTION_AI_PROVIDER -or $env:AUCTION_AI_PROVIDER.Trim() -in @('', 'gemini')) {
                $keyPath = if ($env:LOCALAPPDATA) {
                    Join-Path $env:LOCALAPPDATA 'DigitalAuction\gemini-key.dpapi'
                }
                $previousGeminiKey = $env:GEMINI_API_KEY
                $hasEnvironmentKey = $previousGeminiKey -match '[^\s\uFEFF]'
                $explicitInput = $ConfigureGemini -or $GeminiFromClipboard
                $geminiValue = $null
                try {
                    if (-not $explicitInput -and -not $hasEnvironmentKey -and
                        $keyPath -and (Test-Path -LiteralPath $keyPath)) {
                        try {
                            $geminiValue = Read-SavedGeminiKey $keyPath
                        } catch {
                            if ($RememberGemini -or $VerifyGemini) { throw }
                            Write-Warning $_.Exception.Message
                        }
                    }
                    if ($explicitInput -or
                        (-not ($hasEnvironmentKey -or $geminiValue) -and
                         ($RememberGemini -or $VerifyGemini))) {
                        $geminiValue = Normalize-EnteredGeminiKey (Read-GeminiKey)
                        # Verifying a key supplied via clipboard never replaces a stored key.
                        if ($RememberGemini -and ($ConfigureGemini -or -not $VerifyGemini)) {
                            if (-not $keyPath) { throw 'Windows LOCALAPPDATA is unavailable.' }
                            try {
                                Save-GeminiKey $keyPath $geminiValue
                                Write-Host 'Gemini key saved for this Windows account.'
                            } catch {
                                Write-Warning 'Windows could not save the encrypted key. Gemini will use it for this run.'
                            }
                        }
                    }
                    if ($geminiValue) {
                        $env:GEMINI_API_KEY = $geminiValue
                        $geminiKeyEntered = $true
                    }
                } finally {
                    $geminiValue = $null
                }
            }
            if ($VerifyGemini) {
                & mvn -B -Plive-gemini surefire:test '-Dauction.ai.live=true' '-Dauction.ui.tests=true' `
                    '-Dtest=ServerUiIntegrationTest#liveGeminiThroughAuctionRoom'
                if ($LASTEXITCODE -ne 0) { throw 'Live Gemini verification failed.' }
                if (-not $ConfigureGemini) { return }
            }
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
} finally {
    if ($geminiKeyEntered) { $env:GEMINI_API_KEY = $previousGeminiKey }
    if ($restoreProvider) { $env:AUCTION_AI_PROVIDER = $previousProvider }
    Pop-Location
}
