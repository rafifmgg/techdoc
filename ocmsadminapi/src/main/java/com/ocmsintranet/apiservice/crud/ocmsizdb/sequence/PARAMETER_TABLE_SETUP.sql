-- =====================================================================
-- Sequence Configuration using Parameter Table
-- =====================================================================
-- This script populates the ocms_parameter table with sequence mappings
-- to replace hardcoded switch statements in SequenceRepository.java
--
-- Structure:
-- - code = 'SEQUENCE' (identifies this as a sequence configuration)
-- - parameter_id = subsystem identifier (CES, EEPS, OCMS, etc.)
-- - description = human-readable description
-- - value = comma-separated list of codes that map to this sequence
-- - Full sequence name will be constructed as: ocmsizmgr.seq_notice_no_{parameter_id_lowercase}
-- =====================================================================

-- Insert sequence configurations
INSERT INTO ocmsizmgr.ocms_parameter 
    (parameter_id, code, description, value, cre_date, cre_user_id) 
VALUES
    -- Car Park System
    ('CES', 'SEQUENCE', 'Sequence for CES', '001,CES', GETDATE(), 'ocmsiz_app_conn'),
    
    -- Electronic Enforcement Payment System
    ('EEPS', 'SEQUENCE', 'Sequence for EEPS', '003,EEPS', GETDATE(), 'ocmsiz_app_conn'),
    
    -- OCMS
    ('OCMS', 'SEQUENCE', 'Sequence for OCMS', '004,OCMS', GETDATE(), 'ocmsiz_app_conn'),
    
    -- PLUS
    ('PLUS', 'SEQUENCE', 'Sequence for PLUS', '005,PLUS', GETDATE(), 'ocmsiz_app_conn'),
    
    -- MEV (Multiple codes: 006-010, MEV)
    ('MEV', 'SEQUENCE', 'Sequence for MEV', '006,007,008,009,010,MEV', GETDATE(), 'ocmsiz_app_conn'),
    
    -- REPCCS ENC
    ('REPCCS_ENC', 'SEQUENCE', 'Sequence for REPCCS ENC', '020,021,REPCCS_ENC', GETDATE(), 'ocmsiz_app_conn'),
    
    -- REPCCS EHT
    ('REPCCS_EHT', 'SEQUENCE', 'Sequence for REPCCS EHT', '023,REPCCS_EHT', GETDATE(), 'ocmsiz_app_conn');

-- Verify the inserted data
SELECT 
    parameter_id,
    code,
    description,
    value,
    cre_date,
    cre_user_id
FROM ocmsizmgr.ocms_parameter
WHERE code = 'SEQUENCE'
ORDER BY parameter_id;

-- =====================================================================
-- How it works:
-- =====================================================================
-- 1. When getNextNoticeNumber("001") is called:
--    - First tries to find parameter_id='001', code='SEQUENCE' (not found)
--    - Then searches value fields for '001'
--    - Finds CES record with value='001,CES'
--    - Gets parameter_id='CES'
--    - Builds sequence name: 'ocmsizmgr.seq_notice_no_ces'
--    - Returns the constructed sequence name
--
-- 2. When getNextNoticeNumber("CES") is called:
--    - Finds parameter_id='CES', code='SEQUENCE' directly
--    - Builds sequence name: 'ocmsizmgr.seq_notice_no_ces'
--    - Returns the constructed sequence name
--
-- 3. When getNextNoticeNumber("006") is called:
--    - First tries to find parameter_id='006', code='SEQUENCE' (not found)
--    - Then searches value fields for '006'
--    - Finds MEV record with value='006,007,008,009,010,MEV'
--    - Gets parameter_id='MEV'
--    - Builds sequence name: 'ocmsizmgr.seq_notice_no_mev'
--    - Returns the constructed sequence name
-- =====================================================================

-- =====================================================================
-- Adding new subsystems (Example):
-- =====================================================================
-- To add a new subsystem, simply insert a new record:
--
-- INSERT INTO ocmsizmgr.ocms_parameter 
--     (parameter_id, code, description, value, cre_date, cre_user_id) 
-- VALUES
--     ('NEW_SUBSYSTEM', 'SEQUENCE', 'Sequence for New Subsystem', '099,NEW_SUBSYSTEM', 
--      GETDATE(), 'ocmsiz_app_conn');
--
-- The code will automatically build: ocmsizmgr.seq_notice_no_new_subsystem
-- NO CODE CHANGES REQUIRED! 🎉
-- =====================================================================
