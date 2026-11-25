package com.dvil.retui.pdf

import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.FileInputStream

object ThemeManager {
    // V6 Re:T-UI Color Palette Defaults
    var backgroundColor: Int = Color.parseColor("#080B10")
    var windowBackgroundColor: Int = Color.parseColor("#F0080B10")
    var headerBackgroundColor: Int = Color.parseColor("#F50D1117")
    var headerTextColor: Int = Color.parseColor("#00E5FF")
    var borderColor: Int = Color.parseColor("#00E5FF")
    var headerBorderColor: Int = Color.parseColor("#00E5FF")
    var promptColor: Int = Color.parseColor("#39FF14")
    var inputTextColor: Int = Color.parseColor("#00E5FF")
    var outputTextColor: Int = Color.parseColor("#E6EDF3")
    var outputBackgroundColor: Int = Color.parseColor("#161B22")
    var buttonBgColor: Int = Color.parseColor("#121820")
    var buttonTextColor: Int = Color.parseColor("#00E5FF")
    var buttonBorderColor: Int = Color.parseColor("#00E5FF")
    var moduleBgColor: Int = Color.parseColor("#0D1117")

    // Phosphor Color Modes: MATRIX, AMBER, CYAN, MONO, THEME
    var phosphorColor: Int = Color.parseColor("#33FF77")
    var phosphorMode: String = "MATRIX"

    fun loadTheme() {
        val paths = listOf(
            "/sdcard/Android/data/com.dvil.tui_renewed/files/Re-T-UI/theme.xml",
            "/sdcard/t-ui/theme.xml"
        )
        for (p in paths) {
            val file = File(p)
            if (file.exists() && file.canRead()) {
                try {
                    parseThemeXml(file)
                    return
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun parseThemeXml(file: File) {
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        val fis = FileInputStream(file)
        parser.setInput(fis, "UTF-8")

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                val tag = parser.name
                val value = parser.getAttributeValue(null, "value")
                if (value != null && value.startsWith("#")) {
                    try {
                        val parsedColor = Color.parseColor(value)
                        when (tag) {
                            "background_color", "bg_color" -> backgroundColor = parsedColor
                            "terminal_window_background_color" -> windowBackgroundColor = parsedColor
                            "terminal_header_background_color" -> headerBackgroundColor = parsedColor
                            "terminal_header_text_color" -> headerTextColor = parsedColor
                            "terminal_border_color" -> borderColor = parsedColor
                            "terminal_header_border_color" -> headerBorderColor = parsedColor
                            "prompt_color" -> promptColor = parsedColor
                            "input_color", "input_text_color" -> inputTextColor = parsedColor
                            "output_color", "output_text_color" -> outputTextColor = parsedColor
                            "output_bg", "output_background_color" -> outputBackgroundColor = parsedColor
                            "button_bg" -> buttonBgColor = parsedColor
                            "button_text" -> buttonTextColor = parsedColor
                            "button_border" -> buttonBorderColor = parsedColor
                            "module_bg_color" -> moduleBgColor = parsedColor
                        }
                    } catch (_: Exception) {}
                }
            }
            eventType = parser.next()
        }
        fis.close()
        updatePhosphorColor()
    }

    fun setPhosphorType(type: String) {
        phosphorMode = type.uppercase()
        updatePhosphorColor()
    }

    private fun updatePhosphorColor() {
        phosphorColor = when (phosphorMode) {
            "AMBER" -> Color.parseColor("#FFB000")
            "CYAN" -> Color.parseColor("#00E5FF")
            "MONO" -> Color.parseColor("#E0E0E0")
            "THEME" -> promptColor
            else -> Color.parseColor("#33FF77") // Matrix green
        }
    }

    fun getPhosphorFilter(): ColorMatrixColorFilter {
        val pr = Color.red(phosphorColor) / 255f
        val pg = Color.green(phosphorColor) / 255f
        val pb = Color.blue(phosphorColor) / 255f

        val br = Color.red(backgroundColor) / 255f
        val bg = Color.green(backgroundColor) / 255f
        val bb = Color.blue(backgroundColor) / 255f

        val dr = br - pr
        val dg = bg - pg
        val db = bb - pb

        val matrix = floatArrayOf(
            0.299f * dr, 0.587f * dr, 0.114f * dr, 0f, pr * 255f,
            0.299f * dg, 0.587f * dg, 0.114f * dg, 0f, pg * 255f,
            0.299f * db, 0.587f * db, 0.114f * db, 0f, pb * 255f,
            0f,          0f,          0f,          1f, 0f
        )
        return ColorMatrixColorFilter(ColorMatrix(matrix))
    }
}
