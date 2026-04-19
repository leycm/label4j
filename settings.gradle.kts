// gradle plugin mgmt
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

// replacement for property(String) that checks gradle.properties
fun getGradleProperty(name: String): String? {
    return try {
        val propsFile = rootDir.resolve("gradle.properties")
        if (!propsFile.exists()) return null

        val props = java.util.Properties()
        propsFile.inputStream().use { props.load(it) }

        props.getProperty(name)
    } catch (e: Exception) {
        logger.warn("Couldn't find \"$name\" in gradle.properties")
        logger.debug("Exception: ${e.message}\n${e.stackTraceToString()}")
        null
    }
}

// gradle dependency mgmt
dependencyResolutionManagement {
    logger.debug("[+] Checking gradle.properties for version-catalog...")
    val versionCatalogProp = getGradleProperty("version-catalog")
    if (versionCatalogProp.isNullOrEmpty() || versionCatalogProp == "gradle/libs.versions.toml")
        return@dependencyResolutionManagement

    versionCatalogs {
        create("libs") {
            logger.info("[+] Loading $versionCatalogProp ...")
            from(files("$rootDir/$versionCatalogProp"))
        }
    }
}

// project includes
rootProject.name = getGradleProperty("artifact") ?: "null"
var prefix = (getGradleProperty("prefix") ?: "sub") + "-"

include("api", "impl")

project(":api").projectDir = file("${prefix}api")
project(":impl").projectDir = file("${prefix}impl")