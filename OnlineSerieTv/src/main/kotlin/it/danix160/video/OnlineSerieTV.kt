package it.danix160.video

import android.util.Log
import com.lagradost.cloudstream3.*
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.SocketTimeoutException

class OnlineSerieTV : MainAPI() {
    override var mainUrl = "https://onlineserietv.com"
    override var name = "OnlineSerieTV"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries, TvType.Cartoon, TvType.Anime, TvType.AnimeMovie, TvType.Documentary)
    override var lang = "it"
    override val hasMainPage = true

    override val mainPage = mainPageOf(
        "$mainUrl/movies/" to "Film: Ultimi aggiunti",
        "$mainUrl/serie-tv/" to "Serie TV: Ultime aggiunte",
        "$mainUrl/serie-tv-generi/animazione/" to "Serie TV: Animazione",
        "$mainUrl/film-generi/animazione/" to "Film: Animazione",
        "$mainUrl/serie-tv-generi/action-adventure/" to "Serie TV: Azione e Avventura"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val response = try {
            app.get(request.data).document
        } catch (e: SocketTimeoutException) {
            return null
        }
        val searchResponses = getItems(request.name, response)
        return newHomePageResponse(HomePageList(request.name, searchResponses), false)
    }

    private suspend fun getItems(section: String, page: Document): List<SearchResponse> {
        val searchResponses = when (section) {
            "Film: Ultimi aggiunti", "Serie TV: Ultime aggiunte" -> {
                val itemGrid = page.selectFirst(".wp-block-uagb-post-grid")!!
                val items = itemGrid.select(".uagb-post__inner-wrap")
                items.map {
                    val itemTag = it.select(".uagb-post__title > a")
                    val title = itemTag.text().trim().replace(Regex("\\d{4}$"), "")
                    val url = itemTag.attr("href")
                    val poster = it.select(".uagb-post__image > a > img").attr("src")
                    newTvSeriesSearchResponse(title, url) {
                        this.posterUrl = poster
                    }
                }
            }
            "Top 10 Film", "Top 10 Serie TV" -> {
                val sidebar = page.selectFirst(".sidebar_right")!!
                val bothTop10 = sidebar.select(".links")
                val currentTop10 = if (section == "Top 10 Film") {
                    bothTop10.last()
                } else {
                    bothTop10.first()
                }
                val items = currentTop10?.select(".scrolling > li")
                items?.map {
                    val title = it.select("a").text().trim().replace(Regex("\\d{4}$"), "")
                    val url = it.select("a").attr("href")
                    val showPage = try {
                        app.get(url).document
                    } catch (e: SocketTimeoutException) {
                        null
                    }
                    val poster = showPage?.select(".imgs > img:nth-child(1)")?.attr("src")
                    newTvSeriesSearchResponse(title, url) {
                        this.posterUrl = poster
                    }
                } ?: emptyList()
            }
            else -> emptyList()
        }
        return searchResponses
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val response = app.get("$mainUrl/?s=$query")
        val page = response.document
        val itemGrid = page.selectFirst("#box_movies")!!
        val items = itemGrid.select(".movie")
        return items.map {
            it.toSearchResponse()
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val response = app.get(url).document
        val dati = response.selectFirst(".headingder")!!
        val poster = dati.select(".imgs > img").attr("src").replace(Regex("-\\d+x\\d+"), "")
        val title = dati.select(".dataplus > div:nth-child(1) > h1").text().trim().replace(Regex("\\d{4}$"), "")
        val rating = dati.select(".stars > span:nth-child(3)").text().trim().removeSuffix("/10")
        val genres = dati.select(".stars > span:nth-child(6) > i:nth-child(1)").text().trim()
        val year = dati.select(".stars > span:nth-child(8) > i:nth-child(1)").text().trim()
        val duration = dati.select(".stars > span:nth-child(10) > i:nth-child(1)").text().removeSuffix(" minuti")
        val isMovie = url.contains("/film/")

        return if (isMovie) {
            val streamUrl = response.select("#hostlinks").select("a").map { it.attr("href") }
            val plot = response.select(".post > p:nth-child(16)").text().trim()
            newMovieLoadResponse(title, url, TvType.Movie, streamUrl) {
                addPoster(poster)
                addRating(rating)
                this.duration = duration.toIntOrNull()
                this.year = year.toIntOrNull()
                this.tags = genres.split(",")
                this.plot = plot
            }
        } else {
            val episodes = getEpisodes(response)
            val plot = response.select(".post > p:nth-child(17)").text().trim()
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                addPoster(poster)
                addRating(rating)
                this.year = year.toIntOrNull()
                this.tags = genres.split(",")
                this.plot = plot
            }
        }
    }

    private fun getEpisodes(page: Document): List<Episode> {
        val table = page.selectFirst("#hostlinks > table:nth-child(1)")!!
        var season: Int? = 1
        val rows = table.select("tr")
        val episodes = rows.mapNotNull {
            if (it.childrenSize() == 0) {
                null
            } else if (it.childrenSize() == 1) {
                val seasonText = it.select("td:nth-child(1)").text().substringBefore("- Episodi disponibi")
                season = Regex("\\d+").find(seasonText)?.value?.toInt()
                null
            } else {
                val title = it.select("td:nth-child(1)").text()
                val links = it.select("a").map { a -> "${a.attr("href")}" }
                Episode("$links").apply {
                    this.season = season
                    this.episode = title.substringAfter("x").substringBefore(" ").toIntOrNull()
                }
            }
        }
        return episodes
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ): Boolean {
        Log.d("OnlineSerieTV:Links", "Data: $data")
        val links = parseJson<List<String>>(data)
        links.forEach {
            if (it.contains("uprot")) {
                val url = bypassUprot(it)
                Log.d("OnlineSerieTV:Links", "Bypassed Url: $url")
                if (url != null) {
                    if (url.contains("streamtape")) {
                        StreamTapeExtractor().getUrl(url, null, subtitleCallback, callback)
                    } else {
                        MaxStreamExtractor().getUrl(url, null, subtitleCallback, callback)
                    }
                    loadExtractor(url, subtitleCallback, callback)
                }
            }
        }
        return true
    }

    private suspend fun bypassUprot(link: String): String? {
        val updatedLink = if ("msf" in link) link.replace("msf", "mse") else link
        val headers = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36"
        )
        val response = app.get(updatedLink, headers = headers, timeout = 10_000)
        val document = response.document
        val maxstreamUrl = document.selectFirst("a")?.attr("href")
        return maxstreamUrl
    }
}
