$ErrorActionPreference = "Stop"

$sdk = "$env:LOCALAPPDATA\Android\Sdk"
$bt  = "$sdk\build-tools\36.0.0"
$aj  = "$sdk\platforms\android-37.0\android.jar"
$jdk = "C:\Program Files\Java\jdk-21.0.12.1\bin"

function Build-SampleApk {
    param(
        [string]$Name,
        [string]$SrcDir,
        [string]$ApkOut
    )
    $outDir = Join-Path $PSScriptRoot "..\build\sample-apk-work\$Name"
    if (Test-Path $outDir) { Remove-Item -Recurse -Force $outDir }
    New-Item -ItemType Directory -Force "$outDir\compiled-res", "$outDir\gen", "$outDir\classes", "$outDir\dex" | Out-Null

    Write-Host "[$Name 1/4] aapt2 compile resources..."
    & "$bt\aapt2.exe" compile --dir "$SrcDir\res" -o "$outDir\compiled-res\res.zip"
    if ($LASTEXITCODE -ne 0) { throw "aapt2 compile failed for $Name" }

    Write-Host "[$Name 2/4] aapt2 link AndroidManifest.xml + resources.arsc + R.java..."
    & "$bt\aapt2.exe" link `
        -I $aj `
        --manifest "$SrcDir\AndroidManifest.xml" `
        --java "$outDir\gen" `
        -o "$outDir\base.apk" `
        "$outDir\compiled-res\res.zip"
    if ($LASTEXITCODE -ne 0) { throw "aapt2 link failed for $Name" }

    Write-Host "[$Name 3/4] javac against official android.jar..."
    $javaFiles = Get-ChildItem -Path "$SrcDir\src", "$outDir\gen" -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
    & "$jdk\javac.exe" -encoding UTF-8 --release 17 -classpath $aj -d "$outDir\classes" $javaFiles
    if ($LASTEXITCODE -ne 0) { throw "javac failed for $Name" }

    Write-Host "[$Name 4/4] d8 bytecode -> Dalvik/ART classes.dex & packaging APK..."
    $classFiles = Get-ChildItem -Path "$outDir\classes" -Recurse -Filter "*.class" | ForEach-Object { $_.FullName }
    & "$bt\d8.bat" --lib $aj --min-api 26 --output "$outDir\dex" $classFiles
    if ($LASTEXITCODE -ne 0) { throw "d8 failed for $Name" }

    Copy-Item "$outDir\base.apk" $ApkOut -Force
    Push-Location "$outDir\dex"
    & "$jdk\jar.exe" uf $ApkOut "classes.dex"
    Pop-Location

    Write-Host "Hoàn tất đóng gói APK: $ApkOut ($((Get-Item $ApkOut).Length) bytes)"
}

Build-SampleApk -Name "showcase" `
    -SrcDir (Join-Path $PSScriptRoot "..\samples\showcase-src") `
    -ApkOut (Join-Path $PSScriptRoot "..\samples\aaow-showcase.apk")

Build-SampleApk -Name "game2048" `
    -SrcDir (Join-Path $PSScriptRoot "..\samples\2048-src") `
    -ApkOut (Join-Path $PSScriptRoot "..\samples\aaow-2048.apk")
