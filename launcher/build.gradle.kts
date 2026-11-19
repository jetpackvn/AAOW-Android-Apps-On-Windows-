plugins {
    application
}

dependencies {
    implementation(project(":framework"))
    implementation("de.femtopedia.dex2jar:dex-translator:2.4.39")
    implementation("org.ow2.asm:asm:9.7.1")

    testImplementation(platform("org.junit:junit-bom:5.11.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(project(path = ":framework", configuration = "microgElements"))
}

tasks.test {
    useJUnitPlatform()
    workingDir = rootDir
    maxHeapSize = "1536m"
}

application {
    mainClass.set("dev.aaow.launcher.Main")
    applicationName = "aaow"
    applicationDefaultJvmArgs = listOf("-Dfile.encoding=UTF-8", "-Dsun.stdout.encoding=UTF-8")
}

tasks.named<JavaExec>("run") {
    workingDir = rootDir
}

tasks.register<Exec>("packageWindows") {
    group = "distribution"
    description = "Đóng gói AAOW thành AAOW.exe (jlink + jpackage) và bộ cài Windows."
    dependsOn("installDist")
    workingDir = rootDir
    commandLine("powershell", "-ExecutionPolicy", "Bypass", "-File",
        rootProject.file("scripts/package-windows.ps1").absolutePath, "-SkipGradleBuild")
}
