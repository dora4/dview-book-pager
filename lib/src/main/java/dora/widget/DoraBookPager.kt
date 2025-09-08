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
    private var flipToNext = true

    private enum class FlipMode { TOP, BOTTOM, MIDDLE }
    private var flipMode = FlipMode.MIDDLE

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pathFront = Path()
    private val pathFold = Path()

    init {
        context.withStyledAttributes(attrs, R.styleable.DoraBookPager) {
            curlDuration  = getInt(
                R.styleable.DoraBookPager_dview_bp_duration,
                curlDuration.toInt()
            ).toLong()
            maxCurlRadius = getDimension(
                R.styleable.DoraBookPager_dview_bp_maxCurlRadius,
                maxCurlRadius
            )
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

                // 决定翻页方向
                flipToNext = touchX >= width / 2f
                // 决定翻页模式
                flipMode = when {
                    touchY < height * 0.3f -> FlipMode.TOP
                    touchY > height * 0.7f -> FlipMode.BOTTOM
                    else -> FlipMode.MIDDLE
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                touchX = ev.x; touchY = ev.y
                animFraction = if (flipToNext) {
                    (width - touchX) / width
                } else {
                    touchX / width
                }
                animFraction = animFraction.coerceIn(0f, 1f)
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                if (animFraction > 0.5f) {
                    if (flipToNext && currentIndex < pages.size - 1) {
                        startCurlAnimation(true)
                    } else if (!flipToNext && currentIndex > 0) {
                        startCurlAnimation(false)
                    } else rollback()
                } else rollback()
            }
        }
        return true
    }

    private fun rollback() {
        ValueAnimator.ofFloat(animFraction, 0f).apply {
            duration = curlDuration / 2
            addUpdateListener {
                animFraction = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (pages.isEmpty()) return

        val currBmp = pages[currentIndex]
        val targetBmp = if (flipToNext) {
            pages.getOrNull(currentIndex + 1) ?: currBmp
        } else {
            pages.getOrNull(currentIndex - 1) ?: currBmp
        }

        // 先画目标页（底层）
        canvas.drawBitmap(targetBmp, null, Rect(0, 0, width, height), paint)

        // 翻页参数
        val curlX = width * animFraction
        val radius = maxCurlRadius * animFraction

        pathFront.reset()
        pathFold.reset()

        when (flipMode) {
            FlipMode.MIDDLE -> {
                val curlY = height * 0.5f
                val Ax = curlX; val Ay = 0f
                val Bx = curlX + radius; val By = curlY - radius
                val Cx = curlX + radius; val Cy = curlY + radius
                val Dx = curlX; val Dy = height.toFloat()

                pathFront.moveTo(0f, 0f)
                pathFront.lineTo(Ax, Ay)
                pathFront.cubicTo(Bx, By, Cx, Cy, Dx, Dy)
                pathFront.lineTo(0f, height.toFloat())
                pathFront.close()

                pathFold.moveTo(Ax, Ay)
                pathFold.cubicTo(Bx, By, Cx, Cy, Dx, Dy)
                pathFold.lineTo(width.toFloat(), height.toFloat())
                pathFold.lineTo(width.toFloat(), 0f)
                pathFold.close()
            }
            FlipMode.TOP -> {
                val Ax = width.toFloat(); val Ay = 0f
                val Bx = curlX; val By = height * 0.3f
                val Cx = curlX; val Cy = height.toFloat()

                pathFront.moveTo(0f, 0f)
                pathFront.lineTo(Ax, Ay)
                pathFront.quadTo(Bx, By, Cx, Cy)
                pathFront.lineTo(0f, height.toFloat())
                pathFront.close()

                pathFold.moveTo(Ax, Ay)
                pathFold.quadTo(Bx, By, Cx, Cy)
                pathFold.lineTo(width.toFloat(), height.toFloat())
                pathFold.close()
            }
            FlipMode.BOTTOM -> {
                val Ax = width.toFloat(); val Ay = height.toFloat()
                val Bx = curlX; val By = height * 0.7f
                val Cx = curlX; val Cy = 0f

                pathFront.moveTo(0f, 0f)
                pathFront.lineTo(Ax, Ay)
                pathFront.quadTo(Bx, By, Cx, Cy)
                pathFront.lineTo(0f, 0f)
                pathFront.close()

                pathFold.moveTo(Ax, Ay)
                pathFold.quadTo(Bx, By, Cx, Cy)
                pathFold.lineTo(width.toFloat(), 0f)
                pathFold.close()
            }
        }

        // 当前页正面
        canvas.save()
        canvas.clipPath(pathFront)
        canvas.drawBitmap(currBmp, null, Rect(0, 0, width, height), paint)
        canvas.restore()

        // 背面（翻折区域）
        canvas.save()
        canvas.clipPath(pathFold)
        val matrix = Matrix().apply { setScale(-1f, 1f, curlX, height / 2f) }
        val backPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
                setScale(0.7f, 0.7f, 0.7f, 1f) // 调暗
            })
        }
        canvas.drawBitmap(currBmp, matrix, backPaint)

        // 光影效果
        val lightShader = LinearGradient(
            curlX, 0f, curlX + radius, height.toFloat(),
            intArrayOf(Color.WHITE, Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = lightShader
        canvas.drawPath(pathFold, paint)
        paint.shader = null

        val darkShader = LinearGradient(
            curlX, 0f, curlX - radius, height.toFloat(),
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
