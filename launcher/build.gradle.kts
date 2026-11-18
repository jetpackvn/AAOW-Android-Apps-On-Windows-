plugins {
    application
}

dependencies {
    implementation(project(":framework"))
    implementation("de.femtopedia.dex2jar:dex-translator:2.4.39")
    implementation("org.ow2.asm:asm:9.7.1")
}

application {
    mainClass.set("dev.aaow.launcher.Main")
    applicationName = "aaow"
    applicationDefaultJvmArgs = listOf("-Dfile.encoding=UTF-8", "-Dsun.stdout.encoding=UTF-8")
}

tasks.named<JavaExec>("run") {
    workingDir = rootDir
}
