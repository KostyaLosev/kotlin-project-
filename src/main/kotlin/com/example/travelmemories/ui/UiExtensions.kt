package com.example.travelmemories.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.TextView

fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

fun roundedBackground(color: Int, radiusDp: Int, context: Context): GradientDrawable {
    return GradientDrawable().apply {
        setColor(color)
        cornerRadius = context.dp(radiusDp).toFloat()
    }
}

fun circleBackground(color: Int): GradientDrawable {
    return GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
    }
}

fun TextView.heading(textValue: String) {
    text = textValue
    textSize = 20f
    setTextColor(Color.rgb(33, 33, 33))
    typeface = Typeface.DEFAULT_BOLD
}

fun View.setMargins(left: Int = 0, top: Int = 0, right: Int = 0, bottom: Int = 0) {
    val params = layoutParams as? android.view.ViewGroup.MarginLayoutParams ?: return
    params.setMargins(left, top, right, bottom)
    layoutParams = params
}
