plugins {
    `java-library`
}

val mainSourceSet = sourceSets.named("main")

val microg = sourceSets.create("microg") {
    java.srcDir("src/microg/java")
    val mainCompile = files(tasks.named("compileJava"))
    compileClasspath += mainCompile + configurations.named("compileClasspath").get()
    runtimeClasspath += output + compileClasspath
}

val packageMicrogJar = tasks.register<Jar>("packageMicrogJar") {
    dependsOn(tasks.named("microgClasses"))
    from(microg.output)
    archiveFileName.set("microg-lite.jar")
    destinationDirectory.set(layout.buildDirectory.dir("generated-resources/microg/dev/aaow"))
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(packageMicrogJar)
    from(layout.buildDirectory.dir("generated-resources/microg"))
}

val microgElements = configurations.create("microgElements") {
    isCanBeConsumed = true
    isCanBeResolved = false
}

artifacts {
    add(microgElements.name, packageMicrogJar)
}

dependencies {
    api(project(":apk"))
    implementation("org.xerial:sqlite-jdbc:3.54.1.0")
    val fxVersion = "21.0.5"
    val fxPlatform = "win"
    implementation("org.openjfx:javafx-base:$fxVersion:$fxPlatform")
    implementation("org.openjfx:javafx-graphics:$fxVersion:$fxPlatform")
    implementation("org.openjfx:javafx-controls:$fxVersion:$fxPlatform")
    implementation("org.openjfx:javafx-media:$fxVersion:$fxPlatform")
    implementation("org.openjfx:javafx-web:$fxVersion:$fxPlatform")
    implementation("org.openjfx:javafx-swing:$fxVersion:$fxPlatform")
}
