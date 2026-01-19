# OCMS 41 Section 4 - Validation Conditions

## Document Information

| Field | Value |
|-------|-------|
| Version | 1.0 |
| Date | 2026-01-07 |
| Source | Functional Document v1.1, Section 4 |
| Module | OCMS 41 - Section 4: Staff Portal Manual Furnish or Update |

---

## Table of Contents

1. [Frontend Validations](#1-frontend-validations)
2. [Backend Validations](#2-backend-validations)
3. [Decision Trees](#3-decision-trees)
4. [Assumptions Log](#4-assumptions-log)

---

## 1. Frontend Validations

### 1.1 Action Button Display Rules (Section 4.5.1)

#### Rule: Determine which buttons to display

| Rule ID | Condition | FURNISH | REDIRECT | UPDATE |
|---------|-----------|---------|----------|--------|
| BTN-001 | No existing offender record for role | Show | Hide | Hide |
| BTN-002 | Offender exists AND is Current Offender | Hide | Hide | Show |
| BTN-003 | Offender exists AND NOT Current Offender | Hide | Show | Hide |

### 1.2 Furnish Form Validations

| Field | Rule ID | Validation | Error Message |
|-------|---------|------------|---------------|
| ID Type | FRN-001 | Required | ID Type is required |
| ID Number | FRN-002 | Required | ID Number is required |
| ID Number | FRN-003 | Format check based on ID Type | Invalid {idType} format |
| Name | FRN-004 | Required | Name is required |
| Name | FRN-005 | Max 100 characters | Name exceeds maximum length |
| Email | FRN-006 | Valid email format if provided | Invalid email format |
| Contact No | FRN-007 | Numeric only, 8 digits for SG | Invalid contact number |
| Postal Code | FRN-008 | 6 digits for Singapore | Invalid postal code |

### 1.3 Redirect Form Validations

| Field | Rule ID | Validation | Error Message |
|-------|---------|------------|---------------|
| Target Role | RDR-001 | Required | Target role is required |
| Target Role | RDR-002 | Different from current role | Cannot redirect to the same role |
| ID Type | RDR-003 | Required | ID Type is required |
| ID Number | RDR-004 | Required | ID Number is required |
| Name | RDR-005 | Required | Name is required |

### 1.4 Update Form Validations

| Field | Rule ID | Validation | Error Message |
|-------|---------|------------|---------------|
| At least one field | UPD-001 | At least one field must be changed | No changes detected |
| Email | UPD-002 | Valid email format if provided | Invalid email format |
| Contact No | UPD-003 | Valid format if provided | Invalid contact number |

---

## 2. Backend Validations

### 2.1 Furnish Eligibility Check (Section 4.6.2.2)

| Rule ID | Check | Condition | Action if Failed |
|---------|-------|-----------|------------------|
| FRN-BE-001 | Notice Exists | Notice must exist in database | Return NOT_FOUND |
| FRN-BE-002 | Processing Stage | Notice must be at furnishable stage | Return NOT_FURNISHABLE |
| FRN-BE-003 | Not Final Stage | Stage must not be after CPC | Return LAST_STAGE_AFTER_CPC |

#### Furnishable Stages

| Stage | Can Furnish Owner | Can Furnish Hirer | Can Furnish Driver |
|-------|-------------------|-------------------|-------------------|
| OW | No | Yes | Yes |
| RD1 | No | No | Yes |
| RD2 | No | No | Yes |
| DN | No | No | No |
| CPC+ | No | No | No |

### 2.2 Redirect Eligibility Check (Section 4.7.2.2)

| Rule ID | Check | Condition | Action if Failed |
|---------|-------|-----------|------------------|
| RDR-BE-001 | Notice Exists | Notice must exist in database | Return NOT_FOUND |
| RDR-BE-002 | Current Offender Exists | Must have a current offender | Return NO_CURRENT_OFFENDER |
| RDR-BE-003 | Different Role | Target role must be different | Return REDIRECT_SAME_ROLE |
| RDR-BE-004 | Processing Stage | Notice must be at redirectable stage | Return NOT_REDIRECTABLE |

### 2.3 Update Eligibility Check (Section 4.8.2.2)

| Rule ID | Check | Condition | Action if Failed |
|---------|-------|-----------|------------------|
| UPD-BE-001 | Notice Exists | Notice must exist in database | Return NOT_FOUND |
| UPD-BE-002 | Offender Exists | Offender record must exist | Return OFFENDER_NOT_FOUND |
| UPD-BE-003 | Is Current Offender | Must be the current offender | Return NOT_CURRENT_OFFENDER |

### 2.4 Furnish Processing Rules (Section 4.6.2.3)

| Step | Rule ID | Check | Action |
|------|---------|-------|--------|
| 1 | FRN-PR-001 | Check if existing offender for role | If exists → PATCH, else → POST |
| 2 | FRN-PR-002 | Request type = POST | Create new offender record |
| 3 | FRN-PR-003 | Request type = PATCH | Overwrite existing offender record |
| 4 | FRN-PR-004 | Set offender_indicator | Set to 'Y' for new offender |
| 5 | FRN-PR-005 | Clear previous offender | Set previous offender_indicator to 'N' |
| 6 | FRN-PR-006 | Update processing stage | Change stage based on offender type |

#### Processing Stage Update Rules

| Offender Type | Current Stage | New Stage |
|---------------|---------------|-----------|
| HIRER | OW | RD1 |
| HIRER | RD1 | RD2 |
| DRIVER | OW | DN |
| DRIVER | RD1 | DN |
| DRIVER | RD2 | DN |

### 2.5 Redirect Processing Rules (Section 4.7.2.3)

| Step | Rule ID | Check | Action |
|------|---------|-------|--------|
| 1 | RDR-PR-001 | Clear current offender | Set offender_indicator = 'N' |
| 2 | RDR-PR-002 | Check target offender exists | If exists → Update, else → Create |
| 3 | RDR-PR-003 | Set new offender_indicator | Set to 'Y' for target offender |
| 4 | RDR-PR-004 | Reset processing stage | Reset to appropriate starting stage |

### 2.6 Update Processing Rules (Section 4.8.2.3)

| Step | Rule ID | Check | Action |
|------|---------|-------|--------|
| 1 | UPD-PR-001 | Validate offender is current | Verify offender_indicator = 'Y' |
| 2 | UPD-PR-002 | Update particulars | Update provided fields only |
| 3 | UPD-PR-003 | No stage change | Processing stage remains unchanged |

---

## 3. Decision Trees

### 3.1 Action Button Decision Tree

```
START: User clicks UNLOCK button
  │
  ├── Check: Does offender record exist for this role?
  │     │
  │     ├── NO → Display FURNISH button only
  │     │
  │     └── YES → Check: Is this offender the Current Offender?
  │           │
  │           ├── YES → Display UPDATE button only
  │           │
  │           └── NO → Display REDIRECT button only
  │
END
```

### 3.2 Furnish Decision Tree

```
START: OIC clicks FURNISH
  │
  ├── Frontend Validation
  │     │
  │     ├── FAIL → Show validation error
  │     │
  │     └── PASS → Call Backend API
  │           │
  │           ├── Check Notice Stage
  │           │     │
  │           │     ├── NOT FURNISHABLE → Return error
  │           │     │
  │           │     └── FURNISHABLE → Check existing offender
  │           │           │
  │           │           ├── EXISTS → PATCH (overwrite)
  │           │           │
  │           │           └── NOT EXISTS → POST (create new)
  │           │
  │           ├── Set offender_indicator = Y
  │           ├── Clear previous offender indicator
  │           ├── Update processing stage
  │           │
  │           └── Return success
  │
END
```

### 3.3 Redirect Decision Tree

```
START: OIC clicks REDIRECT
  │
  ├── Frontend Validation
  │     │
  │     ├── FAIL → Show validation error
  │     │
  │     └── PASS → Call Backend API
  │           │
  │           ├── Check can redirect
  │           │     │
  │           │     ├── CANNOT → Return error with reason
  │           │     │
  │           │     └── CAN → Process redirect
  │           │           │
  │           │           ├── Clear current offender indicator
  │           │           ├── Create/Update target offender
  │           │           ├── Set new offender indicator
  │           │           ├── Reset processing stage
  │           │           │
  │           │           └── Return success
  │
END
```

---

## 4. Assumptions Log

### 4.1 Assumptions Made

| ID | Assumption | Basis | Impact |
|----|------------|-------|--------|
| ASM-001 | Only one offender per role (Owner/Hirer/Driver) can be current at a time | Functional doc mentions "Current Offender" as singular | Single offender_indicator per role |
| ASM-002 | Furnish overwrites existing offender if exists | Section 4.6.2.3 mentions PATCH for existing | No separate "replace" function needed |
| ASM-003 | Update only changes particulars, not offender indicator | Section 4.8 describes update as editing existing | Simpler update logic |
| ASM-004 | Redirect resets processing stage to starting stage for target role | Logical flow for re-processing | Stage reset logic needed |

### 4.2 Questions for Clarification

| ID | Question | Status |
|----|----------|--------|
| Q-001 | Can an OIC furnish/redirect/update multiple notices at once in Section 4? | Assumed NO (Batch is Section 5) |
| Q-002 | What happens to suspension status when redirect occurs? | Assumed: Follows existing suspension rules |

---

*Document generated for OCMS 41 Section 4 condition planning*
