#!/usr/bin/env python
"""
Rename drawio sections to match technical document numbering.

Current → New:
- Section 2 → Section 1 (Processing Furnished from eService)
- Section 3 → Section 2 (Staff Portal Manual Review)
- Section 4 → Section 3 (Staff Portal Manual Furnish)
- Section 5 → Section 4 (Batch Furnish and Update)
- Section 6 → Section 5 (PLUS Integration)
"""

import os
import re
import shutil
from pathlib import Path

# Mapping: old section number → new section number
SECTION_MAP = {
    '2': '1',
    '3': '2',
    '4': '3',
    '5': '4',
    '6': '5',
}

# Tab name mapping based on technical doc structure
TAB_NAME_MAP = {
    # Section 1 (was 2)
    '2.4.2_Sync_Furnish_Cron': '1.2_Sync_Furnish_Cron',
    '2.4.3_Auto_Approval_Review': '1.3_Auto_Approval_Review',
    '2.4.4_Manual_OIC_Review': '1.4_Manual_OIC_Review',
    '2.4.5_Update_Internet_Outcome': '1.5_Update_Internet_Outcome',

    # Section 2 (was 3)
    '3.2_Manual_Review_HL': '2.2_Manual_Review_HL',
    '3.3_Email_Report_Cron': '2.3_Email_Report_Cron',
    '3.4_List_Applications': '2.4_List_Applications',
    '3.4.3_Default_Page_Behaviour': '2.4.1_Default_Page_Behaviour',
    '3.4.4_Search_Submissions': '2.4.2_Search_Submissions',
    '3.4.5_Check_Furnishability': '2.4.3_Check_Furnishability',
    '3.5_Get_Detail': '2.5_Get_Detail',
    '3.6_Approve_Submission': '2.6_Approve_Submission',
    '3.7_Reject_Submission': '2.7_Reject_Submission',

    # Section 3 (was 4)
    '4.2_High_Level_Furnish_Update': '3.2_High_Level_Furnish_Update',
    '4.5_Action_Button_Check': '3.3_Action_Button_Check',
    '4.6_Furnish_Offender': '3.4_Furnish_Offender',
    '4.7_Redirect_Notice': '3.5_Redirect_Notice',
    '4.8_Update_Particulars': '3.6_Update_Particulars',

    # Section 4 (was 5)
    '5.2_Batch_Furnish_Offender': '4.2_Batch_Furnish_Offender',
    '5.3_Batch_Update_Mailing_Addr': '4.3_Batch_Update_Mailing_Addr',
    '5.3.1_Retrieve_Outstanding_Notices': '4.3.1_Retrieve_Outstanding_Notices',
    '5.2.1_API_Payloads_Batch_Furnish': '4.2.1_API_Payloads_Batch_Furnish',
    '5.2.2_UI_Field_Behavior': '4.2.2_UI_Field_Behavior',
    '5.3.1_API_Payloads_Update_Addr': '4.3.2_API_Payloads_Update_Addr',

    # Section 5 (was 6)
    '6.2_PLUS_Update_Hirer_Driver': '5.2_PLUS_Update_Hirer_Driver',
    '6.3_PLUS_Redirect': '5.3_PLUS_Redirect',
}


def update_drawio_tab_names(drawio_path):
    """Update tab names in a drawio file."""
    with open(drawio_path, 'r', encoding='utf-8') as f:
        content = f.read()

    original_content = content
    changes = []

    # Find and replace tab names
    for old_name, new_name in TAB_NAME_MAP.items():
        if f'name="{old_name}"' in content:
            content = content.replace(f'name="{old_name}"', f'name="{new_name}"')
            changes.append(f'  {old_name} -> {new_name}')

    if content != original_content:
        with open(drawio_path, 'w', encoding='utf-8') as f:
            f.write(content)
        return changes

    return []


def rename_sections(base_path):
    """Rename section folders and update drawio files."""
    base_path = Path(base_path)

    print("=" * 60)
    print("Renaming sections to match technical document numbering")
    print("=" * 60)
    print()

    # Step 1: Rename folders (in reverse order to avoid conflicts)
    print("Step 1: Renaming folders...")

    # First, rename to temp names to avoid conflicts
    temp_renames = []
    for old_num, new_num in SECTION_MAP.items():
        old_folder = base_path / f'Section {old_num}'
        if old_folder.exists():
            temp_folder = base_path / f'_temp_Section_{new_num}'
            print(f"  Section {old_num} -> _temp_Section_{new_num}")
            shutil.move(str(old_folder), str(temp_folder))
            temp_renames.append((temp_folder, base_path / f'Section {new_num}'))

    # Then rename from temp to final names
    for temp_folder, final_folder in temp_renames:
        print(f"  {temp_folder.name} -> {final_folder.name}")
        shutil.move(str(temp_folder), str(final_folder))

    print()

    # Step 2: Rename drawio files in each section folder
    print("Step 2: Renaming drawio files...")
    for new_num in SECTION_MAP.values():
        section_folder = base_path / f'Section {new_num}'
        if not section_folder.exists():
            continue

        for drawio_file in section_folder.glob('*.drawio'):
            # Find the old section number in filename
            match = re.search(r'Section (\d+)', drawio_file.name)
            if match:
                old_section_in_name = match.group(1)
                # Map back to find what new number this should be
                # The folder is already renamed, so we need to figure out the correct new number
                new_name = drawio_file.name.replace(f'Section {old_section_in_name}', f'Section {new_num}')
                new_path = drawio_file.parent / new_name
                if new_name != drawio_file.name:
                    print(f"  {drawio_file.name} -> {new_name}")
                    shutil.move(str(drawio_file), str(new_path))

    print()

    # Step 3: Update tab names inside drawio files
    print("Step 3: Updating tab names in drawio files...")
    for new_num in SECTION_MAP.values():
        section_folder = base_path / f'Section {new_num}'
        if not section_folder.exists():
            continue

        for drawio_file in section_folder.glob('*.drawio'):
            changes = update_drawio_tab_names(drawio_file)
            if changes:
                print(f"  {drawio_file.name}:")
                for change in changes:
                    print(f"    {change}")

    print()

    # Step 4: Delete old images folders
    print("Step 4: Deleting old images folders...")
    for new_num in SECTION_MAP.values():
        images_folder = base_path / f'Section {new_num}' / 'images'
        if images_folder.exists():
            print(f"  Deleting: {images_folder}")
            shutil.rmtree(images_folder)

    print()
    print("Done! Now run drawio_to_png.py to regenerate images.")


def main():
    import sys

    if len(sys.argv) < 2:
        print("Usage: python rename_sections.py <ocms_folder>")
        print("Example: python rename_sections.py \"OCMS 41\"")
        sys.exit(1)

    base_path = Path(sys.argv[1]).resolve()

    if not base_path.exists():
        print(f"Error: Folder not found: {base_path}")
        sys.exit(1)

    rename_sections(base_path)


if __name__ == '__main__':
    main()
