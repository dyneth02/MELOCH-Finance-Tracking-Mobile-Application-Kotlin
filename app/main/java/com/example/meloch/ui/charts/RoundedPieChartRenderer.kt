package com.example.meloch.ui.chart

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.interfaces.datasets.IPieDataSet
import com.github.mikephil.charting.renderer.PieChartRenderer
import com.github.mikephil.charting.utils.MPPointF
import com.github.mikephil.charting.utils.Utils
import com.github.mikephil.charting.utils.ViewPortHandler

/**
 * Custom PieChartRenderer that ensures the pie chart has the correct styling
 */
class RoundedPieChartRenderer(
    private val chart: PieChart,
    animator: ChartAnimator,
    viewPortHandler: ViewPortHandler,
    private val cornerRadius: Float = 15f
) : PieChartRenderer(chart, animator, viewPortHandler) {

    private val mHolePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mCenterTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mTransparentCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mWhiteColor = Color.WHITE

    init {
        // Apply styling to the chart in the constructor
        chart.apply {
            // Configure appearance
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT) // Make the hole transparent
            setTransparentCircleColor(Color.TRANSPARENT) // Make the transparent circle transparent
            setTransparentCircleAlpha(0) // Fully transparent
            holeRadius = 58f
            transparentCircleRadius = 61f

            // Configure center text
            setDrawCenterText(true)
            setCenterTextColor(Color.WHITE)
            setCenterTextSize(14f)

            // Disable legend (we're using our own)
            legend.isEnabled = false

            // Configure labels
            setEntryLabelColor(chart.context.getColor(com.example.meloch.R.color.text_primary))
            setEntryLabelTextSize(12f)
            setUsePercentValues(false)

            // Disable rotation
            isRotationEnabled = false

            // Enable rounded slices for rounded corners
            setDrawRoundedSlices(true)
        }

        // Initialize paints
        mHolePaint.color = Color.TRANSPARENT
        mHolePaint.style = Paint.Style.FILL

        mCenterTextPaint.color = mWhiteColor
        mCenterTextPaint.textSize = 14f * chart.context.resources.displayMetrics.density // 14sp
        mCenterTextPaint.textAlign = Paint.Align.CENTER

        mTransparentCirclePaint.color = Color.TRANSPARENT
        mTransparentCirclePaint.style = Paint.Style.FILL
    }

    /**
     * Override the drawHole method to ensure the hole is transparent
     */
    override fun drawHole(c: Canvas) {
        if (mChart.isDrawHoleEnabled) {
            val radius = mChart.radius
            val holeRadius = radius * (mChart.holeRadius / 100f)
            val center = mChart.centerCircleBox

            // Draw the hole with transparent color
            mHolePaint.color = Color.TRANSPARENT
            c.drawCircle(center.x, center.y, holeRadius, mHolePaint)

            // Draw center text if enabled
            if (mChart.isDrawCenterTextEnabled && mChart.centerText != null) {
                // Draw the center text
                val lines = mChart.centerText.toString().split("\n")
                val textSize = 14f * mChart.context.resources.displayMetrics.density

                mCenterTextPaint.color = mWhiteColor
                mCenterTextPaint.textSize = textSize

                if (lines.size == 1) {
                    // Single line text
                    c.drawText(mChart.centerText.toString(), center.x, center.y + textSize / 4, mCenterTextPaint)
                } else if (lines.size == 2) {
                    // Two line text
                    val lineHeight = textSize * 1.2f
                    c.drawText(lines[0], center.x, center.y - lineHeight / 2, mCenterTextPaint)
                    c.drawText(lines[1], center.x, center.y + lineHeight / 2, mCenterTextPaint)
                }
            }

            MPPointF.recycleInstance(center)
        }
    }
}
