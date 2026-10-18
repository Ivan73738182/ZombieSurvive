package com.ivangames.zombiesurvive

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.TextView
import android.graphics.Color

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
        )

        try {
            // Пробуем открыть меню
            val intent = Intent(this, MenuActivity::class.java)
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            // Если упало — показываем ошибку на экране
            val textView = TextView(this).apply {
                text = "ОШИБКА ПРИ ЗАПУСКЕ:\n\n${e.message}\n\n${e.stackTraceToString()}"
                textSize = 16f
                setTextColor(Color.RED)
                setBackgroundColor(Color.BLACK)
            }
            setContentView(textView)
        }
    }
}
