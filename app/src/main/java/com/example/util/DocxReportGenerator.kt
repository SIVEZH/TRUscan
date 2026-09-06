package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.domain.model.OverallStatus
import com.example.domain.model.RuleResult
import com.example.domain.model.RuleStatus
import com.example.domain.model.ValidationReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object DocxReportGenerator {

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

        val cleanScanId = scanId.take(8).replace(Regex("[^a-zA-Z0-9]"), "_")
        val cleanDate = timestamp.take(10).replace(Regex("[^a-zA-Z0-9]"), "-")
        val file = File(context.cacheDir, "TRUscan_Report_${cleanScanId}_${cleanDate}.docx")

        // Prepare image bytes if available
        val embeddedImages = mutableListOf<ByteArray>()
        for (uriStr in imageUris) {
            try {
                val uri = Uri.parse(uriStr)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    if (bmp != null) {
                        val baos = ByteArrayOutputStream()
                        bmp.compress(Bitmap.CompressFormat.JPEG, 85, baos)
                        embeddedImages.add(baos.toByteArray())
                        bmp.recycle()
                    }
                }
            } catch (e: Exception) {
                // Continue if an image fails to load
            }
        }

        ZipOutputStream(FileOutputStream(file)).use { zos ->
            // 1. [Content_Types].xml
            zos.putNextEntry(ZipEntry("[Content_Types].xml"))
            zos.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Default Extension="jpeg" ContentType="image/jpeg"/>
  <Default Extension="jpg" ContentType="image/jpeg"/>
  <Default Extension="png" ContentType="image/png"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>""".toByteArray())
            zos.closeEntry()

            // 2. _rels/.rels
            zos.putNextEntry(ZipEntry("_rels/.rels"))
            zos.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>""".toByteArray())
            zos.closeEntry()

            // 3. word/_rels/document.xml.rels
            zos.putNextEntry(ZipEntry("word/_rels/document.xml.rels"))
            val relsSb = StringBuilder()
            relsSb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
            embeddedImages.forEachIndexed { idx, _ ->
                val rId = "rIdImg${idx + 1}"
                relsSb.append("""<Relationship Id="$rId" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="media/image${idx + 1}.jpeg"/>""")
            }
            relsSb.append("</Relationships>")
            zos.write(relsSb.toString().toByteArray())
            zos.closeEntry()

            // 4. Save embedded images to word/media/
            embeddedImages.forEachIndexed { idx, bytes ->
                zos.putNextEntry(ZipEntry("word/media/image${idx + 1}.jpeg"))
                zos.write(bytes)
                zos.closeEntry()
            }

            // 5. word/document.xml
            zos.putNextEntry(ZipEntry("word/document.xml"))
            val docSb = StringBuilder()
            docSb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
            xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
            xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing"
            xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
            xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture">
  <w:body>""")

            fun addHeading(text: String, size: Int = 28, color: String = "1E293B") {
                docSb.append("""
                    <w:p>
                        <w:pPr>
                            <w:spacing w:before="240" w:after="120"/>
                        </w:pPr>
                        <w:r>
                            <w:rPr>
                                <w:b/>
                                <w:sz w:val="$size"/>
                                <w:color w:val="$color"/>
                            </w:rPr>
                            <w:t>${text.escapeXml()}</w:t>
                        </w:r>
                    </w:p>
                """)
            }

            fun addPara(text: String, bold: Boolean = false, size: Int = 22, color: String = "334155") {
                val bTag = if (bold) "<w:b/>" else ""
                docSb.append("""
                    <w:p>
                        <w:pPr>
                            <w:spacing w:after="80"/>
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

            fun addKeyValue(label: String, value: String) {
                docSb.append("""
                    <w:p>
                        <w:pPr>
                            <w:spacing w:after="60"/>
                        </w:pPr>
                        <w:r>
                            <w:rPr>
                                <w:b/>
                                <w:sz w:val="22"/>
                                <w:color w:val="0F172A"/>
                            </w:rPr>
                            <w:t>${label.escapeXml()}: </w:t>
                        </w:r>
                        <w:r>
                            <w:rPr>
                                <w:sz w:val="22"/>
                                <w:color w:val="334155"/>
                            </w:rPr>
                            <w:t>${value.escapeXml()}</w:t>
                        </w:r>
                    </w:p>
                """)
            }

            fun addTable(headers: List<String>, rows: List<List<String>>) {
                docSb.append("""
                    <w:tbl>
                        <w:tblPr>
                            <w:tblW w:w="5000" w:type="pct"/>
                            <w:tblBorders>
                                <w:top w:val="single" w:sz="6" w:space="0" w:color="CBD5E1"/>
                                <w:left w:val="none"/>
                                <w:bottom w:val="single" w:sz="6" w:space="0" w:color="CBD5E1"/>
                                <w:right w:val="none"/>
                                <w:insideH w:val="single" w:sz="4" w:space="0" w:color="E2E8F0"/>
                                <w:insideV w:val="none"/>
                            </w:tblBorders>
                        </w:tblPr>
                        <w:tr>
                """)
                for (h in headers) {
                    docSb.append("""
                        <w:tc>
                            <w:tcPr>
                                <w:shd w:fill="F1F5F9"/>
                                <w:tcMar><w:top w:w="120"/><w:bottom w:w="120"/><w:left w:w="120"/><w:right w:w="120"/></w:tcMar>
                            </w:tcPr>
                            <w:p>
                                <w:r>
                                    <w:rPr><w:b/><w:sz w:val="20"/><w:color w:val="0F172A"/></w:rPr>
                                    <w:t>${h.escapeXml()}</w:t>
                                </w:r>
                            </w:p>
                        </w:tc>
                    """)
                }
                docSb.append("</w:tr>")

                for (row in rows) {
                    docSb.append("<w:tr>")
                    for (cell in row) {
                        docSb.append("""
                            <w:tc>
                                <w:tcPr>
                                    <w:tcMar><w:top w:w="100"/><w:bottom w:w="100"/><w:left w:w="120"/><w:right w:w="120"/></w:tcMar>
                                </w:tcPr>
                                <w:p>
                                    <w:r>
                                        <w:rPr><w:sz w:val="20"/><w:color w:val="334155"/></w:rPr>
                                        <w:t>${cell.escapeXml()}</w:t>
                                    </w:r>
                                </w:p>
                            </w:tc>
                        """)
                    }
                    docSb.append("</w:tr>")
                }
                docSb.append("</w:tbl>")
            }

            // --- TITLE ---
            addHeading("TRUscan Compliance Report", size = 36, color = "1A56DB")
            addPara("Legal Metrology (Packaged Commodities) Rules, 2011 • Official Record", size = 18, color = "64748B")

            // --- 1. REPORT INFORMATION ---
            addHeading("1. Report Information")
            addKeyValue("Report ID", scanId)
            addKeyValue("Date & Time", timestamp)

            // --- 2. PRODUCT INFORMATION ---
            addHeading("2. Product Information")
            addKeyValue("Product Name", resolvedProductName)
            addKeyValue("Category", report.category)

            // --- 3. OVERALL COMPLIANCE RESULT ---
            addHeading("3. Overall Compliance Result")
            val (statusText, statusColor) = when (report.overall_status) {
                OverallStatus.COMPLIANT -> Pair("COMPLIANT — Meets all mandatory declarations", "16A34A")
                OverallStatus.NON_COMPLIANT -> Pair("NON-COMPLIANT — Mandatory violations detected", "DC2626")
                OverallStatus.MANUAL_REVIEW -> Pair("MANUAL REVIEW REQUIRED — Inspection needed", "D97706")
            }
            addPara(statusText, bold = true, size = 26, color = statusColor)

            // --- 4. PRODUCT IMAGES ---
            addHeading("4. Product Images")
            if (embeddedImages.isNotEmpty()) {
                addPara("Total images attached: ${embeddedImages.size}")
                embeddedImages.forEachIndexed { idx, _ ->
                    val rId = "rIdImg${idx + 1}"
                    addPara("Image ${idx + 1}:", bold = true, size = 20)
                    docSb.append("""
                        <w:p>
                            <w:r>
                                <w:drawing>
                                    <wp:inline distT="0" distB="0" distL="0" distR="0">
                                        <wp:extent cx="2800000" cy="2100000"/>
                                        <wp:docPr id="${idx + 1}" name="Product Image ${idx + 1}"/>
                                        <a:graphic>
                                            <a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture">
                                                <pic:pic>
                                                    <pic:nvPicPr>
                                                        <pic:cNvPr id="${idx + 1}" name="Image ${idx + 1}"/>
                                                        <pic:cNvPicPr/>
                                                    </pic:nvPicPr>
                                                    <pic:blipFill>
                                                        <a:blip r:embed="$rId"/>
                                                        <a:stretch><a:fillRect/></a:stretch>
                                                    </pic:blipFill>
                                                    <pic:spPr>
                                                        <a:xfrm><a:off x="0" y="0"/><a:ext cx="2800000" cy="2100000"/></a:xfrm>
                                                        <a:prstGeom prst="rect"><a:avLst/></a:prstGeom>
                                                    </pic:spPr>
                                                </pic:pic>
                                            </a:graphicData>
                                        </a:graphic>
                                    </wp:inline>
                                </w:drawing>
                            </w:r>
                        </w:p>
                    """)
                }
            } else {
                addPara("No product images attached.")
            }

            // --- 5. EXTRACTED DECLARATIONS ---
            addHeading("5. Extracted Declarations")
            val declHeaders = listOf("Declaration Field", "Extracted Value", "Status")
            val declRows = report.results.map { rule ->
                listOf(
                    rule.field,
                    rule.extracted_value ?: "(Not detected)",
                    rule.status.name
                )
            }
            addTable(declHeaders, declRows)

            // --- 6. RULE EVALUATION ---
            addHeading("6. Rule Evaluation")
            val evalHeaders = listOf("Rule / Reference", "Classification", "Status", "Finding")
            val evalRows = report.results.map { rule ->
                listOf(
                    "${rule.field}\n(${rule.legal_reference})",
                    if (rule.mandatory) "MANDATORY" else "OPTIONAL",
                    rule.status.name,
                    rule.message
                )
            }
            addTable(evalHeaders, evalRows)

            // --- 7. VIOLATIONS (CONFIRMED MANDATORY ONLY) ---
            addHeading("7. Violations", color = "DC2626")
            val violations = report.violations
            if (violations.isNotEmpty()) {
                addPara("The following mandatory declarations were missing or non-compliant:", bold = true, color = "DC2626")
                violations.forEachIndexed { idx, rule ->
                    addPara("${idx + 1}. ${rule.field} — Mandatory declaration missing / non-compliant.", bold = true, color = "DC2626")
                    addKeyValue("Legal Reference", rule.legal_reference)
                    addKeyValue("Requirement", rule.requirement)
                    addKeyValue("Detected Value", rule.extracted_value ?: "None")
                    addKeyValue("Reason", rule.message)
                }
            } else {
                addPara("No mandatory violations detected. Product satisfies all evaluated mandatory declaration requirements.", color = "16A34A")
            }

            // --- 8. MANUAL REVIEW ---
            val manualReviewItems = report.results.filter { it.status == RuleStatus.MANUAL_REVIEW }
            if (manualReviewItems.isNotEmpty()) {
                addHeading("8. Manual Review", color = "D97706")
                addPara("The following items require physical or visual manual verification:", bold = true)
                manualReviewItems.forEach { rule ->
                    addKeyValue("Field", "${rule.field} (${rule.legal_reference})")
                    addKeyValue("Issue", rule.message)
                }
            } else {
                addHeading("8. Manual Review")
                addPara("No manual review items required.")
            }

            // --- 9. SUMMARY ---
            addHeading("9. Summary")
            addKeyValue("Total Rules Evaluated", report.summary.total_rules.toString())
            addKeyValue("Passed", report.summary.passed.toString())
            addKeyValue("Confirmed Violations", report.summary.total_violations.toString())
            addKeyValue("Optional (Not Required)", report.summary.not_required.toString())
            addKeyValue("Not Applicable", report.summary.not_applicable.toString())
            addKeyValue("Manual Review", report.summary.manual_review.toString())

            // --- 10. REPORT GENERATION INFORMATION ---
            addHeading("10. Report Generation Information", size = 24, color = "64748B")
            addPara("Generated by TRUscan Compliance Engine", bold = true, size = 20, color = "64748B")
            addPara("This document is an automated compliance inspection record generated in accordance with the Legal Metrology (Packaged Commodities) Rules, 2011.", size = 18, color = "94A3B8")

            docSb.append("""</w:body></w:document>""")
            zos.write(docSb.toString().toByteArray())
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
