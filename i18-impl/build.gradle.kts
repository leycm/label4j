dependencies {
    implementation(libs.leycm.init)
    implementation(libs.bundles.parser)
    implementation(project(":api"))
    compileOnly(libs.annos.jetbrains)

    compileOnly(variantOf(libs.javafx.controls) { classifier("linux") })
    compileOnly(variantOf(libs.javafx.graphics) { classifier("linux") })
    compileOnly(variantOf(libs.javafx.base) { classifier("linux") })
    compileOnly(libs.mcstructs.text)
    compileOnly(libs.bungee.chat)
    compileOnly(libs.javafx.controls)
    compileOnly(libs.adventure.gson)
    compileOnly(libs.adventure.plain)
    compileOnly(libs.adventure.legacy)
    compileOnly(libs.adventure.minimessage)
}

tasks.named("sourcesJar") {
    mustRunAfter(":api:jar")
}
