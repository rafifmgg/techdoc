#!/usr/bin/env python
"""
Update TOC Pages Script
Opens a Word document, finds the TOC table, and fills in page numbers automatically.

Requirements:
  - pip install pywin32
  - Microsoft Word must be installed

Usage:
  python update_toc_pages.py <input.docx> [output.docx]
"""

import sys
import time
from pathlib import Path

try:
    import win32com.client as win32
    from win32com.client import constants
except ImportError:
    print("Error: pywin32 is required. Install with: pip install pywin32")
    sys.exit(1)


def find_toc_table(doc):
    """Find the Table of Contents table in the document."""
    for i, table in enumerate(doc.Tables):
        # Check if first row has "Section" and "Content" headers
        try:
            first_row = table.Rows(1)
            cell_texts = []
            for j in range(1, first_row.Cells.Count + 1):
                cell_text = first_row.Cells(j).Range.Text.strip().lower()
                # Remove special characters
                cell_text = cell_text.replace('\r', '').replace('\x07', '')
                cell_texts.append(cell_text)

            if 'section' in cell_texts and 'content' in cell_texts:
                return table, i + 1
        except Exception:
            continue

    return None, -1


def get_heading_pages(doc):
    """Get all headings and their page numbers."""
    headings = {}

    # Iterate through all paragraphs
    for para in doc.Paragraphs:
        style_name = para.Style.NameLocal

        # Check if it's a heading style
        if 'Heading' in style_name or 'heading' in style_name:
            text = para.Range.Text.strip()
            page_num = para.Range.Information(3)  # wdActiveEndPageNumber = 3

            # Extract section number if present (e.g., "1.1 Use Case" -> "1.1")
            # Also handle "Section 1 – Title" format
            import re

            # Try to match section patterns
            # Pattern 1: "1.1 Title" or "1.1.1 Title"
            match = re.match(r'^(\d+(?:\.\d+)*)\s+(.+)$', text)
            if match:
                section_num = match.group(1)
                headings[section_num] = page_num
                continue

            # Pattern 2: "Section 1 – Title" or "Section 1 - Title"
            match = re.match(r'^Section\s+(\d+)\s*[–\-]\s*(.+)$', text, re.IGNORECASE)
            if match:
                section_num = match.group(1)
                headings[section_num] = page_num
                continue

            # Pattern 3: Just a number at the start
            match = re.match(r'^(\d+(?:\.\d+)*)\s*$', text)
            if match:
                section_num = match.group(1)
                headings[section_num] = page_num

    return headings


def set_column_widths(table, widths_cm):
    """Set column widths for a table in centimeters."""
    # Convert cm to points (1 cm = 28.35 points)
    CM_TO_POINTS = 28.35

    for col_idx, width_cm in enumerate(widths_cm, start=1):
        try:
            column = table.Columns(col_idx)
            column.SetWidth(width_cm * CM_TO_POINTS, 2)  # 2 = wdAdjustNone
        except Exception as e:
            print(f"Warning: Could not set width for column {col_idx}: {e}")


