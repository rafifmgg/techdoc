#!/usr/bin/env python
"""
Markdown to Word Converter
Converts .md (Markdown) files to .docx (Word) format with proper formatting

Formatting:
- Headers: Arial font (size varies by level)
- Body text: Arial 10pt
- Tables: Borders with 0.2cm left/right padding
"""

import sys
import re
from pathlib import Path
from docx import Document
from docx.shared import Inches, Pt, RGBColor, Cm, Twips
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.enum.style import WD_STYLE_TYPE
from docx.oxml.ns import qn
from docx.oxml import OxmlElement


# Font settings
FONT_NAME = 'Arial'
BODY_FONT_SIZE = Pt(10)
CODE_FONT_NAME = 'Consolas'
CODE_FONT_SIZE = Pt(9)

# Header font sizes
HEADER_SIZES = {
    1: Pt(16),  # H1
    2: Pt(14),  # H2
    3: Pt(12),  # H3
    4: Pt(11),  # H4
    5: Pt(10),  # H5
    6: Pt(10),  # H6
}

# Table cell padding (0.2 cm = approximately 0.08 inches)
TABLE_CELL_PADDING = Cm(0.2)


def set_cell_padding(cell, left=None, right=None, top=None, bottom=None):
    """Set padding for a table cell."""
    tc = cell._tc
    tcPr = tc.get_or_add_tcPr()
    tcMar = OxmlElement('w:tcMar')

    if left is not None:
        left_elem = OxmlElement('w:left')
        left_elem.set(qn('w:w'), str(int(left.twips)))
        left_elem.set(qn('w:type'), 'dxa')
        tcMar.append(left_elem)

    if right is not None:
        right_elem = OxmlElement('w:right')
        right_elem.set(qn('w:w'), str(int(right.twips)))
        right_elem.set(qn('w:type'), 'dxa')
        tcMar.append(right_elem)

    if top is not None:
        top_elem = OxmlElement('w:top')
        top_elem.set(qn('w:w'), str(int(top.twips)))
        top_elem.set(qn('w:type'), 'dxa')
        tcMar.append(top_elem)

    if bottom is not None:
        bottom_elem = OxmlElement('w:bottom')
        bottom_elem.set(qn('w:w'), str(int(bottom.twips)))
        bottom_elem.set(qn('w:type'), 'dxa')
        tcMar.append(bottom_elem)

    tcPr.append(tcMar)


def set_table_border(table):
    """Set borders for all cells in a table."""
    tbl = table._tbl
    tblPr = tbl.tblPr
    if tblPr is None:
        tblPr = OxmlElement('w:tblPr')
        tbl.insert(0, tblPr)

    tblBorders = OxmlElement('w:tblBorders')

    for border_name in ['top', 'left', 'bottom', 'right', 'insideH', 'insideV']:
        border = OxmlElement(f'w:{border_name}')
        border.set(qn('w:val'), 'single')
        border.set(qn('w:sz'), '4')
        border.set(qn('w:space'), '0')
        border.set(qn('w:color'), '000000')
        tblBorders.append(border)

    tblPr.append(tblBorders)


def shade_cells(row, color='D9E2F3'):
    """Shade cells in a row with specified color."""
    for cell in row.cells:
        shading = OxmlElement('w:shd')
        shading.set(qn('w:fill'), color)
        cell._tc.get_or_add_tcPr().append(shading)


def set_run_font(run, font_name=FONT_NAME, font_size=BODY_FONT_SIZE, bold=False, italic=False):
    """Set font properties for a run."""
    run.font.name = font_name
    run.font.size = font_size
    run.bold = bold
    run.italic = italic
    # Set font for East Asian and complex scripts as well
    run._element.rPr.rFonts.set(qn('w:eastAsia'), font_name)


