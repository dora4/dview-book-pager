package dora.widget

import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Scroller
import kotlin.math.max
import kotlin.math.min

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
    private val scroller = Scroller(context, LinearInterpolator())

    private var style: DragStyle = DragStyle.DragBottomRight

    private lateinit var drawableRightLowerRight: GradientDrawable
    private lateinit var drawableBLowerRight: GradientDrawable
    private lateinit var drawableCLowerRight: GradientDrawable

    private var listener: OnPageChangeListener? = null

    init {
        createGradientDrawable()
    }

    sealed class DragStyle(val dragPosition: Int) {

        object DragLeft : DragStyle(DRAG_LEFT)
        object DragRight : DragStyle(DRAG_RIGHT)
        object DragTopRight : DragStyle(DRAG_TOP_RIGHT)
        object DragBottomRight : DragStyle(DRAG_BOTTOM_RIGHT)

        companion object {
            const val DRAG_LEFT = 0
            const val DRAG_RIGHT = 1
            const val DRAG_TOP_RIGHT = 2
            const val DRAG_BOTTOM_RIGHT = 3
        }
    }

    private fun createGradientDrawable() {
        val deepColor = 0x22333333
        val lightColor = 0x01333333
        val gradientColors = intArrayOf(deepColor, lightColor)
        drawableRightLowerRight = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, gradientColors)
        drawableRightLowerRight.gradientType = GradientDrawable.LINEAR_GRADIENT

        drawableBLowerRight = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, gradientColors)
        drawableBLowerRight.gradientType = GradientDrawable.LINEAR_GRADIENT

        drawableCLowerRight = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, gradientColors)
        drawableCLowerRight.gradientType = GradientDrawable.LINEAR_GRADIENT
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
        canvas.drawColor(Color.WHITE)
        if (a.x == -1f && a.y == -1f) {
            drawPathAContent(canvas, getPathDefault())
        } else {
            val pathA = getPathAFromBottomRight()
            drawPathAContent(canvas, pathA)
            drawPathBContent(canvas, pathA)
            drawPathCContent(canvas, pathA)
            drawShadow(canvas)
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

    private fun getPathAFromBottomRight(): Path {
        pathA.reset()
        pathA.moveTo(a.x, a.y)
        pathA.lineTo(b.x, b.y)
        pathA.lineTo(c.x, c.y)
        pathA.lineTo(d.x, d.y)
        pathA.lineTo(a.x, a.y)
        pathA.close()
        return pathA
    }

    fun updateTouchPoint(x: Float, y: Float, style: DragStyle) {
        a.x = max(1f, min(x, viewWidth.toFloat() - 1f))
        a.y = max(1f, min(y, viewHeight.toFloat() - 1f))
        this.style = style
        f.x = viewWidth.toFloat()
        f.y = viewHeight.toFloat()
        calcXYPoints(a, f)
        postInvalidate()
    }

    fun startCancelAnimation() {
        val dx = (viewWidth - a.x).toInt()
        val dy = (viewHeight - a.y).toInt()
        scroller.startScroll(a.x.toInt(), a.y.toInt(), dx, dy, 400)
        invalidate()
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            val x = scroller.currX.toFloat()
            val y = scroller.currY.toFloat()
            updateTouchPoint(x, y, style)
        } else {
            listener?.onPageCancel()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                style = DragStyle.DragBottomRight
                updateTouchPoint(event.x, event.y, style)
            }
            MotionEvent.ACTION_MOVE -> updateTouchPoint(event.x, event.y, style)
            MotionEvent.ACTION_UP -> startCancelAnimation()
        }
        return true
    }

    private fun drawPathAContent(canvas: Canvas, pathA: Path) {
        canvas.save()
        canvas.clipPath(pathA)
        canvas.drawPath(pathA, pathAPaint)
        if (pathAText.isNotEmpty()) {
            canvas.drawText(pathAText, a.x - 100f, a.y - 50f, textPaint)
        }
        canvas.restore()
    }

    private fun drawPathBContent(canvas: Canvas, pathA: Path) {
        canvas.save()
        val pathB = getPathB()
        // 创建一个临时 path 做差集
        val temp = Path(pathB)
        temp.op(pathA, Path.Op.DIFFERENCE) // pathB - pathA
        // 绘制差集 path
        canvas.drawPath(temp, pathBPaint)
        if (pathBText.isNotEmpty()) {
            canvas.drawText(pathBText, b.x, b.y - 30f, textPaint)
        }
        canvas.restore()
    }

    private fun drawPathCContent(canvas: Canvas, pathA: Path) {
        canvas.save()
        val pathC = getPathC()
        val temp = Path(pathC)
        temp.op(pathA, Path.Op.DIFFERENCE) // pathC - pathA
        canvas.drawPath(temp, pathCPaint)
        if (pathCText.isNotEmpty()) {
            canvas.drawText(pathCText, c.x, c.y - 30f, textPaint)
        }
        canvas.restore()
    }

    private fun drawShadow(canvas: Canvas) {
        val shadowWidth = 60
        drawableRightLowerRight.setBounds(
            (viewWidth - shadowWidth),
            0,
            viewWidth,
            viewHeight
        )
        drawableRightLowerRight.draw(canvas)
    }

    private fun getPathB(): Path {
        pathB.reset()
        pathB.moveTo(a.x, a.y)
        pathB.lineTo(b.x, b.y)
        pathB.lineTo(c.x, c.y)
        pathB.close()
        return pathB
    }

    private fun getPathC(): Path {
        pathC.reset()
        pathC.moveTo(a.x, a.y)
        pathC.lineTo(k.x, k.y)
        pathC.lineTo(j.x, j.y)
        pathC.lineTo(h.x, h.y)
        pathC.close()
        return pathC
    }

    private fun calcXYPoints(a: PagerPoint, f: PagerPoint) {
        g.x = (a.x + f.x) / 2
        g.y = (a.y + f.y) / 2

        e.x = g.x - (f.y - g.y) * (f.y - g.y) / max(1f, (f.x - g.x))
        e.y = f.y

        h.x = f.x
        h.y = g.y - (f.x - g.x) * (f.x - g.x) / max(1f, (f.y - g.y))

        c.x = e.x - (f.x - e.x) / 2
        c.y = f.y

        j.x = f.x
        j.y = h.y - (f.y - h.y) / 2

        b.set(getIntersectionPoint(a, e, c, j))
        k.set(getIntersectionPoint(a, h, c, j))
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
            ((x1 - x2) * (x3 * y4 - x4 * y3) - (x3 - x4) * (x1 * y2 - x2 * y1)) /
                    max(1f, ((x3 - x4) * (y1 - y2) - (x1 - x2) * (y3 - y4)))
        val py =
            ((y1 - y2) * (x3 * y4 - x4 * y3) - (x1 * y2 - x2 * y1) * (y3 - y4)) /
                    max(1f, ((y1 - y2) * (x3 - x4) - (x1 - x2) * (y3 - y4)))
        return PagerPoint(px, py)
    }

    fun resetPath() {
        a.x = -1f
        a.y = -1f
        postInvalidate()
    }

    fun setOnPageChangeListener(listener: OnPageChangeListener) {
        this.listener = listener
    }

    interface OnPageChangeListener {
        fun onPagePre()
        fun onPageNext()
        fun onPageCancel()
    }

    data class PagerPoint(var x: Float = 0f, var y: Float = 0f) {
        fun set(p: PagerPoint) {
            x = p.x
            y = p.y
        }
    }
}
