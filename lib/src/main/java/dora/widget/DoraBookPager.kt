package dora.widget

import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Scroller
import kotlin.math.abs
import kotlin.math.hypot

class DoraBookPager @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var pathAText: String = ""
    var pathBText: String = ""
    var pathCText: String = ""

    private val pathAPaint = Paint().apply {
        color = Color.GREEN
        isAntiAlias = true
    }
    private val pathBPaint = Paint().apply {
        color = Color.BLUE
        isAntiAlias = true
    }
    private val pathCPaint = Paint().apply {
        color = Color.YELLOW
        isAntiAlias = true
    }
    private val textPaint = Paint().apply {
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
        isSubpixelText = true
        textSize = 30f
    }

    private var a = PagerPoint()
    private var f = PagerPoint()
    private var g = PagerPoint()
    private var e = PagerPoint()
    private var h = PagerPoint()
    private var c = PagerPoint()
    private var j = PagerPoint()
    private var b = PagerPoint()
    private var k = PagerPoint()
    private var d = PagerPoint()
    private var i = PagerPoint()

    private val pathA = Path()
    private val pathB = Path()
    private val pathC = Path()

    private var defaultWidth = 600
    private var defaultHeight = 1000
    private var viewWidth = 0
    private var viewHeight = 0

    private var lPathAShadowDis = 0f
    private var rPathAShadowDis = 0f
    private val mScroller = Scroller(context, LinearInterpolator())

    private var style: String = STYLE_LOWER_RIGHT

    private lateinit var drawableLeftTopRight: GradientDrawable
    private lateinit var drawableLeftLowerRight: GradientDrawable
    private lateinit var drawableRightTopRight: GradientDrawable
    private lateinit var drawableRightLowerRight: GradientDrawable
    private lateinit var drawableHorizontalLowerRight: GradientDrawable
    private lateinit var drawableBTopRight: GradientDrawable
    private lateinit var drawableBLowerRight: GradientDrawable
    private lateinit var drawableCTopRight: GradientDrawable
    private lateinit var drawableCLowerRight: GradientDrawable

    companion object {
        const val STYLE_LEFT = "STYLE_LEFT"
        const val STYLE_RIGHT = "STYLE_RIGHT"
        const val STYLE_MIDDLE = "STYLE_MIDDLE"
        const val STYLE_TOP_RIGHT = "STYLE_TOP_RIGHT"
        const val STYLE_LOWER_RIGHT = "STYLE_LOWER_RIGHT"
    }

    init {
        createGradientDrawable()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = measureSize(defaultWidth, widthMeasureSpec)
        val height = measureSize(defaultHeight, heightMeasureSpec)
        setMeasuredDimension(width, height)

        viewWidth = width
        viewHeight = height
        a.x = -1f
        a.y = -1f
    }

    private fun measureSize(defaultSize: Int, measureSpec: Int): Int {
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)
        return when (specMode) {
            MeasureSpec.EXACTLY -> specSize
            MeasureSpec.AT_MOST -> defaultSize.coerceAtMost(specSize)
            else -> defaultSize
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.YELLOW)
        if (a.x == -1f && a.y == -1f) {
            drawPathAContent(canvas, getPathDefault())
        } else {
            when {
                f.x == viewWidth.toFloat() && f.y == 0f -> {
                    val path = getPathAFromTopRight(a.x, a.y)
                    drawPathAContent(canvas, path)
                    drawPathCContent(canvas, path)
                    drawPathBContent(canvas, path)
                }

                f.x == viewWidth.toFloat() && f.y == viewHeight.toFloat() -> {
                    val path = getPathAFromLowerRight()
                    drawPathAContent(canvas, path)
                    drawPathCContent(canvas, path)
                    drawPathBContent(canvas, path)
                }
            }
        }
    }

    private fun getPathDefault(): Path {
        pathA.reset()
        pathA.lineTo(0f, viewHeight.toFloat())
        pathA.lineTo(viewWidth.toFloat(), viewHeight.toFloat())
        pathA.lineTo(viewWidth.toFloat(), 0f)
        pathA.close()
        return pathA
    }

    private fun getPathAFromTopRight(touchX: Float, touchY: Float): Path {
        val path = Path()
        path.reset()
        path.moveTo(0f, 0f)
        path.lineTo(viewWidth.toFloat(), 0f)
        path.lineTo(touchX, touchY)
        path.lineTo(0f, viewHeight.toFloat())
        path.close()
        return path
    }

    private fun getPathAFromLowerRight(): Path {
        pathA.reset()
        pathA.lineTo(0f, viewHeight.toFloat())
        pathA.lineTo(c.x, c.y)
        pathA.quadTo(e.x, e.y, b.x, b.y)
        pathA.lineTo(a.x, a.y)
        pathA.lineTo(k.x, k.y)
        pathA.quadTo(h.x, h.y, j.x, j.y)
        pathA.lineTo(viewWidth.toFloat(), 0f)
        pathA.close()
        return pathA
    }

    fun setTouchPoint(x: Float, y: Float, style: String) {
        a.x = x
        a.y = y
        this.style = style
        when (style) {
            STYLE_TOP_RIGHT -> {
                f.x = viewWidth.toFloat()
                f.y = 0f
                calcPointsXY(a, f)
                postInvalidate()
            }

            STYLE_LEFT, STYLE_RIGHT, STYLE_LOWER_RIGHT -> {
                a.y = viewHeight - 1f
                f.x = viewWidth.toFloat()
                f.y = viewHeight.toFloat()
                calcPointsXY(a, f)
                postInvalidate()
            }
        }
    }

    fun startCancelAnim() {
        val dx: Int
        val dy: Int
        if (style == STYLE_TOP_RIGHT) {
            dx = (viewWidth - 1 - a.x).toInt()
            dy = (1 - a.y).toInt()
        } else {
            dx = (viewWidth - 1 - a.x).toInt()
            dy = (viewHeight - 1 - a.y).toInt()
        }
        mScroller.startScroll(a.x.toInt(), a.y.toInt(), dx, dy, 400)
        invalidate()
    }

    override fun computeScroll() {
        if (mScroller.computeScrollOffset()) {
            val x = mScroller.currX.toFloat()
            val y = mScroller.currY.toFloat()
            setTouchPoint(x, y, style)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val x = event.x
                val y = event.y
                style = when {
                    x <= viewWidth / 3 -> STYLE_LEFT
                    x > viewWidth / 3 && y <= viewHeight / 3 -> STYLE_TOP_RIGHT
                    x > viewWidth * 2 / 3 && y > viewHeight / 3 && y <= viewHeight * 2 / 3 -> STYLE_RIGHT
                    x > viewWidth / 3 && y > viewHeight * 2 / 3 -> STYLE_LOWER_RIGHT
                    else -> STYLE_MIDDLE
                }
                setTouchPoint(x, y, style)
            }

            MotionEvent.ACTION_MOVE -> setTouchPoint(event.x, event.y, style)
            MotionEvent.ACTION_UP -> startCancelAnim()
        }
        return true
    }

    private fun drawPathAContent(canvas: Canvas, pathA: Path) {
        canvas.save()
        canvas.clipPath(pathA)
        canvas.drawPath(pathA, pathAPaint)
        if (pathAText.isNotEmpty()) {
            canvas.drawText(pathAText, viewWidth - 260f, viewHeight - 100f, textPaint)
        }
        canvas.restore()
    }

    private fun drawPathBContent(canvas: Canvas, pathA: Path) {
        canvas.save()
        val pathB = getPathB()
        canvas.clipPath(pathA)
        canvas.clipPath(pathB, Region.Op.REVERSE_DIFFERENCE)
        canvas.drawPath(pathB, pathBPaint)
        if (pathBText.isNotEmpty()) {
            canvas.drawText(pathBText, viewWidth - 260f, viewHeight - 100f, textPaint)
        }
        canvas.restore()
    }

    private fun drawPathCContent(canvas: Canvas, pathA: Path) {
        canvas.save()
        val pathC = getPathC()
        canvas.clipPath(pathA)
        canvas.clipPath(pathC, Region.Op.REVERSE_DIFFERENCE)
        canvas.drawPath(pathC, pathCPaint)
        if (pathCText.isNotEmpty()) {
            canvas.drawText(pathCText, viewWidth - 260f, viewHeight - 100f, textPaint)
        }
        canvas.restore()
    }

    private fun getPathB(): Path {
        pathB.reset()
        pathB.moveTo(c.x, c.y)
        pathB.lineTo(b.x, b.y)
        pathB.lineTo(a.x, a.y)
        pathB.lineTo(k.x, k.y)
        pathB.lineTo(j.x, j.y)
        pathB.lineTo(h.x, h.y)
        pathB.close()
        return pathB
    }

    private fun getPathC(): Path {
        pathC.reset()
        pathC.moveTo(c.x, c.y)
        pathC.lineTo(b.x, b.y)
        pathC.lineTo(a.x, a.y)
        pathC.lineTo(k.x, k.y)
        pathC.lineTo(j.x, j.y)
        pathC.lineTo(h.x, h.y)
        pathC.close()
        return pathC
    }

    private fun calcPointsXY(a: PagerPoint, f: PagerPoint) {
        g.x = (a.x + f.x) / 2
        g.y = (a.y + f.y) / 2

        e.x = g.x - (f.y - g.y) * (f.y - g.y) / (f.x - g.x)
        e.y = f.y

        h.x = f.x
        h.y = g.y - (f.x - g.x) * (f.x - g.x) / (f.y - g.y)

        c.x = e.x - (f.x - e.x) / 2
        c.y = f.y

        j.x = f.x
        j.y = h.y - (f.y - h.y) / 2

        b.set(getIntersectionPoint(a, e, c, j))
        k.set(getIntersectionPoint(a, h, c, j))

        d.x = (c.x + 2 * e.x + b.x) / 4
        d.y = (2 * e.y + c.y + b.y) / 4

        i.x = (j.x + 2 * h.x + k.x) / 4
        i.y = (2 * h.y + j.y + k.y) / 4

        val lA = a.y - e.y
        val lB = e.x - a.x
        val lC = a.x * e.y - e.x * a.y
        lPathAShadowDis =
            abs((lA * d.x + lB * d.y + lC) / hypot(lA.toDouble(), lB.toDouble()).toFloat())

        val rA = a.y - h.y
        val rB = h.x - a.x
        val rC = a.x * h.y - h.x * a.y
        rPathAShadowDis =
            abs((rA * i.x + rB * i.y + rC) / hypot(rA.toDouble(), rB.toDouble()).toFloat())
    }

    private fun getIntersectionPoint(p1: PagerPoint, p2: PagerPoint, p3: PagerPoint, p4: PagerPoint): PagerPoint {
        val x1 = p1.x
        val y1 = p1.y
        val x2 = p2.x
        val y2 = p2.y
        val x3 = p3.x
        val y3 = p3.y
        val x4 = p4.x
        val y4 = p4.y

        val px =
            ((x1 - x2) * (x3 * y4 - x4 * y3) - (x3 - x4) * (x1 * y2 - x2 * y1)) / ((x3 - x4) * (y1 - y2) - (x1 - x2) * (y3 - y4))
        val py =
            ((y1 - y2) * (x3 * y4 - x4 * y3) - (x1 * y2 - x2 * y1) * (y3 - y4)) / ((y1 - y2) * (x3 - x4) - (x1 - x2) * (y3 - y4))
        return PagerPoint(px, py)
    }

    private fun createGradientDrawable() {
        // A区域左上角和左下角阴影
        var deepColor = 0x33333333.toInt()
        var lightColor = 0x01333333.toInt()
        var gradientColors = intArrayOf(lightColor, deepColor)
        drawableLeftTopRight = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, gradientColors).apply {
            gradientType = GradientDrawable.LINEAR_GRADIENT
        }
        drawableLeftLowerRight = GradientDrawable(GradientDrawable.Orientation.RIGHT_LEFT, gradientColors).apply {
            gradientType = GradientDrawable.LINEAR_GRADIENT
        }

        // A区域右上角和右下角阴影
        deepColor = 0x22333333.toInt()
        lightColor = 0x01333333.toInt()
        gradientColors = intArrayOf(deepColor, lightColor, lightColor)
        drawableRightTopRight = GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, gradientColors).apply {
            gradientType = GradientDrawable.LINEAR_GRADIENT
        }
        drawableRightLowerRight = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, gradientColors).apply {
            gradientType = GradientDrawable.LINEAR_GRADIENT
        }

        // 水平翻页阴影
        deepColor = 0x44333333.toInt()
        lightColor = 0x01333333.toInt()
        gradientColors = intArrayOf(lightColor, deepColor)
        drawableHorizontalLowerRight = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, gradientColors).apply {
            gradientType = GradientDrawable.LINEAR_GRADIENT
        }

        // B区域阴影
        deepColor = 0x55111111.toInt()
        lightColor = 0x00111111.toInt()
        gradientColors = intArrayOf(deepColor, lightColor)
        drawableBTopRight = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, gradientColors).apply {
            gradientType = GradientDrawable.LINEAR_GRADIENT
        }
        drawableBLowerRight = GradientDrawable(GradientDrawable.Orientation.RIGHT_LEFT, gradientColors).apply {
            gradientType = GradientDrawable.LINEAR_GRADIENT
        }

        // C区域阴影
        deepColor = 0x55333333.toInt()
        lightColor = 0x00333333.toInt()
        gradientColors = intArrayOf(lightColor, deepColor)
        drawableCTopRight = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, gradientColors).apply {
            gradientType = GradientDrawable.LINEAR_GRADIENT
        }
        drawableCLowerRight = GradientDrawable(GradientDrawable.Orientation.RIGHT_LEFT, gradientColors).apply {
            gradientType = GradientDrawable.LINEAR_GRADIENT
        }
    }

    fun setDefaultPath() {
        a.x = -1f
        a.y = -1f
        postInvalidate()
    }

    data class PagerPoint(var x: Float = 0f, var y: Float = 0f) {
        fun set(p: PagerPoint) {
            x = p.x
            y = p.y
        }
    }
}
