package com.jkstudio.jarvis

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        )
        setContentView(HolographicHudView(this))
    }
}

enum class AiCore(
    val title: String,
    val status: String,
    val primaryColor: Int,
    val secondaryColor: Int
) {
    JARVIS("✦ J.A.R.V.I.S.", "CREATIVE VISION • ACTIVE", Color.parseColor("#00F0FF"), Color.parseColor("#0077FF")),
    FRIDAY("🛡 F.R.I.D.A.Y.", "SECURITY & RISK • ACTIVE", Color.parseColor("#FFD700"), Color.parseColor("#FF8C00")),
    ULTRON("⚡ U.L.T.R.O.N.", "BRUTAL REALITY • ACTIVE", Color.parseColor("#FF1E42"), Color.parseColor("#8B0000")),
    AARYA("♥ A.R.Y.A.", "PERSONAL CORE • ONLINE", Color.parseColor("#D500F9"), Color.parseColor("#651FFF"))
}

class HolographicHudView(context: Context) : View(context) {
    // null means ALL-4 COUNCIL MODE is active by default
    private var lockedCore: AiCore? = null
    private var travelMode = false
    private var angle = 0f
    private var pulse = 0f
    private var pulseDir = 1f

    private val bgPaint = Paint().apply { color = Color.BLACK }
    private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.CENTER
    }
    private val leftTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.LEFT
    }

    private val nodes = List(45) {
        FloatArray(3) { (Random.nextFloat() - 0.5f) * 2f }
    }
    private val tabRects = mutableListOf<RectF>()
    private var travelBtnRect = RectF()
    private var councilBtnRect = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        val current = lockedCore
        val mainColor = current?.primaryColor ?: Color.parseColor("#D500F9")
        val subColor = current?.secondaryColor ?: Color.parseColor("#00F0FF")

        // Top Bar Title
        textPaint.color = Color.parseColor("#D4AF37")
        textPaint.textSize = h * 0.035f
        val headerTitle = if (current == null) {
            "⚡ ALL-4 COUNCIL DEBATE MODE • ONLINE"
        } else {
            "${current.title} • 1-TO-1 LOCKED MODE"
        }
        canvas.drawText(headerTitle, w * 0.5f, h * 0.08f, textPaint)

        // All-4 Council Reset Button (Top Left)
        councilBtnRect.set(w * 0.04f, h * 0.03f, w * 0.24f, h * 0.10f)
        corePaint.color = if (current == null) Color.parseColor("#00F0FF") else Color.GRAY
        corePaint.strokeWidth = 3f
        canvas.drawRoundRect(councilBtnRect, 12f, 12f, corePaint)
        textPaint.color = corePaint.color
        textPaint.textSize = h * 0.026f
        canvas.drawText("⚡ ALL-4 COUNCIL", councilBtnRect.centerX(), councilBtnRect.centerY() + 9f, textPaint)

        // Travel Mode Button (Top Right)
        travelBtnRect.set(w * 0.78f, h * 0.03f, w * 0.96f, h * 0.10f)
        corePaint.color = if (travelMode) Color.GREEN else Color.parseColor("#FF5252")
        corePaint.strokeWidth = 3f
        canvas.drawRoundRect(travelBtnRect, 12f, 12f, corePaint)
        textPaint.color = corePaint.color
        textPaint.textSize = h * 0.026f
        canvas.drawText(if (travelMode) "🔋 TRAVEL: ON" else "🔋 TRAVEL: OFF", travelBtnRect.centerX(), travelBtnRect.centerY() + 9f, textPaint)

        // Left Telemetry Panel
        leftTextPaint.color = mainColor
        leftTextPaint.textSize = h * 0.038f
        canvas.drawText(current?.title ?: "4-CORE WAR ROOM", w * 0.05f, h * 0.40f, leftTextPaint)
        leftTextPaint.textSize = h * 0.025f
        leftTextPaint.color = Color.LTGRAY
        canvas.drawText(current?.status ?: "DEBATE ENGINE: SYNCHRONIZED", w * 0.05f, h * 0.46f, leftTextPaint)
        canvas.drawText("LEAD VOICE: A.R.Y.A.", w * 0.05f, h * 0.51f, leftTextPaint)
        canvas.drawText(if (travelMode) "POWER: ULTRA-SAVER" else "POWER: FULL REACTOR", w * 0.05f, h * 0.56f, leftTextPaint)

        // Center 3D Neural Orb
        val cx = w * 0.5f
        val cy = h * 0.48f
        val baseRadius = h * 0.24f + pulse * 10f

        corePaint.color = mainColor
        corePaint.strokeWidth = 4f
        canvas.drawCircle(cx, cy, baseRadius, corePaint)

        corePaint.color = subColor
        corePaint.strokeWidth = 2f
        canvas.drawArc(
            RectF(cx - baseRadius * 1.18f, cy - baseRadius * 1.18f, cx + baseRadius * 1.18f, cy + baseRadius * 1.18f),
            angle, 240f, false, corePaint
        )
        canvas.drawArc(
            RectF(cx - baseRadius * 0.82f, cy - baseRadius * 0.82f, cx + baseRadius * 0.82f, cy + baseRadius * 0.82f),
            -angle * 1.5f, 200f, false, corePaint
        )

        if (!travelMode) {
            fillPaint.color = mainColor
            val radAngle = Math.toRadians(angle.toDouble())
            val cosA = cos(radAngle).toFloat()
            val sinA = sin(radAngle).toFloat()
            for (i in nodes.indices) {
                val nx = nodes[i][0] * cosA - nodes[i][2] * sinA
                val ny = nodes[i][1]
                val px = cx + nx * (baseRadius * 0.72f)
                val py = cy + ny * (baseRadius * 0.72f)
                canvas.drawCircle(px, py, 4f, fillPaint)
            }
        }

        textPaint.color = Color.WHITE
        textPaint.textSize = h * 0.028f
        canvas.drawText(if (current == null) "COUNCIL READY" else "LOCKED: ${current.name}", cx, cy + 8f, textPaint)

        // Right Radar Scanner
        val rx = w * 0.82f
        val ry = h * 0.46f
        val rr = h * 0.14f
        corePaint.color = mainColor
        corePaint.strokeWidth = 2f
        canvas.drawCircle(rx, ry, rr, corePaint)
        canvas.drawCircle(rx, ry, rr * 0.5f, corePaint)
        val sweepRad = Math.toRadians((angle * 2).toDouble())
        canvas.drawLine(rx, ry, rx + cos(sweepRad).toFloat() * rr, ry + sin(sweepRad).toFloat() * rr, corePaint)

        // Bottom 4 AI Switcher Tabs (All lit in Council mode, 1 lit in Locked mode)
        tabRects.clear()
        val cores = AiCore.values()
        val tabW = w * 0.21f
        val startX = w * 0.05f
        val gap = w * 0.02f
        val tabTop = h * 0.84f
        val tabBottom = h * 0.94f

        for (i in cores.indices) {
            val c = cores[i]
            val left = startX + i * (tabW + gap)
            val rect = RectF(left, tabTop, left + tabW, tabBottom)
            tabRects.add(rect)

            val isActive = (current == null) || (c == current)
            corePaint.color = if (isActive) c.primaryColor else Color.DKGRAY
            corePaint.strokeWidth = if (c == current) 5f else if (current == null) 3.5f else 2f
            canvas.drawRoundRect(rect, 14f, 14f, corePaint)

            textPaint.color = if (isActive) c.primaryColor else Color.GRAY
            textPaint.textSize = h * 0.031f
            canvas.drawText(c.title, rect.centerX(), rect.centerY() + 10f, textPaint)
        }

        val speed = if (travelMode) 0.5f else 2.2f
        angle = (angle + speed) % 360f
        pulse += 0.05f * pulseDir
        if (pulse > 1f || pulse < 0f) pulseDir *= -1f

        postInvalidateDelayed(if (travelMode) 60L else 16L)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            if (councilBtnRect.contains(event.x, event.y)) {
                lockedCore = null
                invalidate()
                return true
            }
            if (travelBtnRect.contains(event.x, event.y)) {
                travelMode = !travelMode
                invalidate()
                return true
            }
            val cores = AiCore.values()
            for (i in tabRects.indices) {
                if (tabRects[i].contains(event.x, event.y)) {
                    lockedCore = if (lockedCore == cores[i]) null else cores[i]
                    invalidate()
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }
}
