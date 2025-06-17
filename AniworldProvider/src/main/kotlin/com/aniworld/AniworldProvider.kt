package com.aniworld

import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.ExtractorLink
import com.lagradost.cloudstream3.app

class AniworldProvider : MainAPI() { // All providers must be an instance of MainAPI
    override var mainUrl = "https://aniworld.to/"
    override var name = "Aniworld"
    override val supportedTypes = setOf(TvType.Anime)

    override var lang = "de"

    // Enable this when your provider has a main page
    override val hasMainPage = true

    // This function gets called when you search for something
    override suspend fun search(query: String): List<SearchResponse> {
        // val document = app.get("$mainUrl/search?keyword=$query").document
        // TODO: Parse HTML and extract search results
        return emptyList()
    }

    override suspend fun load(url: String): LoadResponse? {
        // val document = app.get(url).document
        // TODO: Parse HTML and extract anime details
        return null
    }

    override suspend fun loadLinks(
        data: String, // Passed from load
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        // TODO: Extract episode links
        return false // Return false in case of error
    }
}