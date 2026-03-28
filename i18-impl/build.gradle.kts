dependencies {
    implementation(libs.leycm.init)
    implementation(libs.bundles.parser)
    implementation(project(":api"))
    compileOnly(libs.annos.jetbrains)

    compileOnly(libs.bundles.adventure)
}

tasks.named("sourcesJar") {
    mustRunAfter(":api:jar")
}
