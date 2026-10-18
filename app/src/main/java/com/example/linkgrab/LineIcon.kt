package com.example.linkgrab

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.View

/** Small, consistent vector icons, drawn at any screen density. */
class LineIcon(context: Context, private val kind: String, var tint: Int) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.8f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        val scale = minOf(width, height) / 24f
        canvas.translate((width - 24 * scale) / 2, (height - 24 * scale) / 2)
        canvas.scale(scale, scale)
        paint.color = tint
        fun line(vararg points: Float) {
            val path = Path().apply {
                moveTo(points[0], points[1])
                for (i in 2 until points.size step 2) lineTo(points[i], points[i + 1])
            }
            canvas.drawPath(path, paint)
        }
        when (kind) {
            "download" -> {
                line(12f, 3f, 12f, 15f); line(7f, 10f, 12f, 15f, 17f, 10f)
                line(4f, 16f, 4f, 21f, 20f, 21f, 20f, 16f)
            }
            "home" -> {
                line(3f, 10f, 12f, 3f, 21f, 10f)
                line(5f, 9f, 5f, 21f, 10f, 21f, 10f, 14f, 14f, 14f, 14f, 21f, 19f, 21f, 19f, 9f)
            }
            "settings" -> {
                line(4f, 6f, 20f, 6f); line(4f, 18f, 20f, 18f)
                canvas.drawCircle(9f, 6f, 3f, paint); canvas.drawCircle(15f, 18f, 3f, paint)
            }
            "paste" -> {
                canvas.drawRoundRect(5f, 5f, 19f, 22f, 2f, 2f, paint)
                canvas.drawRoundRect(8f, 2f, 16f, 7f, 1f, 1f, paint)
            }
            "play" -> line(9f, 5f, 20f, 12f, 9f, 19f, 9f, 5f)
            "share" -> {
                line(8f, 11f, 16f, 6f); line(8f, 13f, 16f, 18f)
                canvas.drawCircle(5f, 12f, 3f, paint)
                canvas.drawCircle(19f, 4f, 3f, paint); canvas.drawCircle(19f, 20f, 3f, paint)
            }
            "arrow" -> { line(5f, 12f, 20f, 12f, 14f, 6f); line(20f, 12f, 14f, 18f) }
            "close" -> { line(6f, 6f, 18f, 18f); line(18f, 6f, 6f, 18f) }
            "check" -> line(5f, 12f, 10f, 17f, 20f, 6f)
            "refresh" -> {
                canvas.drawArc(4f, 4f, 20f, 20f, 40f, 285f, false, paint)
                line(20f, 3f, 20f, 9f, 14f, 9f)
            }
            "folder" -> line(3f, 7f, 3f, 4f, 10f, 4f, 13f, 7f, 21f, 7f, 21f, 20f, 3f, 20f, 3f, 7f)
            else -> {
                canvas.drawRoundRect(3f, 3f, 21f, 21f, 5f, 5f, paint)
                line(8f, 12f, 16f, 12f); line(12f, 8f, 12f, 16f)
            }
        }
        canvas.restore()
    }
}
