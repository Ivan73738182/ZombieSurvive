package com.ivangames.zombiesurvive

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.view.WindowManager
import android.graphics.Color

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Убираем статус-бар для полного экрана
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        // Пока просто выводим текст по центру
        val textView = TextView(this).apply {
            text = "ZombieSurvive\n\nПроект успешно собран!\nСкоро здесь будет игра."
            textSize = 24f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.BLACK)
            gravity = android.view.Gravity.CENTER
        }
        
        setContentView(textView)
    }
}