def add_formatted_text(paragraph, text, font_name=FONT_NAME, font_size=BODY_FONT_SIZE):
    """Add text with inline formatting (bold, italic, code)."""
    # Pattern to match **bold**, *italic*, `code`
    pattern = r'(\*\*[^*]+\*\*|\*[^*]+\*|`[^`]+`)'
    parts = re.split(pattern, text)

    for part in parts:
        if not part:
            continue
        if part.startswith('**') and part.endswith('**'):
            run = paragraph.add_run(part[2:-2])
            set_run_font(run, font_name, font_size, bold=True)
        elif part.startswith('*') and part.endswith('*'):
            run = paragraph.add_run(part[1:-1])
            set_run_font(run, font_name, font_size, italic=True)
        elif part.startswith('`') and part.endswith('`'):
            run = paragraph.add_run(part[1:-1])
            run.font.name = CODE_FONT_NAME
            run.font.size = CODE_FONT_SIZE
            run.font.color.rgb = RGBColor(0x80, 0x00, 0x00)
        else:
            run = paragraph.add_run(part)
            set_run_font(run, font_name, font_size)


def parse_markdown_table(lines, start_index):
    """Parse a markdown table and return table data and end index."""
    table_data = []
    i = start_index

    while i < len(lines):
        line = lines[i].strip()
        if not line.startswith('|'):
            break

        # Skip separator line (|---|---|)
        if re.match(r'^\|[\s\-:|]+\|$', line):
            i += 1
            continue

        # Parse cells
        cells = [cell.strip() for cell in line.split('|')[1:-1]]
        if cells:
            table_data.append(cells)
        i += 1

    return table_data, i


def setup_document_styles(doc):
    """Setup document styles with Arial font."""
    # Set default Normal style
    style = doc.styles['Normal']
    style.font.name = FONT_NAME
    style.font.size = BODY_FONT_SIZE
    style._element.rPr.rFonts.set(qn('w:eastAsia'), FONT_NAME)

    # Set heading styles
    for level in range(1, 7):
        style_name = f'Heading {level}'
        if style_name in doc.styles:
            heading_style = doc.styles[style_name]
            heading_style.font.name = FONT_NAME
            heading_style.font.size = HEADER_SIZES.get(level, Pt(10))
            heading_style.font.bold = True
            heading_style.font.color.rgb = RGBColor(0x00, 0x00, 0x00)
            heading_style._element.rPr.rFonts.set(qn('w:eastAsia'), FONT_NAME)

    # Set list styles
    for style_name in ['List Bullet', 'List Bullet 2', 'List Number']:
        if style_name in doc.styles:
            list_style = doc.styles[style_name]
            list_style.font.name = FONT_NAME
            list_style.font.size = BODY_FONT_SIZE


