package com.example.wavelock

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.LinearGradient
import android.graphics.Shader
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import android.view.MotionEvent
import android.os.Handler
import android.os.Looper
import kotlin.math.sin

class WaveWallpaperService : WallpaperService() {
    private data class Ripple(val x: Float, val y: Float, val startedAt: Long)

    override fun onCreateEngine(): Engine = WaveEngine()

    private inner class WaveEngine : Engine() {
        private val ripples = mutableListOf<Ripple>()
        private val handler = Handler(Looper.getMainLooper())
        private var visible = false
        private var startTime = System.nanoTime()
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val frame = Runnable { drawFrame() }

        init {
            setTouchEventsEnabled(true)
        }

        override fun onTouchEvent(event: MotionEvent) {
            if (event.actionMasked == MotionEvent.ACTION_DOWN ||
                event.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
                ripples.add(Ripple(event.x, event.y, System.currentTimeMillis()))
                while (ripples.size > 8) ripples.removeAt(0)
                if (visible) drawFrame()
            }
            super.onTouchEvent(event)
        }

        override fun onVisibilityChanged(visibleNow: Boolean) {
            visible = visibleNow
            if (visible) drawFrame() else handler.removeCallbacks(frame)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            if (visible) drawFrame()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            visible = false
            handler.removeCallbacks(frame)
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            visible = false
            handler.removeCallbacks(frame)
            super.onDestroy()
        }

        private fun drawFrame() {
            if (!visible) return
            var canvas: Canvas? = null
            try {
                canvas = surfaceHolder.lockCanvas()
                if (canvas != null) render(canvas)
            } catch (_: Exception) {
            } finally {
                if (canvas != null) {
                    try { surfaceHolder.unlockCanvasAndPost(canvas) } catch (_: Exception) { }
                }
            }
            handler.removeCallbacks(frame)
            if (visible) handler.postDelayed(frame, 33L)
        }

        private fun render(canvas: Canvas) {
            val prefs = getSharedPreferences(WaveConfig.PREFS, MODE_PRIVATE)
            val style = prefs.getInt(WaveConfig.STYLE, 0)
            val speed = prefs.getFloat(WaveConfig.SPEED, 1f)
            val amplitude = prefs.getFloat(WaveConfig.AMPLITUDE, .65f)
            val glow = prefs.getFloat(WaveConfig.GLOW, .7f)
            val w = canvas.width.toFloat()
            val h = canvas.height.toFloat()
            val t = (System.nanoTime() - startTime) / 1_000_000_000.0 * speed
            canvas.drawColor(android.graphics.Color.rgb(4, 19, 35))
            paint.shader = LinearGradient(0f, 0f, 0f, h,
                intArrayOf(0xFF09213D.toInt(), 0xFF087D91.toInt(), 0xFF03101F.toInt()),
                null, Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, w, h, paint)
            paint.shader = null

            val colors = when (style) {
                1 -> intArrayOf(0xFF3A9FAD.toInt(), 0xFF56C8C8.toInt(), 0xFF8DE1D8.toInt(), 0xFFB0F0E8.toInt())
                2 -> intArrayOf(0xFF7656C8.toInt(), 0xFF318EBB.toInt(), 0xFF31BDAE.toInt(), 0xFF8DDBFF.toInt())
                else -> intArrayOf(0xFF07516F.toInt(), 0xFF087D9B.toInt(), 0xFF16A6B0.toInt(), 0xFF65D7D0.toInt())
            }
            for (layer in 0..3) {
                val base = h * (.49f + layer * .115f)
                val path = Path().apply {
                    moveTo(0f, h)
                    var x = 0f
                    while (x <= w) {
                        val wave = sin(x / w * 6.0 + t * (1.0 + layer * .12) + layer).toFloat()
                        val secondary = sin(x / w * 11.0 - t * .7 + layer * 2).toFloat() * .22f
                        val y = base + (wave + secondary) * h * .043f * amplitude
                        lineTo(x, y)
                        x += 4f
                    }
                    lineTo(w, h); close()
                }
                paint.color = colors[layer]
                paint.alpha = (150 + glow * 105).toInt().coerceIn(0, 255)
                paint.shader = LinearGradient(0f, base - h * .08f, 0f, h,
                    colors[layer], colors[layer] and 0x00FFFFFF, Shader.TileMode.CLAMP)
                canvas.drawPath(path, paint)
                paint.shader = null
            }
            val now = System.currentTimeMillis()
            val iterator = ripples.iterator()
            while (iterator.hasNext()) {
                val ripple = iterator.next()
                val age = now - ripple.startedAt
                val duration = 1250f
                if (age >= duration) {
                    iterator.remove()
                    continue
                }
                val progress = age / duration
                val maxRadius = (w.coerceAtMost(h) * .24f)
                val radius = maxRadius * progress
                val alpha = ((1f - progress) * 190).toInt().coerceIn(0, 190)
                paint.shader = null
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = (3.5f * (1f - progress) + .8f)
                paint.color = if (style == 2) 0xFFB9B0FF.toInt() else 0xFFD9FFFF.toInt()
                paint.alpha = alpha
                canvas.drawCircle(ripple.x, ripple.y, radius, paint)
                canvas.drawCircle(ripple.x, ripple.y, radius * .72f, paint)
                paint.style = Paint.Style.FILL
            }
            paint.alpha = 255
            paint.style = Paint.Style.FILL
        }
    }
}
