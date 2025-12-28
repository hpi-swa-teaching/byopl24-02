# Verification Checklist

**MANDATORY**: Complete this checklist for EVERY theory before including it in the report.

**CRITICAL RULE**: Code analysis finds POTENTIAL issues. Tools PROVE which issues actually matter.

---

## Pre-Report Verification Gate

**Before writing the performance analysis report, you MUST:**

1. ✅ Complete this checklist for EVERY theory generated
2. ✅ Have tool-based evidence for EVERY issue marked as verified
3. ✅ Document inconclusive results with attempted alternatives
4. ✅ Remove theories that couldn't be verified from the report

**NO EXCEPTIONS**: If a theory lacks tool verification, it MUST NOT appear in the report as a verified issue.

---

## Per-Theory Verification Checklist

Copy this template for each theory. ALL boxes must be checked before the theory can be reported as verified.

### Theory: [Theory Name/Description]

**Source**: [Code analysis location / Performance gap / Pattern detection]

**Category**: [Implementation / Configuration / Architectural]

**Severity (from code analysis)**: [Critical / High / Medium / Low]

---

#### Step 1: Tool Selection

- [ ] **Listed ALL tools needed** for complete verification (not just one)
  - Tool 1: _______________ → Purpose: _______________
  - Tool 2: _______________ → Purpose: _______________
  - Tool 3: _______________ → Purpose: _______________
  - (Add more as needed)

- [ ] **Verified tool selection** covers all aspects:
  - [ ] Frequency/count measurement (if applicable)
  - [ ] Impact/time measurement (if applicable)
  - [ ] Root cause identification (if applicable)
  - [ ] Quantitative data (not just yes/no)

---

#### Step 2: Documentation Loading (Per Tool)

**For EACH tool**, check:

- [ ] **Tool 1**: Read documentation file BEFORE execution (or reuse from previous theory)
  - File: _______________
  - Understood: Command syntax, output format, interpretation guidelines

- [ ] **Tool 2**: Read documentation file BEFORE execution (or reuse from previous theory)
  - File: _______________
  - Understood: Command syntax, output format, interpretation guidelines

- [ ] **Tool 3**: Read documentation file BEFORE execution (or reuse from previous theory)
  - File: _______________
  - Understood: Command syntax, output format, interpretation guidelines

**NOTE**: Documentation loading can be reused across theories if already loaded.

---

#### Step 3: Fermi Verification (Per Tool)

**For EACH tool**, complete:

**Tool 1: _______________**

- [ ] **Pre-calculated** expected output magnitude
  - Estimate: _______________
  - Reasoning: _______________

- [ ] **Smoke test** passed (trivial input) OR reused from previous theory
  - Command: _______________
  - Result: ✅ Works / ❌ Failed / 🔄 Reused from Theory #___

- [ ] **Executed** on actual benchmark OR reused from previous theory
  - Command: _______________
  - Output saved to: _______________
  - 🔄 Reusing output from Theory #___ if same tool + same benchmark

- [ ] **Validated** results vs estimate
  - Actual: _______________
  - Within 1 order of magnitude? ✅ Yes / ❌ No
  - If No: Diagnosed and resolved? _______________

**Tool 2: _______________**

- [ ] Pre-calculated expected output magnitude
- [ ] Smoke test passed OR reused
- [ ] Executed on actual benchmark OR reused
- [ ] Validated results vs estimate

**Tool 3: _______________**

- [ ] Pre-calculated expected output magnitude
- [ ] Smoke test passed OR reused
- [ ] Executed on actual benchmark OR reused
- [ ] Validated results vs estimate

**IMPORTANT**: Fermi verification results (smoke tests, executions, validations) can be **reused** across multiple theories if:
- Same tool is used
- Same benchmark is tested
- Previous execution passed validation

Simply note "🔄 Reused from Theory #X" and reference the previous verification.

---

#### Step 4: Evidence Analysis

- [ ] **Extracted quantitative data** from EACH tool
  - Tool 1 data: _______________
  - Tool 2 data: _______________
  - Tool 3 data: _______________

- [ ] **All tools agree** OR contradictions explained
  - Agreement: ✅ All tools confirm / ⚠️ Partial / ❌ Contradictory
  - If contradictory: Explanation: _______________

- [ ] **Impact quantified** with actual numbers
  - Frequency: _______________ (operations/allocations per benchmark)
  - Time impact: _______________ (% of total time or ms)
  - Location: _______________ (% in hot path or cold path)

---

#### Step 5: Verdict

**Based on tool evidence (NOT code analysis alone):**

- [ ] **Verdict**: ✅ VERIFIED / ❌ FALSIFIED / ⚠️ INCONCLUSIVE

**If ✅ VERIFIED:**
- [ ] Have concrete numbers (frequency, time, allocations, etc.)
- [ ] Severity confirmed by tool data (not just code inspection)
- [ ] Root cause identified from tool evidence
- [ ] Ready for report inclusion

**If ❌ FALSIFIED:**
- [ ] Documented why theory was wrong
- [ ] Excluded from report
- [ ] Learned from incorrect assumption

**If ⚠️ INCONCLUSIVE:**
- [ ] Documented which tools worked and which didn't
- [ ] Attempted alternative verification approaches
- [ ] Decision:
  - [ ] Include in report with "⚠️ INCONCLUSIVE" status + explanation
  - [ ] Exclude from report due to insufficient evidence

---

## Mandatory Checks Before Report Generation

Before running Phase 4 (Generate Report), verify:

- [ ] **ALL theories** have completed verification checklists
- [ ] **VERIFIED theories** have quantitative tool data
- [ ] **FALSIFIED theories** are excluded from report
- [ ] **INCONCLUSIVE theories** either:
  - [ ] Excluded (if insufficient evidence)
  - [ ] OR included with explicit ⚠️ INCONCLUSIVE status + explanation
- [ ] **NO theory** in report relies solely on code analysis
- [ ] **Severity ratings** adjusted based on tool data (not just code inspection)
- [ ] **All tool outputs** saved to `tool-outputs/` directory
- [ ] **Report citations** reference specific tool output files

---

## Post-Report Self-Review

After generating the report, verify:

- [ ] **Every issue** has "Evidence:" section with tool output citations
- [ ] **Every recommendation** has quantitative impact estimate from tools
- [ ] **Report structure** matches template in EXAMPLES.md
- [ ] **No claims** without tool backing
- [ ] **Honest assessment** of what was verified vs what needs more investigation

---

## If You Skipped Verification

**STOP. Do not proceed to report generation.**

If you generated theories but skipped systematic tool verification:

1. **Acknowledge the skip**: "I generated theories from code analysis but didn't verify with tools"
2. **Options**:
   - **Option A (Recommended)**: Go back to Phase 3, verify each theory systematically
   - **Option B**: Deliver partial report with clear disclaimer: "These are UNVERIFIED potential issues from code analysis only. Tool verification required before implementing fixes."
3. **Do NOT**: Claim theories are "verified" based on code analysis alone

---

## Remember

**Code analysis → POTENTIAL issues**

**Tool verification → PROVEN issues**

**Only PROVEN issues belong in the final report.**
