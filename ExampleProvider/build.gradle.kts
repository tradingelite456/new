version = 18

cloudstream {
    authors     = listOf("hexated", "keyiflerolsun")
    language    = "tr"
    description = "Türkiye'nin en hızlı hd film izleme sitesi"

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("Movie", "TvSeries")
    iconUrl = "https://www.google.com/s2/favicons?domain=hdfilmcehennemi.com&sz=%size%"
}

android {
    buildFeatures {
        buildConfig = true
    }
    defaultConfig {
        buildConfigField("String", "TMDB_API", "\"eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiIyNDcxZjkxMGE3NDY5YjU0Nzc5MzY3MWJkOGZjYWMxNyIsIm5iZiI6MTc0MDk3NTA0Ni43OTIsInN1YiI6IjY3YzUyYmM2YTg5M2Y4ZGFhNWU3NWYwZiIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.fYsFOukBBd0gxI-mvOuKc9SCJ5AjB7gd_8vcMq_cTPM\"")
        minSdk = 26
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }
    dependencies {
        coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    }
}