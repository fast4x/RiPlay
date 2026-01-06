package it.fast4x.riplay.extensions.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import androidx.core.graphics.createBitmap

private fun createDummyBitmap(width: Int, height: Int, pageNumber: Int): Bitmap {
    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)

    canvas.drawColor(if (pageNumber % 2 == 0) Color.LTGRAY else Color.WHITE)

    val paint = Paint().apply {
        color = Color.BLACK
        textSize = 80f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    canvas.drawText("Page $pageNumber", width / 2f, height / 2f, paint)

    val borderPaint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.STROKE
        strokeWidth = 10f
    }
    canvas.drawRect(50f, 50f, (width - 50).toFloat(), (height - 50).toFloat(), borderPaint)

    return bitmap
}


fun generatePdfWithImages(context: Context): String {
    val pdfDocument = PdfDocument()

    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()

    try {
        for (i in 1..3) {
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val bitmap = createDummyBitmap(595, 842, i)

            canvas.drawBitmap(bitmap, 0f, 0f, null)

            pdfDocument.finishPage(page)

            bitmap.recycle()
        }

        val filePath = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "report_with_images.pdf")

        val outputStream = FileOutputStream(filePath)
        pdfDocument.writeTo(outputStream)
        outputStream.close()

        return filePath.absolutePath

    } catch (e: IOException) {
        e.printStackTrace()
        return "Error: ${e.message}"
    } finally {
        pdfDocument.close()
    }
}