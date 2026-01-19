# OCMS 19 - Revive Suspensions: Condition Planning Document

## Document Information
| Attribute | Value |
|-----------|-------|
| Version | 1.0 |
| Date | 2026-01-07 |
| OCMS Number | OCMS 19 |
| Feature | Revive Suspensions |
| Source | Functional Document v1.1, Backend Code Analysis |

---

## 1. Frontend Validations (Staff Portal)

### 1.1 User Permission Validation
| Rule ID | Field | Validation Rule | Error Message |
|---------|-------|-----------------|---------------|
| FE-001 | User Role | User must have permission to initiate Revival | User not authorised to revive suspensions |
| FE-002 | User Role | Only HST module users can revive TS-HST | User not authorised to revive TS-HST |

### 1.2 Suspension Code Validation
| Rule ID | Field | Validation Rule | Error Message |
|---------|-------|-----------------|---------------|
| FE-003 | Suspension Code | TS-HST can only be revived via HST module | TS-HST can only be revived via HST module |
| FE-004 | Suspension Code | TS-RED can only be revived by PLUS OICs | TS-RED can only be revived via PLUS Staff Portal |

### 1.3 Multiple Notice Validation
| Rule ID | Field | Validation Rule | Error Message |
|---------|-------|-----------------|---------------|
| FE-005 | Notice Selection | Cannot revive PS for multiple Notices | Only one Notice with PS can be revived at a time |
| FE-006 | Notice Selection | Can revive TS for multiple Notices | N/A (allowed) |

### 1.4 Form Field Validation
| Rule ID | Field | Validation Rule | Error Message |
|---------|-------|-----------------|---------------|
| FE-007 | Revival Reason | Revival reason is required | Please select a Revival Reason |
| FE-008 | Revival Remarks | Max 50 characters | Revival remarks cannot exceed 50 characters |

---

## 2. Backend Validations

### 2.1 Authentication & Authorization
| Rule ID | Check | Validation Rule | Error Code | Error Message |
|---------|-------|-----------------|------------|---------------|
| BE-001 | JWT Token | Source must have valid JWT token | OCMS-4000 | Invalid JWT token |
| BE-002 | API Key | Source must have valid API key | OCMS-4000 | Invalid Auth Token |
| BE-003 | Permission | Source allowed to trigger revival for given code | OCMS-4007 | User not authorised |

### 2.2 Request Validation
| Rule ID | Field | Validation Rule | Error Code | Error Message |
|---------|-------|-----------------|------------|---------------|
| BE-004 | noticeNo | Must be non-empty | OCMS-4000 | Notice number is required |
| BE-005 | revivalReason | Must be non-empty | OCMS-4000 | Revival reason is required |
| BE-006 | officerAuthorisingRevival | Must be non-empty | OCMS-4000 | Officer ID is required |
| BE-007 | revivalRemarks | Max 50 characters | OCMS-4000 | Revival remarks too long |
| BE-008 | suspensionType | Must be TS or PS | OCMS-4004 | Unknown suspension type |

### 2.3 Notice Validation
| Rule ID | Check | Validation Rule | Error Code | Error Message |
|---------|-------|-----------------|------------|---------------|
| BE-009 | Notice Exists | Notice must exist in database | OCMS-4001 | Invalid Notice Number |
| BE-010 | Active Suspension | Notice must have active suspension | OCMS-4002 | Notice not suspended |
| BE-011 | Suspension Type | Notice suspension type must match request | OCMS-4004 | Suspension type mismatch |
| BE-012 | Court Stage | Notice must not be under Court processing (except CPC/CFC) | OCMS-4007 | Notice is under Court processing |

### 2.4 Suspended Notice Validation
| Rule ID | Check | Validation Rule | Error Code | Error Message |
|---------|-------|-----------------|------------|---------------|
| BE-013 | Record Exists | Suspended notice record must exist | OCMS-4005 | Suspended notice records not found |
| BE-014 | Active TS | Active TS must exist (date_of_revival IS NULL) | OCMS-4006 | Active TS suspension not found |
| BE-015 | Not HST | Suspension code must not be HST (for regular endpoint) | OCMS-4007 | Revival is not allowed for TS-HST notices |

