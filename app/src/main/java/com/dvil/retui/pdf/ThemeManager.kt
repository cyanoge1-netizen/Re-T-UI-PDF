package com.dvil.retui.pdf

import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.FileInputStream

object ThemeManager {
    var backgroundColor: Int = Color.parseColor("#0A0E14")
    var windowBackgroundColor: Int = Color.parseColor("#D010141C")
    var headerBackgroundColor: Int = Color.parseColor("#F0080B10")
    var borderColor: Int = Color.parseColor("#00F0FF")
    var headerBorderColor: Int = Color.parseColor("#00F0FF")
    var inputTextColor: Int = Color.parseColor("#00F0FF")
    var outputTextColor: Int = Color.parseColor("#E6EDF3")
    var toolbarIconColor: Int = Color.parseColor("#39FF14")
    var phosphorColor: Int = Color.parseColor("#00FF66")

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
                            "terminal_border_color" -> borderColor = parsedColor
                            "terminal_header_border_color" -> headerBorderColor = parsedColor
                            "input_text_color", "input_color" -> {
                                inputTextColor = parsedColor
                                phosphorColor = parsedColor
                            }
                            "output_text_color", "output_color" -> outputTextColor = parsedColor
                            "toolbar_icon_color", "toolbar_color" -> toolbarIconColor = parsedColor
                        }
                    } catch (_: Exception) {}
                }
            }
            eventType = parser.next()
        }
        fis.close()
    }

    fun getPhosphorFilter(): ColorMatrixColorFilter {
        // Invert grayscale: black (0) becomes target phosphor color, white (255) becomes dark background
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
