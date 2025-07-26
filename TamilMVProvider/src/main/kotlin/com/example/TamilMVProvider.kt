package com.example

import com.lagradost.cloudstream3.HomePageList
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.VPNStatus
import com.lagradost.cloudstream3.fixUrl
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newMovieLoadResponse
import com.lagradost.cloudstream3.newMovieSearchResponse
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.*

class TamilMVProvider : MainAPI() { // All providers must be an instance of MainAPI
    override var name = "1TamilMV"
    override var mainUrl = "https://1tamilmv.se"
    override val hasMainPage = true
    override val hasQuickSearch = false
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)
    override var lang = "en"
    override val vpnStatus = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "$mainUrl/index.php?/forums/forum/22-telugu-language/" to "Telugu",
        "$mainUrl/index.php?/forums/forum/9-tamil-language/" to "Tamil",
        "$mainUrl/index.php?/forums/forum/18-malayalam-movies/" to "Malayalam",
        "$mainUrl/index.php?/forums/forum/20-kannada-movies/" to "Kannada",
        "$mainUrl/index.php?/forums/forum/17-hollywood-movies-in-multi-audios/" to "Hindi",
        "$mainUrl/index.php?/forums/forum/19-web-series-tv-shows/" to "Web Series"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page <= 1) request.data else "${request.data}page/$page/"
        val document = app.get(url).document
        val list = document.select("li.ipsDataItem").mapNotNull {
            val title = it.selectFirst(".ipsDataItem_title a")?.text()?.trim() ?: return@mapNotNull null
            val link = fixUrl(it.selectFirst(".ipsDataItem_title a")?.attr("href") ?: return@mapNotNull null)
            val poster = it.selectFirst("img")?.absUrl("src")
            newMovieSearchResponse(title, link, TvType.Movie) {
                this.posterUrl = poster
            }
        }
        return newHomePageResponse(HomePageList(request.name, list, isHorizontalImages = true), hasNext = true)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/index.php?/search/&q=${query.replace(" ", "+")}"
        val document = app.get(url).document
        return document.select("li.ipsDataItem").mapNotNull {
            val title = it.selectFirst(".ipsDataItem_title a")?.text()?.trim() ?: return@mapNotNull null
            val link = fixUrl(it.selectFirst(".ipsDataItem_title a")?.attr("href") ?: return@mapNotNull null)
            val poster = it.selectFirst("img")?.absUrl("src")
            newMovieSearchResponse(title, link, TvType.Movie) {
                this.posterUrl = poster
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("h1.ipsType_pagetitle")?.text()?.trim() ?: "Unknown"
        val description = document.selectFirst("div.ipsType_richText")?.text()?.trim()
        val poster = document.selectFirst("img[src]")?.absUrl("src")

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = poster
            this.plot = description
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        document.select("a[href^=magnet:]").forEach { a ->
            val magnet = a.attr("href")
            callback.invoke(
                ExtractorLink(
                    source = "Magnet",
                    name = "Torrent Magnet",
                    url = magnet,
                    referer = data,
                    quality = Qualities.Unknown.value,
                    type = ExtractorLinkType.TORRENT
                )
            )
        }
        return true
    }
}