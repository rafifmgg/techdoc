-- =========================================================================
--  Name          : 01_ocmspiieztab.sql
--  Author        : Galih
--  Creation Date : 01 July 2025
--  Description   : Create table for OCMS PII
--  Run By        : ocmspiiezmgr
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

:setvar databasename ocmspiiezdb
:setvar schema_name ocmspiiezmgr

set nocount on
:out 01_ocmspiieztab.log

use [$(databasename)]
GO

select 'script run:', current_timestamp, '$(SQLCMDUSER)', '$(SQLCMDDBNAME)';
GO

PRINT 'Start Init Create Table For $(databasename)'
GO

-- ========================================================================
-- Table: eocms_offence_notice_owner_driver
-- ========================================================================

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [$(schema_name)].[eocms_offence_notice_owner_driver](
    [notice_no] VARCHAR(10) NOT NULL,
    [owner_driver_indicator] VARCHAR(1) NOT NULL,
    [id_no] VARCHAR(20) NOT NULL,
    [offender_indicator] VARCHAR(1) NULL,
    [email_addr] VARCHAR(320) NULL,
    [cre_date] DATETIME2(7) NOT NULL,
    [cre_user_id] VARCHAR(50) NOT NULL,
    [upd_date] DATETIME2(7) NULL,
    [upd_user_id] VARCHAR(50) NULL,
    CONSTRAINT [PK__eocms_offence_notice_owner_driver] PRIMARY KEY CLUSTERED
    (
        [notice_no] ASC,
        [owner_driver_indicator] ASC
    ) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF,
    ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
PRINT 'Completed Init Create Table For $(databasename)'
GO
