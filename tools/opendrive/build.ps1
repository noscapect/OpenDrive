param([switch]$SkipUpdate, [switch]$Release, [string]$OutputDirectory)

$ErrorActionPreference = 'Stop'
$source = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$workspace = Split-Path (Split-Path $source -Parent) -Parent
$output = if ($OutputDirectory) { [System.IO.Path]::GetFullPath($OutputDirectory) } else { Join-Path $source 'build/opendrive' }
$windowsHost = $env:OS -eq 'Windows_NT'
$javaBinary = if ($windowsHost) { 'java.exe' } else { 'java' }

if (-not $SkipUpdate) { & "$PSScriptRoot/update-upstream.ps1" }
if (-not $env:JAVA_HOME -and (Test-Path "$workspace/.build-tools/java")) {
    $env:JAVA_HOME = (Get-ChildItem "$workspace/.build-tools/java" -Directory | Select-Object -First 1).FullName
}
if (-not $env:ANDROID_HOME) {
    $env:ANDROID_HOME = if ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } else { "$workspace/.build-tools/android-sdk" }
}
if (-not (Test-Path "$env:JAVA_HOME/bin/$javaBinary")) { throw 'Set JAVA_HOME to a JDK 17-21 installation.' }
if (-not (Test-Path $env:ANDROID_HOME)) { throw 'Set ANDROID_HOME to an Android SDK installation.' }
$signerName = if ($windowsHost) { 'apksigner.bat' } else { 'apksigner' }
$buildTools = Get-ChildItem "$env:ANDROID_HOME/build-tools" -Directory |
    Where-Object { $_.Name -match '^\d+\.\d+\.\d+$' -and (Test-Path (Join-Path $_.FullName $signerName)) } |
    Sort-Object { [version]$_.Name } -Descending | Select-Object -First 1
if (-not $buildTools) { throw 'Install Android SDK Build Tools with apksigner.' }
New-Item -ItemType Directory -Path $output -Force | Out-Null

Push-Location $source
try {
    $changes = & git status --porcelain
    if ($LASTEXITCODE -ne 0 -or $changes) { throw 'Build requires committed OpenDrive sources.' }
    $releaseTag = & git describe upstream/main --tags --abbrev=0 --match 'v[0-9]*'
    if ($LASTEXITCODE -ne 0) { throw 'Cannot determine upstream release version.' }
    $appVersion = Get-Content (Join-Path $source 'opendrive-version.properties') -Raw | ConvertFrom-StringData
    $revision = & git rev-parse --short HEAD
    $sourceRevision = & git rev-parse HEAD
    $upstreamRevision = & git rev-parse upstream/main
    $versionName = if ($Release) { $appVersion.versionName } else { "$($appVersion.versionName)-dev.$revision" }
    $versionCode = [int]$appVersion.versionCode
    $log = Join-Path $output 'build.log'
    Write-Host "Building OpenDrive $versionName; output: $output"
    $gradle = if ($windowsHost) { './gradlew.bat' } else { './gradlew' }
    & $gradle :aap:test :app:testDebugUnitTest :app:assembleDebug "-PversionName=$versionName" "-PversionCode=$versionCode" --console=plain *> $log
    if ($LASTEXITCODE -ne 0) {
        Get-Content $log -Tail 60
        throw "Gradle failed. See $log"
    }

    $apk = Join-Path $output 'opendrive-debug.apk'
    Copy-Item app/build/outputs/apk/debug/app-debug.apk $apk
    & (Join-Path $buildTools.FullName $signerName) verify --verbose --print-certs $apk *> "$output/apk-verification.txt"
    if ($LASTEXITCODE -ne 0) { throw 'APK signature verification failed.' }
    Copy-Item app/build/outputs/apk/debug/output-metadata.json "$output/apk-metadata.json"

    $testSummary = @()
    foreach ($module in @('aap', 'app')) {
        $task = if ($module -eq 'aap') { 'test' } else { 'testDebugUnitTest' }
        $reports = Get-ChildItem "$module/build/test-results/$task" -Filter 'TEST-*.xml'
        if (-not $reports) { throw "No test reports for $module." }
        $destination = Join-Path $output "tests/$module"
        New-Item -ItemType Directory -Path $destination -Force | Out-Null
        foreach ($report in $reports) {
            [xml]$xml = Get-Content $report.FullName
            $testSummary += [ordered]@{
                module = $module
                suite = [string]$xml.testsuite.name
                tests = [int]$xml.testsuite.tests
                failures = [int]$xml.testsuite.failures
                errors = [int]$xml.testsuite.errors
                skipped = [int]$xml.testsuite.skipped
            }
            Copy-Item $report.FullName $destination
        }
    }
    [ordered]@{
        application = 'OpenDrive'
        applicationId = $appVersion.applicationId
        versionName = $versionName
        versionCode = $versionCode
        releaseVersion = [bool]$Release
        builtAt = [DateTimeOffset]::Now.ToString('o')
        upstreamRepository = 'https://github.com/prototux/AARE.git'
        upstreamRelease = $releaseTag
        upstreamRevision = $upstreamRevision
        sourceRevision = $sourceRevision
        fetchedBeforeBuild = -not $SkipUpdate
        apk = 'opendrive-debug.apk'
        sha256 = (Get-FileHash $apk -Algorithm SHA256).Hash.ToLowerInvariant()
        testResults = $testSummary
        vehicleTested = $false
    } | ConvertTo-Json -Depth 5 | Set-Content "$output/build-info.json"
    Write-Host "Build and signature verification succeeded: $apk"
    Get-FileHash $apk -Algorithm SHA256
} finally {
    Pop-Location
}
