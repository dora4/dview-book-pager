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

        canvas.drawBitmap(currBmp, null, Rect(0,0,width,height), paint)

        val curlX = width * animFraction
        val curlY = height * 0.5f
        val radius = maxCurlRadius * animFraction

        val Ax = curlX;         val Ay = 0f
        val Bx = curlX + radius; val By = curlY - radius
        val Cx = curlX + radius; val Cy = curlY + radius
        val Dx = curlX;         val Dy = height.toFloat()

        pathFront.reset()
        pathFront.moveTo(0f,0f)
        pathFront.lineTo(Ax,Ay)
        pathFront.quadTo(Bx,By, Cx,Cy)
        pathFront.lineTo(Dx,Dy)
        pathFront.lineTo(0f,height.toFloat())
        pathFront.close()

        pathFold.reset()
        pathFold.moveTo(Ax,Ay)
        pathFold.quadTo(Bx,By, Cx,Cy)
        pathFold.lineTo(Dx,Dy)
        pathFold.close()

        canvas.save()
        canvas.clipPath(pathFront)
        canvas.drawBitmap(currBmp, null, Rect(0,0,width,height), paint)
        canvas.restore()

        val shader = LinearGradient(
            Ax,Ay, Bx,By,
            intArrayOf(0x80FFFFFF.toInt(), 0x00FFFFFF),
            floatArrayOf(0f,1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = shader
        canvas.save()
        canvas.clipPath(pathFold)
        canvas.drawBitmap(nextBmp, null, Rect(0,0,width,height), paint)
        canvas.restore()
        paint.shader = null

        // 阴影效果
        paint.setShadowLayer(20f, 0f, 0f, Color.BLACK)
        canvas.drawPath(pathFold, paint)
        paint.clearShadowLayer()
    }
}
