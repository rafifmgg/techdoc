# OCMS 20 - HST Suspension: API Plan

## Document Information

| Item | Value |
|------|-------|
| Version | 1.0 |
| Date | 2026-01-07 |
| Feature | HST (House-to-House Search/Tenant) Suspension |
| Source | Functional Flow HST.drawio, Functional Document v1.1 |

---

## 1. Internal APIs (OCMS Staff Portal to Backend)

### 1.1 Check Offender HST Status

Check if offender ID is already suspended under HST.

| Property | Value |
|----------|-------|
| Endpoint | `/hstexists` |
| Method | POST |
| Content-Type | application/json |
| Authentication | Bearer Token |

**Request Body:**
```json
{
  "idNo": "S1234567A"
}
```

**Success Response (200 OK) - Not in HST:**
```json
{
  "data": {
    "appCode": "OCMS-2000",
    "isHstId": false,
    "offenderName": "John Tan",
    "latestAddress": {
      "postalCode": "123456",
      "blockHseNo": "123",
      "streetName": "Example Street",
      "floorUnitNo": "#01-01",
      "buildingName": "Example Building"
    },
    "noticesCount": 5
  }
}
```

**Success Response (200 OK) - Already in HST:**
```json
{
  "data": {
    "appCode": "OCMS-2000",
    "isHstId": true,
    "hstIdNo": "S1234567A",
    "message": "ID is suspended under HST"
  }
}
```

**Error Response (400 Bad Request):**
```json
{
  "data": {
    "appCode": "OCMS-4000",
    "message": "idNo is required"
  }
}
```

---

### 1.2 Apply HST Suspension (Manual)

Create HST record and suspend all notices under this ID with TS-HST.

| Property | Value |
|----------|-------|
| Endpoint | `/v1/apply-hst` |
| Method | POST |
| Content-Type | application/json |
| Authentication | Bearer Token |

**Request Body:**
```json
{
  "idNo": "S1234567A",
  "idType": "N",
  "name": "John Tan",
  "postalCode": "123456",
  "blkHseNo": "123",
  "streetName": "Example Street",
  "floorNo": "01",
  "unitNo": "01",
  "bldgName": "Example Building"
}
```

**Success Response (200 OK):**
```json
{
  "data": {
    "appCode": "OCMS-2000",
    "message": "HST ID created successfully",
    "hstIdCreated": true,
    "noticesSuspended": [
      {
        "noticeNo": "500500001A",
        "status": "SUSPENDED",
        "suspensionType": "TS-HST"
      },
      {
        "noticeNo": "500500002B",
        "status": "SUSPENDED",
        "suspensionType": "TS-HST"
      }
    ],
    "totalSuspended": 2
  }
}
```

**Success Response - HST Created but No Notices:**
```json
{
  "data": {
    "appCode": "OCMS-2000",
    "message": "HST ID created successfully. There were no Notices to suspend.",
    "hstIdCreated": true,
    "noticesSuspended": [],
    "totalSuspended": 0
  }
}
```

**Error Response (400 Bad Request):**
```json
{
  "data": {
    "appCode": "OCMS-4000",
    "message": "Unable to create HST ID. Notices were not suspended."
  }
}
```

---

### 1.3 Update HST Record (New Address Found)

Update HST record with new address when found via DataHive/MHA.

| Property | Value |
|----------|-------|
| Endpoint | `/v1/update-hst/{idNo}` |
| Method | POST |
| Content-Type | application/json |
| Authentication | Bearer Token |

**Path Parameter:**
- `idNo` - The offender ID number (e.g., S1234567A)

**Request Body:**
```json
{
  "name": "John Tan Updated",
  "postalCode": "654321",
  "blkHseNo": "456",
  "streetName": "New Street",
  "floorNo": "02",
  "unitNo": "02",
  "bldgName": "New Building"
}
```

**Success Response (200 OK):**
```json
{
  "data": {
    "appCode": "OCMS-2000",
    "message": "HST record updated successfully",
    "noticesUpdated": 5
  }
}
```

