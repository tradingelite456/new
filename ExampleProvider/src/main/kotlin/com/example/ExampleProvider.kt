package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class ExampleProvider : MainAPI() {
    override var mainUrl = "https://cinepulse.to"
    override var name = "CinePulse"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)
    override var lang = "fr"
    override val hasMainPage = true

    override val mainPage = mainPageOf(
        "$mainUrl/films/" to "Films Populaires",
        "$mainUrl/series/" to "Séries Populaires",
        "$mainUrl/annee/2024/" to "Nouveautés 2024",
        "$mainUrl/tendance/" to "Tendances",
        "$mainUrl/imdb/" to "Top IMDB"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page > 1) "${request.data}page/$page/" else request.data
        val document = app.get(url).document
        val items = document.select("div.flw-item, div.item, div.movie-item").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, items)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h2.film-name, h3.film-name, a.title")?.text() ?: return null
        val href = this.selectFirst("a")?.attr("href") ?: return null
        val fullUrl = if (href.startsWith("http")) href else mainUrl + href
        val poster = this.selectFirst("img")?.attr("data-src") ?: this.selectFirst("img")?.attr("src")
        val quality = this.selectFirst("div.quality")?.text()
        
        val type = when {
            fullUrl.contains("/series/", true) || fullUrl.contains("/tv/", true) -> TvType.TvSeries
            else -> TvType.Movie
        }

        return newMovieSearchResponse(title, fullUrl, type) {
            this.posterUrl = poster
            this.quality = getQualityFromString(quality)
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/search/$query/").document
        return document.select("div.flw-item, div.item, div.movie-item").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("h2.heading-name, h1.title")?.text() ?: return null
        val poster = document.selectFirst("img.poster, img.film-poster")?.attr("src")
        val description = document.selectFirst("div.description, div.synopsis")?.text()
        
        val type = when {
            url.contains("/series/", true) || url.contains("/tv/", true) -> TvType.TvSeries
            else -> TvType.Movie
        }

        val recommendations = document.select("div.swiper-slide, div.related-item").mapNotNull {
            it.toSearchResult()
        }

        return if (type == TvType.TvSeries) {
            val episodes = document.select("div.ss-list > a").mapNotNull { episode ->
                val episodeTitle = episode.selectFirst("strong")?.text() ?: "Episode"
                val episodeUrl = episode.attr("href")
                val episodeNumber = episodeTitle.filter { it.isDigit() }.toIntOrNull() ?: 1
                Episode(episodeUrl, episodeTitle, episodeNumber)
            }

            newTvSeriesLoadResponse(title, url, type, episodes) {
                this.posterUrl = poster
                this.plot = description
                this.recommendations = recommendations
            }
        } else {
            val videoLinks = document.select("a[data-link], a.server-item").mapNotNull { server ->
                val serverName = server.text()
                val serverUrl = server.attr("data-link") ?: server.attr("href")
                if (serverUrl.isNotBlank()) {
                    ExtractorLink(serverName, serverName, serverUrl, mainUrl, Qualities.Unknown.value)
                } else null
            }

            newMovieLoadResponse(title, url, type, videoLinks) {
                this.posterUrl = poster
                this.plot = description
                this.recommendations = recommendations
            }
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val document = app.get(data).document
        val servers = document.select("a[data-link], a.server-item")
        
        servers.forEach { server ->
            val serverUrl = server.attr("data-link") ?: server.attr("href")
            if (serverUrl.isNotBlank() && serverUrl != "#") {
                val extractorLink = ExtractorLink(
                    name,
                    server.text(),
                    serverUrl,
                    mainUrl,
                    Qualities.Unknown.value
                )
                callback(extractorLink)
            }
        }
        return true
    }

    private fun getQualityFromString(quality: String?): Qualities {
        return when {
            quality?.contains("4k", true) == true -> Qualities.Q4K
            quality?.contains("1080", true) == true -> Qualities.Q1080
            quality?.contains("720", true) == true -> Qualities.Q720
            quality?.contains("480", true) == true -> Qualities.Q480
            else -> Qualities.Unknown
        }
    }
}
