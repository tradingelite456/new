dependencies {
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
}

// Use an integer for version numbers
version = 1

cloudstream {
    description = "Telugu, Tamil, Malayalam, Kannada, Hindi & Web Series torrents from 1TamilMV.se"
    authors = listOf("YouKnowWho")
    language = "en"
    status = 1
    tvTypes = listOf("Movie", "Series")
}

android {
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}