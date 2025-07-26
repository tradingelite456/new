package com.dinostreaming

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class DinoStreaming : MainAPI() {
    override var mainUrl = "https://www.dinostreaming.it"
    override var name = "DinoStreaming"
    override val hasMainPage = true
    override val lang = "it"

    override val mainPage = listOf(
        "Film" to "$mainUrl/film-streaming/",
        "Serie TV" to "$mainUrl/serie-tv-streaming/"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val doc = app.get(request.data).document
        val items = doc.select("div.result-item").mapNotNull {
            val title = it.selectFirst("h3.result-title")?.text() ?: return@mapNotNull null
            val link = it.selectFirst("a")?.attr("href") ?: return@mapNotNull null
            val poster = it.selectFirst("img")?.attr("abs:src")
            val type = if (request.name.contains("Serie", ignoreCase = true)) TvType.TvSeries else TvType.Movie

            newMovieSearchResponse(title, link, type) {
                this.posterUrl = poster
            }
        }

        return newHomePageResponse(request.name, items)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/?s=${query.trim().replace(" ", "+")}"
        val doc = app.get(url).document
        return doc.select("div.result-item").mapNotNull {
            val title = it.selectFirst("h3.result-title")?.text() ?: return@mapNotNull null
            val link = it.selectFirst("a")?.attr("href") ?: return@mapNotNull null
            val poster = it.selectFirst("img")?.attr("abs:src")
            val type = if (link.contains("/serie-tv/")) TvType.TvSeries else TvType.Movie

            newMovieSearchResponse(title, link, type) {
                this.posterUrl = poster
            }
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val doc = app.get(url).document
        val title = doc.selectFirst("h1")?.text() ?: return null
        val poster = doc.select("img").firstOrNull()?.attr("abs:src")
        val description = doc.select("div.entry-content p").firstOrNull()?.text()

        val isSerie = url.contains("/serie-tv/")

        return if (isSerie) {
            val episodes = doc.select("ul.episodios li").mapNotNull {
                val epLink = it.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val epName = it.selectFirst("a")?.text() ?: ""
                Episode(epLink, epName)
            }

            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.plot = description
            }
        } else {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.plot = description
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val doc = app.get(data).document
        val iframeList = doc.select("iframe")
        for (iframe in iframeList) {
            val src = iframe.attr("src")
            if (src.isNotBlank()) {
                loadExtractor(src, data, subtitleCallback, callback)
            }
        }
    }
}
