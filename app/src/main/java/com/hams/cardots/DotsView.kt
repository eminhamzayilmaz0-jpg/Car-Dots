package com.hams.cardots

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View

class DotsView(context: Context) : View(context) {

    private val d = resources.displayMetrics.density
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(255, 255, 255, 255) }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * d
        color = Color.argb(180, 0, 0, 0)
    }
    private val maxOff = 60f * d

    private var fx = 0f
    private var fy = 0f
    private var tx = 0f
    private var ty = 0f
    private var ox = 0f
    private var oy = 0f
    private var vx = 0f
    private var vy = 0f
    private var last = 0L

    /** sx: ekranın sağına doğru ivme, sy: ekranın üstüne doğru ivme (m/s²) */
    fun setAccel(sx: Float, sy: Float, dpPerMs2: Float) {
        fx += 0.25f * (sx - fx)
        fy += 0.25f * (sy - fy)
        val k = dpPerMs2 * d
        // noktalar eylemsizlik gibi ivmenin tersine kayar
        tx = (-fx * k).coerceIn(-maxOff, maxOff)
        ty = (fy * k).coerceIn(-maxOff, maxOff)
    }

    override fun onDraw(c: Canvas) {
        val now = System.nanoTime()
        val dt = if (last == 0L) 0.016f else ((now - last) / 1e9f).coerceIn(0.001f, 0.05f)
        last = now

        vx += (120f * (tx - ox) - 16f * vx) * dt
        vy += (120f * (ty - oy) - 16f * vy) * dt
        ox += vx * dt
        oy += vy * dt

        val w = width.toFloat()
        val h = height.toFloat()
        val r = 5f * d
        val m = 14f * d

        for (col in 0..1) {
            val bx = if (col == 0) m else w - m
            val x = (bx + ox).coerceIn(r + 2f * d, w - r - 2f * d)
            for (i in 1..7) {
                val y = h * i / 8f + oy
                c.drawCircle(x, y, r, fill)
                c.drawCircle(x, y, r, ring)
            }
        }
        postInvalidateOnAnimation()
    }
}
