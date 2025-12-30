# Verification Checklist

**MANDATORY**: Complete this checklist for the **current highest-priority theory** only.

**CRITICAL RULE**: Code analysis finds POTENTIAL issues. Tools PROVE which issues actually matter.

---

## Iterative Verification Approach

**Work on ONE theory at a time, highest-priority first:**

1. ✅ Complete this checklist for the **highest-severity unverified theory**
2. ✅ If VERIFIED → **Recommend fix to user and STOP**
3. ✅ After fix applied → Re-profile and start checklist for next theory
4. ✅ If FALSIFIED → Move to next highest-priority theory

**WHY?** Multiple issues create noise in profiling tools. Fix the biggest issue first to get cleaner signal for the next investigation.

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

## After Verification: Next Steps

**If ✅ VERIFIED:**
- [ ] Present fix recommendation to user
- [ ] Tool outputs saved to `tool-outputs/` directory
- [ ] **STOP** - Wait for user to apply fix
- [ ] After fix → Re-profile → Verify next highest-priority theory

**If ❌ FALSIFIED:**
- [ ] Document why theory was wrong
- [ ] Proceed immediately to next highest-priority theory

**If ⚠️ INCONCLUSIVE:**
- [ ] Document tool limitations
- [ ] Proceed to next theory (may revisit after other fixes)

---

## Remember

**Code analysis → POTENTIAL issues**
**Tool verification → PROVEN issues**
**Fix one issue at a time → Cleaner signal for next investigation**
