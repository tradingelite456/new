pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://jitpack.io")
    }
    plugins {
        id("com.lagradost.cloudstream3.gradle") version "1.0.0"
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "DinoStreaming"


// This file sets what projects are included.
// All new projects should get automatically included unless specified in the "disabled" variable.

val disabled = listOf<String>()

File(rootDir, ".").eachDir { dir ->
    if (!disabled.contains(dir.name) && File(dir, "build.gradle.kts").exists()) {
        include(dir.name)
    }
}

fun File.eachDir(block: (File) -> Unit) {
    listFiles()?.filter { it.isDirectory }?.forEach { block(it) }
}

// To only include a single project, comment out the previous lines (except the first one), and include your plugin like so:
// include("PluginName")
