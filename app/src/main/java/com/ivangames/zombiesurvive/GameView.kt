package com.ivangames.zombiesurvive

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class GameView(context: Context) : View(context) {
    private val paint = Paint()
    
    private var lastTime = 0L
    
    private var playerX = 0f
    private var playerY = 0f
    private val playerSpeed = 250f
    private val playerRadius = 30f

    private var camX = 0f
    private var camY = 0f

    // Левый джойстик
    private var joyLeftX = 200f
    private var joyLeftY = 500f
    private val joyRadius = 120f
    private var isTouchingLeft = false
    private var moveX = 0f
    private var moveY = 0f

    // Правый джойстик
    private var joyRightX = 1000f
    private var joyRightY = 500f
    private var isTouchingRight = false
    private var aimX = 0f
    private var aimY = 0f

    // Пули
    class Bullet(var x: Float, var y: Float, var dx: Float, var dy: Float) {
        val speed = 800f
        val radius = 8f
    }
    private val bullets = mutableListOf<Bullet>()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val currentTime = System.currentTimeMillis()
        val deltaTime = if (lastTime == 0L) 0f else (currentTime - lastTime) / 1000f
        lastTime = currentTime

        // Движение
        if (isTouchingLeft) {
            playerX += moveX * playerSpeed * deltaTime
            playerY += moveY * playerSpeed * deltaTime
        }

        camX = playerX - width / 2f
        camY = playerY - height / 2f

        // Земля (сетка)
        canvas.drawColor(Color.parseColor("#1A1A1A"))
        paint.color = Color.parseColor("#333333")
        paint.strokeWidth = 2f
        val gridSize = 100f
        val startX = (camX / gridSize).toInt() * gridSize
        val startY = (camY / gridSize).toInt() * gridSize
        for (i in -10..20) {
            val x = startX + i * gridSize - camX
            val y = startY + i * gridSize - camY
            canvas.drawLine(x, 0f, x, height.toFloat(), paint)
            canvas.drawLine(0f, y, width.toFloat(), y, paint)
        }

        // Пули
        paint.color = Color.YELLOW
        val iterator = bullets.iterator()
        while (iterator.hasNext()) {
            val bullet = iterator.next()
            bullet.x += bullet.dx * bullet.speed * deltaTime
            bullet.y += bullet.dy * bullet.speed * deltaTime
            canvas.drawCircle(bullet.x - camX, bullet.y - camY, bullet.radius, paint)
            if (Math.abs(bullet.x - playerX) > 2000 || Math.abs(bullet.y - playerY) > 2000) {
                iterator.remove()
            }
        }

        // Игрок
        val screenPlayerX = playerX - camX
        val screenPlayerY = playerY - camY
        paint.color = Color.BLACK
        canvas.drawCircle(screenPlayerX, screenPlayerY, playerRadius, paint)
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 5f
        canvas.drawCircle(screenPlayerX, screenPlayerY, playerRadius, paint)
        paint.style = Paint.Style.FILL

        // Левый джойстик
        paint.color = Color.parseColor("#40FFFFFF")
        canvas.drawCircle(joyLeftX, joyLeftY, joyRadius, paint)
        paint.color = Color.WHITE
        canvas.drawCircle(joyLeftX + moveX * joyRadius, joyLeftY + moveY * joyRadius, joyRadius / 2, paint)

        // Правый джойстик
        paint.color = Color.parseColor("#40FF0000")
        canvas.drawCircle(joyRightX, joyRightY, joyRadius, paint)
        paint.color = Color.RED
        // Ручка правого джойстика теперь двигается в сторону прицела
        canvas.drawCircle(joyRightX + aimX * joyRadius, joyRightY + aimY * joyRadius, joyRadius / 2, paint)

        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        joyLeftX = joyRadius + 80f
        joyLeftY = h - joyRadius - 80f
        joyRightX = w - joyRadius - 80f
        joyRightY = h - joyRadius - 80f
        playerX = w / 2f
        playerY = h / 2f
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val pointerIndex = event.actionIndex
                val x = event.getX(pointerIndex)
                val y = event.getY(pointerIndex)
                
                if (Math.hypot((x - joyLeftX).toDouble(), (y - joyLeftY).toDouble()) < joyRadius * 1.5) {
                    isTouchingLeft = true
                } else if (Math.hypot((x - joyRightX).toDouble(), (y - joyRightY).toDouble()) < joyRadius * 1.5) {
                    isTouchingRight = true
                    updateAim(x, y)
                    shoot()
                }
            }
            MotionEvent.ACTION_MOVE -> {
                // Движение левого
                for (i in 0 until event.pointerCount) {
                    val id = event.getPointerId(i)
                    val x = event.getX(i)
                    val y = event.getY(i)

                    // Если мы держим левый джойстик
                    if (isTouchingLeft && Math.hypot((x - joyLeftX).toDouble(), (y - joyLeftY).toDouble()) < joyRadius * 2) {
                        var dx = x - joyLeftX
                        var dy = y - joyLeftY
                        val distance = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
                        if (distance > joyRadius) {
                            dx = (dx / distance) * joyRadius
                            dy = (dy / distance) * joyRadius
                        }
                        moveX = dx / joyRadius
                        moveY = dy / joyRadius
                    }

                    // Если мы держим правый джойстик (целимся)
                    if (isTouchingRight && Math.hypot((x - joyRightX).toDouble(), (y - joyRightY).toDouble()) < joyRadius * 2) {
                        updateAim(x, y)
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                isTouchingLeft = false
                isTouchingRight = false
                moveX = 0f
                moveY = 0f
                aimX = 0f
                aimY = 0f
            }
        }
        return true
    }

    private fun updateAim(x: Float, y: Float) {
        var dx = x - joyRightX
        var dy = y - joyRightY
        val distance = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
        
        if (distance > 0) {
            aimX = dx / distance
            aimY = dy / distance
        } else {
            aimX = 0f
            aimY = 0f
        }
    }

    private fun shoot() {
        // Пуля летит в сторону прицела
        if (aimX != 0f || aimY != 0f) {
            bullets.add(Bullet(playerX, playerY, aimX, aimY))
        }
    }
}
