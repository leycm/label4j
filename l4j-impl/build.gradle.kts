dependencies {
    implementation(libs.leycm.init)
    compileOnly(libs.annos.jetbrains)

    implementation(project(":api"))
    implementation(libs.bundles.parser.night)

    compileOnly(libs.mcstructs.text)
    compileOnly(libs.bungee.chat)
    compileOnly(libs.javafx.controls)
    compileOnly(variantOf(libs.javafx.controls) { classifier("linux") })
    compileOnly(variantOf(libs.javafx.graphics) { classifier("linux") })
    compileOnly(variantOf(libs.javafx.base) { classifier("linux") })
    compileOnly(libs.adventure.gson)
    compileOnly(libs.adventure.plain)
    compileOnly(libs.adventure.legacy)
    compileOnly(libs.adventure.minimessage)
}

tasks.named("sourcesJar") {
    mustRunAfter(":api:jar")
}