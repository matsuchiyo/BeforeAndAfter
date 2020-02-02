package org.macho.beforeandafter.graph

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.alpha
import org.macho.beforeandafter.shared.data.Record
import org.macho.beforeandafter.shared.util.LogUtil
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

class TwoScaleGraphView: View {
    companion object {
        const val AXIS_WIDTH = 2f
        const val LINE_WIDTH = 1f
        const val LABEL_BORDER_WIDTH = 4f
    }

    var oX = 0f
    var oY = 0f
    var maxX = 0f
    var maxY = 0f

    var unitHeight = 0f
    var maxValues = FloatArray(2)
    var minValues = FloatArray(2)

//    var dateFrom = Date()
//    var dateTo = Date()
//    var unitTime = 0L
//    var range: GraphRange = GraphRange.ONE_YEAR
//    var range: GraphRange = GraphRange.THREE_MONTHS
    var range: GraphRange = GraphRange.THREE_WEEKS
    var from: Date? = null

    constructor(context: Context): super(context) {
    }

    constructor(context: Context, attrs: AttributeSet?): super(context, attrs) {
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): super(context, attrs, defStyleAttr) {
    }

    var dataSetList: List<DataSet> = mutableListOf()

    private val axisPaint = Paint().also {
        it.color = Color.BLACK
        it.strokeWidth = AXIS_WIDTH
//        it.isAntiAlias = false
        it.style = Paint.Style.STROKE
    }

    private val linePaint = Paint().also {
        it.color = Color.GRAY
        it.strokeWidth = LINE_WIDTH
        it.style = Paint.Style.STROKE
    }

    private val yAxisCategory1LabelPaint = Paint().also {
//        it.color = Color.GRAY
        it.color = Color.argb(128, 64, 64, 64)
        it.style = Paint.Style.FILL
        it.textSize = 40f
        it.isAntiAlias = true
    }

    private val yAxisCategory1LabelBorderPaint = Paint().also {
        it.color = Color.argb(128, 255, 255, 255)
        it.strokeWidth = LABEL_BORDER_WIDTH
        it.style = Paint.Style.STROKE
        it.textSize = 40f
        it.isAntiAlias = true
    }

    private val xAxisLabelPaint = Paint().also {
        it.color = Color.GRAY
        it.style = Paint.Style.FILL
    }

    private val graphPaint = Paint().also {
        it.style = Paint.Style.STROKE
        it.isAntiAlias = true
    }

    private val graphDotPaint = Paint().also {
        it.style = Paint.Style.FILL
        it.isAntiAlias = true
    }

    override fun draw(canvas: Canvas?) {
        super.draw(canvas)
        LogUtil.d(this, "draw")

        if (canvas == null) return

        LogUtil.d(this, "width : ${this.width}")
        LogUtil.d(this, "height: ${this.height}")

        oX = 0f
        oY = height * 0.95f
        maxX = width.toFloat()
        maxY = height * 0.05f

        drawAxis(canvas)
        drawHorizontalLines(canvas)
        drawVerticalLines(canvas)
        drawLineGraph(canvas)

        drawYAxisLabel(canvas)
    }

    private fun drawAxis(canvas: Canvas) {
        val yAxis1X = oX + AXIS_WIDTH / 2
        canvas.drawLine(yAxis1X, oY, yAxis1X, maxY, axisPaint) // yAxis1
        val yAxis2X = maxX - AXIS_WIDTH / 2
        canvas.drawLine(yAxis2X, oY, yAxis2X, maxY, axisPaint) // yAxis1
        canvas.drawLine(oX, oY, maxX, oY, axisPaint) // xAxis
    }

    private fun drawHorizontalLines(canvas: Canvas) {
        val ranges = FloatArray(2)
        val minFloors = FloatArray(2)
        val maxCeils = FloatArray(2)

        val margin = 1f



        val sortedByCategory1 = dataSetList.filter { it.type == DataType.LEFT }.flatMap { it.dataList }.sortedBy { it.value }
        minFloors[0] = floor(sortedByCategory1.firstOrNull()?.value ?: 0f) - margin
        maxCeils[0] = ceil(sortedByCategory1.lastOrNull()?.value ?: 0f) + margin
        ranges[0] = maxCeils[0] - minFloors[0]

        val sortedByCategory2 = dataSetList.filter { it.type == DataType.RIGHT }.flatMap { it.dataList }.sortedBy { it.value }
        minFloors[1] = floor(sortedByCategory2.firstOrNull()?.value ?: 0f) - margin
        maxCeils[1] = ceil(sortedByCategory2.lastOrNull()?.value ?: 0f) + margin
        ranges[1] = maxCeils[1] - minFloors[1]

        val isCategory1Base = ranges[0] > ranges[1]
        val index = if (isCategory1Base) 0 else 1
        unitHeight = (oY - maxY) / ranges[index]
        val from = minFloors[index].toInt()
        val to = maxCeils[index].toInt()
        for (i in (from + 1)..to) {
            val lineY = oY - unitHeight * (i - from)
            canvas.drawLine(oX, lineY, maxX, lineY, linePaint)
        }

        minValues[index] = minFloors[index]
        maxValues[index] = maxCeils[index]

        val index2 = if (isCategory1Base) 1 else 0
        minValues[index2] = minFloors[index2] - ((ranges[index] - ranges[index2]) / 2).toInt()
        maxValues[index2] = minValues[index2] + ranges[index]
    }

    private fun drawYAxisLabel(canvas: Canvas) {
        drawYAxisLabelCategory1(canvas)
        drawYAxisLabelCategory2(canvas)
    }

