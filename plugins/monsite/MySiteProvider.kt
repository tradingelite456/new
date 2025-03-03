package com.wiflix

import com.lagradost.cloudstream3.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class WiflixProvider : MainAPI() {
    override var name = "Wiflix"
    override var mainUrl = "http://wiflix-pro.mom"
    override var lang = "fr"

    override val hasMainPage = true

    override suspend fun loadHomePage(): HomePageResponse {
        val doc = Jsoup.connect(mainUrl).get()
        val movies = doc.select(".TPostMv").mapNotNull {
            it.toSearchResult()
        }
        return HomePageResponse(listOf(HomePageList("Films récents", movies)))
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/?s=${query.replace(" ", "+")}"
        val doc = Jsoup.connect(url).get()
        return doc.select(".TPostMv").mapNotNull {
            it.toSearchResult()
        }
    }

    private fun Element.toSearchResult(): MovieSearchResponse? {
        val title = select("h2").text()
        val href = select("a").attr("href")
        val img = select("img").attr("src")
        return if (title.isNotBlank() && href.isNotBlank()) {
            MovieSearchResponse(title, href, img, null, "Wiflix")
        } else null
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = Jsoup.connect(url).get()
        val videoUrl = doc.select("iframe").attr("src")
        return VideoLoadResponse(url, videoUrl)
    }
}
