package com.lagradost


import android.util.Log
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.plugins.PluginManager
import com.lagradost.cloudstream3.utils.AppUtils
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.M3u8Helper
import com.lagradost.cloudstream3.utils.loadExtractor
import kotlinx.coroutines.runBlocking
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.util.Locale


class FrenchStreamProvider(val plugin: FrenchStreamProviderPlugin) : MainAPI() {
    override var mainUrl = "https://" + plugin.siteParam + "/"
    override var name = "FrenchStream"
    override val hasQuickSearch = false
    override val hasMainPage = true
    override var lang = "fr"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    override suspend fun search(query: String): List<SearchResponse> {
        val link = "$mainUrl/index.php?do=search&subaction=search&story=$query" // search'
        var success = false
        var document: Document? = null
        while (!success) {
            try {

                val response = app.get(
                    link,
                    timeout = 50
                )// app.get() permet de télécharger la page html avec une requete HTTP (get)
                if (response.isSuccessful) {
                    document = response.document
                    success = true
                }
            } catch (e: Exception) {
                throw e
            }
        }
        val results = document!!.select("div#dle-content > div.short")

        val allresultshome =
            results.mapNotNull { article ->  // avec mapnotnull si un élément est null, il sera automatiquement enlevé de la liste
                article.toSearchResponse()
            }
        return allresultshome
    }

    data class loadLinkData(
        val link: String,
        val lang: String? = null,
        val serverName: String? = null,
    )


