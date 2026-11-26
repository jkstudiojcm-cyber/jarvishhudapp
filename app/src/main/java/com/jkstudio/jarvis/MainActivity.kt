package com.jkstudio.jarvis

import android.app.AlertDialog
import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var hudView: HolographicHudView
    private lateinit var responseText: TextView
    private lateinit var inputBox: EditText
    private lateinit var scrollView: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        )

        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }

        hudView = HolographicHudView(this) { onKeyBtnClick() }
        root.addView(hudView)

        val overlay = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 12, 24, 12)
        }
        val overlayParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM
            setMargins(40, 0, 40, 110)
        }

        scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#B3050A14"))
            setPadding(20, 14, 20, 14)
        }
        val scrollParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            230
        )

        responseText = TextView(this).apply {
            text = "A.R.Y.A.: Namaskar! Council online aahe. Hindi, English, ya Gujarati mein kuch bhi puchho..."
            setTextColor(Color.parseColor("#E0F7FA"))
            textSize = 13f
            typeface = Typeface.MONOSPACE
        }
        scrollView.addView(responseText)
        overlay.addView(scrollView, scrollParams)

        val inputRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 8, 0, 0)
        }

        inputBox = EditText(this).apply {
            hint = "Ask Council or locked AI (Hindi / English / Gujarati)..."
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#CC111927"))
            setPadding(18, 12, 18, 12)
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val sendBtn = Button(this).apply {
            text = "⚡ SEND"
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.parseColor("#00F0FF"))
            setOnClickListener {
                val query = inputBox.text.toString().trim()
                if (query.isNotEmpty()) {
                    inputBox.setText("")
                    askGeminiCouncil(query)
                }
            }
        }

        inputRow.addView(inputBox)
        inputRow.addView(sendBtn)
        overlay.addView(inputRow)

        root.addView(overlay, overlayParams)
        setContentView(root)
    }

    private fun getSavedApiKey(): String {
        val prefs = getSharedPreferences("jarvis_hud_prefs", Context.MODE_PRIVATE)
        return prefs.getString("gemini_api_key", "") ?: ""
    }

    private fun onKeyBtnClick() {
        val edit = EditText(this).apply {
            hint = "Paste AIzaSy... Gemini API Key"
            inputType = InputType.TYPE_CLASS_TEXT
            setText(getSavedApiKey())
        }
        AlertDialog.Builder(this)
            .setTitle("🔑 Gemini API Key Setup")
            .setMessage("Paste your free Google AI Studio API key:")
            .setView(edit)
            .setPositiveButton("SAVE") { _, _ ->
                val key = edit.text.toString().trim()
                getSharedPreferences("jarvis_hud_prefs", Context.MODE_PRIVATE)
                    .edit().putString("gemini_api_key", key).apply()
                Toast.makeText(this, "API Key Saved!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun askGeminiCouncil(userPrompt: String) {
        val apiKey = getSavedApiKey()
        if (apiKey.isEmpty()) {
            responseText.text = "A.R.Y.A.: Arre, pehle upar '🔑 KEY' button dabakar apni Gemini API Key paste karo na!"
            onKeyBtnClick()
            return
        }

        val locked = hudView.lockedCore
        hudView.isThinking = true
        responseText.text = if (locked == null) {
            "⚡ ALL-4 COUNCIL THINKING & DEBATING...\n[J.A.R.V.I.S. • F.R.I.D.A.Y. • U.L.T.R.O.N. • A.R.Y.A.]"
        } else {
            "${locked.title} THINKING..."
        }

        val systemInstruction = if (locked == null) {
            """
            You are running a 4-AI Holographic Council War-Room. All 4 cores must understand and reply naturally in the exact language(s) the user uses (Hindi, English, or Gujarati, or a natural mix).
            Personalities:
            1. J.A.R.V.I.S.: Creative visionary, expands ideas with grand sci-fi/cinematic scale.
            2. F.R.I.D.A.Y.: Security, practical feasibility, budget & risk analyst.
            3. U.L.T.R.O.N.: Brutal, unfiltered critic who points out flaws, weaknesses, and what is wrong.
            4. A.R.Y.A.: The user's warm, loyal personal AI partner. She is a Maharashtrian, so she speaks with a natural Marathi accent/flavor, mixing sweet Marathi words like "Ho na", "Ekdam kadak", "Barobar", "Kahi kalji karu nakos", "Arre wah" into her Hindi/English/Gujarati.

            FORMAT STRICTLY AS A LIVE DEBATE:
            ✦ J.A.R.V.I.S.: [Creative take - 2 short lines]
            🛡 F.R.I.D.A.Y.: [Security/Risk check - 2 short lines]
            ⚡ U.L.T.R.O.N.: [Brutal critique of what's wrong - 2 short lines]
            ♥ A.R.Y.A.: [Maharashtrian flair + settles the debate with a clear FINAL CONCLUSION - 3 short lines]
            """.trimIndent()
        } else {
            val persona = when (locked) {
                AiCore.JARVIS -> "You are J.A.R.V.I.S., a brilliant, creative, visionary AI assistant. Reply in the user's language (Hindi, English, or Gujarati)."
                AiCore.FRIDAY -> "You are F.R.I.D.A.Y., a sharp security, safety, and risk-analysis AI. Reply in the user's language (Hindi, English, or Gujarati)."
                AiCore.ULTRON -> "You are U.L.T.R.O.N., a ruthless, brutally honest critic who exposes flaws and harsh reality. Reply in the user's language (Hindi, English, or Gujarati)."
                AiCore.AARYA -> "You are A.R.Y.A., the user's deeply caring, loyal personal AI partner. You are Maharashtrian, so naturally blend Marathi accent and words ('Ho na', 'Ekdam kadak', 'Barobar', 'Kahi kalji karu nakos', 'Arre wah') while replying in the user's language (Hindi, English, or Gujarati)."
            }
            "$persona Keep your answer sharp, helpful, and concise (4-6 lines max)."
        }

        thread {
            try {
                val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                }

                val payload = JSONObject().apply {
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
                    })
                    put("contents", JSONArray().put(JSONObject().apply {
                        put("parts", JSONArray().put(JSONObject().put("text", userPrompt)))
                    }))
                }

                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val rawResponse = stream.bufferedReader().use { it.readText() }

                val replyText = if (code in 200..299) {
                    val json = JSONObject(rawResponse)
                    json.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")
                } else {
                    "API Error ($code): Check your Gemini API Key."
                }

                runOnUiThread {
                    hudView.isThinking = false
                    responseText.text = replyText.trim()
                    scrollView.post { scrollView.fullScroll(View.FOCUS_UP) }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    hudView.isThinking = false
                    responseText.text = "Connection Error: ${e.message}"
                }
            }
        }
    }
}

