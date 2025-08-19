// use an integer for version numbers
version = 8
dependencies {
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("com.google.android.material:material:1.4.0")
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
}
cloudstream {
    language = "fr"
    // All of these properties are optional, you can safely remove them

     description = "FRENCH STREAM en plus d'être un site efficace et plaisant dispose d'un contenu visuel diversifié"
     authors = listOf("Sarlay", "Eddy976", "AirbnbEcoPlus")

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
     * */
    status = 1 // will be 3 if unspecified
    tvTypes = listOf(
        "TvSeries",
        "Movie",
    )

    iconUrl = "https://www.google.com/s2/favicons?domain=french-stream.ac&sz=%size%"
    requiresResources = true
}
android {
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}
