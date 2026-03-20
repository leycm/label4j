dependencies {
    implementation(libs.leycm.init)
    implementation(libs.annos.jetbrains)
    implementation(project(":api"))
    
    implementation(libs.bundles.parser)
}

tasks.named("sourcesJar") {
    mustRunAfter(":api:jar")
}
