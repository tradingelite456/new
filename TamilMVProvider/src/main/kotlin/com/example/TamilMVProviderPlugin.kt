package com.example

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class TamilMVProviderPlugin: Plugin() {
    private var activity: AppCompatActivity? = null

    override fun load(context: Context) {
        activity = context as? AppCompatActivity

        // All providers should be added in this manner
        registerMainAPI(TamilMVProvider())

        openSettings = {
            val frag = TamilMVFragment(this)
            activity?.let {
                frag.show(it.supportFragmentManager, "Frag")
            }
        }
    }
}