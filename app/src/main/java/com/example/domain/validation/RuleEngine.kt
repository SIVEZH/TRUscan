package com.example.domain.validation

import com.example.domain.model.*

class ProductInformationMerger {
    fun merge(extractions: List<RawExtraction>): Map<String, ExtractedField> {
        val merged = mutableMapOf<String, ExtractedField>()
        val grouped = extractions.groupBy { it.field }
        for ((field, list) in grouped) {
            if (list.isEmpty()) continue
            val allPresent = list.filter { it.present }
            if (allPresent.isEmpty()) {
                merged[field] = ExtractedField(present = false)
                continue
            }
            val validValues = allPresent.mapNotNull { it.value }.filter { it.isNotBlank() }.distinct()
            val sources = allPresent.mapNotNull { it.source_image }.distinct()
            val sourceText = allPresent.mapNotNull { it.source_text }.joinToString(" | ")
            val avgConfidence = allPresent.mapNotNull { it.confidence }.average().takeIf { !it.isNaN() }
            val anyUncertain = allPresent.any { it.uncertain == true }
            
            if (validValues.size > 1) {
                merged[field] = ExtractedField(
                    present = true,
                    value = null,
                    conflicting_values = validValues,
                    sources = sources,
                    source_text = sourceText,
                    confidence = avgConfidence,
                    uncertain = true
                )
            } else {
                merged[field] = ExtractedField(
                    present = true,
                    value = validValues.firstOrNull(),
                    sources = sources,
                    source_text = sourceText,
                    confidence = avgConfidence,
                    uncertain = anyUncertain
                )
            }
        }
        return merged
    }
}

class PackageClassificationEngine {
    fun determineContext(extraction: GeminiExtraction): PackageClassification {
        val pkg = extraction.package_classification ?: PackageClassification(
            is_food = extraction.category == "FOOD",
            is_prepackaged = true,
            is_retail = true
        )
        return pkg.copy(category = pkg.category ?: extraction.category)
    }
}

class RuleApplicabilityEngine {
    fun determineApplicability(rule: Rule, pkgCtx: PackageClassification): Boolean {
        if (rule.applicability.equals("NOT_APPLICABLE", ignoreCase = true)) return false
        
        val ruleCategory = rule.category?.uppercase()
        val pkgCategory = pkgCtx.category?.uppercase()
        
        if (ruleCategory != null && ruleCategory != "GENERAL" && ruleCategory != "ALL") {
            if (pkgCategory != ruleCategory) return false
        }
        
        val appliesToCategory = rule.applies_to?.category?.uppercase()
        if (appliesToCategory != null && appliesToCategory != "ALL") {
            if (pkgCategory != appliesToCategory) return false
        }
        
        val appliesToProducts = rule.applies_to?.product_types?.map { it.uppercase() }
        val pkgProductType = pkgCtx.product_type?.uppercase()
        if (appliesToProducts != null && appliesToProducts.isNotEmpty()) {
            if (pkgProductType == null || pkgProductType !in appliesToProducts) return false
        }
        
        val req = rule.requirement?.uppercase() ?: ""
        if (req == "MANDATORY_IF_IMPORTED" && pkgCtx.is_imported == false) return false
        
        return true
    }
}

class RuleEngine {
    val classificationEngine = PackageClassificationEngine()
    val applicabilityEngine = RuleApplicabilityEngine()
    val selectionEngine = RuleSelectionEngine(applicabilityEngine)
    val complianceEngine = ComplianceRuleEngine()
    val merger = ProductInformationMerger()

    fun evaluate(ruleSet: RuleSet, extraction: GeminiExtraction): ValidationReport {
        val processedExtraction = if (extraction.extractions != null) {
            val mergedFields = merger.merge(extraction.extractions)
            extraction.copy(fields = mergedFields)
        } else {
            extraction
        }
        val pkgCtx = classificationEngine.determineContext(processedExtraction)
        
        val selectedRuleSet = selectionEngine.selectRules(ruleSet, pkgCtx)
        
        val results = mutableListOf<RuleResult>()
        val excludedRuleIds = selectedRuleSet.excludedRules.map { it.ruleId }.toSet()
        
        for (rule in selectedRuleSet.selectedRules) {
            if (rule.rule_id in excludedRuleIds) {
                throw IllegalStateException("RuleEngineConfigurationException: An excluded rule (${rule.rule_id}) entered compliance evaluation.")
            }
            
            val extractedField = processedExtraction.fields[rule.field]
            val result = complianceEngine.evaluateRule(rule, extractedField, processedExtraction.fields)
            results.add(result)
        }

        val confirmedMandatoryFailures = results.count {
            it.status == RuleStatus.FAIL && it.mandatory && it.applicable
        }

        val mandatoryManualReviews = results.count {
            it.status == RuleStatus.MANUAL_REVIEW && it.mandatory && it.applicable
        }

        val overallStatus = when {
            confirmedMandatoryFailures > 0 -> OverallStatus.NON_COMPLIANT
            mandatoryManualReviews > 0 -> OverallStatus.MANUAL_REVIEW
            else -> OverallStatus.COMPLIANT
        }

        val summary = ValidationSummary(
            total_rules = results.size,
            passed = results.count { it.status == RuleStatus.PASS },
            failed = results.count { it.status == RuleStatus.FAIL },
            not_applicable = results.count { it.status == RuleStatus.NOT_APPLICABLE },
            manual_review = results.count { it.status == RuleStatus.MANUAL_REVIEW },
            not_mandatory = results.count { it.status == RuleStatus.NOT_MANDATORY },
            not_required = results.count { it.status == RuleStatus.NOT_REQUIRED },
            total_violations = confirmedMandatoryFailures,
            excluded_rules = selectedRuleSet.excludedRules.size
        )

        return ValidationReport(
            category = ruleSet.categoryName,
            overall_status = overallStatus,
            summary = summary,
            results = results
        )
    }
}