def convert_markdown_to_word(input_path: str, output_path: str = None) -> str:
    """Convert a Markdown file to Word document format."""
    input_path = Path(input_path)

    if not input_path.exists():
        raise FileNotFoundError(f'File not found: {input_path}')

    if input_path.suffix.lower() != '.md':
        raise ValueError(f'File must be a .md file, got: {input_path.suffix}')

    if output_path is None:
        output_path = input_path.with_suffix('.docx')
    else:
        output_path = Path(output_path)

    # Read markdown content
    with open(input_path, 'r', encoding='utf-8') as md_file:
        content = md_file.read()

    # Create Word document
    doc = Document()

    # Setup document styles
    setup_document_styles(doc)

    # Process markdown content
    lines = content.split('\n')
    i = 0
    in_code_block = False
    code_block_content = []

    while i < len(lines):
        line = lines[i]

        # Handle code blocks
        if line.strip().startswith('```'):
            if in_code_block:
                # End code block
                code_text = '\n'.join(code_block_content)
                p = doc.add_paragraph()
                run = p.add_run(code_text)
                run.font.name = CODE_FONT_NAME
                run.font.size = CODE_FONT_SIZE
                p.paragraph_format.left_indent = Inches(0.5)
                code_block_content = []
                in_code_block = False
            else:
                # Start code block
                in_code_block = True
            i += 1
            continue

        if in_code_block:
            code_block_content.append(line)
            i += 1
            continue

        # Skip HTML comments
        if line.strip().startswith('<!--'):
            while i < len(lines) and '-->' not in lines[i]:
                i += 1
            i += 1
            continue

        # Handle headers
        if line.startswith('# '):
            p = doc.add_heading(line[2:].strip(), level=1)
            for run in p.runs:
                set_run_font(run, FONT_NAME, HEADER_SIZES[1], bold=True)
            i += 1
            continue
        elif line.startswith('## '):
            p = doc.add_heading(line[3:].strip(), level=2)
            for run in p.runs:
                set_run_font(run, FONT_NAME, HEADER_SIZES[2], bold=True)
            i += 1
            continue
        elif line.startswith('### '):
            p = doc.add_heading(line[4:].strip(), level=3)
            for run in p.runs:
                set_run_font(run, FONT_NAME, HEADER_SIZES[3], bold=True)
            i += 1
            continue
        elif line.startswith('#### '):
            p = doc.add_heading(line[5:].strip(), level=4)
            for run in p.runs:
                set_run_font(run, FONT_NAME, HEADER_SIZES[4], bold=True)
            i += 1
            continue
        elif line.startswith('##### '):
            p = doc.add_heading(line[6:].strip(), level=5)
            for run in p.runs:
                set_run_font(run, FONT_NAME, HEADER_SIZES[5], bold=True)
            i += 1
            continue
        elif line.startswith('###### '):
            p = doc.add_heading(line[7:].strip(), level=6)
            for run in p.runs:
                set_run_font(run, FONT_NAME, HEADER_SIZES[6], bold=True)
            i += 1
            continue

        # Handle horizontal rule
        if line.strip() in ['---', '***', '___']:
            # Add a thin horizontal line
            p = doc.add_paragraph()
            p.paragraph_format.space_after = Pt(12)
            i += 1
            continue

        # Handle tables
        if line.strip().startswith('|'):
            table_data, end_index = parse_markdown_table(lines, i)
            if table_data:
                # Create table
                num_cols = len(table_data[0])
                table = doc.add_table(rows=len(table_data), cols=num_cols)
                table.alignment = WD_TABLE_ALIGNMENT.LEFT
                set_table_border(table)

                for row_idx, row_data in enumerate(table_data):
                    row = table.rows[row_idx]
                    for col_idx, cell_text in enumerate(row_data):
                        if col_idx < len(row.cells):
                            cell = row.cells[col_idx]

                            # Set cell padding (0.2 cm left and right)
                            set_cell_padding(cell, left=TABLE_CELL_PADDING, right=TABLE_CELL_PADDING)

                            # Clear existing content and add formatted text
                            cell.text = ''
                            paragraph = cell.paragraphs[0]
                            run = paragraph.add_run(cell_text)
                            set_run_font(run, FONT_NAME, BODY_FONT_SIZE, bold=(row_idx == 0))

                    # Shade header row
                    if row_idx == 0:
                        shade_cells(row, 'D9E2F3')

                doc.add_paragraph()  # Add space after table
            i = end_index
            continue

        # Handle bullet lists
        if line.strip().startswith('- ') or line.strip().startswith('* '):
            text = line.strip()[2:]
            p = doc.add_paragraph(style='List Bullet')
            add_formatted_text(p, text, FONT_NAME, BODY_FONT_SIZE)
            i += 1
            continue

        # Handle numbered lists
        numbered_match = re.match(r'^(\d+)\.\s+(.+)$', line.strip())
        if numbered_match:
            text = numbered_match.group(2)
            p = doc.add_paragraph(style='List Number')
            add_formatted_text(p, text, FONT_NAME, BODY_FONT_SIZE)
            i += 1
            continue

        # Handle indented bullet lists (sub-items)
        if line.startswith('  - ') or line.startswith('  * '):
            text = line.strip()[2:]
            p = doc.add_paragraph(style='List Bullet 2')
            add_formatted_text(p, text, FONT_NAME, BODY_FONT_SIZE)
            i += 1
            continue

        # Handle images
        img_match = re.match(r'!\[([^\]]*)\]\(([^)]+)\)', line.strip())
        if img_match:
            alt_text = img_match.group(1)
            img_path = img_match.group(2)

            # Try to add image if it exists
            full_img_path = input_path.parent / img_path
            if full_img_path.exists():
                try:
                    doc.add_picture(str(full_img_path), width=Inches(6))
                except Exception as e:
                    p = doc.add_paragraph()
                    run = p.add_run(f'[Image: {alt_text}] ({img_path})')
                    set_run_font(run, FONT_NAME, BODY_FONT_SIZE, italic=True)
            else:
                p = doc.add_paragraph()
                run = p.add_run(f'[Image: {alt_text}] ({img_path})')
                set_run_font(run, FONT_NAME, BODY_FONT_SIZE, italic=True)
            i += 1
            continue

        # Handle NOTE: lines
        if line.strip().startswith('NOTE:'):
            p = doc.add_paragraph()
            run = p.add_run(line.strip())
            set_run_font(run, FONT_NAME, BODY_FONT_SIZE, italic=True)
            run.font.color.rgb = RGBColor(0x00, 0x00, 0x80)
            i += 1
            continue

        # Handle bold lines (like **Note:**)
        if line.strip().startswith('**') and '**' in line.strip()[2:]:
            p = doc.add_paragraph()
            add_formatted_text(p, line.strip(), FONT_NAME, BODY_FONT_SIZE)
            i += 1
            continue

        # Handle regular paragraphs
        stripped = line.strip()
        if stripped:
            p = doc.add_paragraph()
            add_formatted_text(p, stripped, FONT_NAME, BODY_FONT_SIZE)

        i += 1

    # Save document
    doc.save(str(output_path))
    return str(output_path)


