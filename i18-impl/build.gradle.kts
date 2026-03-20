dependencies {
    compileOnly(libs.leycm.init)
    compileOnly(libs.annos.jetbrains)
    compileOnly(project(":api"))

    compileOnly(libs.bundles.jackson)
}

tasks.named("sourcesJar") {
    mustRunAfter(":api:jar")
}
