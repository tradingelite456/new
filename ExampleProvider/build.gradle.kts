dependencies {
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("com.google.android.material:material:1.4.0")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
}
// use an integer for version numbers
version = 1


cloudstream {
     language = "id"
    // All of these properties are optional, you can safely remove them

    // description = "Lorem ipsum"
    authors = listOf("SkzOfc")

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
     * */
    status = 1 // will be 3 if unspecified
    tvTypes = listOf(
        "AnimeMovie",
        "Anime",
        "OVA",
    )

    iconUrl = "data:image/webp;base64,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"
}
}

android {
    buildFeatures {
        viewBinding = true
    }
}
