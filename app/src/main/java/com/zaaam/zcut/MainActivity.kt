package com.zaaam.zcut

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Stage 2 scaffold. The real Compose navigation graph replaces this activity
 * only after the mockup receives explicit ACC via Telegram.
 */
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val title = TextView(this).apply {
            text = getString(R.string.scaffold_title)
            textSize = 38f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#C9FF4A"))
            gravity = Gravity.CENTER
        }
        val message = TextView(this).apply {
            text = getString(R.string.scaffold_message)
            textSize = 14f
            setTextColor(Color.parseColor("#8A8F9C"))
            gravity = Gravity.CENTER
            setPadding(48, 24, 48, 0)
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#08090D"))
            addView(title)
            addView(message)
        }
        setContentView(root)
    }
}
