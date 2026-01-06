# Sequence Configuration Using Parameter Table

## Overview
This implementation uses the existing `ocms_parameter` table to map subsystem codes to database sequences, eliminating the need for hardcoded switch statements.

## ✅ Benefits
- **No new table needed** - Uses existing `ocms_parameter` table
- **No code changes** when adding new subsystems
- **Easy maintenance** - DBAs can manage via SQL or admin UI
- **Scalable** - Supports unlimited subsystems
- **Consistent** - Follows existing parameter table pattern

---

## 📋 Table Structure

```
ocms_parameter table:
├── parameter_id (PK)  → Subsystem identifier (e.g., "CES", "EEPS", "MEV")
├── code (PK)          → "SEQUENCE" (identifies sequence configurations)
├── description        → Human-readable description
├── value              → Comma-separated codes that map to this sequence
└── ... (audit fields)

Full sequence name is constructed as: ocmsizmgr.seq_notice_no_{parameter_id_lowercase}
```

---

## 🔍 How It Works

### Example 1: Direct Lookup
```java
getNextNoticeNumber("CES")
```
1. Looks for `parameter_id='CES'` AND `code='SEQUENCE'`
2. Finds record directly
3. Builds sequence name: `ocmsizmgr.seq_notice_no_ces` (from parameter_id)
4. Returns the constructed sequence name

### Example 2: Code in Value Field
```java
getNextNoticeNumber("001")
```
1. Tries `parameter_id='001'` AND `code='SEQUENCE'` (not found)
2. Searches all SEQUENCE parameters' `value` fields
3. Finds CES record with `value='001,CES'`
4. Gets `parameter_id='CES'`
5. Builds sequence name: `ocmsizmgr.seq_notice_no_ces`
6. Returns the constructed sequence name

### Example 3: Multiple Codes Mapping to Same Sequence
```java
getNextNoticeNumber("007")
```
1. Tries `parameter_id='007'` AND `code='SEQUENCE'` (not found)
2. Searches value fields
3. Finds MEV record with `value='006,007,008,009,010,MEV'`
4. Gets `parameter_id='MEV'`
5. Builds sequence name: `ocmsizmgr.seq_notice_no_mev`
6. Returns the constructed sequence name

---

## 📊 Current Configuration

| parameter_id | code     | description                                   | value                        |
|--------------|----------|-----------------------------------------------|------------------------------|
| CES          | SEQUENCE | Sequence for Car Park System                  | 001,CES                      |
| EEPS         | SEQUENCE | Sequence for Electronic Enforcement Payment   | 003,EEPS                     |
| OCMS         | SEQUENCE | Sequence for OCMS                             | 004,OCMS                     |
| PLUS         | SEQUENCE | Sequence for PLUS                             | 005,PLUS                     |
| MEV          | SEQUENCE | Sequence for MEV                              | 006,007,008,009,010,MEV      |
| REPCCS_ENC   | SEQUENCE | Sequence for REPCCS ENC                       | 020,021,REPCCS_ENC           |
| REPCCS_EHT   | SEQUENCE | Sequence for REPCCS EHT                       | 023,REPCCS_EHT               |

**Note:** The actual sequence name is built as `ocmsizmgr.seq_notice_no_{parameter_id_lowercase}`

---

## 🚀 Adding New Subsystems

Simply insert a new record:

```sql
INSERT INTO ocmsizmgr.ocms_parameter 
    (parameter_id, code, description, value, cre_date, cre_user_id) 
VALUES
    ('NEW_SYSTEM', 'SEQUENCE', 'Sequence for New System', '099,NEW_SYSTEM', 
     GETDATE(), 'ocmsiz_app_conn');
```

The code will automatically build the sequence name as: `ocmsizmgr.seq_notice_no_new_system`

**No code deployment required!** 🎉

---

## 🔧 Code Changes Made

### 1. SequenceRepository.java
- Added `@Autowired ParameterRepository`
- Replaced switch statement with database lookup
- Added `buildSequenceName()` - Constructs sequence name from parameter_id
- Added `findParameterBySubsystem()` - Direct lookup by parameter_id
- Added `findParameterBySubsystemInValue()` - Search in value field for subsystem codes

### 2. ParameterRepository.java
- Added `findByCode(String code)` method for efficient querying
- Added `findByCodeAndValueContaining()` with custom JPQL query for optimal performance

