$ErrorActionPreference = 'Stop'
$project = (Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
$javaExe = (Get-Command java.exe).Source
$fixture = Join-Path $project ("target/gemini-launch-test-" + [Guid]::NewGuid())
New-Item -ItemType Directory -Path $fixture -Force | Out-Null
$probe = Join-Path $fixture 'GeminiEnvironmentProbe.java'
$opaqueKey = [Guid]::NewGuid().ToString() + '*."'
$oldKey = $env:GEMINI_API_KEY
$oldProvider = $env:AUCTION_AI_PROVIDER
$oldExpected = $env:AUCTION_TEST_EXPECTED_GEMINI
$oldLocal = $env:LOCALAPPDATA
$savedKeyPath = Join-Path $fixture 'DigitalAuction\gemini-key.dpapi'
$observed = @{ Runs = 0; Prompts = 0 }

function Read-Host {
    param([string]$Prompt, [switch]$AsSecureString)
    if (-not $AsSecureString) { throw 'The launcher did not request masked input.' }
    $observed.Prompts++
    $entered = if ($sendPasteShortcut) { [string][char]0x16 } else { $fakeKey }
    ConvertTo-SecureString $entered -AsPlainText -Force
}

function Get-Clipboard {
    [CmdletBinding()]
    param([string]$Format, [switch]$Raw)
    if ($Format -ne 'Text' -or -not $Raw) { throw 'The clipboard must be read as raw text.' }
    if ($clipboardFails) { throw 'Test clipboard unavailable' }
    $fakeKey
}

function Invoke-JavaProbe {
    & $javaExe $probe
    if ($LASTEXITCODE -ne 0) { throw 'The Java child received different environment data.' }
    $observed.Runs++
}

function mvn {
    if ($args -contains 'surefire:test') { Invoke-JavaProbe }
    $global:LASTEXITCODE = 0
}

function java { Invoke-JavaProbe }

try {
    Set-Content -LiteralPath $probe -Encoding ASCII -Value @'
class GeminiEnvironmentProbe {
    public static void main(String[] args) {
        String expected = System.getenv("AUCTION_TEST_EXPECTED_GEMINI");
        if (expected == null || !expected.equals(System.getenv("GEMINI_API_KEY"))
            || !"gemini".equals(System.getenv("AUCTION_AI_PROVIDER"))) {
            System.exit(1);
        }
    }
}
'@
    $env:GEMINI_API_KEY = 'prior-test-value'
    $env:AUCTION_AI_PROVIDER = 'ollama'
    $launcher = Join-Path $project 'run.ps1'
    $escape = [char]27
    $validSamples = @(
        $opaqueKey,
        ([string][char]0xfeff + "`t" + $opaqueKey + "`r`n")
    )
    foreach ($fakeKey in $validSamples) {
        $env:AUCTION_TEST_EXPECTED_GEMINI = $opaqueKey
        $output = @(& $launcher Server -ConfigureGemini 6>&1)
        $output += & $launcher Server -VerifyGemini -GeminiFromClipboard 6>&1
        if (($output -join "`n").Contains($opaqueKey)) { throw 'The launcher wrote the test value.' }
    }
    $invalidSamples = @(
        ($opaqueKey + [char]0x200b + $opaqueKey),
        ($opaqueKey + [char]0x100),
        ($opaqueKey + [char]0x16 + $opaqueKey),
        ("${escape}[200~" + $opaqueKey + "${escape}[201~"),
        "$opaqueKey $opaqueKey",
        '***',
        ([string][char]0xfeff + "`t")
    )
    foreach ($fakeKey in $invalidSamples) {
        foreach ($inputOptions in @(@(), @('-GeminiFromClipboard'))) {
            try {
                & $launcher Server -ConfigureGemini @inputOptions 6>&1 | Out-Null
                throw 'Invalid key input passed to the environment.'
            } catch {
                if ($_.Exception.Message -notmatch '^Gemini key input (contains|is empty)') { throw }
                if ($_.Exception.Message.Contains($opaqueKey)) { throw 'The launcher disclosed the key.' }
            }
        }
    }
    $clipboardFails = $true
    try {
        & $launcher Server -VerifyGemini -GeminiFromClipboard 6>&1 | Out-Null
        throw 'Failed clipboard read was accepted.'
    } catch {
        if (-not $_.Exception.Message.StartsWith('Cannot read the Windows clipboard.')) { throw }
    }
    $clipboardFails = $false
    # Verification and serving inherit one captured value when both flags are selected.
    $fakeKey = $opaqueKey
    $output = & $launcher Server -ConfigureGemini -VerifyGemini -GeminiFromClipboard 6>&1
    if (($output -join "`n").Contains($fakeKey)) { throw 'The launcher wrote the test value.' }

    $sendPasteShortcut = $true
    $fakeKey = $opaqueKey
    $env:AUCTION_TEST_EXPECTED_GEMINI = $fakeKey
    & $launcher Server -ConfigureGemini 6>&1 | Out-Null
    $sendPasteShortcut = $false

    $env:GEMINI_API_KEY = $fakeKey
    $promptsBefore = $observed.Prompts
    & $launcher Server -VerifyGemini 6>&1 | Out-Null
    if ($observed.Prompts -ne $promptsBefore -or $env:GEMINI_API_KEY -cne $fakeKey) {
        throw 'Verification did not reuse the inherited key.'
    }
    $env:GEMINI_API_KEY = 'prior-test-value'
    $fakeKey = $opaqueKey + [char]0 + $opaqueKey
    foreach ($inputOptions in @(@(), @('-GeminiFromClipboard'))) {
        try {
            & $launcher Server -ConfigureGemini @inputOptions 6>&1 | Out-Null
            throw 'Embedded NUL passed to the environment.'
        } catch {
            if (-not $_.Exception.Message.Contains('U+0000 before Java launch')) { throw }
        }
    }
    $env:LOCALAPPDATA = $fixture
    $env:GEMINI_API_KEY = $null
    $fakeKey = $opaqueKey
    $env:AUCTION_TEST_EXPECTED_GEMINI = $fakeKey
    $promptsBefore = $observed.Prompts
    & $launcher Server -RememberGemini 6>&1 | Out-Null
    if ((Get-Content -LiteralPath $savedKeyPath -Raw).Contains($fakeKey)) {
        throw 'The saved Gemini file contains the plaintext value.'
    }
    $fakeKey = ''
    & $launcher Server -RememberGemini 6>&1 | Out-Null
    $env:AUCTION_AI_PROVIDER = 'gemini'
    & $launcher Server 6>&1 | Out-Null
    if ($observed.Prompts -ne ($promptsBefore + 1)) {
        throw 'Gemini startup did not reuse the saved key.'
    }
    $replacement = 'replacement-' + [Guid]::NewGuid().ToString()
    $fakeKey = $replacement
    $env:AUCTION_TEST_EXPECTED_GEMINI = $replacement
    & $launcher Server -ConfigureGemini -RememberGemini 6>&1 | Out-Null
    & $launcher Server 6>&1 | Out-Null
    $env:GEMINI_API_KEY = $opaqueKey
    $env:AUCTION_TEST_EXPECTED_GEMINI = $opaqueKey
    & $launcher Server 6>&1 | Out-Null
    $env:AUCTION_AI_PROVIDER = 'ollama'
    $env:GEMINI_API_KEY = 'prior-test-value'
    if ($observed.Runs -ne 14) { throw "Expected fourteen Java probes, got $($observed.Runs)." }
    if ($env:GEMINI_API_KEY -cne 'prior-test-value' -or $env:AUCTION_AI_PROVIDER -cne 'ollama') {
        throw 'The launcher did not restore its process environment.'
    }
    Write-Output 'Gemini launcher tests passed (masked input, raw clipboard, and DPAPI reuse).'
} finally {
    $env:GEMINI_API_KEY = $oldKey
    $env:AUCTION_AI_PROVIDER = $oldProvider
    $env:AUCTION_TEST_EXPECTED_GEMINI = $oldExpected
    $env:LOCALAPPDATA = $oldLocal
    if (Test-Path -LiteralPath $probe) { Remove-Item -LiteralPath $probe }
    if (Test-Path -LiteralPath $savedKeyPath) { Remove-Item -LiteralPath $savedKeyPath }
    $keyDirectory = Split-Path -Parent $savedKeyPath
    if (Test-Path -LiteralPath $keyDirectory) { Remove-Item -LiteralPath $keyDirectory }
    Remove-Item -LiteralPath $fixture
}
