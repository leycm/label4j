dependencies {
    implementation(libs.leycm.init)
    implementation(libs.bundles.parser.night)
    compileOnly(libs.annos.jetbrains)

    implementation(project(":api"))

    // serializer targets
    compileOnly(libs.mcstructs.text)
    compileOnly(libs.bungee.chat)
    compileOnly(libs.bundles.adventure)

    // javafx serializer targets
    compileOnly(variantOf(libs.javafx.controls) { classifier("linux") })
    compileOnly(variantOf(libs.javafx.graphics) { classifier("linux") })
    compileOnly(variantOf(libs.javafx.base) { classifier("linux") })
}

tasks.named("sourcesJar") {
    mustRunAfter(":api:jar")
}