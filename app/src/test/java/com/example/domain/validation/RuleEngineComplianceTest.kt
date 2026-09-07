package com.example.domain.validation

import com.example.domain.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class RuleEngineComplianceTest {
    private lateinit var ruleEngine: RuleEngine

    @Before
    fun setup() {
        ruleEngine = RuleEngine()
    }

    private fun createTestRule(
        id: String,
        ref: String,
        field: String,
        mandatory: Boolean = true,
        applicability: String = "APPLICABLE",
        condition: String = "",
        category: String = "GENERAL",
        overriddenBy: List<String>? = null,
        overrides: List<String>? = null,
        requirement: String = "",
        appliesTo: RuleAppliesTo? = null,
        exceptions: List<String>? = null
    ) = Rule(
        rule_id = id,
        legal_reference = ref,
        category = category,
        field = field,
        requirement = requirement,
        condition = condition,
        description = "Test rule",
        validation_type = "TEXT",
        mandatory = mandatory,
        applicability = applicability,
        applies_to = appliesTo,
        exceptions = exceptions,
        overridden_by = overriddenBy,
        overrides = overrides
    )

    private fun createTestRuleSet(rules: List<Rule>, exceptions: List<GlobalException>? = null, category: String = "TEST") = RuleSet(
        dataset_name = "Test",
        category = category,
        rules = rules,
        exceptions = exceptions
    )

    private fun createTestExtraction(
        fields: Map<String, ExtractedField>,
        category: String = "GENERAL",
        productType: String? = null,
        isFood: Boolean = false
    ) = GeminiExtraction(
        category = category,
        category_confidence = 1.0,
        package_classification = PackageClassification(
            category = category, 
            product_type = productType,
            is_food = isFood, 
            is_prepackaged = true, 
            is_retail = true
        ),
        fields = fields
    )

    @Test
    fun test1_FoodNormalPackagedFood() {
        val rule1 = createTestRule("FOOD_1", "1", "field1", category = "FOOD")
        val rule2 = createTestRule("COSMETICS_1", "2", "field2", category = "COSMETICS")
        val extraction = createTestExtraction(mapOf(), "FOOD", "NORMAL", true)
        val report = ruleEngine.evaluate(createTestRuleSet(listOf(rule1, rule2)), extraction)
        
        assertEquals(1, report.results.size)
        assertEquals("FOOD_1", report.results.first().rule_id)
    }

    @Test
    fun test2_CosmeticProduct() {
        val rule1 = createTestRule("FOOD_1", "1", "field1", category = "FOOD")
        val rule2 = createTestRule("COSMETICS_1", "2", "field2", category = "COSMETICS")
        val extraction = createTestExtraction(mapOf(), "COSMETICS", "SHAMPOO", false)
        val report = ruleEngine.evaluate(createTestRuleSet(listOf(rule1, rule2)), extraction)
        
        assertEquals(1, report.results.size)
        assertEquals("COSMETICS_1", report.results.first().rule_id)
    }

    @Test
    fun test3_FoodProductWithException() {
        // Now rule exceptions are strings pointing to global exceptions
        val rule1 = createTestRule("FOOD_1", "1", "field1", category = "FOOD", exceptions = listOf("EX_1"))
        val globalException = GlobalException(
            exception_id = "EX_1",
            condition = GlobalExceptionCondition(product_type = "SPECIAL"),
            effect = "EXEMPT_FROM_SELECTED_RULES"
        )
        val extraction = createTestExtraction(mapOf(), "FOOD", "SPECIAL", true)
        val report = ruleEngine.evaluate(createTestRuleSet(listOf(rule1), listOf(globalException)), extraction)
        
        // Excluded rule is NOT evaluated
        assertEquals(0, report.results.size)
    }

    @Test
    fun test4_CosmeticWithRequirementException() {
        val rule1 = createTestRule("COSMETICS_1", "1", "field1", category = "COSMETICS", exceptions = listOf("EX_COSMETIC"))
        val globalException = GlobalException(
            exception_id = "EX_COSMETIC",
            condition = GlobalExceptionCondition(product_type = "SPECIAL_COSMETIC"),
            effect = "EXEMPT_FROM_SELECTED_RULES"
        )
        val extraction = createTestExtraction(mapOf(), "COSMETICS", "NORMAL_COSMETIC", false)
        val report = ruleEngine.evaluate(createTestRuleSet(listOf(rule1), listOf(globalException)), extraction)
        
        // Exception does not apply, rule should be evaluated
        assertEquals(1, report.results.size)
        assertEquals("COSMETICS_1", report.results.first().rule_id)
    }

    @Test
    fun test7_VerifyComplianceEngineReceivesOnlySelectedRules() {
        val rule1 = createTestRule("FOOD_1", "1", "field1", category = "FOOD")
        val rule2 = createTestRule("COSMETICS_1", "2", "field2", category = "COSMETICS")
        val extraction = createTestExtraction(mapOf(), "FOOD", "NORMAL", true)
        val report = ruleEngine.evaluate(createTestRuleSet(listOf(rule1, rule2)), extraction)
        
        // Only 1 rule selected, compliance engine should only output 1 result
        assertEquals(1, report.results.size)
    }

    @Test
    fun test8_VerifyMedicineAndElectronicsRuleIdsAreNeverPassed() {
        val rule1 = createTestRule("FOOD_1", "1", "field1", category = "FOOD")
        val rule2 = createTestRule("MED_1", "2", "field2", category = "MEDICINES")
        val rule3 = createTestRule("ELEC_1", "3", "field3", category = "ELECTRONICS")
        val extraction = createTestExtraction(mapOf(), "FOOD", "NORMAL", true)
        val report = ruleEngine.evaluate(createTestRuleSet(listOf(rule1, rule2, rule3)), extraction)
        
        assertEquals(1, report.results.size)
        assertEquals("FOOD_1", report.results.first().rule_id)
    }
}
