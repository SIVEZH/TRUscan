package com.example.domain.validation

import com.example.domain.model.*

enum class RelationshipType {
    OVERRIDE,
    REPLACEMENT,
    SUPERSESSION
}

data class RuleRelationship(
    val controllingRuleId: String,
    val controlledRuleId: String,
    val type: RelationshipType
)

enum class SelectionState {
    SELECTED,
    EXCLUDED_BY_OVERRIDE,
    EXCLUDED_BY_REPLACEMENT,
    EXCLUDED_BY_SUPERSESSION,
    EXCLUDED_BY_APPLICABILITY,
    EXCLUDED_BY_EXCEPTION,
    CONFIGURATION_ERROR
}

data class RuleSelectionDecision(
    val ruleId: String,
    val state: SelectionState,
    val reason: String,
    val controllingRuleId: String? = null
)

data class RuleSelectionError(
    val ruleId: String,
    val message: String
)

data class SelectedRuleSet(
    val selectedRules: List<Rule>,
    val excludedRules: List<RuleSelectionDecision>,
    val errors: List<RuleSelectionError>
)

class RuleSelectionEngine(
    private val applicabilityEngine: RuleApplicabilityEngine
) {
    fun selectRules(ruleSet: RuleSet, pkgCtx: PackageClassification): SelectedRuleSet {
        val rules = ruleSet.rules
        val ruleMap = rules.associateBy { it.rule_id }
        val exceptionsMap = ruleSet.exceptions?.associateBy { it.exception_id } ?: emptyMap()
        val errors = mutableListOf<RuleSelectionError>()
        
        // Validation
        val ruleIds = ruleMap.keys
        val duplicates = rules.groupBy { it.rule_id }.filter { it.value.size > 1 }.keys
        if (duplicates.isNotEmpty()) {
            duplicates.forEach { errors.add(RuleSelectionError(it, "Duplicate rule ID")) }
        }
        
        // Normalization & Graph Building
        val relationships = mutableListOf<RuleRelationship>()
        for (rule in rules) {
            rule.overridden_by?.forEach { controllingId ->
                if (controllingId !in ruleIds) {
                    errors.add(RuleSelectionError(rule.rule_id, "overridden_by references non-existent rule: $controllingId"))
                } else {
                    relationships.add(RuleRelationship(controllingId, rule.rule_id, RelationshipType.OVERRIDE))
                }
            }
            rule.overrides?.forEach { controlledId ->
                if (controlledId !in ruleIds) {
                    errors.add(RuleSelectionError(rule.rule_id, "overrides references non-existent rule: $controlledId"))
                } else {
                    relationships.add(RuleRelationship(rule.rule_id, controlledId, RelationshipType.OVERRIDE))
                }
            }
        }
        
        // Remove duplicate relationships
        val distinctRelationships = relationships.distinct()
        
        val decisions = mutableListOf<RuleSelectionDecision>()
        val selectedRuleIds = mutableSetOf<String>()
        val applicableRules = mutableSetOf<String>()
        
        // 1. Determine rules relevant to product context
        for (rule in rules) {
            // Check category hard condition
            val isCorrectCategory = rule.category.isNullOrBlank() || rule.category.equals(pkgCtx.category, ignoreCase = true)
            
            if (isCorrectCategory && applicabilityEngine.determineApplicability(rule, pkgCtx)) {
                applicableRules.add(rule.rule_id)
            } else {
                decisions.add(RuleSelectionDecision(rule.rule_id, SelectionState.EXCLUDED_BY_APPLICABILITY, "Not applicable to this product context"))
            }
        }
        
        // 2. Evaluate explicit exceptions
        for (rule in rules) {
            if (rule.rule_id !in applicableRules) continue
            
            var exceptionApplies = false
            rule.exceptions?.forEach { exceptionId ->
                val exception = exceptionsMap[exceptionId]
                if (exception != null) {
                    val cond = exception.condition
                    if (cond != null) {
                        val catMatches = cond.commodity_category.isNullOrBlank() || cond.commodity_category.equals(pkgCtx.category, ignoreCase = true)
                        val prodMatches = cond.product_type.isNullOrBlank() || cond.product_type.equals(pkgCtx.product_type, ignoreCase = true)
                        
                        // If it matches exactly the product type requested (if defined) or category
                        if (catMatches && cond.product_type != null && prodMatches) {
                            exceptionApplies = true
                        }
                    }
                }
            }
            
            if (exceptionApplies) {
                applicableRules.remove(rule.rule_id)
                decisions.add(RuleSelectionDecision(rule.rule_id, SelectionState.EXCLUDED_BY_EXCEPTION, "Explicit exception applies to product"))
            }
        }

        // 3. For each rule, trace to final controlling rule
        for (rule in rules) {
            if (rule.rule_id !in applicableRules) continue
            
            // Find applicable controlling rules
            val controllingRule = findFinalApplicableControllingRule(rule.rule_id, distinctRelationships, applicableRules, ruleMap, emptySet())
            
            if (controllingRule == null || controllingRule == rule.rule_id) {
                // Not controlled by anything applicable (or circular fallback returned itself)
                selectedRuleIds.add(rule.rule_id)
            } else {
                // It is controlled by another applicable rule
                decisions.add(RuleSelectionDecision(
                    ruleId = rule.rule_id, 
                    state = SelectionState.EXCLUDED_BY_OVERRIDE, 
                    reason = "Controlled by $controllingRule", 
                    controllingRuleId = controllingRule
                ))
            }
        }
        
        for (ruleId in selectedRuleIds) {
            decisions.add(RuleSelectionDecision(ruleId, SelectionState.SELECTED, "Selected for compliance evaluation"))
        }
        
        return SelectedRuleSet(
            selectedRules = rules.filter { it.rule_id in selectedRuleIds },
            excludedRules = decisions.filter { it.state != SelectionState.SELECTED },
            errors = errors
        )
    }
    
    private fun findFinalApplicableControllingRule(
        ruleId: String, 
        relationships: List<RuleRelationship>, 
        applicableRules: Set<String>,
        ruleMap: Map<String, Rule>,
        visited: Set<String>
    ): String? {
        if (ruleId in visited) return null 
        
        val directControllers = relationships.filter { it.controlledRuleId == ruleId && it.controllingRuleId in applicableRules }
        
        if (directControllers.isEmpty()) return ruleId
        
        for (controller in directControllers) {
            val finalController = findFinalApplicableControllingRule(controller.controllingRuleId, relationships, applicableRules, ruleMap, visited + ruleId)
            if (finalController != null) {
                return finalController
            }
        }
        
        return null
    }
}