### 2.5 PS-Specific Validation
| Rule ID | Check | Validation Rule | Error Code | Error Message |
|---------|-------|-----------------|------------|---------------|
| BE-016 | PS Support | PS revival not yet supported via PLUS | OCMS-4003 | PS revival not supported |
| BE-017 | Single PS | Only one PS can be revived per request | OCMS-4000 | Multiple PS revival not allowed |

---

## 3. Business Rules for Revival

### 3.1 Global Revival Rules
| Rule ID | Condition | Rule | Description |
|---------|-----------|------|-------------|
| BR-001 | Display | Display most recent suspension only | Revived suspensions not shown in View Notice Overview tab |
| BR-002 | OCMS Permission | OCMS can lift all TS/PS codes | Exception: TS-HST only via HST module |
| BR-003 | PLUS Permission | PLUS can lift all TS/PS codes | Exception: TS-HST only via OCMS HST module |
| BR-004 | Auto PS Revival | Auto revive existing PS when new PS applied | Applies to ERP Reason of Suspension only |
| BR-005 | Global TS Revival | Reviving TS revives all active TS on Notice | Exceptions: TS-HST, TS-RED |
| BR-006 | Global TS+PS Revival | Can revive all active TS and PS on one Notice | Allowed for authorized users |
| BR-007 | Multiple PS | Cannot revive PS for multiple Notices | One PS revival per request only |
| BR-008 | Multiple TS | Can revive TS for multiple Notices | Same revival reason/date used |
| BR-009 | PS-FP Revival | Reviving PS-FP triggers refund | Refund date recorded for MVP |
| BR-010 | Resume Processing | Notice resumes from suspension point | Based on Next Processing Date |
| BR-011 | No eNotification | No eNA/eReminder after revival | Notifications resume at next stage |

### 3.2 Special PS Rules for CRS Suspensions
| Rule ID | Existing PS | New PS (CRS) | Action |
|---------|-------------|--------------|--------|
| BR-012 | DIP | PS-FP or PS-PRA | Apply WITHOUT revival |
| BR-013 | FOR | PS-FP or PS-PRA | Apply WITHOUT revival |
| BR-014 | MID | PS-FP or PS-PRA | Apply WITHOUT revival |
| BR-015 | RIP | PS-FP or PS-PRA | Apply WITHOUT revival |
| BR-016 | RP2 | PS-FP or PS-PRA | Apply WITHOUT revival |
| BR-017 | Other PS | PS-FP or PS-PRA | NOT allowed |
| BR-018 | Any PS | Non-CRS (e.g., PS-APP, APE) | Requires PS revival first |

### 3.3 TS Looping Rules
| Rule ID | Condition | Action |
|---------|-----------|--------|
| BR-019 | TS-CLV revived + other TS exists | Apply TS with LATEST revival date to VON |
| BR-020 | TS revived + no other TS | Clear all suspension fields in VON |
| BR-021 | VIP at RR3/DR3 unpaid | Re-apply TS-CLV (looping) |
| BR-022 | VIP paid (FP status) | Stop TS-CLV looping |

---

## 4. Auto Revival Conditions

### 4.1 Query Conditions for Auto Revival
| Condition | Value | Description |
|-----------|-------|-------------|
| suspension_type | TS | Only Temporary Suspensions |
| date_of_revival | IS NULL | Not yet revived |
| due_date_of_revival | <= TODAY | Due date reached or passed |

### 4.2 Auto Revival Triggers
| Trigger | Condition | Revival Reason |
|---------|-----------|----------------|
| TS Expiry | due_date_of_revival <= TODAY | SPO |
| TS-OLD VIP | 21-day investigation complete | Auto |
| New PS Applied | Existing PS being replaced | PSR |

