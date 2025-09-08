package dora.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.animation.addListener
import androidx.core.content.withStyledAttributes
import dora.widget.bookpager.R
import kotlin.math.max
import kotlin.math.min

class DoraBookPager @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private var pages: List<Bitmap> = emptyList()
    private var currentIndex = 0

    private var curlDuration: Long = 600
    private var maxCurlRadius: Float = 200f

    private var animFraction = 0f

    private var touchX = 0f
    private var touchY = 0f
    private var isDragging = false

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pathFront = Path()
    private val pathFold = Path()

    init {
        context.withStyledAttributes(attrs, R.styleable.DoraBookPager) {
            curlDuration    = getInt(R.styleable.DoraBookPager_dview_bp_duration, curlDuration.toInt()).toLong()
            maxCurlRadius   = getDimension(R.styleable.DoraBookPager_dview_bp_maxCurlRadius, maxCurlRadius)
        }
    }

    fun setPages(bitmaps: List<Bitmap>) {
        pages = bitmaps
        currentIndex = 0
        invalidate()
    }

    fun flipToNext() {
        if (currentIndex >= pages.size - 1 || isDragging) return
        startCurlAnimation(next = true)
    }

    fun flipToPrev() {
        if (currentIndex <= 0 || isDragging) return
        startCurlAnimation(next = false)
    }

    private fun startCurlAnimation(next: Boolean) {
        val start = if (next) 0f else 1f
        val end   = if (next) 1f else 0f
        ValueAnimator.ofFloat(start, end).apply {
            duration = curlDuration
            addUpdateListener {
                animFraction = it.animatedValue as Float
                invalidate()
            }
            addListener(onEnd = {
                currentIndex += if (next) 1 else -1
                animFraction = 0f
                invalidate()
            })
            start()
        }
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isDragging = true
                touchX = ev.x; touchY = ev.y
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                touchX = ev.x; touchY = ev.y
                animFraction = min(1f, max(0f, touchX / width))
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                if (animFraction > 0.5f && currentIndex < pages.size-1) flipToNext()
                else if (animFraction < 0.5f && currentIndex > 0) flipToPrev()
                else {
                    ValueAnimator.ofFloat(animFraction, 0f).apply {
                        duration = curlDuration/2
                        addUpdateListener {
                            animFraction = it.animatedValue as Float
                            invalidate()
                        }
                        start()
                    }
                }
            }
        }
        return super.onTouchEvent(ev)
    }

    override fun onDraw(canvas: Canvas) {
        if (pages.isEmpty()) return

        val currBmp = pages[currentIndex]
        val nextBmp = pages.getOrNull(currentIndex + 1) ?: currBmp

        // 先画下一页（底层）
        canvas.drawBitmap(nextBmp, null, Rect(0, 0, width, height), paint)

        // 翻页参数
        val curlX = width * animFraction
        val curlY = height * 0.5f
        val radius = maxCurlRadius * animFraction

        val Ax = curlX;          val Ay = 0f
        val Bx = curlX + radius; val By = curlY - radius
        val Cx = curlX + radius; val Cy = curlY + radius
        val Dx = curlX;          val Dy = height.toFloat()

        // 当前页未翻部分
        pathFront.reset()
        pathFront.moveTo(0f, 0f)
        pathFront.lineTo(Ax, Ay)
        pathFront.cubicTo(Bx, By, Cx, Cy, Dx, Dy) // 用 cubic 更自然
        pathFront.lineTo(0f, height.toFloat())
        pathFront.close()

        // 翻折部分
        pathFold.reset()
        pathFold.moveTo(Ax, Ay)
        pathFold.cubicTo(Bx, By, Cx, Cy, Dx, Dy)
        pathFold.lineTo(width.toFloat(), height.toFloat())
        pathFold.lineTo(width.toFloat(), 0f)
        pathFold.close()

        // 绘制当前页正面（未翻折区域）
        canvas.save()
        canvas.clipPath(pathFront)
        canvas.drawBitmap(currBmp, null, Rect(0, 0, width, height), paint)
        canvas.restore()

        // 绘制背面（翻折区域）
        canvas.save()
        canvas.clipPath(pathFold)

        // 做水平镜像，模拟背面
        val matrix = Matrix()
        matrix.setScale(-1f, 1f, curlX, curlY)
        val backPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            // 背面调暗
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
                setScale(0.7f, 0.7f, 0.7f, 1f)
            })
        }
        canvas.drawBitmap(currBmp, matrix, backPaint)

        // 光影效果：亮面
        val lightShader = LinearGradient(
            Ax, Ay, Bx, By,
            intArrayOf(Color.WHITE, Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = lightShader
        canvas.drawPath(pathFold, paint)
        paint.shader = null

        // 光影效果：暗面
        val darkShader = LinearGradient(
            Cx, Cy, Dx, Dy,
            intArrayOf(0x80000000.toInt(), Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = darkShader
        canvas.drawPath(pathFold, paint)
        paint.shader = null

        canvas.restore()
    }
}
