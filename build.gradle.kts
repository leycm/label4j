import java.util.Properties
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    java
    `maven-publish`
    alias(libs.plugins.lombok) apply false
}

fun env(name: String): String? =
    System.getenv(name) ?: project.findProperty(name) as String?

// Project Metadata
group = property("group")!!
version = property("version")!!

java {
    toolchain.languageVersion
        .set(JavaLanguageVersion.of(property("version-java").toString().toInt()))
}

repositories {
    mavenCentral()
}

// Load root gradle.properties
val rootProps = Properties().apply {
    val file = rootProject.file("gradle.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

rootProps.forEach { (k, v) ->
    rootProject.extra[k.toString()] = v
    subprojects.forEach { sub -> sub.extra[k.toString()] = v }
}

// Load gradle.properties from subprojects
subprojects.forEach { sub ->
    val subProps = sub.file("gradle.properties")
    if (subProps.exists()) {
        Properties().apply {
            subProps.inputStream().use(::load)
        }.forEach { (k, v) ->
            sub.extra[k.toString()] = v
        }
    }
}

allprojects {

    group = property("group")!!
    version = property("version")!!

    repositories {
        mavenCentral()
        maven("https://oss.sonatype.org/content/groups/public/")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://oss.sonatype.org/content/repositories/releases")
        maven("https://leycm.github.io/repository/")
        maven("https://libraries.minecraft.net")
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.codemc.io/repository/maven-releases/")
        maven("https://repo.codemc.io/repository/maven-snapshots/")
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")
    apply(plugin = "io.freefair.lombok")

    java {
        toolchain.languageVersion
            .set(JavaLanguageVersion.of(property("version-java").toString().toInt()))
        withSourcesJar()
        withJavadocJar()
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    tasks.withType<Javadoc> {
        isFailOnError = false
        options.encoding = "UTF-8"
    }

    tasks.named<Jar>("jar") {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        from({
            configurations["runtimeClasspath"].filter {
                it.name.endsWith(".jar")
            }.map { zipTree(it) }
        })
    }

    publishing {

        publications {

            create<MavenPublication>("mavenJava") {

                from(components["java"])

                groupId = project.group.toString()
                artifactId = "${rootProject.name}-${project.name}"
                version = project.version.toString()

                pom {

                    name.set(project.name)
                    description.set(property("description") as String)
                    url.set(property("remote") as String)

                    licenses {
                        license {
                            name.set("GNU Lesser General Public License v3.0")
                            url.set("https://www.gnu.org/licenses/lgpl-3.0.txt")
                        }
                    }

                    scm {
                        connection.set("scm:git:${property("remote")}.git")
                        developerConnection.set("scm:git:${property("remote")}.git")
                        url.set(property("remote") as String)
                    }

                    developers {

                        val authorsProp = project.findProperty("authors").toString()
                            .removePrefix("[")
                            .removeSuffix("]")
                            .split(",")
                            .map { it.trim() }

                        for (author in authorsProp) {
                            val parts = author.split(";")

                            developer {
                                id.set(parts[0])
                                name.set(parts[1])
                                email.set(parts[2])
                            }
                        }
                    }
                }
            }
        }

        repositories {
            maven {
                name = "leycm-repo"
                val repoDir = rootProject.projectDir.parentFile.resolve("repository")
                url = uri(repoDir)
            }
        }
    }

    tasks.register<Exec>("updateRepo") {
        description = "This updates the locale maven repository."
        val repoDir = rootProject.projectDir.parentFile.resolve("repository")
        val script = repoDir.resolve("publish.sh")

        workingDir = repoDir

        doFirst {
            logger.info("Running push script in repository directory...")
        }

        commandLine("sh", script.absolutePath)
    }

    tasks.named("publish") {
        finalizedBy("updateRepo")
    }
}