---

## 5. HST Revival Conditions

### 5.1 HST-Specific Validations
| Rule ID | Field | Validation Rule | Error Code |
|---------|-------|-----------------|------------|
| HST-001 | idNo | Must be non-empty | OCMS-4000 |
| HST-002 | idNo | Must exist in ocms_hst table | OCMS-4001 |
| HST-003 | All address fields | All fields required | OCMS-4000 |

### 5.2 HST Revival Flow
1. Validate idNo is provided
2. Check HST ID exists in database
3. Find all notices with TS-HST for this HST ID
4. Revive each TS-HST suspension
5. Remove HST record from ocms_hst table
6. Return results for each affected notice

---

## 6. Decision Trees

### 6.1 Revival Type Decision
```
Is suspension type TS?
├── Yes → Is suspension code HST?
│   ├── Yes → Route to HST Revival endpoint
│   └── No → Process via standard TS Revival
└── No (PS) → Is PLUS request?
    ├── Yes → Return "PS revival not supported"
    └── No (OCMS) → Process PS Revival
```

### 6.2 Post-Revival Action Decision
```
Revival completed successfully?
├── No → Return error response
└── Yes → Check for follow-up action
    ├── TS-CLV at VIP stage → Re-apply TS-CLV
    ├── TS-PDP → Re-apply TS-PDP
    ├── PS-FP → Mark refund date
    └── No follow-up → End process
```

### 6.3 Multiple TS Handling Decision
```
TS revived for notice?
├── Check for other active TS
│   ├── Other TS exists → Apply TS with latest due date to VON
│   └── No other TS → Clear all suspension fields in VON
└── Update Next Processing Date = NOW + 2 days
```

---

## 7. Database Field Updates

### 7.1 On Revival Success
| Table | Field | New Value |
|-------|-------|-----------|
| ocms_suspended_notice | date_of_revival | NOW() |
| ocms_suspended_notice | revival_reason | [revival code] |
| ocms_suspended_notice | officer_authorising_revival | [officer ID] |
| ocms_suspended_notice | revival_remarks | [remarks if any] |
| ocms_valid_offence_notice | suspension_type | NULL |
| ocms_valid_offence_notice | epr_reason_suspension | NULL |
| ocms_valid_offence_notice | due_date_of_revival | NULL |
| ocms_valid_offence_notice | next_processing_date | NOW() + 2 days |
| ocms_valid_offence_notice | upd_user_id | [officer ID] |
| ocms_valid_offence_notice | upd_date | NOW() |

### 7.2 On Revival with Looping TS
| Table | Field | New Value |
|-------|-------|-----------|
| ocms_valid_offence_notice | suspension_type | TS |
| ocms_valid_offence_notice | epr_reason_suspension | [next TS code] |
| ocms_valid_offence_notice | due_date_of_revival | [next TS due date] |

---

## 8. Assumptions Log

| ID | Assumption | Rationale |
|----|------------|-----------|
| A-001 | PS revival via PLUS will be implemented in future phase | Backend code shows OCMS-4003 error for PS via PLUS |
| A-002 | TS-RED can only be revived by PLUS | Based on functional document specification |
| A-003 | Revival remarks limited to 50 characters | Backend validation shows max 50 chars |
| A-004 | Refund processing for PS-FP is MVP scope (date only) | Functional document states "store date only for MVP" |

---

## 9. Source Permissions Matrix

| Action | OCMS Staff | PLUS Staff | OCMS Backend |
|--------|------------|------------|--------------|
| Revive TS | Yes | Yes | Yes (Auto) |
| Revive PS | Yes | No (OCMS-4003) | Yes |
| Revive TS-HST | Yes (HST Module only) | No | No |
| Revive TS-RED | No | Yes | No |
| Revive Multiple TS | Yes | Yes | Yes |
| Revive Multiple PS | No | No | No |
