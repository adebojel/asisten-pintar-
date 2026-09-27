package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.DisplayMetrics
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale

class AssistantAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Track current foreground package/activity for contextual assistance
        event?.packageName?.let {
            currentPackageName = it.toString()
        }
    }

    override fun onInterrupt() {
        // Interruption callback
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    /**
     * Scroll down (swipe up gesture or ACTION_SCROLL_FORWARD)
     */
    fun scrollDown(): Boolean {
        // 1. Try finding scrollable nodes first
        val root = rootInActiveWindow
        if (root != null) {
            val scrollable = findScrollableNode(root)
            if (scrollable != null && scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)) {
                return true
            }
        }

        // 2. Fallback to gesture scroll (Swipe from 75% height to 25% height)
        return performSwipeGesture(swipeUp = true)
    }

    /**
     * Scroll up (swipe down gesture or ACTION_SCROLL_BACKWARD)
     */
    fun scrollUp(): Boolean {
        val root = rootInActiveWindow
        if (root != null) {
            val scrollable = findScrollableNode(root)
            if (scrollable != null && scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)) {
                return true
            }
        }

        return performSwipeGesture(swipeUp = false)
    }

    private fun findScrollableNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isScrollable) return node

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            val found = findScrollableNode(child)
            if (found != null) return found
        }
        return null
    }

    private fun performSwipeGesture(swipeUp: Boolean): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false

        val wm = getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return false
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)

        val midX = metrics.widthPixels / 2f
        val startY = if (swipeUp) metrics.heightPixels * 0.72f else metrics.heightPixels * 0.28f
        val endY = if (swipeUp) metrics.heightPixels * 0.28f else metrics.heightPixels * 0.72f

        val path = Path().apply {
            moveTo(midX, startY)
            lineTo(midX, endY)
        }

        val stroke = GestureDescription.StrokeDescription(path, 0, 350)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        return dispatchGesture(gesture, null, null)
    }

    /**
     * Type text into currently active/focused or first editable text field
     */
    fun typeText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val editableNode = findEditableOrFocusedNode(root) ?: return false

        val arguments = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return editableNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }

    private fun findEditableOrFocusedNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isFocused && (node.isEditable || node.className?.contains("EditText", ignoreCase = true) == true)) {
            return node
        }
        if (node.isEditable || node.className?.contains("EditText", ignoreCase = true) == true) {
            return node
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            val found = findEditableOrFocusedNode(child)
            if (found != null) return found
        }
        return null
    }

    /**
     * Click send or search button on screen (WhatsApp send button, YouTube search button, etc.)
     */
    fun clickSendOrSubmit(): Boolean {
        val root = rootInActiveWindow ?: return false

        // Common labels/descriptions for send or submit buttons
        val candidates = listOf(
            "kirim", "send", "cari", "search", "enter", "telusuri", "buka", "putar", "play"
        )

        for (label in candidates) {
            val nodes = root.findAccessibilityNodeInfosByText(label)
            for (node in nodes) {
                if (performClickOnNodeOrParent(node)) {
                    return true
                }
            }
        }

        // Try finding by view ID keywords
        val idKeywords = listOf("send", "submit", "search", "btn_send", "entry_send")
        for (keyword in idKeywords) {
            val found = findNodeByViewIdKeyword(root, keyword)
            if (found != null && performClickOnNodeOrParent(found)) {
                return true
            }
        }

        return false
    }

    /**
     * Click on an element that matches a specific text
     */
    fun clickElementByText(targetText: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByText(targetText)
        for (node in nodes) {
            if (performClickOnNodeOrParent(node)) {
                return true
            }
        }
        return false
    }

    private fun findNodeByViewIdKeyword(node: AccessibilityNodeInfo?, keyword: String): AccessibilityNodeInfo? {
        if (node == null) return null
        val viewId = node.viewIdResourceName?.lowercase(Locale.ROOT)
        if (viewId != null && viewId.contains(keyword)) {
            return node
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            val found = findNodeByViewIdKeyword(child, keyword)
            if (found != null) return found
        }
        return null
    }

    private fun performClickOnNodeOrParent(node: AccessibilityNodeInfo?): Boolean {
        var current = node
        while (current != null) {
            if (current.isClickable) {
                return current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            current = current.parent
        }
        return false
    }

    fun performBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)
    fun performHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)
    fun performRecents(): Boolean = performGlobalAction(GLOBAL_ACTION_RECENTS)
    fun performNotifications(): Boolean = performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    fun performQuickSettings(): Boolean = performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)

    fun performLock(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        } else {
            false
        }
    }

    fun performScreenshot(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
        } else {
            false
        }
    }

    companion object {
        var instance: AssistantAccessibilityService? = null
            private set

        var currentPackageName: String = ""
            private set

        fun isAccessibilityServiceEnabled(context: Context): Boolean {
            val expectedServiceName = "${context.packageName}/${AssistantAccessibilityService::class.java.canonicalName}"
            val enabledServicesSetting = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServicesSetting)

            while (colonSplitter.hasNext()) {
                val componentNameString = colonSplitter.next()
                if (componentNameString.equals(expectedServiceName, ignoreCase = true)) {
                    return true
                }
            }
            return false
        }
    }
}
