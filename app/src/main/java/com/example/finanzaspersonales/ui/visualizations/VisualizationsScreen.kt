package com.example.finanzaspersonales.ui.visualizations

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.DecimalFormat

@Composable
fun VisualizationsScreen(
    viewModel: VisualizationViewModel = hiltViewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val currentMonthData by viewModel.cumulativeSpendingCurrentMonth.collectAsState()
    val previousMonthData by viewModel.cumulativeSpendingPreviousMonth.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (error != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = error!!, color = MaterialTheme.colorScheme.error)
            }
        } else {
            CumulativeSpendingChart(
                currentMonthData = currentMonthData,
                previousMonthData = previousMonthData
            )
        }
    }
}

class MillionsValueFormatter : ValueFormatter() {
    private val format = DecimalFormat("#,##0.0M")

    override fun getFormattedValue(value: Float): String {
        return format.format(value / 1_000_000f)
    }
}

@Composable
fun CumulativeSpendingChart(
    currentMonthData: List<Float>,
    previousMonthData: List<Float>
) {
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val secondaryColor = MaterialTheme.colorScheme.secondary.toArgb()
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f).toArgb()

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                axisRight.isEnabled = false
                axisLeft.valueFormatter = MillionsValueFormatter()
                setTouchEnabled(true)
                setPinchZoom(true)
                setDrawMarkers(true)
                isHighlightPerTapEnabled = true
            }
        },
        update = { chart ->
            chart.marker = ChartMarkerView(chart.context, currentMonthData, previousMonthData)

            // Axis and Legend styling
            chart.xAxis.textColor = textColor
            chart.axisLeft.textColor = textColor
            chart.legend.textColor = textColor

            chart.xAxis.gridColor = gridColor
            chart.axisLeft.gridColor = gridColor
            chart.axisLeft.axisLineColor = gridColor
            chart.xAxis.axisLineColor = gridColor

            // Prepare current & previous month entries
            val currentMonthEntries = currentMonthData.mapIndexed { index, value -> Entry(index.toFloat(), value) }
            val previousMonthEntries = previousMonthData.mapIndexed { index, value -> Entry(index.toFloat(), value) }

            val currentMonthDataSet = LineDataSet(currentMonthEntries, "Current Month").apply {
                color = primaryColor
                valueTextColor = textColor
                lineWidth = 2f
                setDrawValues(false)
                setDrawCircles(false)
                highLightColor = primaryColor
            }

            val previousMonthDataSet = LineDataSet(previousMonthEntries, "Previous Month").apply {
                color = secondaryColor
                valueTextColor = textColor
                lineWidth = 2f
                setDrawValues(false)
                setDrawCircles(false)
                highLightColor = secondaryColor
            }

            chart.data = LineData(currentMonthDataSet, previousMonthDataSet)
            chart.invalidate()
        }
    )
} 