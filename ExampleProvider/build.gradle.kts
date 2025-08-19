plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
        // viewBinding = true // Décommentez seulement si vous en avez vraiment besoin
    }
}

dependencies {
    // Les APIs Cloudstream sont fournies par l'application hôte, pas besoin de dépendance externe
    implementation("org.jsoup:jsoup:1.17.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.core:core-ktx:1.13.1")
}

version = 1

cloudstream {
    description = "Extension CinePulse pour Cloudstream 3"
    authors = listOf("Votre nom ici")
    status = 1
    tvTypes = listOf("Movie", "TvSeries")
    requiresResources = true
    language = "fr"
    iconUrl = "https://upload.wikimedia.org/wikipedia/commons/2/2f/Korduene_Logo.png"
}
