package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.control.AssistantCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FloatingAssistantService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingRootView: LinearLayout? = null
    private var bubbleContainer: LinearLayout? = null
    private var bubbleIcon: ImageView? = null
    private var expandedPanel: LinearLayout? = null
    private var statusTextView: TextView? = null
    private var followUpTextView: TextView? = null
    private var suggestionsContainer: LinearLayout? = null

    private var isExpanded = false
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        createFloatingView()
        AssistantCoordinator.setFloatingServiceRunning(true)
        observeCoordinatorState()
    }

    private fun createFloatingView() {
        val wm = windowManager ?: return

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 260
        }

        val dpToPx = { dp: Int ->
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp.toFloat(),
                resources.displayMetrics
            ).toInt()
        }

        // 1. Root Vertical Layout
        floatingRootView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.START
        }

        // 2. Floating Mic Bubble Container
        bubbleContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            val size = dpToPx(56)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                bottomMargin = dpToPx(6)
            }
            background = createBubbleDrawable(isListening = false)
            elevation = dpToPx(8).toFloat()

            // Mic Icon
            bubbleIcon = ImageView(this@FloatingAssistantService).apply {
                setImageResource(android.R.drawable.ic_btn_speak_now)
                layoutParams = LinearLayout.LayoutParams(dpToPx(30), dpToPx(30))
                setColorFilter(Color.WHITE)
            }
            addView(bubbleIcon)
        }

        // 3. Expandable Control Panel
        expandedPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            val panelWidth = dpToPx(320)
            layoutParams = LinearLayout.LayoutParams(panelWidth, LinearLayout.LayoutParams.WRAP_CONTENT)
            setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10))

            // Rounded Dark Card Background
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(18).toFloat()
                setColor(Color.parseColor("#EE1A2234"))
                setStroke(dpToPx(1), Color.parseColor("#4438BDF8"))
            }
            elevation = dpToPx(10).toFloat()

            // Header Row: Assistant Title & Close Button
            val headerRow = LinearLayout(this@FloatingAssistantService).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

                val titleText = TextView(this@FloatingAssistantService).apply {
                    text = "🤖 Asisten Standby HP"
                    setTextColor(Color.parseColor("#38BDF8"))
                    textSize = 12f
                    typeface = Typeface.DEFAULT_BOLD
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                addView(titleText)

                val minimizeBtn = TextView(this@FloatingAssistantService).apply {
                    text = "✕"
                    setTextColor(Color.LTGRAY)
                    textSize = 14f
                    typeface = Typeface.DEFAULT_BOLD
                    setPadding(dpToPx(6), dpToPx(2), dpToPx(6), dpToPx(2))
                    setOnClickListener {
                        toggleExpanded(false)
                    }
                }
                addView(minimizeBtn)
            }
            addView(headerRow)

            // Status message
            statusTextView = TextView(this@FloatingAssistantService).apply {
                text = "Asisten siap kendalikan HP di latar belakang."
                setTextColor(Color.WHITE)
                textSize = 12f
                typeface = Typeface.DEFAULT
                setPadding(0, dpToPx(4), 0, dpToPx(2))
            }
            addView(statusTextView)

            // Follow-up question in bright accent
            followUpTextView = TextView(this@FloatingAssistantService).apply {
                text = "Apa langkah selanjutnya?"
                setTextColor(Color.parseColor("#FDE047"))
                textSize = 11.5f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 0, 0, dpToPx(6))
            }
            addView(followUpTextView)

            // Fixed Action Buttons Row: [🔽 Gulir] [🔼 Atas] [⌨️ Ketik] [✉️ Kirim] [🔍 YT] [💬 WA] [🏠 Home]
            val actionScrollView = HorizontalScrollView(this@FloatingAssistantService).apply {
                isHorizontalScrollBarEnabled = false
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

                val actionsLayout = LinearLayout(this@FloatingAssistantService).apply {
                    orientation = LinearLayout.HORIZONTAL

                    val defaultActions = listOf(
                        Pair("🔽 Gulir Bawah", "gulir ke bawah"),
                        Pair("🔼 Gulir Atas", "gulir ke atas"),
                        Pair("✉️ Kirim Pesan", "kirim pesan"),
                        Pair("🔍 Cari YouTube", "buka youtube"),
                        Pair("💬 Buka WA", "buka whatsapp"),
                        Pair("🏠 Beranda", "beranda"),
                        Pair("◀️ Kembali", "kembali")
                    )

                    for ((label, cmd) in defaultActions) {
                        val btn = createActionChip(label, dpToPx) {
                            AssistantCoordinator.onFloatingActionSubmitted?.invoke(cmd)
                        }
                        addView(btn)
                    }
                }
                addView(actionsLayout)
            }
            addView(actionScrollView)

            // Dynamic Suggestion Chips Row
            val suggestionsScrollView = HorizontalScrollView(this@FloatingAssistantService).apply {
                isHorizontalScrollBarEnabled = false
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dpToPx(6)
                }

                suggestionsContainer = LinearLayout(this@FloatingAssistantService).apply {
                    orientation = LinearLayout.HORIZONTAL
                }
                addView(suggestionsContainer)
            }
            addView(suggestionsScrollView)
        }

        floatingRootView?.addView(bubbleContainer)
        floatingRootView?.addView(expandedPanel)

        // Setup touch listener for dragging & clicking
        bubbleContainer?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        isDragging = true
                        params.x = initialX + dx.toInt()
                        params.y = initialY + dy.toInt()
                        wm.updateViewLayout(floatingRootView, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        // Click event: toggle expanded panel or toggle voice
                        toggleExpanded(!isExpanded)
                        AssistantCoordinator.onFloatingMicClicked?.invoke()
                    }
                    true
                }
                else -> false
            }
        }

        wm.addView(floatingRootView, params)
    }

    private fun toggleExpanded(expand: Boolean) {
        isExpanded = expand
        expandedPanel?.visibility = if (expand) View.VISIBLE else View.GONE
    }

    private fun createActionChip(label: String, dpToPx: (Int) -> Int, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = label
            setTextColor(Color.WHITE)
            textSize = 10.5f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(12).toFloat()
                setColor(Color.parseColor("#334155"))
                setStroke(dpToPx(1), Color.parseColor("#475569"))
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                rightMargin = dpToPx(6)
            }
            layoutParams = lp
            setOnClickListener {
                onClick()
            }
        }
    }

    private fun createSuggestionChip(label: String, dpToPx: (Int) -> Int, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = "👉 $label"
            setTextColor(Color.parseColor("#FDE047"))
            textSize = 10.5f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(12).toFloat()
                setColor(Color.parseColor("#1E3A8A"))
                setStroke(dpToPx(1), Color.parseColor("#3B82F6"))
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                rightMargin = dpToPx(6)
            }
            layoutParams = lp
            setOnClickListener {
                onClick()
            }
        }
    }

    private fun createBubbleDrawable(isListening: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            if (isListening) {
                setColor(Color.parseColor("#EF4444")) // Red pulsing listening
                setStroke(4, Color.parseColor("#FCA5A5"))
            } else {
                setColor(Color.parseColor("#2563EB")) // Vibrant Blue Standby
                setStroke(3, Color.parseColor("#93C5FD"))
            }
        }
    }

    private fun observeCoordinatorState() {
        val dpToPx = { dp: Int ->
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp.toFloat(),
                resources.displayMetrics
            ).toInt()
        }

        serviceScope.launch {
            AssistantCoordinator.isListening.collect { listening ->
                withContext(Dispatchers.Main) {
                    bubbleContainer?.background = createBubbleDrawable(listening)
                    if (listening) {
                        toggleExpanded(true)
                    }
                }
            }
        }

        serviceScope.launch {
            AssistantCoordinator.floatingStatusText.collect { text ->
                withContext(Dispatchers.Main) {
                    statusTextView?.text = text
                }
            }
        }

        serviceScope.launch {
            AssistantCoordinator.floatingFollowUp.collect { question ->
                withContext(Dispatchers.Main) {
                    followUpTextView?.text = question
                }
            }
        }

        serviceScope.launch {
            AssistantCoordinator.floatingSuggestions.collect { list ->
                withContext(Dispatchers.Main) {
                    suggestionsContainer?.removeAllViews()
                    for (item in list) {
                        val chip = createSuggestionChip(item, dpToPx) {
                            AssistantCoordinator.onFloatingActionSubmitted?.invoke(item)
                        }
                        suggestionsContainer?.addView(chip)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingRootView?.let {
            try {
                windowManager?.removeView(it)
            } catch (_: Exception) {}
        }
        AssistantCoordinator.setFloatingServiceRunning(false)
    }

    companion object {
        fun isOverlayPermissionGranted(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
        }
    }
}
