package com.a7a.giftboxautofill

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

enum class Mode { PAUSE, LEARN, RUN }

class SurveyAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        when (currentMode) {
            Mode.LEARN -> if (
                event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED ||
                event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
            ) {
                recordInteraction(event.source)
            }

            Mode.RUN -> if (
                event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
                event.eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED
            ) {
                performKnownAnswers()
            }

            Mode.PAUSE -> Unit
        }
    }

    private fun recordInteraction(node: AccessibilityNodeInfo?) {
        if (node == null || node.text.isNullOrBlank()) return

        val answer = node.text.toString().trim()

        if (answer.length > 160 || answer.length < 1) return
        if (looksSensitive(node)) return

        val root = rootInActiveWindow ?: return
        val question = findQuestionContext(root, node) ?: return

        if (question.length < 3 || question.equals(answer, true)) return

        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val data = loadData(prefs.getString(DATA, "[]") ?: "[]")

        val key = normalize(question)

        val existing = (0 until data.length())
            .map { data.getJSONObject(it) }
            .filterNot { it.optString("q") == key }
            .toMutableList()

        existing.add(
            JSONObject()
                .put("q", key)
                .put("question", question)
                .put("answer", answer)
        )

        val out = JSONArray()

        existing.takeLast(500).forEach {
            out.put(it)
        }

        prefs.edit()
            .putString(DATA, out.toString())
            .apply()
    }

    private fun performKnownAnswers() {
        val root = rootInActiveWindow ?: return

        val entries = loadData(
            getSharedPreferences(PREFS, MODE_PRIVATE)
                .getString(DATA, "[]") ?: "[]"
        )

        if (entries.length() == 0) return

        val nodes = ArrayList<AccessibilityNodeInfo>()

        collectNodes(root, nodes, 0)

        for (entryIndex in 0 until entries.length()) {
            val entry = entries.optJSONObject(entryIndex) ?: continue

            val q = entry.optString("q")
            val answer = entry.optString("answer")

            if (q.isBlank() || answer.isBlank()) continue

            val hasQuestion = nodes.any { n ->
                val t = n.text?.toString()?.trim().orEmpty()

                normalize(t) == q ||
                    (normalize(t).length > 8 && normalize(t).contains(q))
            }

            if (!hasQuestion) continue

            val option = nodes.firstOrNull { n ->
                n.isClickable &&
                    n.text?.toString()?.trim().equals(answer, true) &&
                    !looksSensitive(n)
            }

            if (option != null) {
                option.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return
            }
        }
    }

    private fun findQuestionContext(
        root: AccessibilityNodeInfo,
        selected: AccessibilityNodeInfo
    ): String? {

        val all = ArrayList<AccessibilityNodeInfo>()

        collectNodes(root, all, 0)

        val selectedText = selected.text?.toString()?.trim().orEmpty()

        val index = all.indexOfFirst {
            it.text?.toString()?.trim() == selectedText &&
                it.className == selected.className
        }

        if (index < 0) return null

        for (i in index - 1 downTo maxOf(0, index - 8)) {

            val candidate = all[i].text?.toString()?.trim().orEmpty()

            if (
                candidate.length in 4..220 &&
                !candidate.equals(selectedText, true) &&
                !looksLikeAnswer(candidate)
            ) {
                return candidate
            }
        }

        return null
    }

    private fun looksLikeAnswer(value: String): Boolean {
        val s = value.lowercase(Locale.ROOT)

        return s in listOf(
            "التالي",
            "السابق",
            "إرسال",
            "متابعة",
            "next",
            "back",
            "submit",
            "continue"
        )
    }

    private fun looksSensitive(node: AccessibilityNodeInfo): Boolean {

        val id = node.viewIdResourceName.orEmpty()
            .lowercase(Locale.ROOT)

        val hint = node.hintText?.toString().orEmpty()
            .lowercase(Locale.ROOT)

        val text = node.text?.toString().orEmpty()
            .lowercase(Locale.ROOT)

        val cls = node.className?.toString().orEmpty()

        return id.contains("phone") ||
            id.contains("otp") ||
            id.contains("password") ||
            hint.contains("phone") ||
            hint.contains("mobile") ||
            hint.contains("رقم الهاتف") ||
            hint.contains("كلمة المرور") ||
            text.contains("رمز التحقق") ||
            cls.contains("EditText", true)
    }

    private fun collectNodes(
        node: AccessibilityNodeInfo,
        out: MutableList<AccessibilityNodeInfo>,
        depth: Int
    ) {

        if (depth > 18 || out.size > 1200) return

        out.add(node)

        for (i in 0 until node.childCount) {

            val child = node.getChild(i) ?: continue

            collectNodes(child, out, depth + 1)
        }
    }

    override fun onInterrupt() {
        currentMode = Mode.PAUSE
    }

    override fun onDestroy() {
        if (instance === this) {
            instance = null
        }

        super.onDestroy()
    }

    companion object {

        private const val PREFS = "giftbox_autofill"
        private const val DATA = "learned_answers"

        private var instance: SurveyAccessibilityService? = null

        @Volatile
        var currentMode: Mode = Mode.PAUSE
            private set

        fun setMode(context: Context, mode: Mode) {

            currentMode = mode

            context.getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putString("mode", mode.name)
                .apply()
        }

        private fun loadData(raw: String): JSONArray = try {
            JSONArray(raw)
        } catch (_: Exception) {
            JSONArray()
        }

        private fun normalize(value: String): String =
            value.lowercase(Locale.ROOT)
                .replace(Regex("[^\\p{L}\\p{N} ]"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
    }
}
