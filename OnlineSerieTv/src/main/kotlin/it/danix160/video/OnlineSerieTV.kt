package it.danix160.video

import android.util.Log
import com.lagradost.cloudstream3.*
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.SocketTimeoutException

class OnlineSerieTV : MainAPI() {
    override var mainUrl = "https://onlineserietv.com"
    override var name = "OnlineSerieTV"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries, TvType.Cartoon)
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
        return when (section) {
            "Film: Ultimi aggiunti", "Serie TV: Ultime aggiunte" -> {
                val itemGrid = page.selectFirst(".wp-block-uagb-post-grid") ?: return emptyList()
                val items = itemGrid.select(".uagb-post__inner-wrap")
                items.mapNotNull {
                    val itemTag = it.select(".uagb-post__title > a")
                    val title = itemTag.text().trim().replace(Regex("""\d{4}\$"""), "").trim()
                    val url = itemTag.attr("href")
                    val poster = it.select(".uagb-post__image > a > img").attr("src")
                    val isMovie = url.contains("/film/")
                    if (isMovie) {
                        newMovieSearchResponse(title, url) { this.posterUrl = poster }
                    } else {
                        newTvSeriesSearchResponse(title, url) { this.posterUrl = poster }
                    }
                }
            }

            "Top 10 Film", "Top 10 Serie TV" -> {
                val sidebar = page.selectFirst(".sidebar_right") ?: return emptyList()
                val bothTop10 = sidebar.select(".links")
                val currentTop10 = if (section == "Top 10 Film") {
                    bothTop10.last()
                } else {
                    bothTop10.first()
                }
                val items = currentTop10?.select(".scrolling > li")
                items?.mapNotNull {
                    val aTag = it.selectFirst("a") ?: return@mapNotNull null
                    val title = aTag.text().trim().replace(Regex("""(19|20)\d{2}\$"""), "").trim()
                    val url = aTag.attr("href")
                    val showPage = try {
                        app.get(url).document
                    } catch (e: SocketTimeoutException) {
                        null
                    }
                    val poster = showPage?.select(".imgs > img:nth-child(1)")?.attr("src")
                    val isMovie = url.contains("/film/")
                    if (isMovie) {
                        newMovieSearchResponse(title, url) { this.posterUrl = poster }
                    } else {
                        newTvSeriesSearchResponse(title, url) { this.posterUrl = poster }
                    }
                } ?: emptyList()
            }

            else -> emptyList()
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val response = app.get("$mainUrl/?s=$query")
        val page = response.document
        val itemGrid = page.selectFirst("#box_movies") ?: return emptyList()
        val items = itemGrid.select(".movie")
        return items.mapNotNull { it.toSearchResponse() }
    }

    override suspend fun load(url: String): LoadResponse {
        val response = app.get(url).document
        val dati = response.selectFirst(".headingder") ?: throw ErrorLoadingException("No headingder found")
        val poster = dati.select(".imgs > img").attr("src").replace(Regex("""-\d+x\d+"""), "")
        val fullPoster = if (poster.startsWith("http")) poster else "$mainUrl$poster"
        val title = dati.select(".dataplus > div:nth-child(1) > h1").text().trim().replace(Regex("""\d{4}\$"""), "").trim()
        val rating = dati.selectFirst(".stars span:contains(/10)")?.text()?.removeSuffix("/10")
        val genreText = dati.select(".stars").text().lowercase()
        val genres = genreText.split("|").firstOrNull()?.split(",")?.map { it.trim() } ?: emptyList()
        val year = Regex("\b(19|20)\d{2}\b").find(genreText)?.value?.toIntOrNull()
        val duration = Regex("(\d+) minuti").find(genreText)?.groupValues?.get(1)?.toIntOrNull()
        val isMovie = url.contains("/film/")
        val plot = response.select(".post p").firstOrNull { it.text().length > 50 }?.text()?.trim() ?: ""

        return if (isMovie) {
            val streamUrl = response.select("#hostlinks").select("a").map { it.attr("href") }
            newMovieLoadResponse(title, url, TvType.Movie, streamUrl) {
                addPoster(fullPoster)
                addRating(rating)
                this.duration = duration
                this.year = year
                this.tags = genres
                this.plot = plot
            }
        } else {
            val episodes = getEpisodes(response)
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                addPoster(fullPoster)
                addRating(rating)
                this.year = year
                this.tags = genres
                this.plot = plot
            }
        }
    }

    private fun getEpisodes(page: Document): List<Episode> {
        val table = page.selectFirst("#hostlinks > table:nth-child(1)") ?: return emptyList()
        var season: Int? = 1
        val rows = table.select("tr")
        return rows.mapNotNull {
            if (it.childrenSize() == 0) return@mapNotNull null
            if (it.childrenSize() == 1) {
                val seasonText = it.select("td:nth-child(1)").text().substringBefore("- Episodi disponibi")
                season = Regex("""\d+""").find(seasonText)?.value?.toInt()
                return@mapNotNull null
            }
            val title = it.select("td:nth-child(1)").text()
            val links = it.select("a").map { a -> a.attr("href") }
            Episode(toJson(links)).apply {
                this.season = season
                this.episode = Regex("""\d+x(\d+)""").find(title)?.groupValues?.get(1)?.toIntOrNull()
            }
        }
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
                Log.d("OnlineSerieTV:Links", "Bypassed $it to $url")
                if (url != null) {
                    if (url.contains("streamtape")) {
                        StreamTapeExtractor().getUrl(url, null, subtitleCallback, callback)
                    } else {
                        MaxStreamExtractor().getUrl(url, null, subtitleCallback, callback)
                    }
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
        return document.selectFirst("a")?.attr("href")
    }

    private fun Element.toSearchResponse(): SearchResponse? {
        val aTag = this.selectFirst("a") ?: return null
        val rawTitle = aTag.attr("title").takeIf { it.isNotBlank() } ?: aTag.text().trim()
        val url = aTag.attr("href")
        val poster = this.selectFirst("img")?.attr("src")?.let {
            if (it.startsWith("http")) it else "$mainUrl$it"
        }
        val isMovie = url.contains("/film/")
        return if (isMovie) {
            newMovieSearchResponse(rawTitle, url) { this.posterUrl = poster }
        } else {
            newTvSeriesSearchResponse(rawTitle, url) { this.posterUrl = poster }
        }
    }
}
