plugins {
    kotlin("jvm") version "1.9.0"
    id("com.lagradost.cloudstream3.gradle") version "1.0.0"
}

group = "com.dinostreaming"
version = "1.0"

repositories {
    mavenCentral()
    // Cloudstream Plugin Gradle
    maven("https://jitpack.io")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jsoup:jsoup:1.15.3") // Per parsing HTML
}

// Configurazione del plugin Cloudstream
cloudstream {
    language = "it"
    description = "Movies and Shows from DinoStreaming"
    authors = listOf("doGior")

    // Specifica il tipo di contenuto supportato
    tvTypes = listOf("Movie", "TvSeries")

    // Nome visibile nel menu di plugin
    name = "DinoStreaming"

    // Icona facoltativa
    iconUrl = "https://dinostreaming.it/favicon.ico"
}
