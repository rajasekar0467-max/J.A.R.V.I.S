package com.jarvis.assistant

import android.graphics.Color
import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jarvis.assistant.core.Redactor
import com.jarvis.assistant.ui.navigation.AppRoot
import com.jarvis.assistant.ui.theme.JarvisTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching { enableEdgeToEdge() }

        try {
            val app = application as JarvisApplication
            setContent {
                JarvisTheme {
                    AppRoot(container = app.container)
                }
            }
        } catch (t: Throwable) {
            showSafeFallback(t)
        }
    }

    override fun onStop() {
        super.onStop()
        runCatching {
            (application as? JarvisApplication)?.container?.speechInput?.cancel()
        }
    }

    private fun showSafeFallback(error: Throwable) {
        val tv = TextView(this).apply {
            setBackgroundColor(Color.parseColor("#050907"))
            setTextColor(Color.parseColor("#E8F5EE"))
            setPadding(48, 96, 48, 48)
            textSize = 15f
            text = buildString {
                appendLine("JARVIS Safe Recovery Mode")
                appendLine()
                appendLine("The UI encountered a startup error instead of crashing:")
                appendLine(Redactor.redact("${error.javaClass.simpleName}: ${error.message ?: "Unknown error"}"))
            }
        }
        val scroll = ScrollView(this).apply { addView(tv) }
        setContentView(scroll)
    }
}
