-- =========================================================================
--  Name          : 28_ocmsiztab.sql
--  Author        : Ilham
--  Creation Date : 01 July 2025
--  Description   : Create tables for OCMS EZ
--  Run By        : ocmsizmgr
--
-- =========================================================================
--  Change History :
--  Change By      :
--  Date           :
--  Remarks        :
--
-- =========================================================================

-- global set schema name
-- SET THE SCHEMA NAME HERE ----------------------------------------------------------

:setvar databasename ocmsezdb
:setvar schema_name ocmsezmgr

set nocount on
:out 28_ocmsiztab.log

use [$(databasename)]
GO

select 'script run:', current_timestamp, '$(SQLCMDUSER)', '$(SQLCMDDBNAME)';
GO

PRINT 'Start Init Create Tables For $(databasename)'
GO

-- ========================================================================
-- Table: eocms_payment_matrix
-- ========================================================================

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [$(schema_name)].[eocms_payment_matrix](
    [rule_code] VARCHAR(20) NOT NULL,
    [notice_type] VARCHAR(50) NOT NULL,
    [last_processing_stage] VARCHAR(50) NULL,
    [next_processing_stage] VARCHAR(50) NULL,
    [vehicle_registration_type] VARCHAR(30) NULL,
    [show] BIT NOT NULL,
    [error_code] VARCHAR(20) NOT NULL,
    [payment_acceptance_flag] BIT NOT NULL,
    [owner_driver_indicator] VARCHAR(10) NULL,
    [payable] BIT NULL,
    [send_letter] BIT NULL,
    [cre_date] DATETIME2(2) NOT NULL,
    [cre_user_id] VARCHAR(50) NOT NULL,
    [upd_date] DATETIME2(7) NULL,
    [upd_user_id] VARCHAR(50) NULL,
    CONSTRAINT [PK__eocms_payment_matrix] PRIMARY KEY CLUSTERED
    (
        [rule_code] ASC,
        [notice_type] ASC
    ) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF,
    ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO

-- ========================================================================
-- Table: eocms_valid_offence_notice
-- ========================================================================

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [$(schema_name)].[eocms_valid_offence_notice](
    [notice_no] VARCHAR(10) NOT NULL,
    [amount_paid] NUMERIC(19,2) NULL,
    [amount_payable] NUMERIC(19,2) NULL,
    [crs_date_of_suspension] DATETIME2(7) NULL,
    [crs_reason_of_suspension] VARCHAR(3) NULL,
    [epr_date_of_suspension] DATETIME2(7) NULL,
    [epr_reason_of_suspension] VARCHAR(3) NULL,
    [last_processing_stage] VARCHAR(3) NOT NULL,
    [next_processing_stage] VARCHAR(3) NULL,
    [notice_date_and_time] DATETIME2(7) NOT NULL,
    [offence_notice_type] VARCHAR(1) NOT NULL,
    [payment_acceptance_allowed] VARCHAR(1) NULL,
    [pp_code] VARCHAR(5) NOT NULL,
    [suspension_type] VARCHAR(2) NULL,
    [vehicle_no] VARCHAR(12) NOT NULL,
    [vehicle_registration_type] VARCHAR(1) NULL,
    [refund_identified_date] DATETIME2(7) NULL,
    [is_sync] BIT NOT NULL,
    [cre_date] DATETIME2(7) NOT NULL,
    [cre_user_id] VARCHAR(50) NOT NULL,
    [upd_date] DATETIME2(7) NULL,
    [upd_user_id] VARCHAR(50) NULL,
    CONSTRAINT [PK__eocms_valid_offence_notice] PRIMARY KEY CLUSTERED
    (
        [notice_no] ASC
    ) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF,
    ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO

-- ========================================================================
-- Table: eocms_web_txn_audit
-- ========================================================================

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [$(schema_name)].[eocms_web_txn_audit](
    [web_txn_id] VARCHAR(30) NOT NULL,
    [msg_error] VARCHAR(250) NULL,
    [record_counter] INT NOT NULL,
    [send_date] DATETIME2(7) NOT NULL,
    [send_time] TIME(7) NOT NULL,
    [sender] VARCHAR(5) NOT NULL,
    [status_num] VARCHAR(1) NOT NULL,
    [target_receiver] VARCHAR(5) NOT NULL,
    [txn_detail] VARCHAR(4000) NOT NULL,
    [cre_date] DATETIME2(7) NOT NULL,
    [cre_user_id] VARCHAR(50) NOT NULL,
    [upd_date] DATETIME2(7) NULL,
    [upd_user_id] VARCHAR(50) NULL,
    CONSTRAINT [PK__eocms_web_txn_audit] PRIMARY KEY CLUSTERED
    (
        [web_txn_id] ASC
    ) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF,
    ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO


-- ========================================================================
-- Table: eocms_web_txn_detail
-- ========================================================================

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--
--CREATE TABLE [$(schema_name)].[eocms_web_txn_detail](
--    [receipt_no] VARCHAR(16) NOT NULL,
--    [type_of_receipt] VARCHAR(2) NULL,
--    [remarks] VARCHAR(100) NULL,
--    [transaction_date_and_time] DATETIME2(7) NULL,
--    [offence_notice_no] VARCHAR(10) NULL,
--    [vehicle_no] VARCHAR(12) NULL,
--    [atoms_flag] VARCHAR(1) NULL,
--    [payment_mode] VARCHAR(6) NULL,
--    [total_amount] VARCHAR(10) NULL,
--    [payment_amount] VARCHAR(10) NULL,
--    [sender] VARCHAR(10) NULL,
--    [status] VARCHAR(1) NULL,
--    [error_remarks] VARCHAR(50) NULL,
--    [is_sync] BIT NOT NULL,
--    [cre_date] DATETIME2(7) NOT NULL,
--    [cre_user_id] VARCHAR(50) NOT NULL,
--    [upd_date] DATETIME2(7) NULL,
--    [upd_user_id] VARCHAR(50) NULL,
--    CONSTRAINT [PK__eocms_web_txn_detail] PRIMARY KEY CLUSTERED
--    (
--        [receipt_no] ASC
--    ) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF,
--    ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
--) ON [PRIMARY]
--GO


-- ========================================================================
-- Table: eocms_user_message
-- ========================================================================

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [$(schema_name)].[eocms_user_message](
    [error_code] VARCHAR(10) NOT NULL,
    [error_message] VARCHAR(255) NOT NULL,
    [cre_date] DATETIME2(7) NOT NULL DEFAULT SYSDATETIME(),
    [cre_user_id] VARCHAR(50) NOT NULL,
    [upd_date] DATETIME2(7) NULL,
    [upd_user_id] VARCHAR(50) NULL,
    CONSTRAINT [PK__eocms_user_message] PRIMARY KEY CLUSTERED
    (
        [error_code] ASC
    ) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF,
    ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO

-- ========================================================================
-- Table: eocms_template_store
-- ========================================================================

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [$(schema_name)].[eocms_template_store](
    [template_name] VARCHAR(64) NOT NULL,
    [bin_data] VARBINARY(MAX) NULL,
    [remarks] VARCHAR(50) NULL,
    [template_type] VARCHAR(32) NULL,
    [cre_date] DATETIME2(2) NOT NULL,
    [cre_user_id] VARCHAR(50) NOT NULL,
    [upd_date] DATETIME2(7) NULL,
    [upd_user_id] VARCHAR(50) NULL,
    CONSTRAINT [PK__eocms_template_store] PRIMARY KEY CLUSTERED
    (
        [template_name] ASC
    ) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF,
    ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO

-- ========================================================================
-- Table: eocms_driver_notice
-- ========================================================================

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [$(schema_name)].[eocms_driver_notice](
    [date_of_processing] DATETIME2(7) NOT NULL,
    [notice_no] VARCHAR(10) NOT NULL,
    [date_of_dn] DATETIME2(7) NOT NULL,
    [date_of_return] DATETIME2(7) NULL,
    [driver_bldg] VARCHAR(65) NULL,
    [driver_blk_hse_no] VARCHAR(10) NULL,
    [driver_floor] VARCHAR(3) NULL,
    [driver_id_type] VARCHAR(1) NOT NULL,
    [driver_name] VARCHAR(66) NULL,
    [driver_nric_no] VARCHAR(20) NOT NULL,
    [driver_postal_code] VARCHAR(6) NULL,
    [driver_street] VARCHAR(32) NULL,
    [driver_unit] VARCHAR(5) NULL,
    [postal_regn_no] VARCHAR(15) NULL,
    [processing_stage] VARCHAR(3) NOT NULL,
    [reason_for_unclaim] VARCHAR(3) NULL,
    [reminder_flag] VARCHAR(1) NULL,
    [cre_date] DATETIME2(7) NOT NULL,
    [cre_user_id] VARCHAR(50) NOT NULL,
    [upd_date] DATETIME2(7) NULL,
    [upd_user_id] VARCHAR(50) NULL,
    CONSTRAINT [PK__eocms_driver_notice] PRIMARY KEY CLUSTERED
    (
        [date_of_processing] ASC,
        [notice_no] ASC
    ) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF,
    ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO

-- ========================================================================
-- Table: eocms_request_driver_particulars
-- ========================================================================

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [$(schema_name)].[eocms_request_driver_particulars](
    [date_of_processing] DATETIME2(7) NOT NULL,
    [notice_no] VARCHAR(10) NOT NULL,
    [date_of_rdp] DATETIME2(7) NOT NULL,
    [date_of_return] DATETIME2(7) NULL,
    [owner_bldg] VARCHAR(65) NULL,
    [owner_blk_hse_no] VARCHAR(10) NULL,
    [owner_floor] VARCHAR(3) NULL,
    [owner_id_type] VARCHAR(1) NOT NULL,
    [owner_name] VARCHAR(66) NULL,
    [owner_nric_no] VARCHAR(20) NOT NULL,
    [owner_postal_code] VARCHAR(6) NULL,
    [owner_street] VARCHAR(32) NULL,
    [owner_unit] VARCHAR(5) NULL,
    [postal_regn_no] VARCHAR(15) NULL,
    [processing_stage] VARCHAR(3) NOT NULL,
    [reminder_flag] VARCHAR(1) NULL,
    [unclaimed_reason] VARCHAR(3) NULL,
    [cre_date] DATETIME2(7) NOT NULL,
    [cre_user_id] VARCHAR(50) NOT NULL,
    [upd_date] DATETIME2(7) NULL,
    [upd_user_id] VARCHAR(50) NULL,
    CONSTRAINT [PK__eocms_request_driver_particulars] PRIMARY KEY CLUSTERED
    (
        [date_of_processing] ASC,
        [notice_no] ASC
    ) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF,
    ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO

PRINT 'Completed Init Create Tables For $(databasename)'
GO

CREATE TABLE [$(schema_name)].[eocms_web_txn_detail](
    [receipt_no] VARCHAR(16) NOT NULL,
    [offence_notice_no] VARCHAR(10) NOT NULL,
    [type_of_receipt] VARCHAR(2) NULL,
    [remarks] VARCHAR(100) NULL,
    [transaction_date_and_time] DATETIME2(7) NULL,
    [vehicle_no] VARCHAR(12) NULL,
    [atoms_flag] VARCHAR(1) NULL,
    [payment_mode] VARCHAR(6) NULL,
    [total_amount] VARCHAR(10) NULL,
    [payment_amount] VARCHAR(10) NULL,
    [sender] VARCHAR(10) NULL,
    [status] VARCHAR(1) NULL,
    [error_remarks] VARCHAR(50) NULL,
    [is_sync] BIT NOT NULL,
    [transaction_id] VARCHAR(16) NULL,
    [cre_date] DATETIME2(7) NOT NULL,
    [cre_user_id] VARCHAR(50) NOT NULL,
    [upd_date] DATETIME2(7) NULL,
    [upd_user_id] VARCHAR(50) NOT NULL,
    CONSTRAINT [PK__eocms_web_txn_detail] PRIMARY KEY CLUSTERED
    (
        [receipt_no] ASC
        [offence_notice_no] ASC
    ) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF,
    ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
