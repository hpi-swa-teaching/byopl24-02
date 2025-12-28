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

**🔄 EFFICIENCY NOTE**: Tool runs can be **reused** across theories if the same tool runs on the same benchmark with the same configuration. Mark reused items with "🔄 Reused from Theory #X" and reference the original verification.

---

## Per-Theory Verification Template

Copy this template for each theory.

### Theory: [Theory Name/Description]

**Source**: [Code analysis location / Performance gap / Pattern detection]
**Category**: [Implementation / Configuration / Architectural]
**Severity (from code analysis)**: [Critical / High / Medium / Low]

---

#### Step 1: Tool Selection

- [ ] **Listed ALL tools needed** for complete verification
  - Tool 1: _______________ → Purpose: _______________
  - Tool 2: _______________ → Purpose: _______________
  - Tool 3: _______________ → Purpose: _______________

- [ ] **Verified tool selection covers all aspects**:
  - [ ] Frequency/count measurement (if applicable)
  - [ ] Impact/time measurement (if applicable)
  - [ ] Root cause identification (if applicable)
  - [ ] Quantitative data (not just yes/no)

---

#### Step 2: Documentation Loading (Per Tool)

- [ ] **Tool 1**: Used skill or read documentation BEFORE execution
  - Skill: _______________
  - Understood: Command syntax, output format, interpretation

- [ ] **Tool 2**: Used skill or read documentation BEFORE execution
  - Skill: _______________

- [ ] **Tool 3**: Used skill or read documentation BEFORE execution
  - Skill: _______________

---

#### Step 3: Fermi Verification (Per Tool)

**Tool 1: _______________**

- [ ] **Pre-calculated** expected output magnitude
  - Estimate: _______________
  - Reasoning: _______________

- [ ] **Smoke test** passed (trivial input) OR reused
  - Result: ✅ Works / ❌ Failed / 🔄 Reused from Theory #___

- [ ] **Executed** on actual benchmark OR reused
  - Output saved to: _______________

- [ ] **Validated** results vs estimate
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

---

#### Step 4: Evidence Analysis

- [ ] **Extracted quantitative data** from EACH tool
  - Tool 1 data: _______________
  - Tool 2 data: _______________
  - Tool 3 data: _______________

- [ ] **All tools agree** OR contradictions explained
  - Agreement: ✅ All confirm / ⚠️ Partial / ❌ Contradictory

- [ ] **Impact quantified** with actual numbers
  - Frequency: _______________ (operations/allocations per benchmark)
  - Time impact: _______________ (% of total time or ms)
  - Location: _______________ (% in hot path or cold path)

---

#### Step 5: Verdict

**Based on tool evidence (NOT code analysis alone):**

- [ ] **Verdict**: ✅ VERIFIED / ❌ FALSIFIED / ⚠️ INCONCLUSIVE

**If ✅ VERIFIED:**
- [ ] Have concrete numbers (frequency, time, allocations)
- [ ] Severity confirmed by tool data
- [ ] Root cause identified from tool evidence
- [ ] Ready for report inclusion

**If ❌ FALSIFIED:**
- [ ] Documented why theory was wrong
- [ ] Excluded from report

**If ⚠️ INCONCLUSIVE:**
- [ ] Documented which tools worked and which didn't
- [ ] Attempted alternative verification approaches
- [ ] Decision: Include with disclaimer / Exclude

---

## Mandatory Checks Before Report Generation

Before proceeding to `generating-performance-reports`:

- [ ] **ALL theories** have completed verification checklists
- [ ] **VERIFIED theories** have quantitative tool data
- [ ] **FALSIFIED theories** are excluded from report
- [ ] **INCONCLUSIVE theories** either excluded or marked with disclaimer
- [ ] **NO theory** in report relies solely on code analysis
- [ ] **Severity ratings** adjusted based on tool data
- [ ] **All tool outputs** saved to `tool-outputs/` directory
- [ ] **Report citations** reference specific tool output files

---

## Remember

**Code analysis → POTENTIAL issues**
**Tool verification → PROVEN issues**
**Only PROVEN issues belong in the final report.**
