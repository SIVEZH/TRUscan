package com.example

import com.example.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class ReportGeneratorTest {

    @Test
    fun testMandatoryViolationsFiltering() {
        val mandatoryViolation = RuleResult(
            rule_id = "R1",
            legal_reference = "Rule 6(1)(e)",
            field = "retail_sale_price_mrp",
            status = RuleStatus.FAIL,
            extracted_value = null,
            message = "Mandatory declaration missing",
            requirement = "Retail sale price (MRP) must be declared",
            mandatory = true,
            violation = true
        )

        val optionalMissing = RuleResult(
            rule_id = "R2",
            legal_reference = "Rule 6(1)(f)",
            field = "consumer_care_email",
            status = RuleStatus.NOT_REQUIRED,
            extracted_value = null,
            message = "Optional field not declared",
            requirement = "Consumer care email optional",
            mandatory = false,
            violation = false
        )

        val report = ValidationReport(
            category = "FOOD",
            overall_status = OverallStatus.NON_COMPLIANT,
            summary = ValidationSummary(
                total_rules = 2,
                passed = 0,
                failed = 1,
                not_applicable = 0,
                manual_review = 0,
                not_required = 1,
                total_violations = 1
            ),
            results = listOf(mandatoryViolation, optionalMissing)
        )

        assertEquals(1, report.violations.size)
        assertEquals("retail_sale_price_mrp", report.violations[0].field)
        assertTrue(report.violations[0].mandatory)
        assertTrue(report.violations[0].violation)
    }

    @Test
    fun testCompliantReportHasZeroViolations() {
        val compliantRule = RuleResult(
            rule_id = "R1",
            legal_reference = "Rule 6(1)(a)",
            field = "name_of_commodity",
            status = RuleStatus.PASS,
            extracted_value = "Almond Milk",
            message = "Commodity name verified",
            requirement = "Must declare generic or common name",
            mandatory = true,
            violation = false
        )

        val report = ValidationReport(
            category = "FOOD",
            overall_status = OverallStatus.COMPLIANT,
            summary = ValidationSummary(
                total_rules = 1,
                passed = 1,
                failed = 0,
                not_applicable = 0,
                manual_review = 0,
                not_required = 0,
                total_violations = 0
            ),
            results = listOf(compliantRule)
        )

        assertTrue(report.violations.isEmpty())
        assertEquals(OverallStatus.COMPLIANT, report.overall_status)
    }
}
