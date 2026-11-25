package com.dvil.retui.pdf

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import java.io.File

class PdfRendererView(context: Context) : View(context) {

    private var fileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null
    private var currentPage: PdfRenderer.Page? = null
    private var pageBitmap: Bitmap? = null

    var currentPageIndex: Int = 0
        private set
    var pageCount: Int = 0
        private set

    var isPhosphorInverted: Boolean = true
        set(value) {
            field = value
            updatePaintFilter()
            invalidate()
        }

    var zoomScale: Float = 1.0f
        private set
    private var offsetX: Float = 0f
    private var offsetY: Float = 0f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val matrixTransform = Matrix()

    var onPageChangedListener: ((page: Int, total: Int) -> Unit)? = null

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            zoomScale *= detector.scaleFactor
            zoomScale = zoomScale.coerceIn(0.5f, 5.0f)
            invalidate()
            return true
        }
    })

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            offsetX -= distanceX
            offsetY -= distanceY
            invalidate()
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            zoomScale = if (zoomScale > 1.2f) 1.0f else 2.0f
            offsetX = 0f
            offsetY = 0f
            invalidate()
            return true
        }
    })

    init {
        updatePaintFilter()
    }

    private fun updatePaintFilter() {
        paint.colorFilter = if (isPhosphorInverted) {
            ThemeManager.getPhosphorFilter()
        } else {
            null
        }
    }

    fun openPdf(file: File) {
        close()
        fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        fileDescriptor?.let {
            pdfRenderer = PdfRenderer(it)
            pageCount = pdfRenderer?.pageCount ?: 0
            showPage(0)
        }
    }

    fun showPage(index: Int) {
        if (pdfRenderer == null || index < 0 || index >= pageCount) return
        currentPage?.close()
        currentPageIndex = index
        currentPage = pdfRenderer?.openPage(index)

        currentPage?.let { page ->
            val w = page.width * 2
            val h = page.height * 2
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            canvas.drawColor(Color.WHITE)
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            pageBitmap?.recycle()
            pageBitmap = bmp
        }
        offsetX = 0f
        offsetY = 0f
        onPageChangedListener?.invoke(currentPageIndex + 1, pageCount)
        invalidate()
    }

    fun nextPage() {
        if (currentPageIndex + 1 < pageCount) {
            showPage(currentPageIndex + 1)
        }
    }

    fun prevPage() {
        if (currentPageIndex - 1 >= 0) {
            showPage(currentPageIndex - 1)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        var handled = scaleDetector.onTouchEvent(event)
        handled = gestureDetector.onTouchEvent(event) || handled
        return handled || super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(ThemeManager.backgroundColor)

        val bmp = pageBitmap ?: return

        val viewW = width.toFloat()
        val viewH = height.toFloat()
        val bmpW = bmp.width.toFloat()
        val bmpH = bmp.height.toFloat()

        val baseScale = (viewW / bmpW).coerceAtMost(viewH / bmpH)
        val finalScale = baseScale * zoomScale

        val cx = (viewW - bmpW * finalScale) / 2f + offsetX
        val cy = (viewH - bmpH * finalScale) / 2f + offsetY

        matrixTransform.reset()
        matrixTransform.postScale(finalScale, finalScale)
        matrixTransform.postTranslate(cx, cy)

        canvas.drawBitmap(bmp, matrixTransform, paint)
    }

    fun close() {
        currentPage?.close()
        currentPage = null
        pdfRenderer?.close()
        pdfRenderer = null
        fileDescriptor?.close()
        fileDescriptor = null
        pageBitmap?.recycle()
        pageBitmap = null
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        close()
    }
}
