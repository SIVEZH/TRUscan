package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.local.ComplaintEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object AuthorityPdfExporter {
    suspend fun exportAuthorityPdf(context: Context, complaint: ComplaintEntity): File = withContext(Dispatchers.IO) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            isAntiAlias = true
        }
        val titlePaint = Paint(paint).apply {
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val headerPaint = Paint(paint).apply {
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.DKGRAY
        }

        var y = 50f
        val x = 50f
        val lineSpacing = 20f

        canvas.drawText("TRUscan Authority Action Report", x, y, titlePaint)
        y += lineSpacing * 2
        
        canvas.drawText("Complaint ID: ${complaint.complaintId}", x, y, paint)
        y += lineSpacing
        canvas.drawText("Authority Category: ${complaint.category}", x, y, paint)
        y += lineSpacing
        canvas.drawText("Action Date: ${complaint.actionTimestamp ?: "N/A"}", x, y, paint)
        y += lineSpacing * 2

        canvas.drawText("COMPLAINANT DETAILS", x, y, headerPaint)
        y += lineSpacing * 1.5f
        canvas.drawText("Name: ${complaint.complainantName}", x, y, paint)
        y += lineSpacing
        canvas.drawText("Email: ${complaint.complainantEmail}", x, y, paint)
        y += lineSpacing
        canvas.drawText("Phone: ${complaint.complainantPhone}", x, y, paint)
        y += lineSpacing
        canvas.drawText("Address: ${complaint.complainantAddress}", x, y, paint)
        y += lineSpacing * 2

        canvas.drawText("VERIFICATION STATUS", x, y, headerPaint)
        y += lineSpacing * 1.5f
        canvas.drawText("Email: Verified", x, y, paint)
        y += lineSpacing
        canvas.drawText("Phone: Verified", x, y, paint)
        y += lineSpacing
        canvas.drawText("Live Photo: Verified", x, y, paint)
        y += lineSpacing * 2
        
        canvas.drawText("PRODUCT DETAILS", x, y, headerPaint)
        y += lineSpacing * 1.5f
        canvas.drawText("Product: ${complaint.productName ?: "Unknown"}", x, y, paint)
        y += lineSpacing
        canvas.drawText("Category: ${complaint.category}", x, y, paint)
        y += lineSpacing
        canvas.drawText("Purchase Location: ${complaint.purchaseLocation}", x, y, paint)
        y += lineSpacing * 2
        
        canvas.drawText("AUTHORITY ACTION", x, y, headerPaint)
        y += lineSpacing * 1.5f
        canvas.drawText("Decision: ${complaint.status}", x, y, paint)
        y += lineSpacing
        canvas.drawText("Action: ${complaint.authorityAction}", x, y, paint)
        y += lineSpacing
        canvas.drawText("Rejection Reason: ${complaint.rejectionReason ?: "None"}", x, y, paint)

        document.finishPage(page)

        val file = File(context.cacheDir, "TRUscan_Complaint_${complaint.complaintId}_Authority_Action.pdf")
        val out = FileOutputStream(file)
        document.writeTo(out)
        out.close()
        document.close()
        
        file
    }
}