def update_toc_table(table, headings):
    """Update the TOC table with page numbers."""
    updated_count = 0

    # Find the Pages column index
    first_row = table.Rows(1)
    pages_col_idx = -1
    section_col_idx = -1
    content_col_idx = -1

    for j in range(1, first_row.Cells.Count + 1):
        cell_text = first_row.Cells(j).Range.Text.strip().lower()
        cell_text = cell_text.replace('\r', '').replace('\x07', '')
        if 'page' in cell_text:
            pages_col_idx = j
        if 'section' in cell_text:
            section_col_idx = j
        if 'content' in cell_text:
            content_col_idx = j

    if pages_col_idx == -1:
        print("Warning: 'Pages' column not found in TOC table")
        return 0

    if section_col_idx == -1:
        print("Warning: 'Section' column not found in TOC table")
        return 0

    # Set column widths (Section: 2cm, Content: 12cm, Pages: 2cm)
    # Adjust based on column order
    num_cols = first_row.Cells.Count
    widths = [2.0] * num_cols  # Default width

    if section_col_idx > 0:
        widths[section_col_idx - 1] = 2.0  # Section column: 2cm
    if content_col_idx > 0:
        widths[content_col_idx - 1] = 12.0  # Content column: 12cm
    if pages_col_idx > 0:
        widths[pages_col_idx - 1] = 2.0  # Pages column: 2cm

    print(f"Setting column widths: {widths} cm")
    set_column_widths(table, widths)

    # Update each row with page numbers
    for i in range(2, table.Rows.Count + 1):  # Skip header row
        try:
            row = table.Rows(i)
            section_cell = row.Cells(section_col_idx)
            pages_cell = row.Cells(pages_col_idx)

            # Get section number
            section_num = section_cell.Range.Text.strip()
            section_num = section_num.replace('\r', '').replace('\x07', '')

            # Look up page number
            if section_num in headings:
                page_num = headings[section_num]
                # Clear and set new value
                pages_cell.Range.Text = str(page_num)
                updated_count += 1

        except Exception as e:
            print(f"Warning: Error updating row {i}: {e}")
            continue

    return updated_count


def update_toc_pages(input_path, output_path=None):
    """Main function to update TOC pages in a Word document."""
    input_path = Path(input_path).resolve()

    if not input_path.exists():
        raise FileNotFoundError(f"File not found: {input_path}")

    if output_path is None:
        output_path = input_path
    else:
        output_path = Path(output_path).resolve()

    print(f"Opening: {input_path}")

    # Initialize Word application
    word = None
    doc = None

    try:
        word = win32.gencache.EnsureDispatch('Word.Application')
        word.Visible = False  # Run in background
        word.DisplayAlerts = False

        # Open document
        doc = word.Documents.Open(str(input_path))

        # Wait for document to fully load
        time.sleep(1)

        # Find TOC table
        toc_table, table_idx = find_toc_table(doc)

        if toc_table is None:
            print("Error: Could not find Table of Contents table")
            print("Make sure the TOC table has 'Section' and 'Content' columns")
            return None

        print(f"Found TOC table at index {table_idx}")

        # Get heading pages
        print("Scanning document for headings...")
        headings = get_heading_pages(doc)
        print(f"Found {len(headings)} headings with section numbers")

        if headings:
            # Print found headings for debugging
            for section, page in sorted(headings.items(), key=lambda x: [int(n) for n in x[0].split('.')]):
                print(f"  Section {section}: Page {page}")

        # Update TOC table
        print("Updating TOC table...")
        updated = update_toc_table(toc_table, headings)
        print(f"Updated {updated} rows with page numbers")

        # Save document
        if output_path != input_path:
            doc.SaveAs(str(output_path))
            print(f"Saved to: {output_path}")
        else:
            doc.Save()
            print(f"Saved: {input_path}")

        return str(output_path)

    except Exception as e:
        print(f"Error: {e}")
        raise

    finally:
        # Clean up
        if doc:
            doc.Close(False)
        if word:
            word.Quit()


def main():
    if len(sys.argv) < 2:
        print("Update TOC Pages Script")
        print("-" * 40)
        print("Automatically fills in page numbers in Table of Contents")
        print()
        print("Usage:")
        print("  python update_toc_pages.py <input.docx> [output.docx]")
        print()
        print("Examples:")
        print("  python update_toc_pages.py document.docx")
        print("  python update_toc_pages.py document.docx output.docx")
        print()
        print("Requirements:")
        print("  - pip install pywin32")
        print("  - Microsoft Word must be installed")
        sys.exit(1)

    input_path = sys.argv[1]
    output_path = sys.argv[2] if len(sys.argv) > 2 else None

    try:
        result = update_toc_pages(input_path, output_path)
        if result:
            print("\nDone! TOC pages updated successfully.")
    except Exception as e:
        print(f"\nError: {e}")
        sys.exit(1)


if __name__ == '__main__':
    main()
