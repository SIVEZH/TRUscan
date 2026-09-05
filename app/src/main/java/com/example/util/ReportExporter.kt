package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.domain.model.OverallStatus
import com.example.domain.model.RuleResult
import com.example.domain.model.RuleStatus
import com.example.domain.model.ValidationReport
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ReportExporter {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    suspend fun exportJson(context: Context, report: ValidationReport, scanId: String): File = withContext(Dispatchers.IO) {
        val json = moshi.adapter(ValidationReport::class.java).indent("  ").toJson(report)
        val file = File(context.cacheDir, "TRUscan_Report_${scanId}.json")
        file.writeText(json)
        file
    }

    suspend fun exportPdf(context: Context, report: ValidationReport, scanId: String, timestamp: String): File = withContext(Dispatchers.IO) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        val titlePaint = Paint().apply {
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.BLACK
        }
        val headerPaint = Paint().apply {
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.DKGRAY
        }
        val textPaint = Paint().apply {
            textSize = 12f
            color = Color.BLACK
        }
        val failPaint = Paint().apply {
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.RED
        }
        val passPaint = Paint().apply {
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(0, 150, 0)
        }

        var y = 50f
        val margin = 50f
        val lineHeight = 20f

        fun checkPageBreak() {
            if (y > 800f) {
                document.finishPage(page)
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = 50f
            }
        }

        fun drawText(text: String, paint: Paint) {
            val words = text.split(" ")
            var line = ""
            for (word in words) {
                val testLine = if (line.isEmpty()) word else "$line $word"
                if (paint.measureText(testLine) > (595f - margin * 2)) {
                    canvas.drawText(line, margin, y, paint)
                    y += lineHeight
                    checkPageBreak()
                    line = word
                } else {
                    line = testLine
                }
            }
            if (line.isNotEmpty()) {
                canvas.drawText(line, margin, y, paint)
                y += lineHeight
                checkPageBreak()
            }
        }

        drawText("TRUscan LEGAL METROLOGY COMPLIANCE REPORT", titlePaint)
        y += lineHeight

        drawText("Report Information", headerPaint)
        drawText("Scan ID: $scanId", textPaint)
        drawText("Date: $timestamp", textPaint)
        drawText("Category: ${report.category}", textPaint)
        y += lineHeight

        drawText("Overall Result", headerPaint)
        val statusPaint = when (report.overall_status) {
            OverallStatus.COMPLIANT -> passPaint
            OverallStatus.NON_COMPLIANT -> failPaint
            OverallStatus.MANUAL_REVIEW -> Paint(failPaint).apply { color = Color.rgb(200, 100, 0) }
        }
        drawText(report.overall_status.name.replace("_", " "), statusPaint)
        y += lineHeight

        drawText("Summary", headerPaint)
        drawText("Total rules checked: ${report.summary.total_rules}", textPaint)
        drawText("Passed: ${report.summary.passed}", textPaint)
        drawText("Failed: ${report.summary.failed}", textPaint)
        drawText("Not Applicable: ${report.summary.not_applicable}", textPaint)
        drawText("Manual Review: ${report.summary.manual_review}", textPaint)
        y += lineHeight

        val failedRules = report.results.filter { it.status == RuleStatus.FAIL }
        if (failedRules.isNotEmpty()) {
            drawText("Violations", headerPaint)
            failedRules.forEach { rule ->
                drawText("Rule: ${rule.legal_reference} - ${rule.field}", failPaint)
                drawText("Requirement: ${rule.requirement}", textPaint)
                drawText("Detected: ${rule.extracted_value ?: "None"}", textPaint)
                drawText("Reason: ${rule.message}", textPaint)
                y += lineHeight
            }
        }

        drawText("Extracted Declarations & Rule Validation", headerPaint)
        report.results.forEach { rule ->
            val p = when (rule.status) {
                RuleStatus.PASS -> passPaint
                RuleStatus.FAIL -> failPaint
                else -> textPaint
            }
            drawText("${rule.legal_reference} [${rule.status.name}]", p)
            drawText("Field: ${rule.field}", textPaint)
            if (rule.extracted_value != null) {
                drawText("Detected: ${rule.extracted_value}", textPaint)
            }
            y += lineHeight
        }

        document.finishPage(page)

        val file = File(context.cacheDir, "TRUscan_Report_${scanId}.pdf")
        val out = FileOutputStream(file)
        document.writeTo(out)
        document.close()
        out.close()
        
        file
    }

    suspend fun exportDocx(context: Context, report: ValidationReport, scanId: String, timestamp: String): File = withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, "TRUscan_Report_${scanId}.docx")
        
        ZipOutputStream(FileOutputStream(file)).use { zos ->
            zos.putNextEntry(ZipEntry("[Content_Types].xml"))
            zos.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>""".toByteArray())
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("_rels/.rels"))
            zos.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>""".toByteArray())
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("word/document.xml"))
            
            val sb = StringBuilder()
            sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:body>""")

            fun addPara(text: String, bold: Boolean = false, size: Int = 24, color: String = "000000") {
                val bTag = if (bold) "<w:b/>" else ""
                sb.append("""
                    <w:p>
                        <w:pPr>
                            <w:spacing w:after="120"/>
                        </w:pPr>
                        <w:r>
                            <w:rPr>
                                $bTag
                                <w:sz w:val="$size"/>
                                <w:color w:val="$color"/>
                            </w:rPr>
                            <w:t>${text.escapeXml()}</w:t>
                        </w:r>
                    </w:p>
                """)
            }

            addPara("TRUscan LEGAL METROLOGY COMPLIANCE REPORT", bold = true, size = 32)
            
            addPara("Report Information", bold = true, size = 28)
            addPara("Scan ID: $scanId")
            addPara("Date: $timestamp")
            addPara("Category: ${report.category}")
            
            addPara("Overall Result", bold = true, size = 28)
            val statusColor = when (report.overall_status) {
                OverallStatus.COMPLIANT -> "009600"
                OverallStatus.NON_COMPLIANT -> "FF0000"
                OverallStatus.MANUAL_REVIEW -> "CC6600"
            }
            addPara(report.overall_status.name.replace("_", " "), bold = true, size = 28, color = statusColor)
            
            addPara("Summary", bold = true, size = 28)
            addPara("Total rules checked: ${report.summary.total_rules}")
            addPara("Passed: ${report.summary.passed}")
            addPara("Failed: ${report.summary.failed}")
            
            val failedRules = report.results.filter { it.status == RuleStatus.FAIL }
            if (failedRules.isNotEmpty()) {
                addPara("Violations", bold = true, size = 28, color = "FF0000")
                failedRules.forEach { rule ->
                    addPara("Rule: ${rule.legal_reference} - ${rule.field}", bold = true, color = "FF0000")
                    addPara("Requirement: ${rule.requirement}")
                    addPara("Detected: ${rule.extracted_value ?: "None"}")
                    addPara("Reason: ${rule.message}")
                }
            }

            addPara("Extracted Declarations & Rule Validation", bold = true, size = 28)
            report.results.forEach { rule ->
                val c = when(rule.status) {
                    RuleStatus.PASS -> "009600"
                    RuleStatus.FAIL -> "FF0000"
                    else -> "000000"
                }
                addPara("${rule.legal_reference} [${rule.status.name}]", bold = true, color = c)
                addPara("Field: ${rule.field}")
                if (rule.extracted_value != null) addPara("Detected: ${rule.extracted_value}")
                addPara("Requirement: ${rule.requirement}")
            }
            
            sb.append("""</w:body></w:document>""")
            zos.write(sb.toString().toByteArray())
            zos.closeEntry()
        }
        
        file
    }
    
    private fun String.escapeXml(): String {
        return this.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
