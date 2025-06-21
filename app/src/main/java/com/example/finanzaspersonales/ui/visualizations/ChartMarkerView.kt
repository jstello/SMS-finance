package com.example.finanzaspersonales.ui.visualizations

import android.annotation.SuppressLint
import android.content.Context
import android.widget.TextView
import com.example.finanzaspersonales.R
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import java.text.DecimalFormat

@SuppressLint("ViewConstructor")
class ChartMarkerView(
    context: Context,
    private val currentMonthData: List<Float>,
    private val previousMonthData: List<Float>
) : MarkerView(context, R.layout.chart_marker_view) {

    private val tvContent: TextView = findViewById(R.id.tvContent)
    private val format = DecimalFormat("#,##0.00")

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        e?.let {
            val index = it.x.toInt()
            if (index >= 0) {
                val currentValue = currentMonthData.getOrNull(index)
                val previousValue = previousMonthData.getOrNull(index)

                val text = if (currentValue != null && previousValue != null) {
                    val difference = currentValue - previousValue
                    val sign = if (difference >= 0) "+" else "-"
                    "Day ${index + 1}\nDiff: $sign${format.format(Math.abs(difference))}"
                } else {
                    "Day ${index + 1}"
                }
                tvContent.text = text
            }
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2f), -height.toFloat())
    }
}
