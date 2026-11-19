param(
    [string]$Version = "1.0.0",
    [switch]$SkipGradleBuild
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Push-Location $root

try {
    # 1. Tìm JDK 21 (chứa jlink.exe và jpackage.exe)
    $jdkBin = $null
    $candidates = @()
    if ($env:JAVA_HOME) { $candidates += (Join-Path $env:JAVA_HOME "bin") }
    $candidates += Get-ChildItem "C:\Program Files\Java\jdk-21*" -ErrorAction SilentlyContinue | ForEach-Object { Join-Path $_.FullName "bin" }
    foreach ($c in $candidates) {
        if ((Test-Path (Join-Path $c "jpackage.exe")) -and (Test-Path (Join-Path $c "jlink.exe"))) {
            $jdkBin = $c
            break
        }
    }
    if (-not $jdkBin) {
        throw "Không tìm thấy JDK 21 có jlink.exe và jpackage.exe."
    }
    $javaExe = Join-Path $jdkBin "java.exe"
    $jlinkExe = Join-Path $jdkBin "jlink.exe"
    $jpackageExe = Join-Path $jdkBin "jpackage.exe"
    Write-Host "[AAOW-Pkg] Sử dụng JDK: $jdkBin"

    # 2. Biên dịch bản phân phối :launcher:installDist
    if (-not $SkipGradleBuild) {
        Write-Host "[AAOW-Pkg] Đang build :launcher:installDist..."
        & "$root\gradlew.bat" :launcher:installDist --no-daemon --console=plain -q
        if ($LASTEXITCODE -ne 0) { throw "Gradle installDist thất bại." }
    }

    $distLibRel = "launcher/build/install/aaow/lib"
    $distLib = Join-Path $root "launcher\build\install\aaow\lib"
    if (-not (Test-Path $distLib)) {
        throw "Không tìm thấy thư mục JAR tại $distLib"
    }

    $pkgWorkRel = "build/packaging"
    $pkgWork = Join-Path $root "build\packaging"
    $jpkgOutRel = "build/jpackage"
    $jpkgOut = Join-Path $root "build\jpackage"
    $releaseDir = Join-Path $root "build\release"
    if (Test-Path $pkgWork) { Remove-Item -Recurse -Force $pkgWork }
    if (Test-Path $jpkgOut) { Remove-Item -Recurse -Force $jpkgOut }
    New-Item -ItemType Directory -Force $pkgWork, $jpkgOut, $releaseDir | Out-Null

    # 3. Tạo biểu tượng Windows đa độ phân giải (.ico)
    $iconsDirRel = "$pkgWorkRel/icons"
    $iconsDir = Join-Path $pkgWork "icons"
    New-Item -ItemType Directory -Force $iconsDir | Out-Null
    Write-Host "[AAOW-Pkg] Đang tạo icon Windows aaow.ico & apk-file.ico..."
    & $javaExe -cp "$distLibRel/*" dev.aaow.launcher.IconAssetGenerator $iconsDirRel
    if ($LASTEXITCODE -ne 0) { throw "Tạo icon thất bại." }
    $aaowIcoRel = "$iconsDirRel/aaow.ico"
    $apkIcoRel = "$iconsDirRel/apk-file.ico"
    $aaowIco = Join-Path $iconsDir "aaow.ico"
    $apkIco = Join-Path $iconsDir "apk-file.ico"

    # 4. Tạo Java 21 Runtime rút gọn bằng jlink
    $runtimeDirRel = "$pkgWorkRel/runtime"
    $runtimeDir = Join-Path $pkgWork "runtime"
    $modules = @(
        "java.base",
        "java.desktop",
        "java.logging",
        "java.management",
        "java.naming",
        "java.net.http",
        "java.scripting",
        "java.sql",
        "java.xml",
        "jdk.charsets",
        "jdk.crypto.cryptoki",
        "jdk.crypto.ec",
        "jdk.jfr",
        "jdk.jsobject",
        "jdk.localedata",
        "jdk.unsupported",
        "jdk.unsupported.desktop",
        "jdk.xml.dom",
        "jdk.zipfs"
    ) -join ","
    Write-Host "[AAOW-Pkg] Đang tạo JRE rút gọn với jlink..."
    & $jlinkExe `
        --add-modules $modules `
        --strip-debug `
        --no-header-files `
        --no-man-pages `
        --strip-native-commands `
        --compress=zip-6 `
        --output $runtimeDirRel
    if ($LASTEXITCODE -ne 0) { throw "jlink thất bại." }

    $runtimeBytes = (Get-ChildItem -Recurse -File $runtimeDir | Measure-Object -Property Length -Sum).Sum
    Write-Host ("[AAOW-Pkg] JRE rút gọn jlink: {0:N1} MB" -f ($runtimeBytes / 1MB))

    # 5. Chuẩn bị file cấu hình launcher phụ (aaow-cli.exe có console cho CLI)
    $cliPropsRel = "$pkgWorkRel/aaow-cli.properties"
    $cliProps = Join-Path $pkgWork "aaow-cli.properties"
    Set-Content -Path $cliProps -Encoding ASCII -Value @(
        "win-console=true"
        "description=AAOW Command Line Launcher"
    )

    $samplesDir = Join-Path $root "samples"
    $appContentSamplesRel = "$pkgWorkRel/payload-samples/samples"
    $appContentSamples = Join-Path $pkgWork "payload-samples\samples"
    New-Item -ItemType Directory -Force $appContentSamples | Out-Null
    Copy-Item "$samplesDir\*.apk" $appContentSamples -Force

    # 6. Đóng gói app-image độc lập bằng jpackage (tạo AAOW.exe + aaow-cli.exe + runtime/)
    Write-Host "[AAOW-Pkg] Đang đóng gói app-image bằng jpackage..."
    & $jpackageExe `
        --type app-image `
        --name AAOW `
        --app-version $Version `
        --vendor "AAOW Project" `
        --description "AAOW - Android Apps On Windows Compatibility Layer" `
        --icon $aaowIcoRel `
        --input $distLibRel `
        --main-jar "launcher.jar" `
        --main-class "dev.aaow.launcher.Main" `
        --runtime-image $runtimeDirRel `
        --java-options "-Dfile.encoding=UTF-8" `
        --java-options "-Dsun.stdout.encoding=UTF-8" `
        --java-options "-XX:+UseG1GC" `
        --java-options "-XX:MaxGCPauseMillis=16" `
        --java-options "-Xmx1024m" `
        --add-launcher "aaow-cli=$cliPropsRel" `
        --app-content $appContentSamplesRel `
        --app-content $apkIcoRel `
        --dest $jpkgOutRel
    if ($LASTEXITCODE -ne 0) { throw "jpackage --type app-image thất bại." }

    $appImageDir = Join-Path $jpkgOut "AAOW"
    $appExe = Join-Path $appImageDir "AAOW.exe"
    if (-not (Test-Path $appExe)) {
        throw "Không thấy AAOW.exe tại $appExe"
    }
    $imgBytes = (Get-ChildItem -Recurse -File $appImageDir | Measure-Object -Property Length -Sum).Sum
    Write-Host ("[AAOW-Pkg] Đã tạo app-image AAOW.exe ({0:N1} MB) tại $appImageDir" -f ($imgBytes / 1MB))

    # 7. Đóng gói bản Portable ZIP
    $portableZip = Join-Path $releaseDir "AAOW-Portable-$Version.zip"
    if (Test-Path $portableZip) { Remove-Item -Force $portableZip }
    Compress-Archive -Path "$appImageDir\*" -DestinationPath $portableZip -CompressionLevel Optimal
    Write-Host ("[AAOW-Pkg] Đã tạo bản Portable ZIP: $portableZip ({0:N1} MB)" -f ((Get-Item $portableZip).Length / 1MB))

    # 8. Nếu có WiX Toolset (wix.exe hoặc candle+light), chạy jpackage --type msi & exe
    $hasWix = (Get-Command "wix.exe" -ErrorAction SilentlyContinue) -or
              ((Get-Command "candle.exe" -ErrorAction SilentlyContinue) -and (Get-Command "light.exe" -ErrorAction SilentlyContinue))
    if ($hasWix) {
        $assocProps = Join-Path $pkgWork "apk-assoc.properties"
        Set-Content -Path $assocProps -Encoding ASCII -Value @(
            "extension=apk"
            "mime-type=application/vnd.android.package-archive"
            "description=Android Application Package"
            "icon=$($apkIco -replace '\\','/')"
        )
        Write-Host "[AAOW-Pkg] Phát hiện WiX Toolset -> tạo bộ cài .msi/.exe qua jpackage..."
        & $jpackageExe `
            --type msi `
            --name AAOW `
            --app-version $Version `
            --vendor "AAOW Project" `
            --app-image $appImageDir `
            --file-associations $assocProps `
            --win-dir-chooser --win-menu --win-shortcut --win-per-user-install `
            --dest $releaseDir
    } else {
        Write-Host "[AAOW-Pkg] Không có WiX Toolset trên máy -> bỏ qua jpackage --type msi, dùng bộ cài native AAOW-Setup.exe."
    }

    # 9. Biên dịch bộ cài Windows AAOW-Setup-x.y.z.exe từ scripts/AaowSetup.cs bằng csc.exe
    $cscExe = "C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe"
    if (Test-Path $cscExe) {
        $setupExe = Join-Path $releaseDir "AAOW-Setup-$Version.exe"
        $setupSrc = Join-Path $PSScriptRoot "AaowSetup.cs"
        Write-Host "[AAOW-Pkg] Đang biên dịch bộ cài đặt Windows $setupExe..."
        & $cscExe `
            /nologo `
            /target:winexe `
            /optimize+ `
            /platform:anycpu `
            "/win32icon:$aaowIco" `
            "/out:$setupExe" `
            "/resource:$portableZip,AAOW-payload.zip" `
            "/resource:$aaowIco,aaow.ico" `
            "/resource:$apkIco,apk-file.ico" `
            /reference:System.dll `
            /reference:System.Drawing.dll `
            /reference:System.Windows.Forms.dll `
            /reference:System.IO.Compression.dll `
            /reference:System.IO.Compression.FileSystem.dll `
            $setupSrc
        if ($LASTEXITCODE -ne 0) { throw "Biên dịch AaowSetup.cs thất bại." }
        Write-Host ("[AAOW-Pkg] Đã tạo bộ cài Windows: $setupExe ({0:N1} MB)" -f ((Get-Item $setupExe).Length / 1MB))
    }

    Write-Host "[AAOW-Pkg] HOÀN TẤT ĐÓNG GÓI SPRINT 8!"
} finally {
    Pop-Location
}
