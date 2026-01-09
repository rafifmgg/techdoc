# Yi Jie Feedback Collection

## Part 1: Technical Standards (Guidelines)

1. Files uploaded for notice creation should be placed in a temporary folder. Once the notice is created, the files must be moved to the actual (permanent) folder.

2. JSON API responses must return only the fields required for the current screen or specific functional purpose. Avoid returning excessive or unused data (e.g., all 156 fields) when not necessary. If data is split across tabs or views, each tab should only request and receive the relevant subset of fields when accessed.

3. All numbering formats (e.g., notice number) must use SQL Server sequences.

4. When inserting a notice number, the main table (VON) must be updated first, followed by the child table (OND).

5. Both backend (BE) and frontend (FE) must implement validation to support multiple offence types (e.g., O, E, U).

6. Excel templates must be stored in blob storage.

7. Dropdown APIs must return only active records (e.g., vehicle category dropdown should only return active entries from the standard code table).

8. All API methods must use the POST method.

9. No sensitive data should be exposed in URL endpoints.

10. Custom API endpoints must be named intuitively and consistently, reflecting their function clearly. If the endpoint performs a CRUD operation on a specific table, its naming should follow the table name. Avoid generic or ambiguous names (e.g., /workflow) unless the endpoint encompasses multiple actions or workflows that justify broader naming.

11. App code SLIFT for each module is different.

12. If token expired or invalid, the system should retry to get another refreshed token to continue. Should not stop processing.

13. In the event OCMS calls SLIFT API and fails to connect, it should auto retry for 3 times before it stops fully and trigger email alert.

14. Shedlock naming convention:
    - `[action]_[subject]_[suffix]` -> for files and report operations
    - `[action]_[subject]` -> for API/other operations
    - `[action]_[subject][specific_term]` -> for special cases (photos, letters)

15. Date range validation for manual report excel in Staff Portal is 1 year.

16. A batch job can start but did not end due to e.g. timeout/loss connection/long running or stuck jobs. There is a possibility that the memory get lost. Can break up the process to record start time only when it starts, so that we can identify which jobs start but did not end properly. And to allow us to identify which process did not start at all.

17. Batch job table delete after 3 months as part of archival.

18. Response endpoint must follow app guideline:
    - Success/fail response:
    ```json
    {
      "data": {
        "appCode": "OCMS-5000",
        "message": "The request timed out. Please try again later"
      }
    }
    ```
    - For data list:
    ```json
    {
      "total": 1,
      "limit": 10,
      "skip": 0,
      "data": [
        { ... }
      ]
    }
    ```

19. Design the table follow the use case in functional process.

20. All value for code that can parameterize can use parameter table.

21. All dropdown or list items that can parameterize can use standard code table.

22. No hardcode values.

23. For template SMS/Email/PDF (receipt) need to put in table ocms_template_store.

24. cre_user_id and upd_user_id cannot use "SYSTEM". Use database user instead:
    - Intranet: `ocmsiz_app_conn`
    - Internet: `ocmsez_app_conn`

25. If reference/content already exists in Functional Document (FD), refer to FD instead of duplicating in Technical Document. Use references like "Refer to FD Section X.X for detailed validation rules".

26. Do not use `SELECT *` in SQL queries. Always specify only the fields that are actually needed for the operation or response.

---

## Part 2: Review Comments (Questions/Clarifications)

1. Where is the sync flag? I don't see sync flag in intranet VON table.

2. Remove / don't need to log to batch table for frequent sync jobs. Just log into application logs for error messages.

3. Why treat as no update? What about scenario of update payment allowance flag?

4. How does this push works? Through API or direct insert? Include details.

5. All these still valid scenarios? Since no more logging to batch job table?

6. When will this scenario happen? This will be handled by the cron retry where it checks for notices that failed to sync=false.

7. When will this happen? Since this is handled realtime, if it fails to sync to internet, it will fail and rollback? Which env don't match with which?

8. This process seems to have a different set of logic comparing to those above sync process—why keep a different logic? It looks like it's performing the same intranet pull internet process—why separate?

9. What does all the response error mean when fail to update intranet? The whole process will fail and not continue? Is it sequential or parallel? ONOD addr intranet table update fail, VON and eVON will not continue? All the action in the square box should be update and insert.. Not just insert right?

10. Stores the provided payment details into the transaction records. (flow description)

11. The incomplete JSON response.

12. The flow of the diagram vs table description vs functional doc is not in sync. Please review and combine all the different sources in 1 flow with all the detailed validation and logic. A single comprehensive diagram would work much better for everyone.

13. Doc is missing key design on how to check and implement for the different eligible scenarios for the different sources.

---

## Part 3: Context Notes

These are context notes about what reviewer thinks:

1. **No need to explain**: Some things don't need detailed explanation in the document.

2. **Separate validation API**: Document has separate API just for validation before calling suspension API.

3. **Focus and context**: Writer forgot the document focus and context.

4. **API payload context**: API payload that send for search notice using ID number.
