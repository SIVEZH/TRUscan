import json

def process_file(filepath):
    with open(filepath, 'r') as f:
        data = json.load(f)
    
    rules = data['rules']
    
    # Simple logic to add empty relationship arrays if they don't exist
    for rule in rules:
        if 'exceptions' not in rule: rule['exceptions'] = []
        if 'overrides' not in rule: rule['overrides'] = []
        if 'overridden_by' not in rule: rule['overridden_by'] = []
        if 'special_cases' not in rule: rule['special_cases'] = []
        
    # Manually wire up the overrides based on the food_rules.json content
    def add_override(overrider, overridden):
        for r in rules:
            if r['rule_id'] == overrider:
                if overridden not in r['overrides']:
                    r['overrides'].append(overridden)
            if r['rule_id'] == overridden:
                if overrider not in r['overridden_by']:
                    r['overridden_by'].append(overrider)

    if "food_rules" in filepath:
        add_override("LMPC_FOOD_6_1_A_EXPLANATION_III", "LMPC_6_1_A")
        add_override("LMPC_FOOD_RULE_26_B", "LMPC_6_1_A")
        add_override("LMPC_FOOD_RULE_26_B", "LMPC_6_1_B")
        add_override("LMPC_FOOD_RULE_26_B", "LMPC_6_1_C")
        add_override("LMPC_FOOD_RULE_26_B", "LMPC_6_1_E")
        add_override("LMPC_FOOD_RULE_26_B", "LMPC_6_2")
        add_override("LMPC_26_B", "LMPC_6_1_A")
        add_override("LMPC_26_A", "LMPC_6_1_C")
        # Food specific overrides
        
    with open(filepath, 'w') as f:
        json.dump(data, f, indent=2)

import glob
for f in glob.glob("app/src/main/assets/rules/*.json"):
    process_file(f)

