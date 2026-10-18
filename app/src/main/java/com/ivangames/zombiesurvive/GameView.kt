package com.ivangames.zombiesurvive

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View

class GameView(context: Context) : View(context) {
    private val paint = Paint()
    
    // Координаты игрока (в пикселях)
    private var playerX = 500f
    private var playerY = 500f
    private val playerRadius = 40f

    // Переменные для джойстика
    private var joyX = 200f
    private var joyY = 700f // Примерно левый нижний угол
    private val joyRadius = 100f
    private var isTouchingJoy = false
    private var moveX = 0f
    private var moveY = 0f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. Рисуем пол (тёмно-серый фон)
        canvas.drawColor(Color.parseColor("#1A1A1A"))

        // 2. Рисуем игрока (чёрный кружок с белой обводкой)
        paint.color = Color.BLACK
        canvas.drawCircle(playerX, playerY, playerRadius, paint)
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 5f
        canvas.drawCircle(playerX, playerY, playerRadius, paint)
        paint.style = Paint.Style.FILL

        // 3. Рисуем джойстик ходьбы (слева)
        paint.color = Color.parseColor("#40FFFFFF") // Полупрозрачный белый
        canvas.drawCircle(joyX, joyY, joyRadius, paint) // База джойстика
        
        paint.color = Color.WHITE
        canvas.drawCircle(joyX + moveX, joyY + moveY, joyRadius / 2, paint) // Ручка джойстика
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Проверяем, нажали ли мы на джойстик
                if (Math.hypot((event.x - joyX).toDouble(), (event.y - joyY).toDouble()) < joyRadius) {
                    isTouchingJoy = true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isTouchingJoy) {
                    // Вычисляем вектор движения
                    var dx = event.x - joyX
                    var dy = event.y - joyY
                    val distance = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
                    
                    // Ограничиваем ручку джойстика радиусом
                    if (distance > joyRadius) {
                        dx = (dx / distance) * joyRadius
                        dy = (dy / distance) * joyRadius
                    }
                    
                    moveX = dx
                    moveY = dy
                    
                    // Двигаем игрока (пока просто напрямую)
                    playerX += moveX * 0.05f
                    playerY += moveY * 0.05f
                    
                    invalidate() // Перерисовать экран
                }
            }
            MotionEvent.ACTION_UP -> {
                // Отпустили палец — джойстик в центр, игрок стоит
                isTouchingJoy = false
                moveX = 0f
                moveY = 0f
                invalidate()
            }
        }
        return true
    }

    // Этот метод нужен, чтобы джойстик был в углу, даже если экран повернут
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        joyX = joyRadius + 50f
        joyY = h - joyRadius - 50f // Внизу слева
        playerX = w / 2f
        playerY = h / 2f
    }
}
