# API Plan: [API Name]

<!--
Template for: API Planning and Documentation
Use this template when planning new APIs or documenting existing APIs.
-->

## Overview

| Attribute | Value |
| --- | --- |
| API Name | [API Name] |
| Version | v1.0 |
| Author | [Author Name] |
| Created Date | [DD/MM/YYYY] |
| Last Updated | [DD/MM/YYYY] |
| Status | Draft / In Review / Approved |
| Related Document | OCMS [X] Technical Doc |

---

## 1. Purpose

[Brief description of what this API does and why it's needed - 2-3 sentences]

---

## 2. API Endpoints

### 2.1 [Endpoint Name]

| Attribute | Value |
| --- | --- |
| Method | GET / POST / PUT / PATCH / DELETE |
| URL | `/api/v1/[resource]/[action]` |
| Authentication | Bearer Token / API Key / None |
| Rate Limit | [X] requests per [minute/hour] |
| Timeout | [X] seconds |

#### Request

**Headers:**

| Header | Required | Description |
| --- | --- | --- |
| Authorization | Yes | Bearer {token} |
| Content-Type | Yes | application/json |
| X-Request-ID | No | Unique request identifier for tracing |

**Path Parameters:**

| Parameter | Type | Required | Description |
| --- | --- | --- | --- |
| [param_name] | string | Yes | [Description] |

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
| --- | --- | --- | --- | --- |
| [param_name] | string | No | [default] | [Description] |
| page | integer | No | 1 | Page number for pagination |
| limit | integer | No | 20 | Number of records per page |

**Request Body:**

```json
{
  "field_1": "string",
  "field_2": 123,
  "field_3": true,
  "nested_object": {
    "sub_field_1": "string",
    "sub_field_2": "string"
  },
  "array_field": [
    {
      "item_field_1": "string",
      "item_field_2": 123
    }
  ]
}
```

**Request Body Schema:**

| Field | Type | Required | Validation | Description |
| --- | --- | --- | --- | --- |
| field_1 | string | Yes | max: 100 chars | [Description] |
| field_2 | integer | Yes | min: 0, max: 9999 | [Description] |
| field_3 | boolean | No | - | [Description] |
| nested_object | object | Yes | - | [Description] |
| nested_object.sub_field_1 | string | Yes | - | [Description] |
| array_field | array | No | max: 10 items | [Description] |

#### Response

**Success Response (200 OK):**

```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": {
    "id": "123",
    "field_1": "value",
    "field_2": 123,
    "created_at": "2025-01-05T10:30:00Z",
    "updated_at": "2025-01-05T10:30:00Z"
  },
  "meta": {
    "total": 100,
    "page": 1,
    "limit": 20,
    "total_pages": 5
  }
}
```

**Response Schema:**

| Field | Type | Description |
| --- | --- | --- |
| success | boolean | Indicates if the operation was successful |
| message | string | Human-readable message |
| data | object | Response payload |
| data.id | string | Unique identifier |
| meta | object | Pagination metadata (for list endpoints) |

---

### 2.2 [Endpoint Name 2]

| Attribute | Value |
| --- | --- |
| Method | [METHOD] |
| URL | `/api/v1/[resource]/[action]` |
| Authentication | Bearer Token |

<!-- Repeat the request/response structure as needed -->

---

## 3. Error Responses

### Standard Error Format

```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "Human-readable error message",
    "details": [
      {
        "field": "field_name",
        "message": "Specific field error"
      }
    ]
  },
  "request_id": "uuid-for-tracing"
}
```

### Error Codes

| HTTP Status | Error Code | Description | Resolution |
| --- | --- | --- | --- |
| 400 | VALIDATION_ERROR | Request validation failed | Check request body/parameters |
| 400 | INVALID_FORMAT | Invalid data format | Ensure correct data types |
| 401 | UNAUTHORIZED | Authentication failed | Provide valid token |
| 403 | FORBIDDEN | Insufficient permissions | Check user permissions |
| 404 | NOT_FOUND | Resource not found | Verify resource ID exists |
| 409 | CONFLICT | Resource conflict (duplicate) | Check for existing records |
| 422 | BUSINESS_RULE_ERROR | Business rule violation | See error details |
| 429 | RATE_LIMITED | Too many requests | Wait and retry |
| 500 | INTERNAL_ERROR | Server error | Contact support |
| 503 | SERVICE_UNAVAILABLE | Service temporarily unavailable | Retry later |

---

## 4. Data Mapping

### Request to Database

| API Field | Database Table | Database Field | Transformation |
| --- | --- | --- | --- |
| [api_field] | [table_name] | [db_field] | [transformation if any] |
| [api_field] | [table_name] | [db_field] | Direct mapping |
| [api_field] | [table_name] | [db_field] | Format: YYYY-MM-DD |

### Database to Response

| Database Table | Database Field | API Field | Transformation |
| --- | --- | --- | --- |
| [table_name] | [db_field] | [api_field] | [transformation if any] |
| [table_name] | [db_field] | [api_field] | Direct mapping |

---

## 5. Validation Rules

| Field | Rule | Error Message |
| --- | --- | --- |
| [field_name] | Required | [field_name] is required |
| [field_name] | Max length: 100 | [field_name] must not exceed 100 characters |
| [field_name] | Pattern: [regex] | [field_name] format is invalid |
| [field_name] | Enum: [A, B, C] | [field_name] must be one of: A, B, C |
| [field_name] | Min: 0, Max: 9999 | [field_name] must be between 0 and 9999 |
| [field_name] | Date format: YYYY-MM-DD | [field_name] must be in YYYY-MM-DD format |
| [field_name] | Unique | [field_name] already exists |

---

## 6. Business Logic

### Pre-conditions

- [Condition 1, e.g., User must be authenticated]
- [Condition 2, e.g., User must have required permission]
- [Condition 3, e.g., Related record must exist]

### Processing Steps

1. [Step 1: Validate request]
2. [Step 2: Check permissions]
3. [Step 3: Validate business rules]
4. [Step 4: Process data]
5. [Step 5: Update database]
6. [Step 6: Return response]

### Post-conditions

- [What state the system should be in after successful execution]
- [Any side effects, e.g., notifications sent, logs created]

---

## 7. Security

### Authentication

- [ ] Bearer Token (JWT)
- [ ] API Key
- [ ] OAuth 2.0
- [ ] No Authentication

### Authorization

| Role | Allowed Actions |
| --- | --- |
| Admin | Full access (CRUD) |
| Manager | Read, Update |
| User | Read only |
| Guest | No access |

### Data Protection

- [ ] PII data masked in logs
- [ ] Sensitive fields encrypted at rest
- [ ] HTTPS required
- [ ] Input sanitization applied

---

## 8. Performance

| Metric | Target |
| --- | --- |
| Response Time (p50) | < 200ms |
| Response Time (p95) | < 500ms |
| Response Time (p99) | < 1000ms |
| Throughput | 100 requests/second |
| Availability | 99.9% |

### Caching Strategy

| Cache Type | TTL | Invalidation |
| --- | --- | --- |
| [Cache name] | [Duration] | [When to invalidate] |

---

## 9. Testing

### Test Cases

| Test ID | Scenario | Input | Expected Output |
| --- | --- | --- | --- |
| TC01 | Success case | Valid request | 200 OK with data |
| TC02 | Missing required field | Missing field_1 | 400 VALIDATION_ERROR |
| TC03 | Invalid token | Expired token | 401 UNAUTHORIZED |
| TC04 | Resource not found | Invalid ID | 404 NOT_FOUND |
| TC05 | Duplicate record | Existing data | 409 CONFLICT |

### Sample cURL

```bash
# Success case
curl -X POST \
  'https://api.example.com/api/v1/resource' \
  -H 'Authorization: Bearer {token}' \
  -H 'Content-Type: application/json' \
  -d '{
    "field_1": "value",
    "field_2": 123
  }'
```

---

## 10. Dependencies

| Service/System | Type | Purpose |
| --- | --- | --- |
| [Service Name] | Database | Primary data store |
| [Service Name] | External API | [Purpose] |
| [Service Name] | Message Queue | [Purpose] |

---

## 11. Changelog

| Version | Date | Author | Changes |
| --- | --- | --- | --- |
| 1.0 | [DD/MM/YYYY] | [Author] | Initial version |
| 1.1 | [DD/MM/YYYY] | [Author] | [Changes description] |
