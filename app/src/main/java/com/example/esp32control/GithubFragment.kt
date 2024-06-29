package com.example.esp32control

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment

class GithubFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_github, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cardAppCode: CardView = view.findViewById(R.id.cardAppCode)
        val cardEsp32Code: CardView = view.findViewById(R.id.cardEsp32Code)

        cardAppCode.setOnClickListener {
            // Navigate to APP Code
            //findNavController().navigate(R.id.action_githubFragment_to_appCodeFragment)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Cabzla/LED_OLED_Control"))
            startActivity(intent)
        }

        cardEsp32Code.setOnClickListener {
            // Navigate to ESP32 Code
            //findNavController().navigate(R.id.action_githubFragment_to_esp32CodeFragment)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Cabzla/LED_OLED_ESP32"))
            startActivity(intent)
        }
    }
}