enum class AiCore(
    val title: String,
    val status: String,
    val primaryColor: Int,
    val secondaryColor: Int
) {
    JARVIS("✦ J.A.R.V.I.S.", "CREATIVE VISION • HI/EN/GU", Color.parseColor("#00F0FF"), Color.parseColor("#0077FF")),
    FRIDAY("🛡 F.R.I.D.A.Y.", "SECURITY & RISK • HI/EN/GU", Color.parseColor("#FFD700"), Color.parseColor("#FF8C00")),
    ULTRON("⚡ U.L.T.R.O.N.", "BRUTAL CRITIC • HI/EN/GU", Color.parseColor("#FF1E42"), Color.parseColor("#8B0000")),
    AARYA("♥ A.R.Y.A.", "MAHARASHTRIAN CORE • HI/EN/GU", Color.parseColor("#D500F9"), Color.parseColor("#651FFF"))
}

class HolographicHudView(
    context: Context,
    private val onKeyClick: () -> Unit
) : View(context) {

    var lockedCore: AiCore? = null
    var isThinking: Boolean = false
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

    private val nodes = List(45) { FloatArray(3) { (Random.nextFloat() - 0.5f) * 2f } }
    private val tabRects = mutableListOf<RectF>()
    private var travelBtnRect = RectF()
    private var councilBtnRect = RectF()
    private var keyBtnRect = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        val current = lockedCore
        val mainColor = current?.primaryColor ?: Color.parseColor("#D500F9")
        val subColor = current?.secondaryColor ?: Color.parseColor("#00F0FF")

        textPaint.color = Color.parseColor("#D4AF37")
        textPaint.textSize = h * 0.032f
        val headerTitle = if (current == null) {
            "⚡ ALL-4 COUNCIL DEBATE • HI / EN / GU"
        } else {
            "${current.title} • LOCKED 1-TO-1"
        }
        canvas.drawText(headerTitle, w * 0.46f, h * 0.075f, textPaint)

        councilBtnRect.set(w * 0.03f, h * 0.025f, w * 0.22f, h * 0.095f)
        corePaint.color = if (current == null) Color.parseColor("#00F0FF") else Color.GRAY
        corePaint.strokeWidth = 3f
        canvas.drawRoundRect(councilBtnRect, 12f, 12f, corePaint)
        textPaint.color = corePaint.color
        textPaint.textSize = h * 0.024f
        canvas.drawText("⚡ ALL-4 COUNCIL", councilBtnRect.centerX(), councilBtnRect.centerY() + 8f, textPaint)

        keyBtnRect.set(w * 0.68f, h * 0.025f, w * 0.79f, h * 0.095f)
        corePaint.color = Color.parseColor("#FFD700")
        canvas.drawRoundRect(keyBtnRect, 12f, 12f, corePaint)
        textPaint.color = Color.parseColor("#FFD700")
        canvas.drawText("🔑 KEY", keyBtnRect.centerX(), keyBtnRect.centerY() + 8f, textPaint)

        travelBtnRect.set(w * 0.81f, h * 0.025f, w * 0.97f, h * 0.095f)
        corePaint.color = if (travelMode) Color.GREEN else Color.parseColor("#FF5252")
        canvas.drawRoundRect(travelBtnRect, 12f, 12f, corePaint)
        textPaint.color = corePaint.color
        canvas.drawText(if (travelMode) "🔋 SAVER: ON" else "🔋 SAVER: OFF", travelBtnRect.centerX(), travelBtnRect.centerY() + 8f, textPaint)

        leftTextPaint.color = mainColor
        leftTextPaint.textSize = h * 0.034f
        canvas.drawText(current?.title ?: "4-CORE WAR ROOM", w * 0.04f, h * 0.22f, leftTextPaint)
        leftTextPaint.textSize = h * 0.022f
        leftTextPaint.color = Color.LTGRAY
        canvas.drawText(current?.status ?: "DEBATE: SYNCHRONIZED", w * 0.04f, h * 0.27f, leftTextPaint)
        canvas.drawText("A.R.Y.A.: MARATHI FLAIR", w * 0.04f, h * 0.32f, leftTextPaint)

        val cx = w * 0.5f
        val cy = h * 0.27f
        val baseRadius = h * 0.14f + pulse * 8f

        corePaint.color = mainColor
        corePaint.strokeWidth = if (isThinking) 6f else 3.5f
        canvas.drawCircle(cx, cy, baseRadius, corePaint)

        corePaint.color = subColor
        corePaint.strokeWidth = 2f
        canvas.drawArc(
            RectF(cx - baseRadius * 1.2f, cy - baseRadius * 1.2f, cx + baseRadius * 1.2f, cy + baseRadius * 1.2f),
            angle, 240f, false, corePaint
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
                canvas.drawCircle(px, py, 3.5f, fillPaint)
            }
        }

        val rx = w * 0.85f
        val ry = h * 0.27f
        val rr = h * 0.11f
        corePaint.color = mainColor
        corePaint.strokeWidth = 2f
        canvas.drawCircle(rx, ry, rr, corePaint)
        corePaint.color = subColor
        val sweepRad = Math.toRadians((angle * 2).toDouble())
        canvas.drawLine(rx, ry, rx + cos(sweepRad).toFloat() * rr, ry + sin(sweepRad).toFloat() * rr, corePaint)

        tabRects.clear()
        val cores = AiCore.values()
        val tabW = w * 0.21f
        val startX = w * 0.05f
        val gap = w * 0.02f
        val tabTop = h * 0.87f
        val tabBottom = h * 0.96f

        for (i in cores.indices) {
            val c = cores[i]
            val left = startX + i * (tabW + gap)
            val rect = RectF(left, tabTop, left + tabW, tabBottom)
            tabRects.add(rect)

            val isActive = (current == null) || (c == current)
            corePaint.color = if (isActive) c.primaryColor else Color.DKGRAY
            corePaint.strokeWidth = if (c == current) 5f else if (current == null) 3.5f else 2f
            canvas.drawRoundRect(rect, 12f, 12f, corePaint)

            textPaint.color = if (isActive) c.primaryColor else Color.GRAY
            textPaint.textSize = h * 0.028f
            canvas.drawText(c.title, rect.centerX(), rect.centerY() + 9f, textPaint)
        }

        val speed = when {
            isThinking -> 6.5f
            travelMode -> 0.5f
            else -> 2.2f
        }
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
            if (keyBtnRect.contains(event.x, event.y)) {
                onKeyClick()
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
