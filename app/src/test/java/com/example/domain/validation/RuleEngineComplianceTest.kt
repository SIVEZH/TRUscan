package com.example.domain.validation

import com.example.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class RuleEngineComplianceTest {

    private val ruleEngine = RuleEngine()

    private fun createTestRule(
        id: String,
        ref: String,
        field: String,
        requirement: String = "MANDATORY",
        condition: String = "NONE",
        desc: String = "Description",
        mandatory: Boolean? = true,
        applicability: String? = "APPLICABLE"
    ) = Rule(
        rule_id = id,
        legal_reference = ref,
        category = "FOOD",
        field = field,
        requirement = requirement,
        condition = condition,
        description = desc,
        validation_type = "PRESENCE",
        mandatory = mandatory,
        applicability = applicability
    )

    private fun createTestRuleSet(rules: List<Rule>) = RuleSet(
        schema_version = "2.0",
        dataset_type = "RULES",
        category = "FOOD",
        title = "Food Rules",
        scope = "Packaged Food",
        source_note = "Legal Metrology Rules 2011",
        rules = rules
    )

    private fun createTestExtraction(fields: Map<String, ExtractedField>) = GeminiExtraction(
        category = "FOOD",
        category_confidence = 0.95,
        fields = fields
    )

    @Test
    fun testCase1_MandatoryDetectedCorrectly_ProducesPASS() {
        val rule = createTestRule(
            id = "LMPC_6_1_B",
            ref = "Rule 6(1)(b)",
            field = "common_or_generic_name",
            mandatory = true,
            applicability = "APPLICABLE"
        )
        val ruleSet = createTestRuleSet(listOf(rule))
        val extraction = createTestExtraction(
            mapOf(
                "common_or_generic_name" to ExtractedField(
                    present = true,
                    value = "Organic Rolled Oats",
                    confidence = 0.95
                )
            )
        )

        val report = ruleEngine.evaluate(ruleSet, extraction)
        val result = report.results.first()

        assertEquals(RuleStatus.PASS, result.status)
        assertTrue(result.mandatory)
        assertFalse(result.violation)
        assertEquals(OverallStatus.COMPLIANT, report.overall_status)
    }

    @Test
    fun testCase2_MandatoryConfirmedMissing_ProducesFAIL() {
        val rule = createTestRule(
            id = "LMPC_6_1_C",
            ref = "Rule 6(1)(c)",
            field = "net_quantity",
            mandatory = true,
            applicability = "APPLICABLE"
        )
        val ruleSet = createTestRuleSet(listOf(rule))
        val extraction = createTestExtraction(
            mapOf(
                "net_quantity" to ExtractedField(
                    present = false,
                    value = null,
                    confidence = 0.0
                )
            )
        )

        val report = ruleEngine.evaluate(ruleSet, extraction)
        val result = report.results.first()

        assertEquals(RuleStatus.FAIL, result.status)
        assertTrue(result.mandatory)
        assertTrue(result.violation)
        assertEquals(OverallStatus.NON_COMPLIANT, report.overall_status)
    }

    @Test
    fun testCase3_MandatoryUncertainOrUnreadable_ProducesMANUAL_REVIEW() {
        val rule = createTestRule(
            id = "LMPC_6_1_AA",
            ref = "Rule 6(1)(aa)",
            field = "country_of_origin",
            requirement = "MANDATORY_IF_IMPORTED",
            mandatory = true,
            applicability = "APPLICABLE"
        )
        val ruleSet = createTestRuleSet(listOf(rule))
        val extraction = createTestExtraction(
            mapOf(
                "country_of_origin" to ExtractedField(
                    present = true,
                    value = "Unreadable text",
                    status = "UNREADABLE",
                    confidence = 0.2
                )
            )
        )

        val report = ruleEngine.evaluate(ruleSet, extraction)
        val result = report.results.first()

        assertEquals(RuleStatus.MANUAL_REVIEW, result.status)
        assertTrue(result.mandatory)
        assertFalse(result.violation)
        assertEquals(OverallStatus.MANUAL_REVIEW, report.overall_status)
    }

    @Test
    fun testCase4_NotMandatoryDetected_ProducesNOT_MANDATORY() {
        val rule = createTestRule(
            id = "LMPC_6_11",
            ref = "Rule 6(11)",
            field = "unit_sale_price",
            requirement = "MANDATORY_IF_APPLICABLE",
            mandatory = false,
            applicability = "APPLICABLE"
        )
        val ruleSet = createTestRuleSet(listOf(rule))
        val extraction = createTestExtraction(
            mapOf(
                "unit_sale_price" to ExtractedField(
                    present = true,
                    value = "Rs. 0.50 / g",
                    confidence = 0.90
                )
            )
        )

        val report = ruleEngine.evaluate(ruleSet, extraction)
        val result = report.results.first()

        assertEquals(RuleStatus.NOT_MANDATORY, result.status)
        assertFalse(result.mandatory)
        assertFalse(result.violation)
        // Not mandatory must never cause non-compliant
        assertEquals(OverallStatus.COMPLIANT, report.overall_status)
    }

    @Test
    fun testCase5_NotMandatoryNotDetected_ProducesNOT_MANDATORY_NeverManualReview() {
        val rule = createTestRule(
            id = "LMPC_6_1_F",
            ref = "Rule 6(1)(f)",
            field = "dimensions",
            requirement = "MANDATORY_IF_APPLICABLE",
            mandatory = false,
            applicability = "APPLICABLE"
        )
        val ruleSet = createTestRuleSet(listOf(rule))
        // AI extraction: missing, blank, or even unreadable
        val extraction = createTestExtraction(
            mapOf(
                "dimensions" to ExtractedField(
                    present = false,
                    value = null,
                    status = "UNREADABLE",
                    confidence = 0.0
                )
            )
        )

        val report = ruleEngine.evaluate(ruleSet, extraction)
        val result = report.results.first()

        // Critical requirement: NOT_MANDATORY must NEVER become MANUAL_REVIEW
        assertEquals(RuleStatus.NOT_MANDATORY, result.status)
        assertNotEquals(RuleStatus.MANUAL_REVIEW, result.status)
        assertNotEquals(RuleStatus.FAIL, result.status)
        assertFalse(result.mandatory)
        assertFalse(result.violation)
        assertEquals(OverallStatus.COMPLIANT, report.overall_status)
    }

    @Test
    fun testCase6_NotApplicableNotDetected_ProducesNOT_APPLICABLE() {
        val rule = createTestRule(
            id = "LMPC_26_A",
            ref = "Rule 26(a)",
            field = "small_package_exemption",
            requirement = "EXEMPTION",
            mandatory = false,
            applicability = "NOT_APPLICABLE"
        )
        val ruleSet = createTestRuleSet(listOf(rule))
        val extraction = createTestExtraction(emptyMap())

        val report = ruleEngine.evaluate(ruleSet, extraction)
        val result = report.results.first()

        assertEquals(RuleStatus.NOT_APPLICABLE, result.status)
        assertFalse(result.mandatory)
        assertFalse(result.violation)
        assertEquals(OverallStatus.COMPLIANT, report.overall_status)
    }

    @Test
    fun testCase7_NotApplicableDetected_ProducesNOT_APPLICABLE_NoViolation() {
        val rule = createTestRule(
            id = "LMPC_6_10",
            ref = "Rule 6(10)",
            field = "ecommerce_mandatory_declarations",
            requirement = "MANDATORY_IF_ECOMMERCE",
            mandatory = false,
            applicability = "NOT_APPLICABLE"
        )
        val ruleSet = createTestRuleSet(listOf(rule))
        val extraction = createTestExtraction(
            mapOf(
                "ecommerce_mandatory_declarations" to ExtractedField(
                    present = true,
                    value = "Some e-commerce data",
                    confidence = 0.8
                )
            )
        )

        val report = ruleEngine.evaluate(ruleSet, extraction)
        val result = report.results.first()

        assertEquals(RuleStatus.NOT_APPLICABLE, result.status)
        assertFalse(result.mandatory)
        assertFalse(result.violation)
        assertEquals(OverallStatus.COMPLIANT, report.overall_status)
    }

    @Test
    fun testOverallCompliance_OnlyMandatoryFailuresCauseNON_COMPLIANT() {
        val mandatoryPass = createTestRule(
            id = "R1",
            ref = "Rule 6(1)(a)",
            field = "mfg",
            mandatory = true,
            applicability = "APPLICABLE"
        )
        val nonMandatoryMissing = createTestRule(
            id = "R2",
            ref = "Rule 6(1)(f)",
            field = "dimensions",
            requirement = "MANDATORY_IF_APPLICABLE",
            mandatory = false,
            applicability = "APPLICABLE"
        )
        val notApplicable = createTestRule(
            id = "R3",
            ref = "Rule 26",
            field = "small_pkg",
            requirement = "EXEMPTION",
            mandatory = false,
            applicability = "NOT_APPLICABLE"
        )

        val ruleSet = createTestRuleSet(listOf(mandatoryPass, nonMandatoryMissing, notApplicable))
        val extraction = createTestExtraction(
            mapOf(
                "mfg" to ExtractedField(present = true, value = "Acme Corp", confidence = 0.9)
            )
        )

        val report = ruleEngine.evaluate(ruleSet, extraction)

        assertEquals(1, report.summary.passed)
        assertEquals(1, report.summary.not_mandatory)
        assertEquals(1, report.summary.not_applicable)
        assertEquals(0, report.summary.total_violations)
        // NON_COMPLIANT must NEVER be caused by not mandatory or not applicable!
        assertEquals(OverallStatus.COMPLIANT, report.overall_status)
        assertFalse(report.canRaiseComplaint)
        assertFalse(report.hasConfirmedMandatoryViolation)
    }

    @Test
    fun testComplaintCase1_AllMandatoryPassed_SomeNotMandatoryAbsent_RaiseComplaintUnavailable() {
        // Test Case 1: All mandatory passed, some not-mandatory absent
        // 15 mandatory PASS, 9 not-mandatory absent, 6 not-required absent
        val rules = mutableListOf<Rule>()
        val fields = mutableMapOf<String, ExtractedField>()

        for (i in 1..15) {
            val fieldName = "mand_field_$i"
            rules.add(createTestRule("M$i", "Rule M$i", fieldName, mandatory = true, applicability = "APPLICABLE"))
            fields[fieldName] = ExtractedField(present = true, value = "Value $i", confidence = 0.95)
        }
        for (i in 1..9) {
            val fieldName = "not_mand_field_$i"
            rules.add(createTestRule("NM$i", "Rule NM$i", fieldName, mandatory = false, applicability = "APPLICABLE"))
            // absent
        }
        for (i in 1..6) {
            val fieldName = "not_req_field_$i"
            rules.add(createTestRule("NR$i", "Rule NR$i", fieldName, mandatory = false, applicability = "NOT_REQUIRED", condition = "CONDITION_UNMET"))
            // absent
        }

        val ruleSet = createTestRuleSet(rules)
        val extraction = createTestExtraction(fields)
        val report = ruleEngine.evaluate(ruleSet, extraction)

        assertEquals(30, report.summary.total_rules)
        assertEquals(15, report.summary.passed)
        assertEquals(9, report.summary.not_mandatory)
        assertEquals(6, report.summary.not_required)
        assertEquals(0, report.summary.total_violations)
        assertEquals(OverallStatus.COMPLIANT, report.overall_status)

        // User requirement: Raise Complaint MUST NOT be available
        assertFalse(report.hasConfirmedMandatoryViolation)
        assertFalse(report.canRaiseComplaint)
    }

    @Test
    fun testComplaintCase2_OneMandatoryMissing_RaiseComplaintAvailable() {
        // Test Case 2: One mandatory missing
        // 1 mandatory declaration missing (mandatory=true, applicable=true, extracted=null)
        // 14 mandatory PASS, 9 not-mandatory absent, 6 not-required absent
        val rules = mutableListOf<Rule>()
        val fields = mutableMapOf<String, ExtractedField>()

        // 1 missing mandatory
        rules.add(createTestRule("M_MISSING", "Rule M Missing", "missing_mandatory_field", mandatory = true, applicability = "APPLICABLE"))

        for (i in 1..14) {
            val fieldName = "mand_field_$i"
            rules.add(createTestRule("M$i", "Rule M$i", fieldName, mandatory = true, applicability = "APPLICABLE"))
            fields[fieldName] = ExtractedField(present = true, value = "Value $i", confidence = 0.95)
        }
        for (i in 1..9) {
            val fieldName = "not_mand_field_$i"
            rules.add(createTestRule("NM$i", "Rule NM$i", fieldName, mandatory = false, applicability = "APPLICABLE"))
        }
        for (i in 1..6) {
            val fieldName = "not_req_field_$i"
            rules.add(createTestRule("NR$i", "Rule NR$i", fieldName, mandatory = false, applicability = "NOT_REQUIRED", condition = "CONDITION_UNMET"))
        }

        val ruleSet = createTestRuleSet(rules)
        val extraction = createTestExtraction(fields)
        val report = ruleEngine.evaluate(ruleSet, extraction)

        assertEquals(30, report.summary.total_rules)
        assertEquals(14, report.summary.passed)
        assertEquals(1, report.summary.total_violations)
        assertEquals(9, report.summary.not_mandatory)
        assertEquals(6, report.summary.not_required)
        assertEquals(OverallStatus.NON_COMPLIANT, report.overall_status)

        // User requirement: Raise Complaint MUST be available
        assertTrue(report.hasConfirmedMandatoryViolation)
        assertTrue(report.canRaiseComplaint)
    }

    @Test
    fun testComplaintCase3_LowConfidenceOnNonMandatoryField_NotMandatory_RaiseComplaintUnavailable() {
        // Test Case 3: Low confidence on non-mandatory field
        val rule = createTestRule(
            id = "NM_LOW_CONF",
            ref = "Rule 6(1)(f)",
            field = "optional_field",
            mandatory = false,
            applicability = "APPLICABLE"
        )
        val ruleSet = createTestRuleSet(listOf(rule))
        val extraction = createTestExtraction(
            mapOf("optional_field" to ExtractedField(present = true, value = "fuzzy text", confidence = 0.3))
        )
        val report = ruleEngine.evaluate(ruleSet, extraction)
        val result = report.results.first()

        // Status MUST be NOT_MANDATORY, never MANUAL_REVIEW or FAIL
        assertEquals(RuleStatus.NOT_MANDATORY, result.status)
        assertEquals(0, report.summary.total_violations)
        assertEquals(OverallStatus.COMPLIANT, report.overall_status)
        assertFalse(report.hasConfirmedMandatoryViolation)
        assertFalse(report.canRaiseComplaint)
    }

    @Test
    fun testComplaintCase4_LowConfidenceOnMandatoryField_ManualReview_RaiseComplaintUnavailable() {
        // Test Case 4: Low confidence on mandatory field
        val rule = createTestRule(
            id = "M_LOW_CONF",
            ref = "Rule 6(1)(a)",
            field = "mandatory_field",
            mandatory = true,
            applicability = "APPLICABLE"
        )
        val ruleSet = createTestRuleSet(listOf(rule))
        val extraction = createTestExtraction(
            mapOf("mandatory_field" to ExtractedField(present = true, value = "unclear text", confidence = 0.3))
        )
        val report = ruleEngine.evaluate(ruleSet, extraction)
        val result = report.results.first()

        assertEquals(RuleStatus.MANUAL_REVIEW, result.status)
        assertEquals(0, report.summary.total_violations)
        assertEquals(OverallStatus.MANUAL_REVIEW, report.overall_status)
        // MANUAL_REVIEW is NOT a confirmed mandatory violation: Raise Complaint must be UNAVAILABLE
        assertFalse(report.hasConfirmedMandatoryViolation)
        assertFalse(report.canRaiseComplaint)
    }

    @Test
    fun testComplaintCase5_ThirtyRulesZeroMandatoryViolations_RaiseComplaintUnavailable() {
        // Test Case 5: 30 evaluated rules, 0 confirmed mandatory violations
        val rules = mutableListOf<Rule>()
        val fields = mutableMapOf<String, ExtractedField>()

        for (i in 1..20) {
            val f = "pass_$i"
            rules.add(createTestRule("P$i", "Ref P$i", f, mandatory = true, applicability = "APPLICABLE"))
            fields[f] = ExtractedField(present = true, value = "Val $i", confidence = 0.99)
        }
        for (i in 1..5) {
            val f = "nm_$i"
            rules.add(createTestRule("NM$i", "Ref NM$i", f, mandatory = false, applicability = "APPLICABLE"))
        }
        for (i in 1..5) {
            val f = "nr_$i"
            rules.add(createTestRule("NR$i", "Ref NR$i", f, mandatory = false, applicability = "NOT_REQUIRED"))
        }

        val report = ruleEngine.evaluate(createTestRuleSet(rules), createTestExtraction(fields))
        assertEquals(30, report.summary.total_rules)
        assertEquals(0, report.summary.total_violations)
        assertEquals(OverallStatus.COMPLIANT, report.overall_status)
        assertFalse(report.canRaiseComplaint)
        assertFalse(report.hasConfirmedMandatoryViolation)
    }

    @Test
    fun testFontSize_ReferencedAgainstBarcode_ExplicitPass() {
        val rule = createTestRule(
            id = "LMPC_7",
            ref = "Rule 7",
            field = "declaration_letter_and_numeral_size",
            mandatory = true,
            applicability = "APPLICABLE"
        )
        val ruleSet = createTestRuleSet(listOf(rule))
        val extraction = createTestExtraction(
            mapOf(
                "declaration_letter_and_numeral_size" to ExtractedField(
                    present = true,
                    value = "Compliant (~1.8mm, verified against barcode scale reference)",
                    confidence = 0.95
                )
            )
        )

        val report = ruleEngine.evaluate(ruleSet, extraction)
        val result = report.results.first()

        assertEquals(RuleStatus.PASS, result.status)
        assertFalse(result.violation)
        assertEquals(OverallStatus.COMPLIANT, report.overall_status)
    }

    @Test
    fun testFontSize_ReferencedAgainstBarcode_InferredFromSurroundingDeclarations() {
        val rule = createTestRule(
            id = "LMPC_7",
            ref = "Rule 7",
            field = "declaration_letter_and_numeral_size",
            mandatory = true,
            applicability = "APPLICABLE"
        )
        val ruleSet = createTestRuleSet(listOf(rule))
        // Model omitted declaration_letter_and_numeral_size or couldn't find literal string,
        // but other declarations are clearly legible:
        val extraction = createTestExtraction(
            mapOf(
                "retail_sale_price" to ExtractedField(present = true, value = "₹50.00", confidence = 0.95),
                "net_quantity" to ExtractedField(present = true, value = "200 g", confidence = 0.95)
            )
        )

        val report = ruleEngine.evaluate(ruleSet, extraction)
        val result = report.results.first()

        assertEquals(RuleStatus.PASS, result.status)
        assertFalse(result.violation)
        assertTrue(result.extracted_value?.contains("barcode") == true)
        assertEquals(OverallStatus.COMPLIANT, report.overall_status)
    }

    @Test
    fun testFontSize_ReferencedAgainstBarcode_ExplicitViolation() {
        val rule = createTestRule(
            id = "LMPC_7",
            ref = "Rule 7",
            field = "declaration_letter_and_numeral_size",
            mandatory = true,
            applicability = "APPLICABLE"
        )
        val ruleSet = createTestRuleSet(listOf(rule))
        val extraction = createTestExtraction(
            mapOf(
                "declaration_letter_and_numeral_size" to ExtractedField(
                    present = true,
                    value = "Violation: font size too small (< 0.5mm relative to barcode)",
                    status = "FAIL",
                    confidence = 0.90
                )
            )
        )

        val report = ruleEngine.evaluate(ruleSet, extraction)
        val result = report.results.first()

        assertEquals(RuleStatus.FAIL, result.status)
        assertTrue(result.violation)
        assertEquals(OverallStatus.NON_COMPLIANT, report.overall_status)
    }
}
