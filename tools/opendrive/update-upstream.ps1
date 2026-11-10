$ErrorActionPreference = 'Stop'
$source = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
Push-Location $source
try {
    $branch = & git branch --show-current
    if ($LASTEXITCODE -ne 0 -or $branch -notin @('opendrive', 'main')) {
        throw 'Expected the OpenDrive main or opendrive branch; detached checkouts are not updated.'
    }
    $changes = & git status --porcelain
    if ($LASTEXITCODE -ne 0) { throw 'Cannot inspect source checkout.' }
    if ($changes) { throw 'Commit local OpenDrive changes before updating upstream.' }

    $remotes = & git remote
    if ('upstream' -notin $remotes) {
        & git remote add upstream https://github.com/prototux/AARE.git
        if ($LASTEXITCODE -ne 0) { throw 'Cannot configure the AARE upstream remote.' }
    }
    $upstreamUrl = & git remote get-url upstream
    if ($upstreamUrl -notin @('https://github.com/prototux/AARE.git', 'https://github.com/prototux/AARE')) {
        throw 'The upstream remote must point to the official prototux/AARE repository.'
    }
    # This remote is only a source of updates. OpenDrive changes must never be pushed to AARE.
    & git remote set-url --push upstream DISABLED
    if ($LASTEXITCODE -ne 0) { throw 'Cannot make the AARE remote read-only for pushes.' }
    & git fetch upstream --tags
    if ($LASTEXITCODE -ne 0) { throw 'Cannot fetch AARE; source has not been updated.' }
    & git merge --no-edit upstream/main
    if ($LASTEXITCODE -ne 0) {
        throw 'Upstream merge needs attention in opendrive/src. Resolve conflicts and commit, or use git merge --abort.'
    }
    & git merge-base --is-ancestor upstream/main HEAD
    if ($LASTEXITCODE -ne 0) { throw 'OpenDrive does not contain the latest fetched AARE main.' }
    Write-Host 'OpenDrive contains the latest fetched AARE main:'
    & git log -1 upstream/main --format='%h %s'
} finally {
    Pop-Location
}
