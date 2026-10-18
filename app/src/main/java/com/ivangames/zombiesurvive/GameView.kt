package com.ivangames.zombiesurvive

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot
import kotlin.random.Random

class GameView(context: Context) : View(context) {
    private val paint = Paint()
    private val random = Random(42) // Фиксированный сид, чтобы карта не менялась

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
    private var leftPointerId = -1
    private var moveX = 0f
    private var moveY = 0f

    // Правый джойстик
    private var joyRightX = 1000f
    private var joyRightY = 500f
    private var isTouchingRight = false
    private var rightPointerId = -1
    private var aimX = 0f
    private var aimY = 0f

    // Пули
    class Bullet(var x: Float, var y: Float, var dx: Float, var dy: Float) {
        val speed = 800f
        val radius = 8f
    }
    private val bullets = mutableListOf<Bullet>()

    // Мир (растения и деревья)
    class Plant(var x: Float, var y: Float, var type: Int, var size: Float)
    private val plants = mutableListOf<Plant>()

    init {
        // Генерируем мир (200 объектов на карте 4000x4000)
        for (i in 0..200) {
            val x = random.nextFloat() * 4000f - 2000f
            val y = random.nextFloat() * 4000f - 2000f
            val type = random.nextInt(3) // 0 - трава, 1 - куст, 2 - дерево
            val size = if (type == 2) 40f + random.nextFloat() * 30f else 15f + random.nextFloat() * 20f
            plants.add(Plant(x, y, type, size))
        }
    }

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

        // 1. Фон (Трава / Земля)
        canvas.drawColor(Color.parseColor("#2E4A29")) // Тёмно-зелёный цвет травы

        // 2. Рисуем мир (только то, что видно на экране)
        for (plant in plants) {
            val screenX = plant.x - camX
            val screenY = plant.y - camY

            // Оптимизация: не рисуем то, что за экраном
            if (screenX > -100 && screenX < width + 100 && screenY > -100 && screenY < height + 100) {
                when (plant.type) {
                    0 -> { // Пучок травы
                        paint.color = Color.parseColor("#4CAF50")
                        canvas.drawCircle(screenX, screenY, plant.size / 2, paint)
                    }
                    1 -> { // Куст
                        paint.color = Color.parseColor("#1B5E20")
                        canvas.drawCircle(screenX, screenY, plant.size, paint)
                        paint.color = Color.parseColor("#388E3C")
                        canvas.drawCircle(screenX + 10f, screenY - 10f, plant.size * 0.7f, paint)
                    }
                    2 -> { // Дерево
                        // Ствол
                        paint.color = Color.parseColor("#5D4037")
                        canvas.drawRect(screenX - 10f, screenY, screenX + 10f, screenY + plant.size, paint)
                        // Крона
                        paint.color = Color.parseColor("#2E7D32")
                        canvas.drawCircle(screenX, screenY - 10f, plant.size, paint)
                    }
                }
            }
        }

        // 3. Пули
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

        // 4. Игрок (Чёрный)
        val screenPlayerX = playerX - camX
        val screenPlayerY = playerY - camY
        paint.color = Color.BLACK
        canvas.drawCircle(screenPlayerX, screenPlayerY, playerRadius, paint)
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 5f
        canvas.drawCircle(screenPlayerX, screenPlayerY, playerRadius, paint)
        paint.style = Paint.Style.FILL

        // 5. Джойстики
        // Левый
        paint.color = Color.parseColor("#40FFFFFF")
        canvas.drawCircle(joyLeftX, joyLeftY, joyRadius, paint)
        paint.color = Color.WHITE
        canvas.drawCircle(joyLeftX + moveX * joyRadius, joyLeftY + moveY * joyRadius, joyRadius / 2, paint)

        // Правый
        paint.color = Color.parseColor("#40FF0000")
        canvas.drawCircle(joyRightX, joyRightY, joyRadius, paint)
        paint.color = Color.RED
        canvas.drawCircle(joyRightX + aimX * joyRadius, joyRightY + aimY * joyRadius, joyRadius / 2, paint)

        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        joyLeftX = joyRadius + 80f
        joyLeftY = h - joyRadius - 80f
        joyRightX = w - joyRadius - 80f
        joyRightY = h - joyRadius - 80f
        playerX = 0f
        playerY = 0f
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)
                val x = event.getX(pointerIndex)
                val y = event.getY(pointerIndex)

                if (hypot((x - joyLeftX).toDouble(), (y - joyLeftY).toDouble()) < joyRadius * 1.5) {
                    isTouchingLeft = true
                    leftPointerId = pointerId
                } else if (hypot((x - joyRightX).toDouble(), (y - joyRightY).toDouble()) < joyRadius * 1.5) {
                    isTouchingRight = true
                    rightPointerId = pointerId
                    updateAim(x, y)
                    shoot()
                }
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val pointerId = event.getPointerId(i)
                    val x = event.getX(i)
                    val y = event.getY(i)

                    if (pointerId == leftPointerId && isTouchingLeft) {
                        var dx = x - joyLeftX
                        var dy = y - joyLeftY
                        val distance = hypot(dx.toDouble(), dy.toDouble()).toFloat()
                        if (distance > joyRadius) {
                            dx = (dx / distance) * joyRadius
                            dy = (dy / distance) * joyRadius
                        }
                        moveX = dx / joyRadius
                        moveY = dy / joyRadius
                    }

                    if (pointerId == rightPointerId && isTouchingRight) {
                        updateAim(x, y)
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)

                if (pointerId == leftPointerId) {
                    isTouchingLeft = false
                    leftPointerId = -1
                    moveX = 0f
                    moveY = 0f
                }
                if (pointerId == rightPointerId) {
                    isTouchingRight = false
                    rightPointerId = -1
                    aimX = 0f
                    aimY = 0f
                }
            }
        }
        return true
    }

    private fun updateAim(x: Float, y: Float) {
        var dx = x - joyRightX
        var dy = y - joyRightY
        val distance = hypot(dx.toDouble(), dy.toDouble()).toFloat()
        if (distance > 0) {
            aimX = dx / distance
            aimY = dy / distance
        }
    }

    private fun shoot() {
        if (aimX != 0f || aimY != 0f) {
            bullets.add(Bullet(playerX, playerY, aimX, aimY))
        }
    }
}
