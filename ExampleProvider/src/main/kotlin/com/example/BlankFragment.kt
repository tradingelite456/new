package com.example

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.lagradost.cloudstream3.R
import android.widget.TextView

class BlankFragment : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return TextView(requireContext()).apply {
            text = "Extension CinePulse\n\nAucun paramètre disponible pour le moment"
            setTextAppearance(R.style.ResultInfoText)
            setPadding(50, 50, 50, 50)
            textSize = 16f
        }
    }
}