    private fun drawYAxisLabelCategory1(canvas: Canvas) {
        val from = minValues[0].toInt()
        val to = maxValues[0].toInt()
        for (i in (from + 1)..to) {
            val y = oY - unitHeight * (i - from) + (yAxisCategory1LabelPaint.textSize / 2)
            canvas.drawText(i.toString(), 10f /* offset */, y, yAxisCategory1LabelBorderPaint)
            canvas.drawText(i.toString(), 10f /* offset */, y, yAxisCategory1LabelPaint)
        }
    }

    private fun drawYAxisLabelCategory2(canvas: Canvas) {
        val from = minValues[1].toInt()
        val to = maxValues[1].toInt()
        for (i in (from + 1)..to) {
            val x = maxX - yAxisCategory1LabelPaint.textSize - 20 // offset
            val y = oY - unitHeight * (i - from) + (yAxisCategory1LabelPaint.textSize / 2)
            canvas.drawText(i.toString(), x, y, yAxisCategory1LabelBorderPaint)
            canvas.drawText(i.toString(), x, y, yAxisCategory1LabelPaint)
        }
    }

    private fun drawVerticalLines(canvas: Canvas) {

        val to = Date() // TODO: refactor 外から渡せるようにしたい。
        from = Date(to.time - range.time)
        val firstDate = Calendar.getInstance().also {
            it.time = from
            it.set(Calendar.MILLISECOND, 0)
            it.set(Calendar.SECOND, 0)
            it.set(Calendar.MINUTE, 0)
            it.set(Calendar.HOUR_OF_DAY, 0)
            if (range == GraphRange.THREE_WEEKS) {
                it.add(Calendar.DAY_OF_MONTH, 1)
            } else if (range == GraphRange.THREE_MONTHS) {
                it.set(Calendar.DAY_OF_WEEK, 1) // 1が日曜日
                it.add(Calendar.WEEK_OF_YEAR, 1)
            } else if (range == GraphRange.ONE_YEAR) {
                LogUtil.d(this, it.time.toString())
                it.set(Calendar.DAY_OF_MONTH, 1) // 1が1日
                LogUtil.d(this, it.time.toString())
                it.add(Calendar.MONTH, 1)
                LogUtil.d(this, it.time.toString())
            }
        }.time
        LogUtil.d(this, firstDate.toString())

        val format = SimpleDateFormat(range.labelFormat)

        val widthPerStep = (maxX - oX) / (range.time / range.step)
        val calculatedTextSize = widthPerStep / range.maxCharCount * 1.5f // 文字の幅:高さ=1:1.5という想定。
        val maxTextSize = (height - oY) - 10
        xAxisLabelPaint.textSize = min(calculatedTextSize, maxTextSize)
        val y = (height - oY) / 2 + xAxisLabelPaint.textSize / 2 + oY
        for (i in firstDate.time..to.time step range.step) {
            val x = oX + (maxX - oX) * (i - from!!.time) / (to.time - from!!.time)
            canvas.drawLine(x, oY, x, maxY, linePaint)

            val isLastColumn = (i + range.step) > to.time
            if (isLastColumn) return

            val date = Date(if (range == GraphRange.ONE_YEAR) i + range.step / 2 else i)
            val text = format.format(date)
            val marginLeft = (widthPerStep - (xAxisLabelPaint.textSize / 1.5f) * text.length) / 2
            val textX = if (range.alignCenter) x + marginLeft else x
            canvas.drawText(text, textX, y, xAxisLabelPaint)
        }
    }

    private fun drawLineGraph(canvas: Canvas) {
        for ((type, dataList, color) in dataSetList) {
            graphPaint.color = color
            graphPaint.strokeWidth = range.graphWidth
            for ((index, data) in dataList.withIndex()) {
                // dot
                val x1 = (maxX - oX) * ((data.time - from!!.time).toFloat() / (range.time))
                val y1 = oY - (oY - maxY) * ((data.value - minValues[type.index]) / (maxValues[type.index] - minValues[type.index]))
                if (range.drawDot) {
                    graphDotPaint.color = color
                    canvas.drawCircle(x1, y1,  range.graphWidth * 2, graphDotPaint)
                }

                // line
                if (dataList.size < index + 2) continue
                val nextData = dataList[index + 1]
                val x2 = (maxX - oX) * ((nextData.time - from!!.time).toFloat() / (range.time))
                val y2 = oY - (oY - maxY) * ((nextData.value - minValues[type.index]) / (maxValues[type.index] - minValues[type.index]))
                canvas.drawLine(x1, y1, x2, y2, graphPaint)
                LogUtil.d(this, "${x1}, ${y1}, ${x2}, ${y2}" )

            }
        }
    }

}

enum class GraphRange(val time: Long, val step: Long, val labelFormat: String, val maxCharCount: Int, val alignCenter: Boolean, val graphWidth: Float, val drawDot: Boolean) {
    THREE_WEEKS(1000L * 60 * 60 * 24 * 7 * 3, 1000L * 60 * 60 * 24, "d", 2, true, 2.5f, true),
    THREE_MONTHS(1000L * 60 * 60 * 24 * 90, 1000L * 60 * 60 * 24 * 7, "M/d", 5, false, 2.5f, true),
    ONE_YEAR(1000L * 60 * 60 * 24 * 365, 1000L * 60 * 60 * 24 * 30, "M", 2, true, 2.5f, true),
}

enum class DataType(val index: Int) {
    LEFT(0), RIGHT(1)
}