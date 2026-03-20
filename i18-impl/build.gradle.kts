dependencies {
    implementation(libs.leycm.init)
    implementation(libs.annos.jetbrains)
    implementation(project(":api"))

    implementation(libs.bundles.jackson)
}

tasks.named("sourcesJar") {
    mustRunAfter(":api:jar")
}
