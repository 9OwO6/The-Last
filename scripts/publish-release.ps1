param([Parameter(Mandatory = $true)][string]$NotesFile)
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
function Check-Exit([string]$Step) { if ($LASTEXITCODE -ne 0) { throw "$Step 失败（退出码 $LASTEXITCODE）" } }
if (!(Test-Path -LiteralPath $NotesFile -PathType Leaf)) { throw '请提供真实更新说明文件。' }
$releaseNotesPath = (Resolve-Path -LiteralPath $NotesFile).Path
if (git status --porcelain) { throw '请先提交工作区中的改动，再发布与提交一致的版本。' }
$buildConfigText = Get-Content app/build.gradle.kts -Raw
$releaseVersion = [regex]::Match($buildConfigText, 'versionName = "(\d+\.\d+\.\d+)"').Groups[1].Value
if (!$releaseVersion) { throw '未找到正式语义版本号。' }
$repoInfo = gh repo view --json nameWithOwner --jq '.nameWithOwner'
Check-Exit '获取发布仓库'
$commit = git rev-parse HEAD
Check-Exit '获取提交'
git fetch origin main
Check-Exit '检查远程提交'
$remoteCommit = git rev-parse origin/main
Check-Exit '获取远程提交'
if ($commit -ne $remoteCommit) { throw '请先把当前 main 提交推送到远程。' }
$existing = gh api "repos/$repoInfo/releases" --jq ".[] | select(.tag_name == `"v$releaseVersion`") | .id"
Check-Exit '检查已有版本'
if ($existing) { throw "v$releaseVersion 已存在，请递增版本号，避免覆盖已发布版本。" }
& ./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --console=plain
Check-Exit '构建与验证'
$sdkLine = Get-Content local.properties | Where-Object { $_ -match '^sdk.dir=' } | Select-Object -First 1
if (!$sdkLine) { throw 'local.properties 中未找到 Android SDK。' }
$androidSdkPath = $sdkLine.Substring(8).Replace('\\', '\').Replace('\:', ':')
$apkSigner = Get-ChildItem -LiteralPath (Join-Path $androidSdkPath 'build-tools') -Filter apksigner.bat -Recurse | Sort-Object FullName -Descending | Select-Object -First 1
if (!$apkSigner) { throw '找不到 apksigner。' }
$apkPath = 'app/build/outputs/apk/debug/app-debug.apk'
$certificate = & $apkSigner.FullName verify --print-certs $apkPath
Check-Exit '校验安装包签名'
if (($certificate -join "`n") -notmatch 'SHA-256 digest: 35bcb314aa2e4b781a9ab0ef6c92b5a8525dc530b43d020c5f2bfc38220c6eba') {
    throw '签名与历史安装包不同，停止发布。请使用原本保存的签名密钥。'
}
$outputDirectory = New-Item -ItemType Directory -Path "releases/$releaseVersion" -Force
$outputApk = Join-Path $outputDirectory.FullName "thelast-$releaseVersion.apk"
Copy-Item -LiteralPath $apkPath -Destination $outputApk
$hash = (Get-FileHash -LiteralPath $outputApk -Algorithm SHA256).Hash.ToLowerInvariant()
$hashFile = Join-Path $outputDirectory.FullName 'SHA256.txt'
"$hash  thelast-$releaseVersion.apk" | Set-Content -LiteralPath $hashFile -Encoding ascii
gh release create "v$releaseVersion" $outputApk $hashFile --repo $repoInfo --target $commit --title "The Last $releaseVersion" --notes-file $releaseNotesPath
Check-Exit '发布 GitHub Release'
gh release view "v$releaseVersion" --repo $repoInfo --json url,tagName,assets
Check-Exit '读取已发布版本'