---

## ⚡ Performance Optimization

### Database-Level Search
Instead of loading all SEQUENCE parameters and looping through them in Java, we use a **custom JPQL query** that performs the search at the database level:

```java
@Query("SELECT p FROM Parameter p WHERE p.code = :code AND " +
       "(p.value = :subsystem OR " +
       "p.value LIKE CONCAT(:subsystem, ',%') OR " +
       "p.value LIKE CONCAT('%,', :subsystem, ',%') OR " +
       "p.value LIKE CONCAT('%,', :subsystem))")
Optional<Parameter> findByCodeAndValueContaining(@Param("code") String code, 
                                                  @Param("subsystem") String subsystem);
```

**Benefits:**
- ✅ **No Java loops** - Search happens in database
- ✅ **Single query** - One database call instead of loading all records
- ✅ **Indexed search** - Database can use indexes efficiently
- ✅ **Exact matching** - Handles comma-separated values correctly
- ✅ **Scalable** - Performance doesn't degrade with more subsystems

**Query Patterns:**
- `value = '001'` → Matches exact value
- `value LIKE '001,%'` → Matches at start (e.g., "001,CES")
- `value LIKE '%,001,%'` → Matches in middle (e.g., "CES,001,EEPS")
- `value LIKE '%,001'` → Matches at end (e.g., "CES,001")

---

## 🧪 Testing

### Test Case 1: All existing codes work
```java
String[] testCodes = {"001", "CES", "003", "EEPS", "004", "OCMS", 
                      "005", "PLUS", "006", "007", "008", "009", "010", "MEV",
                      "020", "021", "REPCCS_ENC", "023", "REPCCS_EHT"};

for (String code : testCodes) {
    Long nextNumber = sequenceRepository.getNextNoticeNumber(code);
    assertNotNull(nextNumber);
}
```

### Test Case 2: Case-insensitive matching
```java
assertEquals(
    sequenceRepository.getNextNoticeNumber("ces"),
    sequenceRepository.getNextNoticeNumber("CES")
);
```

### Test Case 3: Invalid code throws exception
```java
assertThrows(IllegalArgumentException.class, () -> {
    sequenceRepository.getNextNoticeNumber("INVALID");
});
```

---

## 📝 Response to Code Review

**Original Comment:**
> "is this how it should be handled? if i have 20 more, are you going to list out 20 more case xx?"

**Solution:**
✅ Switch statement completely removed  
✅ Uses existing `ocms_parameter` table  
✅ Adding 20 more subsystems = 20 SQL INSERT statements  
✅ Zero code changes required  
✅ Follows existing system patterns  

---

## 🔄 Migration Steps

1. **Run SQL script** - Execute `PARAMETER_TABLE_SETUP.sql`
2. **Verify data** - Check records inserted correctly
3. **Deploy code** - Deploy updated `SequenceRepository.java` and `ParameterRepository.java`
4. **Test** - Verify all existing subsystem codes work
5. **Monitor** - Check logs for any issues

---

## 💡 Best Practices

1. **Use uppercase** for parameter_id (e.g., "CES" not "ces")
2. **Comma-separated** codes in description with no spaces after commas
3. **Full sequence name** in value field including schema
4. **Document** new subsystems in description field
5. **Test** new configurations in UAT before production

---

## 🆘 Troubleshooting

### Error: "Unknown subsystem: 'XXX'"
**Cause:** Subsystem not configured in parameter table  
**Fix:** Insert record with `parameter_id='XXX'` and `code='SEQUENCE'`

### Error: Sequence returns null
**Cause:** Sequence name in value field is incorrect  
**Fix:** Verify sequence exists in database and update value field

### Error: Wrong sequence used
**Cause:** Code appears in multiple description fields  
**Fix:** Ensure each code appears in only one SEQUENCE parameter's description

---

## 📚 Related Files

- `SequenceRepository.java` - Main repository with lookup logic
- `ParameterRepository.java` - JPA repository with findByCode method
- `PARAMETER_TABLE_SETUP.sql` - SQL script to populate data
- `Parameter.java` - Entity class for ocms_parameter table

---

## 🎯 Future Enhancements

1. Add caching for sequence configurations (reduce DB calls)
2. Create admin UI for managing sequence configurations
3. Add validation to prevent duplicate codes in descriptions
4. Implement audit logging for sequence configuration changes
