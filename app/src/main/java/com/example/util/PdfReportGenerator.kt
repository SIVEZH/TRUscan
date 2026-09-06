package com.example.util

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.example.domain.model.OverallStatus
import com.example.domain.model.RuleResult
import com.example.domain.model.RuleStatus
import com.example.domain.model.ValidationReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object PdfReportGenerator {

    suspend fun generate(
        context: Context,
        report: ValidationReport,
        scanId: String,
        timestamp: String,
        productName: String? = null,
        imageUris: List<String> = emptyList()
    ): File = withContext(Dispatchers.IO) {
        val resolvedProductName = productName
            ?: report.results.find { it.field in listOf("common_or_generic_name", "name_of_commodity", "product_name") }?.extracted_value
            ?: "Packaged Commodity"

        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        val margin = 40f
        val contentWidth = pageWidth - (margin * 2)
        var y = margin

        // Paints
        val brandPaint = Paint().apply {
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(26, 86, 219) // TRUscan Blue
            isAntiAlias = true
        }
        val titlePaint = Paint().apply {
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(30, 41, 59) // Slate 800
            isAntiAlias = true
        }
        val subtitlePaint = Paint().apply {
            textSize = 9f
            color = Color.rgb(100, 116, 139) // Slate 500
            isAntiAlias = true
        }
        val sectionHeaderPaint = Paint().apply {
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42) // Slate 900
            isAntiAlias = true
        }
        val bodyBoldPaint = Paint().apply {
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(30, 41, 59)
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            textSize = 10f
            color = Color.rgb(51, 65, 85)
            isAntiAlias = true
        }
        val smallPaint = Paint().apply {
            textSize = 8.5f
            color = Color.rgb(100, 116, 139)
            isAntiAlias = true
        }
        val failPaint = Paint().apply {
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(220, 38, 38) // Red 600
            isAntiAlias = true
        }
        val passPaint = Paint().apply {
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(22, 163, 74) // Green 600
            isAntiAlias = true
        }
        val warningPaint = Paint().apply {
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(217, 119, 6) // Amber 600
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240) // Slate 200
            strokeWidth = 1f
        }

        fun drawFooter() {
            val footerY = pageHeight - 25f
            canvas.drawLine(margin, footerY - 10f, pageWidth - margin, footerY - 10f, linePaint)
            canvas.drawText("TRUscan Legal Metrology Compliance Inspection Record • Generated: $timestamp", margin, footerY, smallPaint)
            val pageStr = "Page $pageNumber"
            val pageStrWidth = smallPaint.measureText(pageStr)
            canvas.drawText(pageStr, pageWidth - margin - pageStrWidth, footerY, smallPaint)
        }

        fun checkPageBreak(requiredHeight: Float) {
            if (y + requiredHeight > pageHeight - 45f) {
                drawFooter()
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = margin
                // Mini header on continuation pages
                canvas.drawText("TRUscan Compliance Report (Continuation - $scanId)", margin, y, smallPaint)
                y += 15f
                canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
                y += 15f
            }
        }

        fun drawWrappedText(text: String, startX: Float, maxWidth: Float, paint: Paint, lineHeight: Float = 14f) {
            val words = text.split(" ")
            var currentLine = ""
            for (word in words) {
                val candidate = if (currentLine.isEmpty()) word else "$currentLine $word"
                if (paint.measureText(candidate) > maxWidth) {
                    checkPageBreak(lineHeight)
                    canvas.drawText(currentLine, startX, y, paint)
                    y += lineHeight
                    currentLine = word
                } else {
                    currentLine = candidate
                }
            }
            if (currentLine.isNotEmpty()) {
                checkPageBreak(lineHeight)
                canvas.drawText(currentLine, startX, y, paint)
                y += lineHeight
            }
        }

        // --- 1. HEADER ---
        canvas.drawText("TRUscan", margin, y + 16f, brandPaint)
        y += 24f
        canvas.drawText("LEGAL METROLOGY COMPLIANCE REPORT", margin, y + 12f, titlePaint)
        y += 18f
        canvas.drawText("The Legal Metrology (Packaged Commodities) Rules, 2011 • Department of Consumer Affairs", margin, y + 8f, subtitlePaint)
        y += 14f
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 16f

        // --- 2. REPORT & PRODUCT INFO ---
        val boxHeight = 65f
        val bgPaint = Paint().apply { color = Color.rgb(248, 250, 252) }
        val borderPaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(margin, y, pageWidth - margin, y + boxHeight, 6f, 6f, bgPaint)
        canvas.drawRoundRect(margin, y, pageWidth - margin, y + boxHeight, 6f, 6f, borderPaint)

        val col1X = margin + 12f
        val col2X = margin + (contentWidth / 2f) + 12f

        canvas.drawText("Report ID: ", col1X, y + 20f, bodyBoldPaint)
        canvas.drawText(scanId, col1X + bodyBoldPaint.measureText("Report ID: "), y + 20f, bodyPaint)

        canvas.drawText("Date & Time: ", col1X, y + 38f, bodyBoldPaint)
        canvas.drawText(timestamp, col1X + bodyBoldPaint.measureText("Date & Time: "), y + 38f, bodyPaint)

        canvas.drawText("Product: ", col2X, y + 20f, bodyBoldPaint)
        val prodDisplay = if (resolvedProductName.length > 25) resolvedProductName.take(22) + "..." else resolvedProductName
        canvas.drawText(prodDisplay, col2X + bodyBoldPaint.measureText("Product: "), y + 20f, bodyPaint)

        canvas.drawText("Category: ", col2X, y + 38f, bodyBoldPaint)
        canvas.drawText(report.category, col2X + bodyBoldPaint.measureText("Category: "), y + 38f, bodyPaint)

        y += boxHeight + 16f

        // --- 3. OVERALL COMPLIANCE RESULT ---
        val resultBannerHeight = 44f
        val (bannerBg, bannerBorder, bannerTitle, statusPaint) = when (report.overall_status) {
            OverallStatus.COMPLIANT -> Quad(
                Color.rgb(240, 253, 244),
                Color.rgb(187, 247, 208),
                "COMPLIANT — Meets all mandatory declaration requirements",
                passPaint
            )
            OverallStatus.NON_COMPLIANT -> Quad(
                Color.rgb(254, 242, 242),
                Color.rgb(254, 202, 202),
                "NON-COMPLIANT — Mandatory declaration violations detected",
                failPaint
            )
            OverallStatus.MANUAL_REVIEW -> Quad(
                Color.rgb(255, 251, 235),
                Color.rgb(253, 230, 138),
                "MANUAL REVIEW REQUIRED — Inspection needed for inconclusive fields",
                warningPaint
            )
        }

        val bannerBgPaint = Paint().apply { color = bannerBg }
        val bannerBorderPaint = Paint().apply {
            color = bannerBorder
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        canvas.drawRoundRect(margin, y, pageWidth - margin, y + resultBannerHeight, 6f, 6f, bannerBgPaint)
        canvas.drawRoundRect(margin, y, pageWidth - margin, y + resultBannerHeight, 6f, 6f, bannerBorderPaint)

        canvas.drawText("Overall Compliance Status:", margin + 12f, y + 18f, bodyBoldPaint)
        canvas.drawText(bannerTitle, margin + 12f, y + 34f, statusPaint)
        y += resultBannerHeight + 16f

        // --- 4. SUMMARY STATISTICS ---
        canvas.drawText("Evaluation Summary", margin, y + 10f, sectionHeaderPaint)
        y += 18f

        val summaryText = "Evaluated: ${report.summary.total_rules}  |  Passed: ${report.summary.passed}  |  " +
                "Violations: ${report.summary.total_violations}  |  Optional: ${report.summary.not_required}  |  " +
                "Manual Review: ${report.summary.manual_review}"
        canvas.drawText(summaryText, margin, y + 10f, bodyBoldPaint)
        y += 22f

        // --- 5. PRODUCT IMAGES ---
        if (imageUris.isNotEmpty()) {
            checkPageBreak(50f)
            canvas.drawText("Product Images (${imageUris.size})", margin, y + 10f, sectionHeaderPaint)
            y += 18f

            var imgIndex = 1
            for (uriStr in imageUris) {
                try {
                    val bitmap = loadScaledBitmap(context, uriStr, 180)
                    if (bitmap != null) {
                        val imgW = bitmap.width.toFloat()
                        val imgH = bitmap.height.toFloat()
                        checkPageBreak(imgH + 28f)

                        canvas.drawBitmap(bitmap, margin, y, null)
                        canvas.drawText("Image $imgIndex", margin, y + imgH + 14f, smallPaint)
                        y += imgH + 22f
                        bitmap.recycle()
                        imgIndex++
                    }
                } catch (e: Exception) {
                    // Continue with remaining images if one fails
                }
            }
        }

        // --- 6. VIOLATIONS SECTION (CONFIRMED MANDATORY ONLY) ---
        checkPageBreak(50f)
        val violations = report.violations
        if (violations.isNotEmpty()) {
            canvas.drawText("Confirmed Violations (${violations.size})", margin, y + 10f, failPaint)
            y += 14f
            canvas.drawText("The following legally mandatory declarations are missing or non-compliant:", margin, y + 8f, smallPaint)
            y += 16f

            violations.forEachIndexed { idx, rule ->
                checkPageBreak(56f)
                val cardTop = y
                val cardH = 50f
                val vBg = Paint().apply { color = Color.rgb(254, 242, 242) }
                val vBorder = Paint().apply {
                    color = Color.rgb(239, 68, 68)
                    style = Paint.Style.STROKE
                    strokeWidth = 1f
                }
                canvas.drawRoundRect(margin, cardTop, pageWidth - margin, cardTop + cardH, 4f, 4f, vBg)
                canvas.drawRoundRect(margin, cardTop, pageWidth - margin, cardTop + cardH, 4f, 4f, vBorder)

                canvas.drawText("${idx + 1}. Rule: ${rule.legal_reference} — ${rule.field}", margin + 10f, cardTop + 16f, failPaint)
                canvas.drawText("Requirement: ${rule.requirement}", margin + 10f, cardTop + 30f, bodyPaint)
                canvas.drawText("Finding: ${rule.message}", margin + 10f, cardTop + 44f, smallPaint)

                y += cardH + 8f
            }
            y += 10f
        } else {
            canvas.drawText("Confirmed Violations (0)", margin, y + 10f, passPaint)
            y += 16f
            canvas.drawText("No mandatory violations detected. All applicable mandatory declarations are present and compliant.", margin, y + 8f, bodyPaint)
            y += 22f
        }

        // --- 7. MANUAL REVIEW ITEMS (IF ANY) ---
        val manualItems = report.results.filter { it.status == RuleStatus.MANUAL_REVIEW }
        if (manualItems.isNotEmpty()) {
            checkPageBreak(40f)
            canvas.drawText("Manual Review Items (${manualItems.size})", margin, y + 10f, warningPaint)
            y += 16f
            manualItems.forEach { rule ->
                checkPageBreak(30f)
                canvas.drawText("• ${rule.legal_reference}: ${rule.field}", margin + 6f, y + 10f, bodyBoldPaint)
                y += 14f
                drawWrappedText("Finding: ${rule.message}", margin + 14f, contentWidth - 14f, smallPaint)
                y += 4f
            }
            y += 10f
        }

        // --- 8. ALL EVALUATED DECLARATIONS ---
        checkPageBreak(50f)
        canvas.drawText("All Evaluated Declarations (${report.results.size})", margin, y + 10f, sectionHeaderPaint)
        y += 18f

        report.results.forEach { rule ->
            checkPageBreak(34f)
            val p = when (rule.status) {
                RuleStatus.PASS -> passPaint
                RuleStatus.FAIL -> failPaint
                RuleStatus.MANUAL_REVIEW -> warningPaint
                else -> smallPaint
            }
            val tag = if (rule.mandatory) "[MANDATORY]" else "[OPTIONAL]"
            canvas.drawText("${rule.legal_reference} $tag  ${rule.status.name}", margin, y + 10f, p)
            y += 13f

            val extracted = rule.extracted_value?.let { "Detected: \"$it\"" } ?: "Detected: (None)"
            canvas.drawText("Field: ${rule.field}  |  $extracted", margin + 6f, y + 10f, bodyPaint)
            y += 15f
        }

        // Finish last page footer
        drawFooter()
        document.finishPage(page)

        val cleanScanId = scanId.take(8).replace(Regex("[^a-zA-Z0-9]"), "_")
        val cleanDate = timestamp.take(10).replace(Regex("[^a-zA-Z0-9]"), "-")
        val file = File(context.cacheDir, "TRUscan_Report_${cleanScanId}_${cleanDate}.pdf")
        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()

        file
    }

    private fun loadScaledBitmap(context: Context, uriString: String, maxDim: Int): Bitmap? {
        return try {
            val uri = Uri.parse(uriString)
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            val maxSide = maxOf(options.outWidth, options.outHeight)
            if (maxSide <= 0) return null
            var inSampleSize = 1
            while ((maxSide / inSampleSize) > maxDim) {
                inSampleSize *= 2
            }
            val decodeOptions = BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            }
        } catch (e: Exception) {
            null
        }
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
