plugins {
    id("com.lagradost.cloudstream3.gradle") version "1.0.0"
}

group = "com.dinostreaming"
version = "1.0"

cloudstream {
    language = "it"
    description = "Movies and Shows from DinoStreaming"
    authors = listOf("doGior")
    tvTypes = listOf("Movie", "TvSeries")
    name = "DinoStreaming"
    iconUrl = "https://dinostreaming.it/favicon.ico"
}