def convert_directory(input_dir: str, output_dir: str = None) -> list:
    """Convert all Markdown files in a directory to Word documents."""
    input_dir = Path(input_dir)
    output_dir = Path(output_dir) if output_dir else input_dir

    if not input_dir.is_dir():
        raise NotADirectoryError(f'Not a directory: {input_dir}')

    output_dir.mkdir(parents=True, exist_ok=True)
    converted_files = []
    md_files = list(input_dir.glob('*.md'))

    if not md_files:
        print(f'No .md files found in {input_dir}')
        return converted_files

    for md_file in md_files:
        output_path = output_dir / md_file.with_suffix('.docx').name
        try:
            result = convert_markdown_to_word(str(md_file), str(output_path))
            converted_files.append(result)
            print(f'Converted: {md_file.name} -> {output_path.name}')
        except Exception as e:
            print(f'Error converting {md_file.name}: {e}')

    return converted_files


def main():
    if len(sys.argv) < 2:
        print('Markdown to Word Converter')
        print('-' * 40)
        print('Usage:')
        print('  Single file:  python md_to_word.py <input.md> [output.docx]')
        print('  Directory:    python md_to_word.py <input_dir> [output_dir] --dir')
        print()
        print('Examples:')
        print('  python md_to_word.py document.md')
        print('  python md_to_word.py document.md output.docx')
        print('  python md_to_word.py ./docs --dir')
        print('  python md_to_word.py ./docs ./word_output --dir')
        print()
        print('Formatting:')
        print(f'  Font: {FONT_NAME}')
        print(f'  Body text: {BODY_FONT_SIZE.pt}pt')
        print(f'  Headers: {HEADER_SIZES[1].pt}pt (H1) to {HEADER_SIZES[6].pt}pt (H6)')
        print(f'  Table padding: {TABLE_CELL_PADDING.cm}cm left/right')
        print()
        print('Requirements:')
        print('  pip install python-docx')
        sys.exit(1)

    input_path = sys.argv[1]
    output_path = sys.argv[2] if len(sys.argv) > 2 and sys.argv[2] != '--dir' else None
    is_directory = '--dir' in sys.argv

    try:
        if is_directory:
            results = convert_directory(input_path, output_path)
            print(f'\nConverted {len(results)} file(s)')
        else:
            result = convert_markdown_to_word(input_path, output_path)
            print(f'Successfully converted to: {result}')
    except Exception as e:
        print(f'Error: {e}')
        sys.exit(1)


if __name__ == '__main__':
    main()
