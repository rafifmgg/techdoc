# Guideline: Flowchart Planning

This guideline describes how to plan and document flowcharts for technical documentation.

---

## 1. Flowchart Planning Process

### Step 1: Identify the Process

Before creating a flowchart, answer these questions:

| Question | Purpose |
| --- | --- |
| What is the process name? | Title for the flowchart |
| What triggers this process? | Start point (user action, cron job, API call) |
| What is the expected outcome? | End point(s) |
| What systems are involved? | Swimlanes / tiers |
| What decisions are made? | Decision points |
| What can go wrong? | Error handling paths |

### Step 2: List All Steps

Create a sequential list of all process steps:

```
1. [Trigger/Start]
2. [Action 1]
3. [Decision 1] - Yes/No paths
4. [Action 2a] (if Yes)
5. [Action 2b] (if No)
6. [Action 3]
7. [End]
```

### Step 3: Identify Decision Points

For each decision point, document:

| Decision | Condition | Yes Path | No Path |
| --- | --- | --- | --- |
| [Decision Name] | [Condition to check] | [Next step if Yes] | [Next step if No] |

### Step 4: Identify System Boundaries

List all systems/tiers involved:

| System/Tier | Color Code | Description |
| --- | --- | --- |
| Intranet API | Purple (#e1d5e7) | API tier for intranet |
| Intranet Cron | Purple (#e1d5e7) | Cron tier for intranet |
| Internet API | Blue (#dae8fc) | API tier for internet |
| Database | Yellow (#fff2cc) | Database operations |

---

## 2. Flowchart Documentation Template

### Process Overview

| Attribute | Value |
| --- | --- |
| Process Name | [Name] |
| Section | [Section X.X] |
| Trigger | [What starts this process] |
| Frequency | [Real-time / Every X minutes / Daily / etc.] |
| Systems Involved | [List of systems] |

### Process Steps Table

| Step | Definition | Brief Description |
| --- | --- | --- |
| Start | Entry point | [Description of trigger] |
| [Step 1] | [What it does] | [Detailed description] |
| [Decision 1]? | Decision point | [What condition is checked] |
| [Step 2a] | [If Yes] | [Description] |
| [Step 2b] | [If No] | [Description] |
| End | Exit point | [Description of completion] |

### Decision Logic

| Decision | Input | Condition | True Action | False Action |
| --- | --- | --- | --- | --- |
| Success? | Operation result | result == success | Continue | Handle error |
| Any record? | Query result | count > 0 | Process records | Log no record |
| Match? | Comparison | value1 == value2 | Apply action | Handle mismatch |

### Error Handling

| Error Point | Error Type | Handling | Recovery |
| --- | --- | --- | --- |
| [Step Name] | [Error type] | [How to handle] | [Recovery action] |

---

## 3. Standard Flowchart Patterns

### Pattern 1: Simple Linear Flow

```
Start → Action 1 → Action 2 → Action 3 → End
```

### Pattern 2: Single Decision

```
Start → Action 1 → Decision?
                      ├─ Yes → Action 2 → End
                      └─ No  → Action 3 → End
```

### Pattern 3: Multiple Decisions (Nested)

```
Start → Action 1 → Decision A?
                      ├─ Yes → Decision B?
                      │           ├─ Yes → Action 2 → End
                      │           └─ No  → Action 3 → End
                      └─ No  → Action 4 → End
```

### Pattern 4: Loop with Retry

```
Start → Action 1 → Success?
                      ├─ Yes → End
                      └─ No  → Retry Count < Max?
                                  ├─ Yes → [back to Action 1]
                                  └─ No  → Log Error → End
```

### Pattern 5: Parallel Processing

```
Start → Action 1 ─┬─► Action 2a ─┬─► Action 3 → End
                  └─► Action 2b ─┘
```

### Pattern 6: Database Sync Flow

```
Start → Query Source DB → Any Record?
                             ├─ Yes → Insert/Update Target DB → Success?
                             │                                    ├─ Yes → Update is_sync → End
                             │                                    └─ No  → Log Error → End
                             └─ No  → Log No Record → End
```

---

## 4. Naming Conventions

### Process Names

| Type | Format | Example |
| --- | --- | --- |
| API Flow | [Action] [Entity] | Create Notice, Update Payment |
| Cron Flow | Cron [Action] [Entity] | Cron Sync Notices, Cron Pull Internet |
| Report Flow | Generate [Report Name] | Generate Daily Report |

### Step Names

| Type | Format | Example |
| --- | --- | --- |
| Start | Start / Cron Start | Start, Cron Start |
| Process | [Verb] [Object] | Insert into table, Query database |
| Decision | [Condition]? | Success?, Any record?, Match? |
| End | End | End |

### Database References

| Type | Format | Example |
| --- | --- | --- |
| Table reference | [zone]_[table_name] | ocms_valid_offence_notice |
| Database icon label | [Zone] | Intranet, Internet |

---

## 5. Flowchart Checklist

Before finalizing a flowchart plan:

- [ ] All steps have clear names
- [ ] All decision points have Yes/No paths defined
- [ ] All paths lead to an End point
- [ ] Error handling paths are included
- [ ] Database operations are marked with dashed lines
- [ ] Swimlanes are defined for each system/tier
- [ ] Color coding is consistent
- [ ] Step descriptions are complete

---

## 6. Mapping to Technical Document

Each flowchart should map to these sections in the technical document:

| Flowchart Element | Document Section |
| --- | --- |
| Process overview | ## [Flow Name] |
| Diagram image | ![Diagram](path) |
| Steps table | Process Steps table |
| Data operations | ### Data Mapping |
| Success path | ### Success Outcome |
| Error paths | ### Error Handling |

---

## 7. Example: Complete Flowchart Plan

### Process: Intranet Push to Internet

**Overview:**

| Attribute | Value |
| --- | --- |
| Process Name | Intranet Push to Internet |
| Section | 1.2 |
| Trigger | New notice created in Intranet |
| Systems | Intranet API, Intranet DB, Internet DB |

**Steps:**

| Step | Type | Description | Next Step |
| --- | --- | --- | --- |
| 1 | Start | Process triggered | 2 |
| 2 | Process | Insert into intranet table | 3 |
| 3 | Decision | Success? | Yes→4, No→E1 |
| 4 | Process | Insert into internet table | 5 |
| 5 | Decision | Success? | Yes→6, No→E2 |
| 6 | Process | Patch is_sync = true | 7 |
| 7 | Process | Response success | 8 |
| 8 | End | Process complete | - |
| E1 | Error | Response error | End |
| E2 | Error | Log error | End |

**Decisions:**

| ID | Decision | Condition | Yes | No |
| --- | --- | --- | --- | --- |
| D1 | Success? (Intranet) | Insert succeeded | Step 4 | Error E1 |
| D2 | Success? (Internet) | Insert succeeded | Step 6 | Error E2 |