**Error Response (400 Bad Request):**
```json
{
  "data": {
    "appCode": "OCMS-4003",
    "message": "HST ID not found"
  }
}
```

---

### 1.4 Revive HST Suspension

Lift all HST suspensions under an ID and delete HST record.

| Property | Value |
|----------|-------|
| Endpoint | `/v1/revive-hst` |
| Method | POST |
| Content-Type | application/json |
| Authentication | Bearer Token |

**Request Body:**
```json
{
  "idNo": "S1234567A",
  "revivalReason": "New address confirmed valid",
  "remarks": "Offender contacted successfully"
}
```

**Success Response (200 OK):**
```json
{
  "data": {
    "appCode": "OCMS-2000",
    "message": "HST suspensions revived successfully",
    "noticesRevived": [
      {
        "noticeNo": "500500001A",
        "previousStatus": "TS-HST",
        "newStatus": "ACTIVE"
      },
      {
        "noticeNo": "500500002B",
        "previousStatus": "TS-HST",
        "newStatus": "ACTIVE"
      }
    ],
    "totalRevived": 2,
    "hstDeleted": true
  }
}
```

**Error Response (400 Bad Request):**
```json
{
  "data": {
    "appCode": "OCMS-4003",
    "message": "HST ID not found"
  }
}
```

---

### 1.5 Get HST List (Staff Portal Display)

Retrieve list of HST records for Staff Portal display.

| Property | Value |
|----------|-------|
| Endpoint | `/hstlist` |
| Method | POST |
| Content-Type | application/json |
| Authentication | Bearer Token |

**Request Body:**
```json
{
  "$skip": 0,
  "$limit": 10,
  "filter": {
    "idNo": "",
    "dateFrom": "2025-01-01",
    "dateTo": "2026-01-07"
  }
}
```

**Success Response (200 OK):**
```json
{
  "total": 100,
  "limit": 10,
  "skip": 0,
  "data": [
    {
      "idNo": "S1234567A",
      "idType": "N",
      "name": "John Tan",
      "postalCode": "123456",
      "blkHseNo": "123",
      "streetName": "Example Street",
      "createdDate": "2025-11-01T00:00:00",
      "noticesCount": 5,
      "lastDataHiveCheck": "2025-12-01T00:00:00"
    }
  ]
}
```

---

## 2. External APIs

### 2.1 DataHive Query (Monthly Cron)

Query DataHive for latest address information.

| Property | Value |
|----------|-------|
| Integration | DataHive |
| Schedule | Monthly cron |
| Purpose | Check for new address for HST IDs |

**Refer to DataHive Integration Technical Document for detailed specification.**

---

### 2.2 MHA Query

Query MHA for address via SFTP.

| Property | Value |
|----------|-------|
| Integration | MHA |
| Method | SFTP |
| Purpose | Check for new address for HST IDs |

**Refer to MHA Integration Technical Document for detailed specification.**

---

## 3. Error Codes Summary

| Code | Message | Scenario |
|------|---------|----------|
| OCMS-2000 | Success | Operation completed successfully |
| OCMS-4000 | Required field missing | idNo, idType not provided |
| OCMS-4001 | API authentication failed | JWT validation failed |
| OCMS-4003 | HST ID not found | ID not in HST table |
| OCMS-4004 | Invalid operation | HST ID already exists/revived |
| OCMS-5001 | Report generation failed | Excel generation error |
| OCMS-5004 | Blob upload failed | Azure storage error |

---

## 4. Database Tables

| Table | Purpose |
|-------|---------|
| ocms_hst | HST record master table (stores ID with address) |
| ocms_suspended_notice | Suspension records (TS-HST, date_of_revival for revival) |
| ocms_offence_notice_owner_driver | Current offender info |
| ocms_offence_notice_owner_driver_addr | Offender address history |
| ocms_temp_unc_hst_addr | Temporary address storage from MHA/DataHive |
| ocms_adhoc_nro_queue | Adhoc NRO query queue |

**Note:** `ocms_hst` does not have a `status` column. HST active status is determined by existence in the table. Upon revival, the record is deleted from `ocms_hst`.

---

*End of Document*