    override suspend fun load(url: String): LoadResponse {
        val soup = app.post(
            url,
            data = mapOf("skin_name" to "VFV2", "action_skin_change" to "yes")
        ).document


        val title = soup.selectFirst("h1#s-title")?.text().toString()

        val description =
            soup.selectFirst("div.fdesc")?.text().toString()

        var poster: String? = null
        soup.select("style:containsData(tmdb)").firstOrNull {
            val regex = """\.fmain\s*\{[^}]*url\((.*)\)"""
            val posterFind = regex.toRegex().find(it.data())?.groupValues?.get(
                1
            )
            poster = posterFind
            !posterFind.isNullOrEmpty()
        }

        if (poster.isNullOrEmpty())
            poster = soup.selectFirst("div.fposter img")?.attr("src").toString()

        var actorsData = listOf<ActorData>()
        val actors_html = soup.select("script:containsData(actorData)").html()
        val actorData =
            Regex("""actorData[^"]*([^]]*)""").find(actors_html)?.groupValues?.get(1).toString()
        val actors = actorData.split(',')


        actorsData = actors.mapNotNull {

            val name = Regex(""""([^(]*)""").find(it)?.groupValues?.get(1)
            if (!name.isNullOrEmpty()) {
                val role = Regex("""\((.*)\)\s-""").find(it)?.groupValues?.get(1)
                val actorPoster = Regex("""-\s*([^"]*)""").find(it)?.groupValues?.get(1)
                ActorData(Actor(name, actorPoster), roleString = role)
            } else null
        }


        val tags = soup.select("ul#s-list > li a").map {
            it.text()
        }
        val isMovie = url.contains("/films/")

        val runtime = soup.select("span.runtime").text()
        val rungtime_gv = Regex("""(\d)h(\d)|(\d*)\s*min""").find(runtime)?.groupValues
        val duration = if (!rungtime_gv.isNullOrEmpty()) {
            if (!rungtime_gv.get(3).isNullOrEmpty())
                rungtime_gv.get(3).toInt()
            else {
                val hours = rungtime_gv?.get(1).toString().toInt() * 60
                val minutes = rungtime_gv?.get(2).toString().toInt()
                hours + minutes
            }
        } else null

        if (isMovie) {
            val servers = mutableListOf<loadLinkData>()
            val year = soup.selectFirst("ul.flist-col > li:contains(Date de sortie:)")?.ownText()

            val playerScript = soup.select("script:containsData(playerUrls)").html()

            val json =
                Regex("""playerUrls[^{]*(\{[^;]*);""").find(playerScript)?.groupValues?.get(1)
                    .toString()

            val parsed = tryParseJson<Map<String, Map<String, String>>>(json)
            parsed?.map { server ->
                val serverName = server.key
                server.value.map { langLink ->
                    val link = langLink.value
                    val lang = langLink.key
                    if (lang != "Default" && link.isNotEmpty())
                        servers.add(loadLinkData(link, lang, serverName))

                }
            }

            return newMovieLoadResponse(title, url, TvType.Movie, servers.toJson()) {
                //this.backgroundPosterUrl
                this.posterUrl = poster
                this.year = year?.toIntOrNull()
                this.tags = tags
                this.plot = description
                //this.rating = rating
                this.duration = duration
                this.actors = actorsData
                //addTrailer(soup.selectFirst("button#myBtn > a")?.attr("href"))
            }
        } else {
            val episodes = mutableListOf<Episode>()

            val fsEpisodes = mutableMapOf<String, MutableMap<Int, fsEpisode>>()

            soup.select("script[type=text/template]").forEach { template ->
                Jsoup.parse(template.data()).select(".episode-container")
                    .forEachIndexed { eIndex, eElement ->

                        val watch_button = eElement.select(".watch-button")
                        if (watch_button.isNotEmpty()) {
                            watch_button.forEach {
                                val ep = it.attr("data-episode").toString()
                                val lang = Regex("-(.*)").find(ep)?.groupValues?.get(1).toString()
                                    .uppercase(Locale.getDefault())
                                if (fsEpisodes[lang] == null)
                                    fsEpisodes[lang] = mutableMapOf<Int, fsEpisode>()

                                val epNumber = Regex("\\d+").find(ep)?.value.toString().toInt()

                                if (fsEpisodes[lang]!![epNumber] == null) {
                                    val title = eElement.select(".episode-title").text()
                                    val poster = eElement.select(".episode-image").attr("src")

                                    fsEpisodes[lang]!![epNumber] = fsEpisode(
                                        title,
                                        poster,
                                        mutableListOf(),
                                        epNumber,
                                        lang
                                    )
                                }
                                val url = it.attr("data-url")
                                fsEpisodes[lang]!![epNumber]!!.links.add(loadLinkData(url))
                            }
                        }
                    }
            }

            fsEpisodes.forEach { lang ->
                lang.value.forEach { e ->
                    episodes.add(newEpisode(e.value.links.toJson(), {
                        this.posterUrl = e.value.poster
                        this.episode = e.value.epNumber
                        this.name = e.value.title
                        this.season = fsEpisodes.keys.indexOf(lang.key) + 1
                    }))

                }

            }


            return newTvSeriesLoadResponse(
                title,
                url,
                TvType.TvSeries,
                episodes = episodes
            ) {
                this.year =
                    Regex("\\d+").find(soup.select(".release").text()).toString().toIntOrNull()
                this.posterUrl = poster
                this.plot = description
                this.tags = tags
                this.actors = actorsData
                this.duration = duration
                //addTrailer(soup.selectFirst("button#myBtn > a")?.attr("href"))
                addSeasonNames(fsEpisodes.keys.toList())
            }
        }
    }


    override suspend fun loadLinks(
        // TODO FIX *Garbage* data transmission betwenn function
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ): Boolean {
        val servers = tryParseJson<MutableList<loadLinkData>>(data)

        servers?.apmap {
            val urlplayer = it.link
            var playerUrl =
                if (urlplayer.contains("opsktp.com") || urlplayer.contains("flixeo.xyz")) {
                    val header = app.get(
                        urlplayer,
                        allowRedirects = false
                    ).headers
                    header["location"].toString()
                } else urlplayer


            Log.d("zzikozz", "playerUrl: $playerUrl")
            loadExtractor(playerUrl, mainUrl, subtitleCallback) { link ->
                val lang = when (it.lang) {
                    "VFF" -> "TRUEFRENCH"
                    "VFQ" -> "FRENCH"
                    "VOSTFR" -> "VOSTFR"
                    else -> {
                        ""
                    }
                }
                val serverName =
                    if (it.serverName != null) it.serverName + " " + lang else link.name
                callback.invoke(
                    ExtractorLink( // ici je modifie le callback pour ajouter des informations, normalement ce n'est pas nécessaire
                        link.source,
                        serverName,
                        link.url,
                        link.referer,
                        link.quality,
                        link.isM3u8,
                        link.headers,
                        link.extractorData
                    )
                )
            }
        }

        return true
    }


