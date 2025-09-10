package dora.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.LinearInterpolator
import android.widget.Scroller
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

class DoraBookPager @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var pathAText: String = ""
    var pathBText: String = ""
    var pathCText: String = ""

    private val pathAPaint = Paint().apply {
        color = Color.LTGRAY
        isAntiAlias = true
    }
    private val pathBPaint = Paint().apply {
        color = Color.LTGRAY
        isAntiAlias = true
    }
    private val pathCPaint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
    }
    private val textPaint = Paint().apply {
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
        isSubpixelText = true
        textSize = 30f
    }
    private var isDragging = false
    private var touchSlop = 10
    private var downX = 0f
    private var downY = 0f

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
    var lPathAShadowDis: Float = 0f
    var rPathAShadowDis: Float = 0f
    private val matrixArray = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1.0f)
    private var matrix = Matrix()
    private var defaultWidth = 600
    private var defaultHeight = 1000
    private var viewWidth = 0
    private var viewHeight = 0
    private val scroller = Scroller(context, LinearInterpolator())

    private var style: DragStyle = DragStyle.DragBottomRight

    private lateinit var drawableLeftTopRight: GradientDrawable
    private lateinit var drawableLeftLowerRight: GradientDrawable

    private lateinit var drawableRightTopRight: GradientDrawable
    private lateinit var drawableRightLowerRight: GradientDrawable
    private lateinit var drawableHorizontalLowerRight: GradientDrawable

    private lateinit var drawableBTopRight: GradientDrawable
    private lateinit var drawableBLowerRight: GradientDrawable

    private lateinit var drawableCTopRight: GradientDrawable
    private lateinit var drawableCLowerRight: GradientDrawable

    private lateinit var pathAContentBitmap: Bitmap
    private lateinit var pathBContentBitmap: Bitmap
    private lateinit var pathCContentBitmap: Bitmap

    private var listener: OnPageChangeListener? = null

    init {
        createGradientDrawable()
    }

    sealed class DragStyle(val dragPosition: Int) {

        data object DragLeft : DragStyle(DRAG_LEFT)
        data object DragRight : DragStyle(DRAG_RIGHT)
        data object DragTopRight : DragStyle(DRAG_TOP_RIGHT)
        data object DragBottomRight : DragStyle(DRAG_BOTTOM_RIGHT)

        companion object {
            const val DRAG_LEFT = 0
            const val DRAG_RIGHT = 1
            const val DRAG_TOP_RIGHT = 2
            const val DRAG_BOTTOM_RIGHT = 3
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    }

    private fun createGradientDrawable() {
        var deepColor = 0x33333333
        var lightColor = 0x01333333
        var gradientColors = intArrayOf(lightColor, deepColor)
        drawableLeftTopRight =
            GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, gradientColors)
        drawableLeftTopRight.setGradientType(GradientDrawable.LINEAR_GRADIENT)
        drawableLeftLowerRight =
            GradientDrawable(GradientDrawable.Orientation.RIGHT_LEFT, gradientColors)
        drawableLeftLowerRight.setGradientType(GradientDrawable.LINEAR_GRADIENT)
        deepColor = 0x22333333
        lightColor = 0x01333333
        gradientColors = intArrayOf(deepColor, lightColor, lightColor)
        drawableRightTopRight =
            GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, gradientColors)
        drawableRightTopRight.setGradientType(GradientDrawable.LINEAR_GRADIENT)
        drawableRightLowerRight =
            GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, gradientColors)
        drawableRightLowerRight.gradientType = GradientDrawable.LINEAR_GRADIENT
        deepColor = 0x44333333
        lightColor = 0x01333333
        gradientColors = intArrayOf(lightColor, deepColor)
        drawableHorizontalLowerRight =
            GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, gradientColors)
        drawableHorizontalLowerRight.setGradientType(GradientDrawable.LINEAR_GRADIENT)
        deepColor = 0x55111111
        lightColor = 0x00111111
        gradientColors = intArrayOf(deepColor, lightColor)
        drawableBTopRight =
            GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, gradientColors)
        drawableBTopRight.setGradientType(GradientDrawable.LINEAR_GRADIENT)
        drawableBLowerRight =
            GradientDrawable(GradientDrawable.Orientation.RIGHT_LEFT, gradientColors)
        drawableBLowerRight.gradientType = GradientDrawable.LINEAR_GRADIENT
        deepColor = 0x55333333
        lightColor = 0x00333333
        gradientColors = intArrayOf(lightColor, deepColor)
        drawableCTopRight =
            GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, gradientColors)
        drawableCTopRight.setGradientType(GradientDrawable.LINEAR_GRADIENT)
        drawableCLowerRight =
            GradientDrawable(GradientDrawable.Orientation.RIGHT_LEFT, gradientColors)
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
        pathAContentBitmap = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.RGB_565);
        drawPathAContentBitmap(pathAContentBitmap, pathAPaint);
        pathBContentBitmap = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.RGB_565);
        drawPathBContentBitmap(pathBContentBitmap, pathBPaint);
        pathCContentBitmap = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.RGB_565);
        drawPathCContentBitmap(pathCContentBitmap, pathCPaint);
    }

    private fun drawPathAContentBitmap(bitmap: Bitmap, pathPaint: Paint) {
        val canvas = Canvas(bitmap)
        canvas.drawPath(getPathDefault(), pathPaint)
        canvas.drawText(
            pathAText,
            (viewWidth - 260).toFloat(),
            (viewHeight - 100).toFloat(),
            textPaint
        )
    }

    private fun drawPathBContentBitmap(bitmap: Bitmap, pathPaint: Paint) {
        val canvas = Canvas(bitmap)
        canvas.drawPath(getPathDefault(), pathPaint)
        canvas.drawText(
            pathBText,
            (viewWidth - 260).toFloat(),
            (viewHeight - 100).toFloat(),
            textPaint
        )
    }

    private fun drawPathCContentBitmap(bitmap: Bitmap, pathPaint: Paint) {
        val canvas = Canvas(bitmap)
        canvas.drawPath(getPathDefault(), pathPaint)
        canvas.drawText(
            pathCText,
            (viewWidth - 260).toFloat(),
            (viewHeight - 100).toFloat(),
            textPaint
        )
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
        canvas.drawColor(Color.LTGRAY)
        if (a.x == -1f && a.y == -1f) {
            drawPathAContent(canvas, getPathDefault())
        } else {
            if (f.x.toInt() == viewWidth && f.y.toInt() == 0) {
                drawPathAContent(canvas, getPathAFromTopRight())
                drawPathCContent(canvas, getPathAFromTopRight())
                drawPathBContent(canvas, getPathAFromTopRight())
            } else if (f.x.toInt() == viewWidth && f.y.toInt() == viewHeight) {
                drawPathAContent(canvas, getPathAFromBottomRight())
                drawPathCContent(canvas, getPathAFromBottomRight())
                drawPathBContent(canvas, getPathAFromBottomRight())
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

    private fun getPathAFromTopRight(): Path {
        pathA.reset()
        pathA.lineTo(c.x, c.y)
        pathA.quadTo(e.x, e.y, b.x, b.y)
        pathA.lineTo(a.x, a.y)
        pathA.lineTo(k.x, k.y)
        pathA.quadTo(h.x, h.y, j.x, j.y)
        pathA.lineTo(viewWidth.toFloat(), viewHeight.toFloat())
        pathA.lineTo(0f, viewHeight.toFloat())
        pathA.close()
        return pathA
    }

    private fun getPathAFromBottomRight(): Path {
        pathA.reset()
        pathA.lineTo(0f, viewHeight.toFloat())
        pathA.lineTo(c.x, c.y)
        pathA.quadTo(e.x, e.y, b.x, b.y)
        pathA.lineTo(a.x, a.y)
        pathA.lineTo(k.x, k.y)
        pathA.quadTo(h.x, h.y, j.x, j.y)
        pathA.lineTo(viewWidth.toFloat(), 0f)
        pathA.close()
        return pathA;
    }

    private fun calcPointAByTouchPoint() {
        val w0 = viewWidth - c.x
        val w1 = abs((f.x - a.x).toDouble()).toFloat()
        val w2 = viewWidth * w1 / w0
        a.x = abs((f.x - w2).toDouble()).toFloat()
        val h1 = abs((f.y - a.y).toDouble()).toFloat()
        val h2 = w2 * h1 / w1
        a.y = abs((f.y - h2).toDouble()).toFloat()
    }

    private fun updateTouchPoint(x: Float, y: Float, style: DragStyle) {
        val touchPoint: PagerPoint?
        a.x = x
        a.y = y
        this.style = style
        when (style) {
            DragStyle.DragTopRight -> {
                f.x = viewWidth.toFloat()
                f.y = 0f
                calcXYPoints(a, f)
                touchPoint = PagerPoint(x, y)
                if (calcCXPoints(touchPoint, f) < 0) {
                    calcPointAByTouchPoint()
                    calcXYPoints(a, f)
                }
                postInvalidate()
            }
            DragStyle.DragLeft, DragStyle.DragRight -> {
                a.y = (viewHeight - 1).toFloat()
                f.x = viewWidth.toFloat()
                f.y = viewHeight.toFloat()
                calcXYPoints(a, f)
                postInvalidate()
            }
            DragStyle.DragBottomRight -> {
                f.x = viewWidth.toFloat()
                f.y = viewHeight.toFloat()
                calcXYPoints(a, f)
                touchPoint = PagerPoint(x, y)
                if (calcCXPoints(touchPoint, f) < 0) {
                    calcPointAByTouchPoint()
                    calcXYPoints(a, f)
                }
                postInvalidate()
            }
        }
    }

    private fun startCancelAnimation() {
        val dx: Int
        val dy: Int
        // 让a滑动到f点所在位置，留出1像素是为了防止当a和f重叠时出现View闪烁的情况
        if (style == DragStyle.DragTopRight) {
            dx = (viewWidth - 1 - a.x).toInt()
            dy = (1 - a.y).toInt()
        } else {
            dx = (viewWidth - 1 - a.x).toInt()
            dy = (viewHeight - 1 - a.y).toInt()
        }
        scroller.startScroll(a.x.toInt(), a.y.toInt(), dx, dy, 400)
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            val x: Float = scroller.currX.toFloat()
            val y: Float = scroller.currY.toFloat()
            if (style == DragStyle.DragTopRight) {
                updateTouchPoint(x, y, DragStyle.DragTopRight)
            } else {
                updateTouchPoint(x, y, DragStyle.DragBottomRight)
            }
            if (scroller.isFinished) {
                resetPath()
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        super.onTouchEvent(event)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                if (downX <= viewWidth / 3) {
                    style = DragStyle.DragLeft
                    updateTouchPoint(downX, downY, style)
                } else if (downX > viewWidth / 3 && downY <= viewHeight / 3) {
                    style = DragStyle.DragTopRight
                    updateTouchPoint(downX, downY, style)
                } else if (downX > viewWidth * 2 / 3 && downY > viewHeight / 3 && downY <= viewHeight * 2 / 3) {
                    style = DragStyle.DragRight
                    updateTouchPoint(downX, downY, style)
                } else if (downX > viewWidth / 3 && downY > viewHeight * 2 / 3) {
                    style = DragStyle.DragBottomRight
                    updateTouchPoint(downX, downY, style)
                } else if (downX > viewWidth / 3 && downX < viewWidth * 2 / 3 && downY > viewHeight / 3 && downY < viewHeight * 2 / 3) {
                    // 中间
                }
                isDragging = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - downX
                val dy = event.y - downY
                if (!isDragging && hypot(dx, dy) > touchSlop) {
                    isDragging = true
                }
                updateTouchPoint(event.x, event.y, style)
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (isDragging) {
                    isDragging = false
                    if (style == DragStyle.DragLeft) {
                        listener?.onPagePre()
                    } else {
                        listener?.onPageNext()
                    }
                    return true
                }
                startCancelAnimation()
                listener?.onPageCancel()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun drawPathAContent(canvas: Canvas, pathA: Path) {
        canvas.save()
        val temp = Path(pathA)
        temp.op(pathA, Path.Op.INTERSECT)
        canvas.clipPath(temp)
        canvas.drawBitmap(pathAContentBitmap, 0f, 0f, null)
        if (style == DragStyle.DragLeft || style == DragStyle.DragRight) {
            drawPathAHorizontalShadow(canvas, pathA)
        } else {
            drawPathALeftShadow(canvas, pathA)
            drawPathARightShadow(canvas, pathA)
        }
        canvas.restore()
    }

    private fun drawPathALeftShadow(canvas: Canvas, pathA: Path) {
        canvas.restore()
        canvas.save()
        val left: Int
        val right: Int
        val top = e.y.toInt()
        val bottom = (e.y + viewHeight).toInt()
        val gradientDrawable: GradientDrawable
        if (style == DragStyle.DragTopRight) {
            gradientDrawable = drawableLeftTopRight
            left = (e.x - lPathAShadowDis / 2).toInt()
            right = (e.x).toInt()
        } else {
            gradientDrawable = drawableLeftLowerRight
            left = (e.x).toInt()
            right = (e.x + lPathAShadowDis / 2).toInt()
        }
        val path = Path()
        path.moveTo(a.x - rPathAShadowDis.coerceAtLeast(lPathAShadowDis) / 2, a.y)
        path.lineTo(d.x, d.y)
        path.lineTo(e.x, e.y)
        path.lineTo(a.x, a.y)
        path.close()
        val temp = Path(path)
        temp.op(pathA, Path.Op.INTERSECT)
        canvas.clipPath(temp)
        val degrees =
            Math.toDegrees(atan2((e.x - a.x).toDouble(), (a.y - e.y).toDouble())).toFloat()
        canvas.rotate(degrees, e.x, e.y)
        gradientDrawable.setBounds(left, top, right, bottom)
        gradientDrawable.draw(canvas)
    }

    private fun drawPathARightShadow(canvas: Canvas, pathA: Path) {
        canvas.restore()
        canvas.save()
        val viewDiagonalLength =
            hypot(viewWidth.toDouble(), viewHeight.toDouble()).toFloat()
        val left = h.x.toInt()
        val right = (h.x + viewDiagonalLength * 10).toInt()
        val top: Int
        val bottom: Int
        val gradientDrawable: GradientDrawable
        if (style == DragStyle.DragTopRight) {
            gradientDrawable = drawableRightTopRight
            top = (h.y - rPathAShadowDis / 2).toInt()
            bottom = h.y.toInt()
        } else {
            gradientDrawable = drawableRightLowerRight
            top = h.y.toInt()
            bottom = (h.y + rPathAShadowDis / 2).toInt()
        }
        gradientDrawable.setBounds(left, top, right, bottom)
        val path = Path()
        path.moveTo(a.x - rPathAShadowDis.coerceAtLeast(lPathAShadowDis) / 2, a.y)
        path.lineTo(h.x, h.y)
        path.lineTo(a.x, a.y)
        path.close()
        val temp = Path(path)
        temp.op(pathA, Path.Op.INTERSECT)
        canvas.clipPath(temp)
        val degrees =
            Math.toDegrees(atan2((a.y - h.y).toDouble(), (a.x - h.x).toDouble())).toFloat()
        canvas.rotate(degrees, h.x, h.y)
        gradientDrawable.draw(canvas)
    }

    private fun drawPathAHorizontalShadow(canvas: Canvas, pathA: Path) {
        canvas.restore()
        canvas.save()
        val clipPath = Path(pathA)
        canvas.clipPath(clipPath)
        val maxShadowWidth = 30
        val left = (a.x - maxShadowWidth.toFloat().coerceAtMost((rPathAShadowDis / 2))).toInt()
        val right = (a.x).toInt()
        val top = 0
        val bottom = viewHeight
        val gradientDrawable = drawableHorizontalLowerRight
        gradientDrawable.setBounds(left, top, right, bottom)
        val degrees =
            Math.toDegrees(atan2((f.x - a.x).toDouble(), (f.y - h.y).toDouble())).toFloat()
        canvas.rotate(degrees, a.x, a.y)
        gradientDrawable.draw(canvas)
    }

    private fun drawPathBContent(canvas: Canvas, pathA: Path) {
        canvas.save()
        val pathB = getPathB()
        val pathC = getPathC()
        val clipPath = Path(pathC)
        clipPath.op(pathB, Path.Op.UNION)
        clipPath.op(pathA, Path.Op.DIFFERENCE)
        canvas.clipPath(clipPath)
        canvas.drawBitmap(pathBContentBitmap, 0f, 0f, null)
        drawPathBShadow(canvas)
        canvas.restore()
    }

    private fun drawPathBShadow(canvas: Canvas) {
        val deepOffset = 0
        val lightOffset = 0
        val aTof = hypot((a.x - f.x).toDouble(), (a.y - f.y).toDouble()).toFloat()
        val viewDiagonalLength = hypot(viewWidth.toDouble(), viewHeight.toDouble()).toFloat()
        val left: Int
        val right: Int
        val top = c.y.toInt()
        val bottom = (viewDiagonalLength + c.y).toInt()
        val gradientDrawable: GradientDrawable
        if (style == DragStyle.DragTopRight) {
            gradientDrawable = drawableBTopRight
            left = (c.x - deepOffset).toInt()
            right = (c.x + aTof / 4 + lightOffset).toInt()
        } else {
            gradientDrawable = drawableBLowerRight
            left = (c.x - aTof / 4 - lightOffset).toInt()
            right = (c.x + deepOffset).toInt()
        }
        gradientDrawable.setBounds(left, top, right, bottom)
        val rotateDegrees =
            Math.toDegrees(atan2((e.x - f.x).toDouble(), (h.y - f.y).toDouble())).toFloat()
        canvas.rotate(rotateDegrees, c.x, c.y)
        gradientDrawable.draw(canvas)
    }

    private fun drawPathCContent(canvas: Canvas, pathA: Path) {
        canvas.save()
        val pathC = getPathC()
        val clipPath = Path(pathC)
        clipPath.op(pathA, Path.Op.DIFFERENCE)
        canvas.clipPath(clipPath)
        val eh = hypot((f.x - e.x).toDouble(), (h.y - f.y).toDouble()).toFloat()
        val sin0 = (f.x - e.x) / eh
        val cos0 = (h.y - f.y) / eh
        matrixArray[0] = -(1 - 2 * sin0 * sin0)
        matrixArray[1] = 2 * sin0 * cos0
        matrixArray[3] = 2 * sin0 * cos0
        matrixArray[4] = 1 - 2 * sin0 * sin0
        matrix.reset()
        matrix.setValues(matrixArray)
        matrix.preTranslate(-e.x, -e.y)
        matrix.postTranslate(e.x, e.y)
        canvas.drawBitmap(pathCContentBitmap, matrix, null)
        drawPathCShadow(canvas)
        canvas.restore()
    }

    private fun drawPathCShadow(canvas: Canvas) {
        val deepOffset = 1
        val lightOffset = -30
        val viewDiagonalLength =
            hypot(viewWidth.toDouble(), viewHeight.toDouble()).toFloat()
        val midpoint_ce = (c.x + e.x).toInt() / 2
        val midpoint_jh = (j.y + h.y).toInt() / 2
        val minDisToControlPoint =
            min(abs((midpoint_ce - e.x).toDouble()), abs((midpoint_jh - h.y).toDouble()))
                .toFloat()
        val left: Int
        val right: Int
        val top = c.y.toInt()
        val bottom = (viewDiagonalLength + c.y).toInt()
        val gradientDrawable: GradientDrawable
        if (style == DragStyle.DragTopRight) {
            gradientDrawable = drawableCTopRight
            left = (c.x - lightOffset).toInt()
            right = (c.x + minDisToControlPoint + deepOffset).toInt()
        } else {
            gradientDrawable = drawableCLowerRight
            left = (c.x - minDisToControlPoint - deepOffset).toInt()
            right = (c.x + lightOffset).toInt()
        }
        gradientDrawable.setBounds(left, top, right, bottom)
        val degrees =
            Math.toDegrees(atan2((e.x - f.x).toDouble(), (h.y - f.y).toDouble())).toFloat()
        canvas.rotate(degrees, c.x, c.y)
        gradientDrawable.draw(canvas)
    }

    private fun getPathB(): Path {
        pathB.reset()
        pathB.lineTo(0f, viewHeight.toFloat())
        pathB.lineTo(viewWidth.toFloat(), viewHeight.toFloat())
        pathB.lineTo(viewWidth.toFloat(), 0f)
        pathB.close()
        return pathB
    }

    private fun getPathC(): Path {
        pathC.reset()
        pathC.moveTo(i.x, i.y)
        pathC.lineTo(d.x, d.y)
        pathC.lineTo(b.x, b.y)
        pathC.lineTo(a.x, a.y)
        pathC.lineTo(k.x, k.y)
        pathC.close()
        return pathC
    }

    fun calcXYPoints(a: PagerPoint, f: PagerPoint) {
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
        b = getIntersectionPoint(a, e, c, j)
        k = getIntersectionPoint(a, h, c, j)
        d.x = (c.x + 2 * e.x + b.x) / 4
        d.y = (2 * e.y + c.y + b.y) / 4
        i.x = (j.x + 2 * h.x + k.x) / 4
        i.y = (2 * h.y + j.y + k.y) / 4
        val lA: Float = a.y - e.y
        val lB: Float = e.x - a.x
        val lC: Float = a.x * e.y - e.x * a.y
        lPathAShadowDis = abs(
            ((lA * d.x + lB * d.y + lC) / hypot(
                lA,
                lB
            ))
        )
        val rA: Float = a.y - h.y
        val rB: Float = h.x - a.x
        val rC: Float = a.x * h.y - h.x * a.y
        rPathAShadowDis = abs(
            ((rA * i.x + rB * i.y + rC) / hypot(
                rA,
                rB
            ))
        )
    }

    private fun calcCXPoints(a: PagerPoint, f: PagerPoint): Float {
        val g = PagerPoint()
        val e = PagerPoint()
        g.x = (a.x + f.x) / 2
        g.y = (a.y + f.y) / 2
        e.x = g.x - (f.y - g.y) * (f.y - g.y) / (f.x - g.x)
        e.y = f.y
        return e.x - (f.x - e.x) / 2
    }

    private fun getIntersectionPoint(
        p1: PagerPoint,
        p2: PagerPoint,
        p3: PagerPoint,
        p4: PagerPoint
    ): PagerPoint {
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
