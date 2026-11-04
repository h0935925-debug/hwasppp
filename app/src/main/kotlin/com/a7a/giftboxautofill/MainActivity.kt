package com.a7a.giftboxautofill

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (20 * resources.displayMetrics.density).toInt()
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            gravity = Gravity.CENTER_HORIZONTAL
        }
        layout.addView(TextView(this).apply {
            text = "GiftBox AutoFill"
            textSize = 26f
            setPadding(0, 0, 0, pad)
        })
        layout.addView(TextView(this).apply {
            text = "احفظ اختياراتك بنفسك في أول مرة، ثم استخدمها لاحقًا عند تطابق السؤال.\n\n" +
                "ملاحظة: دعم قراءة عناصر صفحات الويب يختلف حسب المتصفح والموقع. لن يجيب التطبيق عن أسئلة جديدة أو يتجاوز التحقق."
            textSize = 16f
            setPadding(0, 0, 0, pad)
        })
        layout.addView(button("1) تفعيل خدمة إمكانية الوصول") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        layout.addView(button("2) السماح بالزر العائم") {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")))
            } else {
                OverlayController.show(this)
            }
        })
        layout.addView(button("إظهار / إخفاء الزر العائم") {
            if (Settings.canDrawOverlays(this)) OverlayController.toggle(this)
            else startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")))
        })
        layout.addView(button("فتح إعدادات التطبيق") {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:$packageName")))
        })
        setContentView(layout)
    }

    private fun button(label: String, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            setOnClickListener { action() }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
}