    private fun Element.toSearchResponse(): SearchResponse {

        var posterUrl = select("a.short-poster > img").attr("data-src")
        if (posterUrl.isNullOrEmpty())
            posterUrl = select("a.short-poster > img").attr("src")

        val qualityExtracted = select(".film-quality").text()
        val type = select("span.mli-eps").text().lowercase()
        val title = select("div.short-title").text()
        val link = select("a.short-poster").attr("href")

        val dubElement = select(".film-version").text().uppercase()
        val isSub = dubElement == "VOSTFR" || dubElement == "VOSTENG"

        val quality = getQualityFromString(
            when (!qualityExtracted.isNullOrBlank()) {
                qualityExtracted.contains("HD", ignoreCase = true) -> "HD"
                qualityExtracted.contains("Bdrip") -> "BlueRay"
                qualityExtracted.contains("DVD") -> "SD"
                qualityExtracted.contains("CAM") -> "Cam"
                else -> null
            }
        )

        if (type.isEmpty()) {

            return newAnimeSearchResponse(
                name = title,
                url = fixUrl(link),
                type = TvType.Movie,
            )
            {
                this.quality = quality
                this.posterUrl = posterUrl
                this.addDubStatus(isDub = !isSub)
            }

        } else  // a Serie
        {
            return newAnimeSearchResponse(
                name = title,
                url = fixUrl(link),
                type = TvType.TvSeries,

                ) {
                this.posterUrl = posterUrl
                addDubStatus(
                    isDub = !isSub,
                    episodes = type.toIntOrNull()
                )
            }


        }
    }

    data class mediaData(
        @JsonProperty("title") var title: String,
        @JsonProperty("url") val url: String,
    )

    data class fsEpisode(
        var title: String,
        var poster: String,
        val links: MutableList<loadLinkData>,
        var epNumber: Int?,
        val lang: String,
    )


    override val mainPage = mainPageOf(
        "" to "Box Office Film",
        "/films" to "Derniers Films",
        "/sries-du-moment" to "Box Office Série",
        "/s-tv" to "Dernieres Séries",
        "/netflix-series-" to "Nouveautés NETFLIX",
        "/series-apple-tv" to "Nouveautés Apple TV+",
        "/series-disney-plus" to "Nouveautés Disney+",
        "/serie-amazon-prime-videos" to "Nouveautés Prime Video",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {

        if (page == 1 && request.data == mainPage[0].data) {
            plugin.loadLink("frenchstream") { link ->
                if (link.isNotEmpty()) {
                    Log.d("zzikozz", "link: $link")
                    if (plugin.lastLoadedLink != link) {
                        Log.d("zzikozz", "lastLoadedLink: ${plugin.lastLoadedLink}")
                        plugin.lastLoadedLink = link
                        plugin.siteParam = link
                        runBlocking {
                            plugin.reload(plugin.activity)
                        }
                    }
                }
            }
        }

        val url = mainUrl + request.data + "/page/" + page
        var get = app.get(url)
        while (!get.isSuccessful)
            get = app.get(url)


        val movies = get.document.select("div#dle-content > div.short")

        val home =
            movies.apmap { article ->  // avec mapnotnull si un élément est null, il sera automatiquement enlevé de la liste
                article.toSearchResponse()
            }
        return newHomePageResponse(request.name, home)
    }
}
