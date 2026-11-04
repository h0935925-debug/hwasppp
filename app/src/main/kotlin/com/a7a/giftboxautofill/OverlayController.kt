package com.a7a.giftboxautofill

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout

object OverlayController {
    private var view: LinearLayout? = null
    private var manager: WindowManager? = null

    fun show(context: Context) {
        if (view != null) return
        manager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(6, 6, 6, 6)
            setBackgroundColor(0xEE222222.toInt())
        }
        box.addView(actionButton(context, "تعلم") { SurveyAccessibilityService.setMode(context, Mode.LEARN) })
        box.addView(actionButton(context, "تشغيل") { SurveyAccessibilityService.setMode(context, Mode.RUN) })
        box.addView(actionButton(context, "إيقاف") { SurveyAccessibilityService.setMode(context, Mode.PAUSE) })
        val type = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                   else WindowManager.LayoutParams.TYPE_PHONE
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.END; x = 12; y = 180 }
        manager?.addView(box, params)
        view = box
    }

    fun hide() {
        try { view?.let { manager?.removeView(it) } } catch (_: Exception) {}
        view = null
    }

    fun toggle(context: Context) { if (view == null) show(context) else hide() }

    private fun actionButton(context: Context, title: String, action: () -> Unit) =
        Button(context).apply {
            text = title
            isAllCaps = false
            textSize = 12f
            setOnClickListener { action() }
        }
}
