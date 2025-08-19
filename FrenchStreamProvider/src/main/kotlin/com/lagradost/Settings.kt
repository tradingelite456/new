package com.lagradost

import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import com.lagradost.api.BuildConfig
import com.lagradost.cloudstream3.plugins.PluginManager
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class Settings(val plugin: FrenchStreamProviderPlugin) : BottomSheetDialogFragment() {

    private var param1: String? = null
    private var param2: String? = null

    private val client = OkHttpClient()
    data class Link(
        val provider: String,
        val link: String
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    private fun getDrawable(name: String): Drawable? {
        val id =
            plugin.resources!!.getIdentifier(name, "drawable", com.lagradost.BuildConfig.LIBRARY_PACKAGE_NAME)
        return ResourcesCompat.getDrawable(plugin.resources!!, id, null)
    }

    private fun getString(name: String): String? {
        val id = plugin.resources!!.getIdentifier(name, "string", com.lagradost.BuildConfig.LIBRARY_PACKAGE_NAME)
        return plugin.resources!!.getString(id)
    }

    private fun <T : View> View.findView(name: String): T {
        val id = plugin.resources!!.getIdentifier(name, "id", com.lagradost.BuildConfig.LIBRARY_PACKAGE_NAME)
        return this.findViewById(id)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // collecting required resources
        val settingsLayoutId =
            plugin.resources!!.getIdentifier("settings", "layout", com.lagradost.BuildConfig.LIBRARY_PACKAGE_NAME)
        val settingsLayout = plugin.resources!!.getLayout(settingsLayoutId)
        val settings = inflater.inflate(settingsLayout, container, false)
        return settings
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // collecting required resources
        val outlineId = plugin.resources!!.getIdentifier("outline", "drawable", com.lagradost.BuildConfig.LIBRARY_PACKAGE_NAME)

        val updateIconId = plugin.resources!!.getIdentifier(
            "update_icon",
            "drawable",
            com.lagradost.BuildConfig.LIBRARY_PACKAGE_NAME
        )
        val updateBtn = view.findView<ImageView>("update")
        updateBtn.setImageDrawable(plugin.resources!!.getDrawable(updateIconId, null))
        updateBtn.background = plugin.resources!!.getDrawable(outlineId, null)
        updateBtn.setOnClickListener(
            object : OnClickListener {
                override fun onClick(btn: View) {

                    Toast.makeText(context, "Chargement du nouveau lien.", Toast.LENGTH_SHORT)
                        .show()

                    plugin.loadLink("frenchstream") { link ->
                        if (link.isNotEmpty()) {
                            val textView =
                                view.findView<TextInputEditText>("site_param_frenchstream")
                            textView.setText(link)
                        }
                    }
                }
            }
        )


        val saveIconId =
            plugin.resources!!.getIdentifier("save_icon", "drawable", com.lagradost.BuildConfig.LIBRARY_PACKAGE_NAME)

        // building save button and its click listener
        val saveBtn = view.findView<ImageView>("save")
        saveBtn.setImageDrawable(plugin.resources!!.getDrawable(saveIconId, null))
        saveBtn.background = plugin.resources!!.getDrawable(outlineId, null)
        saveBtn.setOnClickListener(
            object : OnClickListener {
                override fun onClick(btn: View) {
                    val text = view.findView<TextInputEditText>("site_param_frenchstream")
                    plugin.siteParam = text.getText().toString()

                    plugin.loadLink("frenchstream") { link ->
                        if (link.isNotEmpty()) {
                            plugin.lastLoadedLink = link
                        }
                    }

                    runBlocking {
                        plugin.reload(context)
                    }

                    Toast.makeText(context, "Modification enregistrée", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
            }
        )


        val text = view.findView<TextInputEditText>("site_param_frenchstream")
        text.setText(plugin.siteParam)
    }
}
