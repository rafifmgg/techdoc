#!/usr/bin/env python
"""
Add OLE Object Attachments to Word Document

This script adds image files as OLE Package objects (embedded attachments)
to an existing Word document. The attachments appear as icons that can be
double-clicked to open the full-size image.

Usage:
    python add_ole_attachments.py <markdown.md>

The script reads the markdown to find image references, then opens the
corresponding Word document and adds OLE objects after each
"NOTE: Due to page size limit" text.

Requirements:
    - Microsoft Word installed
    - pywin32: pip install pywin32
"""

import sys
import os
import re
from pathlib import Path


def parse_markdown_images(md_path):
    """
    Parse markdown file to extract image references.
    Returns list of (image_path, section_heading) tuples in order.
    """
    md_path = Path(md_path)

    with open(md_path, 'r', encoding='utf-8') as f:
        content = f.read()

    lines = content.split('\n')
    images = []
    current_section = ""

    for i, line in enumerate(lines):
        # Track section headings
        if line.startswith('## '):
            current_section = line[3:].strip()
        elif line.startswith('### '):
            current_section = line[4:].strip()

        # Find image references
        img_match = re.match(r'!\[([^\]]*)\]\(([^)]+)\)', line.strip())
        if img_match:
            alt_text = img_match.group(1)
            img_path = img_match.group(2)

            # Convert relative path to absolute
            full_path = md_path.parent / img_path

            if full_path.exists():
                images.append({
                    'path': full_path.resolve(),
                    'name': full_path.name,
                    'section': current_section,
                    'alt_text': alt_text
                })

    return images


def add_ole_objects_with_word(docx_path, images):
    """
    Add OLE objects using Word COM automation.

    Args:
        docx_path: Path to the Word document
        images: List of image info dicts from parse_markdown_images
    """
    try:
        import win32com.client as win32
    except ImportError:
        print("ERROR: win32com not installed.")
        print("Install with: pip install pywin32")
        return False

    word = None
    doc = None

    try:
        print("Starting Microsoft Word...")
        word = win32.Dispatch('Word.Application')
        word.Visible = False
        word.DisplayAlerts = False

        print(f"Opening: {docx_path}")
        doc = word.Documents.Open(str(docx_path))

        # Search text to find
        search_text = "NOTE: Due to page size limit, the full-sized image is appended."

        # Track how many we've found and processed
        found_count = 0

        # Find all occurrences of the NOTE text
        find = doc.Content.Find
        find.ClearFormatting()

        # Start from beginning
        rng = doc.Content
        rng.Start = 0

        for img_info in images:
            # Reset find
            find = rng.Find
            find.ClearFormatting()
            find.Text = search_text
            find.Forward = True
            find.Wrap = 0  # wdFindStop - don't wrap around

            if find.Execute():
                found_count += 1

                # Get the range of found text
                found_range = rng.Duplicate

                # Move to end of the found text and insert paragraph
                found_range.Collapse(0)  # wdCollapseEnd
                found_range.InsertParagraphAfter()
                found_range.Collapse(0)

                # Insert OLE object as icon
                try:
                    ole_shape = found_range.InlineShapes.AddOLEObject(
                        ClassType="Package",
                        FileName=str(img_info['path']),
                        DisplayAsIcon=True,
                        IconLabel=img_info['name']
                    )
                    print(f"  [{found_count}] Added: {img_info['name']}")
                except Exception as e:
                    print(f"  [{found_count}] Error adding {img_info['name']}: {e}")

                # Move search range past this found text for next iteration
                rng.Start = found_range.End
            else:
                print(f"  Warning: Could not find NOTE text for image {found_count + 1}")
                break

        print()
        print(f"Processed {found_count} of {len(images)} images")

        # Save document
        print("Saving document...")
        doc.Save()

        return True

    except Exception as e:
        print(f"Error: {e}")
        import traceback
        traceback.print_exc()
        return False

    finally:
        if doc:
            doc.Close(SaveChanges=False)  # Already saved above
        if word:
            word.Quit()


def main():
    if len(sys.argv) < 2:
        print("Add OLE Object Attachments to Word Document")
        print("-" * 50)
        print()
        print("Usage:")
        print("  python add_ole_attachments.py <markdown.md>")
        print()
        print("The script will:")
        print("  1. Parse the markdown file to find image references")
        print("  2. Open the corresponding .docx file")
        print("  3. Add each image as an OLE Package object (icon)")
        print("     after the 'NOTE: Due to page size limit' text")
        print()
        print("Requirements:")
        print("  - Microsoft Word installed")
        print("  - pywin32: pip install pywin32")
        print()
        sys.exit(1)

    md_path = Path(sys.argv[1]).resolve()

    if not md_path.exists():
        print(f"Error: File not found: {md_path}")
        sys.exit(1)

    if md_path.suffix.lower() != '.md':
        print(f"Error: Expected a .md file, got: {md_path.suffix}")
        sys.exit(1)

    # Corresponding docx file
    docx_path = md_path.with_suffix('.docx')

    if not docx_path.exists():
        print(f"Error: Word document not found: {docx_path}")
        print("Please run md_to_word.py first to generate the .docx file.")
        sys.exit(1)

    # Parse markdown for images
    print(f"Parsing: {md_path.name}")
    images = parse_markdown_images(md_path)

    if not images:
        print("No images found in markdown file.")
        sys.exit(1)

    print(f"Found {len(images)} images:")
    for i, img in enumerate(images, 1):
        print(f"  {i}. {img['name']}")
    print()

    # Add OLE objects
    success = add_ole_objects_with_word(docx_path, images)

    if success:
        print()
        print(f"Successfully updated: {docx_path}")
    else:
        print()
        print("Failed to add OLE attachments.")
        sys.exit(1)


if __name__ == '__main__':
    main()
