package com.example.esp32control

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

class SplashActivity : AppCompatActivity() {

    private val esp32Ip = "192.168.4.1"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val statusText: TextView = findViewById(R.id.statusText)

        CoroutineScope(Dispatchers.IO).launch {
            var success = false
            try {
                val url = URL("http://$esp32Ip/")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    success = true
                }
                connection.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            runOnUiThread {
                if (success) {
                    statusText.text = "Kommunikation erfolgreich hergestellt"
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(2000)
                        startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                        finish()
                    }
                } else {
                    statusText.text = "Fehler bei der Kommunikation"
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(3000)
                        startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                        finish()
                    }
                }
            }
        }
    }
}
