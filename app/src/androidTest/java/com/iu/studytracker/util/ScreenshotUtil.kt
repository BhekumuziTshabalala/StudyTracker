package com.iu.studytracker.util

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import java.io.File
import java.io.FileOutputStream

object ScreenshotUtil {
    fun takeScreenshot(context: Context, node: SemanticsNodeInteraction, fileName: String) {
        val bitmap = node.captureToImage().asAndroidBitmap()
        
        // Save to the public Download directory so files survive Gradle app uninstallation
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "StudyTrackerScreenshots")
        if (dir != null && !dir.exists()) {
            dir.mkdirs()
        }

        val file = File(dir, "$fileName.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        
        println("Screenshot saved to: ${file.absolutePath}")
    }
}
