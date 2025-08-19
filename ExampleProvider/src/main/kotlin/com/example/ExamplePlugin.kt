package com.example

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class ExamplePlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(ExampleProvider())
        
        // Optional settings - simplified version
        openSettings = {
            BlankFragment().show(it, "CinePulseSettings")
        }
    }
}
