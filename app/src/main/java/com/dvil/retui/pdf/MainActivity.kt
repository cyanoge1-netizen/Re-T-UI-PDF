package com.dvil.retui.pdf

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var rootLayout: LinearLayout
    private lateinit var headerTextView: TextView
    private lateinit var pdfView: PdfRendererView
    private lateinit var bottomBarLayout: LinearLayout
    private var currentPdfFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.loadTheme()
        buildCyberdeckUi()
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun buildCyberdeckUi() {
        val density = resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ThemeManager.backgroundColor)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // 1. Terminal Window Header Bar (1px border)
        val headerBorder = GradientDrawable().apply {
            setColor(ThemeManager.headerBackgroundColor)
            setStroke(dp(1), ThemeManager.headerBorderColor)
            cornerRadius = 0f
        }

        headerTextView = TextView(this).apply {
            typeface = Typeface.MONOSPACE
            textSize = 12f
            setTextColor(ThemeManager.inputTextColor)
            setPadding(dp(8), dp(6), dp(8), dp(6))
            text = "[READY · PDF: NULL] [Pg: 0/0] [100%]"
            background = headerBorder
        }

        val headerParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(dp(3), dp(3), dp(3), dp(2))
        }
        rootLayout.addView(headerTextView, headerParams)

        // 2. Central PDF Viewport
        pdfView = PdfRendererView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1.0f
            ).apply {
                setMargins(dp(3), 0, dp(3), 0)
            }
            onPageChangedListener = { page, total ->
                updateHeader(page, total)
            }
        }
        rootLayout.addView(pdfView)

        // 3. Bottom Function Key Bar (F1-F10 Terminal Style)
        val hScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(dp(3), dp(2), dp(3), dp(3))
            }
        }

        bottomBarLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(ThemeManager.headerBackgroundColor)
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }

        val fKeys = listOf(
            "[F1 Help]" to { showHelp() },
            "[F4 Invert]" to { togglePhosphor() },
            "[F5 Prev]" to { pdfView.prevPage() },
            "[F6 Next]" to { pdfView.nextPage() },
            "[F10 Quit]" to { finish() }
        )

        for ((label, action) in fKeys) {
            val btn = TextView(this).apply {
                text = label
                typeface = Typeface.MONOSPACE
                textSize = 11f
                setTextColor(ThemeManager.outputTextColor)
                setPadding(dp(8), dp(4), dp(8), dp(4))
                background = GradientDrawable().apply {
                    setColor(Color.TRANSPARENT)
                    setStroke(dp(1), ThemeManager.borderColor)
                }
                setOnClickListener { action() }
            }
            val btnParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(dp(2), 0, dp(2), 0)
            }
            bottomBarLayout.addView(btn, btnParams)
        }

        hScroll.addView(bottomBarLayout)
        rootLayout.addView(hScroll)

        setContentView(rootLayout)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        var targetPath = intent.getStringExtra("path") ?: intent.getStringExtra("target")
        
        if (targetPath == null && intent.data != null) {
            val uri: Uri = intent.data!!
            if (uri.scheme == "file") {
                targetPath = uri.path
            } else if (uri.scheme == "content") {
                // Copy to internal cache
                try {
                    val temp = File(cacheDir, "input.pdf")
                    contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(temp).use { output ->
                            input.copyTo(output)
                        }
                    }
                    targetPath = temp.absolutePath
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        if (targetPath != null) {
            val f = File(targetPath)
            if (f.exists()) {
                currentPdfFile = f
                pdfView.openPdf(f)
                updateHeader(1, pdfView.pageCount)
                return
            }
        }

        // Demo sample if no file passed
        headerTextView.text = "// $ pdf [No file specified · Tap [F1 Help] or pass file]"
    }

    private fun updateHeader(page: Int, total: Int) {
        val name = currentPdfFile?.name ?: "UNKNOWN"
        val phosphorStr = if (pdfView.isPhosphorInverted) "PHOSPHOR" else "NORMAL"
        headerTextView.text = "[READY · PDF: $name] [Pg: $page/$total] [$phosphorStr]"
    }

    private fun togglePhosphor() {
        pdfView.isPhosphorInverted = !pdfView.isPhosphorInverted
        updateHeader(pdfView.currentPageIndex + 1, pdfView.pageCount)
    }

    private fun showHelp() {
        val helpMsg = """
            // Re:T-UI PDF Cyberdeck Reader
            [F4] Invert / Phosphor Color Toggle
            [F5] / [h] / [Left]  Previous Page
            [F6] / [l] / [Right] Next Page
            [j] / [k]            Scroll Up / Down
            [Double Tap]         Zoom 200% / Reset
            [F10] / [q]          Quit
        """.trimIndent()
        headerTextView.text = helpMsg.replace("\n", " · ")
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_F1 -> { showHelp(); true }
            KeyEvent.KEYCODE_F4, KeyEvent.KEYCODE_I -> { togglePhosphor(); true }
            KeyEvent.KEYCODE_F5, KeyEvent.KEYCODE_PAGE_UP, KeyEvent.KEYCODE_H -> { pdfView.prevPage(); true }
            KeyEvent.KEYCODE_F6, KeyEvent.KEYCODE_PAGE_DOWN, KeyEvent.KEYCODE_L -> { pdfView.nextPage(); true }
            KeyEvent.KEYCODE_F10, KeyEvent.KEYCODE_Q -> { finish(); true }
            else -> super.onKeyDown(keyCode, event)
        }
    }
}
