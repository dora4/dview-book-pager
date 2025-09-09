package dora.widget

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class BookPageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    companion object {
        private const val STYLE_TOP_RIGHT = "STYLE_TOP_RIGHT"
        private const val STYLE_LOWER_RIGHT = "STYLE_LOWER_RIGHT"
        private const val STYLE_MIDDLE = "STYLE_MIDDLE"
    }

    private var style: String = STYLE_TOP_RIGHT

    private var viewWidth = 0
    private var viewHeight = 0

    private val a = PointF()   // 触摸点
    private val f = PointF()   // 固定点

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val pathA = Path()
    private val pathB = Path()
    private val pathC = Path()

    private var currBitmap: Bitmap? = null
    private var nextBitmap: Bitmap? = null

    fun setBitmaps(curr: Bitmap, next: Bitmap?) {
        currBitmap = curr
        nextBitmap = next
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        viewWidth = w
        viewHeight = h
        super.onSizeChanged(w, h, oldw, oldh)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                a.x = event.x
                a.y = event.y
                style = when {
                    a.y < viewHeight / 3f -> {
                        f.x = viewWidth.toFloat()
                        f.y = 0f
                        STYLE_TOP_RIGHT
                    }
                    a.y > viewHeight * 2f / 3f -> {
                        f.x = viewWidth.toFloat()
                        f.y = viewHeight.toFloat()
                        STYLE_LOWER_RIGHT
                    }
                    else -> {
                        f.x = viewWidth.toFloat()
                        f.y = viewHeight / 2f
                        STYLE_MIDDLE
                    }
                }
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                a.x = event.x
                a.y = event.y
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // 松手回弹（简单处理）
                a.x = viewWidth - 1f
                a.y = viewHeight / 2f
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (currBitmap == null) return

        when (style) {
            STYLE_TOP_RIGHT, STYLE_LOWER_RIGHT -> drawCornerFlip(canvas)
            STYLE_MIDDLE -> drawMiddleFlip(canvas)
        }
    }

    /** 顶部/底部角落翻页 */
    private fun drawCornerFlip(canvas: Canvas) {
        canvas.save()

        // 当前页
        currBitmap?.let {
            canvas.drawBitmap(it, null, Rect(0, 0, viewWidth, viewHeight), paint)
        }

        // 下一页区域
        nextBitmap?.let {
            pathB.reset()
            pathB.moveTo(a.x, a.y)
            pathB.lineTo(f.x, f.y)
            pathB.lineTo(viewWidth.toFloat(), viewHeight.toFloat())
            pathB.lineTo(0f, viewHeight.toFloat())
            pathB.close()

            canvas.save()
            canvas.clipPath(pathB)
            canvas.drawBitmap(it, null, Rect(0, 0, viewWidth, viewHeight), paint)
            canvas.restore()
        }

        // 卷起区域阴影
        paint.color = 0x55000000
        pathC.reset()
        pathC.moveTo(a.x, a.y)
        pathC.lineTo(f.x, f.y)
        pathC.lineTo(viewWidth.toFloat(), 0f)
        pathC.close()
        canvas.drawPath(pathC, paint)

        canvas.restore()
    }

    /** 中间水平翻页 */
    private fun drawMiddleFlip(canvas: Canvas) {
        canvas.save()

        // 背景（下一页）
        nextBitmap?.let {
            canvas.drawBitmap(it, null, Rect(0, 0, viewWidth, viewHeight), paint)
        }

        // 折叠线
        val foldX = (a.x + f.x) / 2f

        // 当前页可见部分
        pathA.reset()
        pathA.moveTo(0f, 0f)
        pathA.lineTo(foldX, 0f)
        pathA.lineTo(foldX, viewHeight.toFloat())
        pathA.lineTo(0f, viewHeight.toFloat())
        pathA.close()

        canvas.save()
        canvas.clipPath(pathA)
        currBitmap?.let {
            canvas.drawBitmap(it, null, Rect(0, 0, viewWidth, viewHeight), paint)
        }
        canvas.restore()

        // 卷起部分（右侧遮罩）
        pathB.reset()
        pathB.moveTo(foldX, 0f)
        pathB.lineTo(viewWidth.toFloat(), 0f)
        pathB.lineTo(viewWidth.toFloat(), viewHeight.toFloat())
        pathB.lineTo(foldX, viewHeight.toFloat())
        pathB.close()

        paint.color = 0x22000000
        canvas.drawPath(pathB, paint)

        canvas.restore()
    }
}
