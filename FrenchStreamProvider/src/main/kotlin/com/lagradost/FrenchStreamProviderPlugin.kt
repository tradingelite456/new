package com.lagradost

import android.content.Context
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.lagradost.Settings.Link
import com.lagradost.cloudstream3.AcraApplication.Companion.getKey
import com.lagradost.cloudstream3.AcraApplication.Companion.setKey
import com.lagradost.cloudstream3.MainActivity.Companion.afterPluginsLoadedEvent
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.extractors.Filesim
import com.lagradost.cloudstream3.extractors.Voe
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import com.lagradost.cloudstream3.plugins.PluginManager
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.extractors.Lulustream2
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities


@CloudstreamPlugin
class FrenchStreamProviderPlugin : Plugin() {
    var activity: AppCompatActivity? = null

    var siteParam: String
        get() = getKey("SITE_PARAM_FRENCHSTREAM") ?: "fsmirror2.lol"
        set(value) {
            setKey("SITE_PARAM_FRENCHSTREAM", value)
        }

    var lastLoadedLink: String
        get() = getKey("LAST_LOADED_LINK_FRENCHSTREAM") ?: siteParam
        set(value) {
            setKey("LAST_LOADED_LINK_FRENCHSTREAM", value)
        }

    fun loadLink(provider: String, onLinkLoaded: (String) -> Unit) {
        var link = String()

        val url = "https://codeberg.org/zzikozz/frencharchive/raw/branch/Release/links.json"
        val request = Request.Builder()
            .url(url)
            .build()

        OkHttpClient().newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: Call, e: IOException) {}

            override fun onResponse(call: okhttp3.Call, response: Response) {
                response.use {
                    val responseData = it.body?.string()
                    if (responseData != null) {
                        val lData = tryParseJson<List<Link>>(responseData)!!
                        link = lData.first { it.provider == provider }.link
                        onLinkLoaded(link)
                    }
                }
            }
        })
    }


    override fun load(context: Context) {
        activity = context as AppCompatActivity

        // All providers should be added in this manner
        registerMainAPI(FrenchStreamProvider(this))
        registerExtractorAPI(MaxFinishSeveral())
        registerExtractorAPI(Bf0skv())
        registerExtractorAPI(Fsvid())

        openSettings = { ctx ->
            val frag = Settings(this)
            frag.show(activity!!.supportFragmentManager, "")
        }
    }

    suspend fun reload(context: Context?) {
        val pluginData =
            PluginManager.getPluginsOnline().find { it.internalName.contains("FrenchStream") }
        if (pluginData == null) {
            PluginManager.hotReloadAllLocalPlugins(context as AppCompatActivity)
        } else {
            PluginManager.unloadPlugin(pluginData.filePath)
            PluginManager.loadSinglePlugin(context!!, pluginData.internalName.replace("provider", "", ignoreCase = true))
            afterPluginsLoadedEvent.invoke(true)
        }
    }
}

class MaxFinishSeveral : Voe() {
    override val mainUrl = "https://maxfinishseveral.com"
}

class Bf0skv : Filesim() {
    override val name = "FileMoon"
    override val mainUrl = "https://bf0skv.org"
}

open class Fsvid : ExtractorApi() {
    override val name = "Fsvid"
    override val mainUrl = "https://fsvid.lol"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {

        val m3u8Regex = """file:\s*"([^"]+\.m3u8[^"]*)"""".toRegex()
        val soup = app.get(url).text

        val m3u8 = m3u8Regex.find(soup)?.groupValues?.getOrNull(1)

        callback.invoke(
            ExtractorLink(
                this.name,
                this.name,
                m3u8 ?: return,
                "",
                Qualities.Unknown.value,
                isM3u8 = true
            )
        )
    }
}
