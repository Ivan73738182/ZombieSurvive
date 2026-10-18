package com.ivangames.zombiesurvive

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot
import kotlin.random.Random

class GameView(context: Context) : View(context) {
    private val paint = Paint()
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 50f
        isAntiAlias = true
    }
    private val random = Random(42)

    private var lastTime = 0L
    private var lastShootTime = 0L
    private val shootCooldown = 150L

    // Игрок
    private var playerX = 0f
    private var playerY = 0f
    private val playerSpeed = 250f
    private val playerRadius = 30f
    private var hp = 100
    private var ammo = 30
    private val maxAmmo = 30

    private var camX = 0f
    private var camY = 0f

    // Джойстики
    private var joyLeftX = 200f
    private var joyLeftY = 500f
    private val joyRadius = 120f
    private var isTouchingLeft = false
    private var leftPointerId = -1
    private var moveX = 0f
    private var moveY = 0f

    private var joyRightX = 1000f
    private var joyRightY = 500f
    private var isTouchingRight = false
    private var rightPointerId = -1
    private var aimX = 0f
    private var aimY = 0f

    // Кнопки UI
    private val invBtnRect = RectF()
    private val doorBtnRect = RectF()
    private var isDoorBtnVisible = false
    private var isDoorOpen = false

    // Пули
    class Bullet(var x: Float, var y: Float, var dx: Float, var dy: Float) {
        val speed = 800f
        val radius = 8f
    }
    private val bullets = mutableListOf<Bullet>()

    // Мир (деревья и кусты)
    class Plant(var x: Float, var y: Float, var type: Int, var size: Float, var radius: Float)
    private val plants = mutableListOf<Plant>()

    // Зомби
    class Zombie(var x: Float, var y: Float) {
        val radius = 30f
        val speed = 100f
        var hp = 100
    }
    private val zombies = mutableListOf<Zombie>()

    // База (координаты)
    private val baseX = 0f
    private val baseY = 0f
    private val fenceRadius = 400f
    private val houseSize = 200f
    private val doorX = 0f
    private val doorY = -400f // Дверь в заборе (сверху)
    private val chestX = 0f
    private val chestY = 0f

    init {
        // Генерация деревьев (вдали от базы)
        for (i in 0..150) {
            var x = random.nextFloat() * 4000f - 2000f
            var y = random.nextFloat() * 4000f - 2000f
            // Не спавним деревья внутри базы
            if (hypot(x.toDouble(), y.toDouble()) < fenceRadius + 100) {
                x += 800f
                y += 800f
            }
            val type = if (random.nextBoolean()) 1 else 2
            val size = if (type == 2) 40f + random.nextFloat() * 30f else 20f + random.nextFloat() * 15f
            // Радиус для коллизий
            val collisionRadius = if (type == 2) 15f else size 
            plants.add(Plant(x, y, type, size, collisionRadius))
        }

        // Спавн зомби (вне базы)
        for (i in 0..4) {
            val angle = random.nextFloat() * Math.PI * 2
            val dist = fenceRadius + 200f + random.nextFloat() * 500f
            val zx = baseX + (Math.cos(angle) * dist).toFloat()
            val zy = baseY + (Math.sin(angle) * dist).toFloat()
            zombies.add(Zombie(zx, zy))
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val currentTime = System.currentTimeMillis()
        val deltaTime = if (lastTime == 0L) 0f else (currentTime - lastTime) / 1000f
        lastTime = currentTime

        // 1. Логика игрока
        if (isTouchingLeft) {
            val nextX = playerX + moveX * playerSpeed * deltaTime
            val nextY = playerY + moveY * playerSpeed * deltaTime
            
            // Проверка коллизий с деревьями
            var canMove = true
            for (plant in plants) {
                if (plant.type == 2) { // Только деревья твёрдые
                    if (hypot((nextX - plant.x).toDouble(), (nextY - plant.y).toDouble()) < playerRadius + plant.radius) {
                        canMove = false
                        break
                    }
                }
            }
            
            // Проверка на забор базы (если дверь закрыта)
            if (!isDoorOpen) {
                val distToBase = hypot(nextX.toDouble(), nextY.toDouble())
                if (distToBase < fenceRadius && distToBase > fenceRadius - 20f) {
                    // Если пытаемся пройти через забор, но не через дверь
                    if (hypot((nextX - doorX).toDouble(), (nextY - doorY).toDouble()) > 60f) {
                        canMove = false
                    }
                }
            }

            if (canMove) {
                playerX = nextX
                playerY = nextY
            }
        }

        // 2. Логика зомби (идут к игроку)
        for (zombie in zombies) {
            val dx = playerX - zombie.x
            val dy = playerY - zombie.y
            val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()
            
            if (dist > 0) {
                var nextZx = zombie.x + (dx / dist) * zombie.speed * deltaTime
                var nextZy = zombie.y + (dy / dist) * zombie.speed * deltaTime
                
                // Коллизии зомби с деревьями
                var canMove = true
                for (plant in plants) {
                    if (plant.type == 2 && hypot((nextZx - plant.x).toDouble(), (nextZy - plant.y).toDouble()) < zombie.radius + plant.radius) {
                        canMove = false
                        break
                    }
                }
                
                // Коллизии зомби с другими зомби
                for (other in zombies) {
                    if (other != zombie && hypot((nextZx - other.x).toDouble(), (nextZy - other.y).toDouble()) < zombie.radius * 2) {
                        canMove = false
                        break
                    }
                }
                
                if (canMove) {
                    zombie.x = nextZx
                    zombie.y = nextZy
                }
            }
        }

        // 3. Стрельба
        if (isTouchingRight && ammo > 0) {
            if (currentTime - lastShootTime > shootCooldown) {
                shoot()
                lastShootTime = currentTime
            }
        }

        // 4. Логика пуль
        val bulletIterator = bullets.iterator()
        while (bulletIterator.hasNext()) {
            val bullet = bulletIterator.next()
            bullet.x += bullet.dx * bullet.speed * deltaTime
            bullet.y += bullet.dy * bullet.speed * deltaTime
            
            // Проверка попадания в зомби
            var hit = false
            val zombieIterator = zombies.iterator()
            while (zombieIterator.hasNext()) {
                val z = zombieIterator.next()
                if (hypot((bullet.x - z.x).toDouble(), (bullet.y - z.y).toDouble()) < z.radius) {
                    z.hp -= 25
                    hit = true
                    if (z.hp <= 0) zombieIterator.remove()
                    break
                }
            }
            
            if (hit || Math.abs(bullet.x - playerX) > 2000 || Math.abs(bullet.y - playerY) > 2000) {
                bulletIterator.remove()
            }
        }

        camX = playerX - width / 2f
        camY = playerY - height / 2f

        // --- РИСОВАНИЕ ---
        canvas.drawColor(Color.parseColor("#2E4A29"))

        // База (Забор)
        paint.color = Color.parseColor("#4E342E")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 10f
        canvas.drawCircle(baseX - camX, baseY - camY, fenceRadius, paint)
        paint.style = Paint.Style.FILL

        // Дверь в заборе
        paint.color = if (isDoorOpen) Color.GREEN else Color.RED
        canvas.drawRect(doorX - camX - 30f, doorY - camY - 10f, doorX - camX + 30f, doorY - camY + 10f, paint)

        // Дом внутри базы
        paint.color = Color.parseColor("#3E2723")
        canvas.drawRect(baseX - camX - houseSize/2, baseY - camY - houseSize/2, baseX - camX + houseSize/2, baseY - camY + houseSize/2, paint)
        
        // Ящик
        paint.color = Color.parseColor("#FFB300")
        canvas.drawRect(chestX - camX - 20f, chestY - camY - 20f, chestX - camX + 20f, chestY - camY + 20f, paint)

        // Растения
        for (plant in plants) {
            val screenX = plant.x - camX
            val screenY = plant.y - camY
            if (screenX > -100 && screenX < width + 100 && screenY > -100 && screenY < height + 100) {
                if (plant.type == 1) {
                    paint.color = Color.parseColor("#1B5E20")
                    canvas.drawCircle(screenX, screenY, plant.size, paint)
                } else if (plant.type == 2) {
                    paint.color = Color.parseColor("#5D4037")
                    canvas.drawRect(screenX - 10f, screenY, screenX + 10f, screenY + plant.size, paint)
                    paint.color = Color.parseColor("#2E7D32")
                    canvas.drawCircle(screenX, screenY - 10f, plant.size, paint)
                }
            }
        }

        // Зомби (зеленые)
        paint.color = Color.parseColor("#00FF00")
        for (z in zombies) {
            canvas.drawCircle(z.x - camX, z.y - camY, z.radius, paint)
            // Полоска ХП зомби
            paint.color = Color.RED
            canvas.drawRect(z.x - camX - 20, z.y - camY - 40, z.x - camX + 20, z.y - camY - 35, paint)
            paint.color = Color.GREEN
            val hpWidth = (z.hp / 100f) * 40
            canvas.drawRect(z.x - camX - 20, z.y - camY - 40, z.x - camX - 20 + hpWidth, z.y - camY - 35, paint)
            paint.color = Color.parseColor("#00FF00")
        }

        // Пули
        paint.color = Color.YELLOW
        for (b in bullets) {
            canvas.drawCircle(b.x - camX, b.y - camY, b.radius, paint)
        }

        // Игрок
        paint.color = Color.BLACK
        canvas.drawCircle(playerX - camX, playerY - camY, playerRadius, paint)
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 5f
        canvas.drawCircle(playerX - camX, playerY - camY, playerRadius, paint)
        paint.style = Paint.Style.FILL

        // Джойстики
        paint.color = Color.parseColor("#40FFFFFF")
        canvas.drawCircle(joyLeftX, joyLeftY, joyRadius, paint)
        paint.color = Color.WHITE
        canvas.drawCircle(joyLeftX + moveX * joyRadius, joyLeftY + moveY * joyRadius, joyRadius / 2, paint)

        paint.color = Color.parseColor("#40FF0000")
        canvas.drawCircle(joyRightX, joyRightY, joyRadius, paint)
        paint.color = Color.RED
        canvas.drawCircle(joyRightX + aimX * joyRadius, joyRightY + aimY * joyRadius, joyRadius / 2, paint)

        // --- UI ---
        textPaint.color = Color.RED
        canvas.drawText("HP: $hp", 50f, 80f, textPaint)
        textPaint.color = Color.YELLOW
        canvas.drawText("Ammo: $ammo/$maxAmmo", width - 350f, 80f, textPaint)

        // Кнопка Инвентарь
        paint.color = Color.parseColor("#80000000")
        canvas.drawRect(invBtnRect, paint)
        textPaint.textSize = 40f
        textPaint.color = Color.WHITE
        val invTextWidth = textPaint.measureText("ИНВЕНТАРЬ")
        canvas.drawText("ИНВЕНТАРЬ", invBtnRect.centerX() - invTextWidth / 2, invBtnRect.centerY() + 15f, textPaint)
        textPaint.textSize = 50f

        // Кнопка Дверь (показывается, если рядом)
        if (hypot((playerX - doorX).toDouble(), (playerY - doorY).toDouble()) < 120f) {
            isDoorBtnVisible = true
            doorBtnRect.set(width / 2f - 150f, height - 200f, width / 2f + 150f, height - 100f)
            paint.color = Color.parseColor("#800000FF")
            canvas.drawRect(doorBtnRect, paint)
            textPaint.textSize = 40f
            val doorText = if (isDoorOpen) "ЗАКРЫТЬ" else "ОТКРЫТЬ"
            val doorTextWidth = textPaint.measureText(doorText)
            canvas.drawText(doorText, doorBtnRect.centerX() - doorTextWidth / 2, doorBtnRect.centerY() + 15f, textPaint)
            textPaint.textSize = 50f
        } else {
            isDoorBtnVisible = false
        }

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

        invBtnRect.set(w / 2f - 150f, h - 100f, w / 2f + 150f, h - 20f)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)
                val x = event.getX(pointerIndex)
                val y = event.getY(pointerIndex)

                // Кнопка инвентаря
                if (invBtnRect.contains(x, y)) {
                    return true
                }

                // Кнопка двери
                if (isDoorBtnVisible && doorBtnRect.contains(x, y)) {
                    isDoorOpen = !isDoorOpen
                    return true
                }

                // Джойстики
                if (hypot((x - joyLeftX).toDouble(), (y - joyLeftY).toDouble()) < joyRadius * 1.5) {
                    isTouchingLeft = true
                    leftPointerId = pointerId
                } else if (hypot((x - joyRightX).toDouble(), (y - joyRightY).toDouble()) < joyRadius * 1.5) {
                    isTouchingRight = true
                    rightPointerId = pointerId
                    updateAim(x, y)
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
        if (ammo > 0 && (aimX != 0f || aimY != 0f)) {
            bullets.add(Bullet(playerX, playerY, aimX, aimY))
            ammo--
        }
    }
}
