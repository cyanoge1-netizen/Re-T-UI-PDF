package com.dvil.retui.pdf

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var rootContainer: FrameLayout
    private lateinit var windowFrame: LinearLayout
    private lateinit var headerLayout: LinearLayout
    private lateinit var headerTitleView: TextView
    private lateinit var pdfView: PdfRendererView
    private lateinit var outputBannerView: TextView
    private lateinit var commandBarLayout: LinearLayout
    private lateinit var commandPromptView: TextView
    private lateinit var commandInputView: EditText
    private lateinit var bottomFKeyBar: HorizontalScrollView

    private var currentPdfFile: File? = null
    private var isHudVisible: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.loadTheme()
        buildCyberdeckUi()
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        ThemeManager.loadTheme()
        applyThemeStyles()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun buildCyberdeckUi() {
        val density = resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        rootContainer = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.TRANSPARENT)
        }

        windowFrame = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply {
                setMargins(dp(2), dp(2), dp(2), dp(2))
            }
        }

        // 1. Top Cyberdeck Header Bar with [RE:TUI PDF] + Controls (?, □, ×)
        headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(4), dp(8), dp(4))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        headerTitleView = TextView(this).apply {
            typeface = Typeface.MONOSPACE
            textSize = 12f
            text = "[RE:TUI PDF] [READY · NO FILE]"
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f)
        }
        headerLayout.addView(headerTitleView)

        // Control Glyphs: ? (Help), □ (HUD/Fullscreen Toggle), × (Close)
        val controls = listOf(
            "?" to { showHelpModal() },
            "\u25a1" to { toggleHud() },
            "\u00d7" to { finish() }
        )

        for ((glyph, action) in controls) {
            val btn = TextView(this).apply {
                typeface = Typeface.MONOSPACE
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(dp(8), dp(2), dp(8), dp(2))
                text = glyph
                isHapticFeedbackEnabled = true
                setOnClickListener {
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    action()
                }
            }
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(dp(3), 0, dp(1), 0)
            }
            headerLayout.addView(btn, lp)
        }
        windowFrame.addView(headerLayout)

        // 2. Central PDF Viewport
        pdfView = PdfRendererView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1.0f
            )
            onPageChangedListener = { page, total ->
                updateStatusHeader(page, total)
            }
            onCenterTapListener = {
                toggleHud()
            }
        }
        windowFrame.addView(pdfView)

        // 3. Command Output / Status Banner (collapsible terminal print line)
        outputBannerView = TextView(this).apply {
            typeface = Typeface.MONOSPACE
            textSize = 11f
            setPadding(dp(8), dp(2), dp(8), dp(2))
            visibility = View.VISIBLE
            text = "[STATUS: READY · Tap screen center or type :help for commands]"
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        windowFrame.addView(outputBannerView)

        // 4. Interactive Terminal Command Bar ($ <input>)
        commandBarLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(6), dp(2), dp(6), dp(2))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(dp(2), dp(2), dp(2), dp(2))
            }
        }

        commandPromptView = TextView(this).apply {
            typeface = Typeface.MONOSPACE
            textSize = 13f
            text = "$ "
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        commandBarLayout.addView(commandPromptView)

        commandInputView = EditText(this).apply {
            typeface = Typeface.MONOSPACE
            textSize = 12f
            setSingleLine(true)
            imeOptions = EditorInfo.IME_ACTION_GO
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            hint = "enter command (:help, :goto, :zoom, :open)..."
            background = null
            setPadding(dp(4), dp(4), dp(4), dp(4))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f)
            setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_GO ||
                    (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
                ) {
                    val raw = text.toString().trim()
                    executeCliCommand(raw)
                    text.clear()
                    true
                } else {
                    false
                }
            }
        }
        commandBarLayout.addView(commandInputView)
        windowFrame.addView(commandBarLayout)

        // 5. Bottom Quick Action Function Bar (F1-F10)
        bottomFKeyBar = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(dp(2), dp(1), dp(2), dp(2))
            }
        }

        val fKeyLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(2), dp(2), dp(2), dp(2))
        }

        val fKeys = listOf(
            "[F1 ?]" to { showHelpModal() },
            "[F2 Inv]" to { togglePhosphor() },
            "[F3 Fit]" to { pdfView.resetZoom(); printOutput("[ZOOM: FIT 100%]") },
            "[F4 Goto]" to { promptGotoDialog() },
            "[F5 \u25c0]" to { pdfView.prevPage() },
            "[F6 \u25b6]" to { pdfView.nextPage() },
            "[F7 Rot]" to { rotateDocument(90) },
            "[F8 Info]" to { showFileInfo() },
            "[F10 \u2715]" to { finish() }
        )

        for ((label, action) in fKeys) {
            val btn = TextView(this).apply {
                text = label
                typeface = Typeface.MONOSPACE
                textSize = 11f
                setPadding(dp(6), dp(4), dp(6), dp(4))
                isHapticFeedbackEnabled = true
                setOnClickListener {
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    action()
                }
            }
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(dp(2), 0, dp(2), 0)
            }
            fKeyLayout.addView(btn, lp)
        }

        bottomFKeyBar.addView(fKeyLayout)
        windowFrame.addView(bottomFKeyBar)

        rootContainer.addView(windowFrame)
        setContentView(rootContainer)
        applyThemeStyles()
    }

    private fun applyThemeStyles() {
        val density = resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        // Outer window cyberdeck border
        windowFrame.background = GradientDrawable().apply {
            setColor(ThemeManager.windowBackgroundColor)
            setStroke(dp(1), ThemeManager.borderColor)
            cornerRadius = 0f
        }

        // Header
        headerLayout.background = GradientDrawable().apply {
            setColor(ThemeManager.headerBackgroundColor)
            setStroke(dp(1), ThemeManager.headerBorderColor)
        }
        headerTitleView.setTextColor(ThemeManager.headerTextColor)

        // Header Glyphs
        for (i in 1 until headerLayout.childCount) {
            val v = headerLayout.getChildAt(i) as? TextView
            v?.setTextColor(ThemeManager.headerTextColor)
            v?.background = GradientDrawable().apply {
                setColor(Color.TRANSPARENT)
                setStroke(dp(1), ThemeManager.headerBorderColor)
            }
        }

        // Output Banner
        outputBannerView.setTextColor(ThemeManager.outputTextColor)
        outputBannerView.setBackgroundColor(ThemeManager.outputBackgroundColor)

        // Command Bar
        commandBarLayout.background = GradientDrawable().apply {
            setColor(ThemeManager.moduleBgColor)
            setStroke(dp(1), ThemeManager.borderColor)
        }
        commandPromptView.setTextColor(ThemeManager.promptColor)
        commandInputView.setTextColor(ThemeManager.inputTextColor)
        commandInputView.setHintTextColor(Color.parseColor("#66" + Integer.toHexString(ThemeManager.inputTextColor).takeLast(6)))

        // Bottom F-Key Buttons
        val fKeyLayout = bottomFKeyBar.getChildAt(0) as? LinearLayout
        if (fKeyLayout != null) {
            for (i in 0 until fKeyLayout.childCount) {
                val btn = fKeyLayout.getChildAt(i) as? TextView
                btn?.setTextColor(ThemeManager.buttonTextColor)
                btn?.background = GradientDrawable().apply {
                    setColor(ThemeManager.buttonBgColor)
                    setStroke(dp(1), ThemeManager.buttonBorderColor)
                }
            }
        }
        pdfView.updatePaintFilter()
        pdfView.invalidate()
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        var targetPath = intent.getStringExtra("path") ?: intent.getStringExtra("target")

        if (targetPath == null && intent.data != null) {
            val uri: Uri = intent.data!!
            if (uri.scheme == "file") {
                targetPath = uri.path
            } else if (uri.scheme == "content") {
                try {
                    val temp = File(cacheDir, "input.pdf")
                    contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(temp).use { output ->
                            input.copyTo(output)
                        }
                    }
                    targetPath = temp.absolutePath
                } catch (e: Exception) {
                    printOutput("[ERR · FAILED TO RESOLVE CONTENT URI]")
                }
            }
        }

        if (targetPath != null) {
            loadFile(File(targetPath))
        } else {
            printOutput("[STATUS: READY · Tap screen or type ':open /sdcard/Download/sample.pdf']")
        }
    }

    private fun loadFile(file: File) {
        if (file.exists() && file.canRead()) {
            currentPdfFile = file
            pdfView.openPdf(file)
            updateStatusHeader(1, pdfView.pageCount)
            printOutput("[OPENED · ${file.name} · ${pdfView.pageCount} Pages]")
        } else {
            printOutput("[ERR · FILE NOT FOUND: ${file.absolutePath}]")
        }
    }

    private fun updateStatusHeader(page: Int, total: Int) {
        val name = currentPdfFile?.name ?: "NO FILE"
        val phosphorStr = if (pdfView.isPhosphorInverted) ThemeManager.phosphorMode else "NORMAL"
        val zoomPct = (pdfView.zoomScale * 100).toInt()
        headerTitleView.text = "[RE:TUI PDF] [READY · $name] [Pg: $page/$total] [$zoomPct%] [$phosphorStr]"
    }

    private fun printOutput(msg: String) {
        outputBannerView.visibility = View.VISIBLE
        outputBannerView.text = msg
    }

    private fun toggleHud() {
        isHudVisible = !isHudVisible
        val vis = if (isHudVisible) View.VISIBLE else View.GONE
        headerLayout.visibility = vis
        commandBarLayout.visibility = vis
        bottomFKeyBar.visibility = vis
        outputBannerView.visibility = vis
    }

    private fun togglePhosphor() {
        pdfView.isPhosphorInverted = !pdfView.isPhosphorInverted
        val state = if (pdfView.isPhosphorInverted) "ON (${ThemeManager.phosphorMode})" else "OFF"
        printOutput("[PHOSPHOR: $state]")
        updateStatusHeader(pdfView.currentPageIndex + 1, pdfView.pageCount)
    }

    private fun rotateDocument(delta: Int) {
        pdfView.rotationAngle = (pdfView.rotationAngle + delta) % 360
        printOutput("[ROTATION: ${pdfView.rotationAngle}\u00b0]")
    }

    private fun executeCliCommand(rawCmd: String) {
        if (rawCmd.isBlank()) return
        val cleanCmd = rawCmd.removePrefix(":").trim()
        val parts = cleanCmd.split("\\s+".toRegex())
        val cmd = parts[0].lowercase()
        val arg = if (parts.size > 1) parts[1] else ""

        when (cmd) {
            "help", "?" -> showHelpModal()
            "goto", "g", "page", "p" -> {
                val page = arg.toIntOrNull()
                if (page != null) {
                    pdfView.showPage(page - 1)
                    printOutput("[JUMPED TO PAGE $page / ${pdfView.pageCount}]")
                } else {
                    printOutput("[USAGE: goto <page_no>]")
                }
            }
            "open", "o", "load" -> {
                if (arg.isNotBlank()) {
                    val target = if (arg.startsWith("/")) File(arg) else File("/sdcard/Download", arg)
                    loadFile(target)
                } else {
                    printOutput("[USAGE: open <filename/path>]")
                }
            }
            "zoom", "z" -> {
                when (arg.lowercase()) {
                    "fit", "reset", "100" -> { pdfView.resetZoom(); printOutput("[ZOOM: FIT 100%]") }
                    "in", "+" -> { pdfView.setZoom(pdfView.zoomScale + 0.25f); printOutput("[ZOOM: ${(pdfView.zoomScale * 100).toInt()}%]") }
                    "out", "-" -> { pdfView.setZoom(pdfView.zoomScale - 0.25f); printOutput("[ZOOM: ${(pdfView.zoomScale * 100).toInt()}%]") }
                    else -> {
                        val num = arg.removeSuffix("%").toFloatOrNull()
                        if (num != null) {
                            pdfView.setZoom(num / 100f)
                            printOutput("[ZOOM: ${num.toInt()}%]")
                        } else {
                            printOutput("[USAGE: zoom <in|out|fit|150%|200%>]")
                        }
                    }
                }
            }
            "inv", "invert" -> togglePhosphor()
            "phosphor", "ph" -> {
                if (arg.isNotBlank()) {
                    ThemeManager.setPhosphorType(arg)
                    pdfView.isPhosphorInverted = true
                    printOutput("[PHOSPHOR MODE: ${ThemeManager.phosphorMode}]")
                    updateStatusHeader(pdfView.currentPageIndex + 1, pdfView.pageCount)
                } else {
                    printOutput("[PHOSPHOR MODES: amber, matrix, cyan, mono, theme]")
                }
            }
            "rotate", "rot", "r" -> {
                val deg = arg.toIntOrNull() ?: 90
                rotateDocument(deg)
            }
            "info", "meta" -> showFileInfo()
            "hud", "toggle" -> toggleHud()
            "q", "quit", "exit" -> finish()
            else -> {
                // If pure number entered, treat as page jump (e.g. :12)
                val page = cmd.toIntOrNull()
                if (page != null) {
                    pdfView.showPage(page - 1)
                    printOutput("[JUMPED TO PAGE $page / ${pdfView.pageCount}]")
                } else {
                    printOutput("[UNKNOWN COMMAND: '$cmd' · Type :help for list]")
                }
            }
        }
    }

    private fun promptGotoDialog() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            typeface = Typeface.MONOSPACE
            setTextColor(ThemeManager.inputTextColor)
            hint = "1..${pdfView.pageCount}"
        }
        AlertDialog.Builder(this)
            .setTitle("[GOTO PAGE]")
            .setView(input)
            .setPositiveButton("JUMP") { _, _ ->
                val p = input.text.toString().toIntOrNull()
                if (p != null) {
                    pdfView.showPage(p - 1)
                    printOutput("[JUMPED TO PAGE $p]")
                }
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun showFileInfo() {
        val f = currentPdfFile
        val info = if (f != null) {
            """
                // RE:TUI PDF DOCUMENT META
                FILE: ${f.name}
                PATH: ${f.absolutePath}
                SIZE: ${(f.length() / 1024)} KB
                PAGES: ${pdfView.pageCount}
                CURRENT: ${pdfView.currentPageIndex + 1}
                ROTATION: ${pdfView.rotationAngle}\u00b0
                ZOOM: ${(pdfView.zoomScale * 100).toInt()}%
                PHOSPHOR: ${ThemeManager.phosphorMode}
            """.trimIndent()
        } else {
            "// NO DOCUMENT LOADED"
        }
        printOutput(info.replace("\n", " · "))
    }

    private fun showHelpModal() {
        val helpMsg = """
            // RE:TUI PDF COMMAND & GESTURE REFERENCE
            [COMMANDS]:
              :goto <pg> / :<pg>  - Jump to page
              :open <path>        - Open PDF file
              :zoom <fit|in|out>  - Change zoom
              :phosphor <type>    - Set CRT phosphor (amber, matrix, cyan, mono)
              :rot <90|180|270>   - Rotate document
              :info               - Show document metadata
              :q / :quit          - Exit reader

            [TOUCH GESTURES]:
              Left 20% Tap        - Previous page (Haptic click)
              Right 20% Tap       - Next page (Haptic click)
              Center Tap          - Toggle HUD / Distraction-free mode
              Double Tap          - Fit to width / 180% zoom toggle
              Horizontal Fling    - Swipe to flip page
              Pinch-to-zoom       - Fluid zoom 70%..500%

            [VIM KEYBOARD SHORTCUTS]:
              [j] / [k]           - Pan Up / Down
              [h] / [l] / [PgUp]  - Previous / Next Page
              [i]                 - Invert / Phosphor toggle
              [r]                 - Rotate 90 degrees
              [f]                 - Fit to screen
              [:]                 - Focus command prompt
              [q]                 - Quit
        """.trimIndent()
        
        AlertDialog.Builder(this)
            .setTitle("[HELP & COMMANDS]")
            .setMessage(helpMsg)
            .setPositiveButton("CLOSE", null)
            .show()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_F1 -> { showHelpModal(); true }
            KeyEvent.KEYCODE_F2, KeyEvent.KEYCODE_I -> { togglePhosphor(); true }
            KeyEvent.KEYCODE_F3, KeyEvent.KEYCODE_F -> { pdfView.resetZoom(); true }
            KeyEvent.KEYCODE_F4, KeyEvent.KEYCODE_G -> { promptGotoDialog(); true }
            KeyEvent.KEYCODE_F5, KeyEvent.KEYCODE_PAGE_UP, KeyEvent.KEYCODE_H -> { pdfView.prevPage(); true }
            KeyEvent.KEYCODE_F6, KeyEvent.KEYCODE_PAGE_DOWN, KeyEvent.KEYCODE_L -> { pdfView.nextPage(); true }
            KeyEvent.KEYCODE_F7, KeyEvent.KEYCODE_R -> { rotateDocument(90); true }
            KeyEvent.KEYCODE_F8 -> { showFileInfo(); true }
            KeyEvent.KEYCODE_F10, KeyEvent.KEYCODE_Q -> { finish(); true }
            else -> super.onKeyDown(keyCode, event)
        }
    }
}
