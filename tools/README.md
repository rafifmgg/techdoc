# Technical Documentation Tools

Tools for generating Word documents from markdown files with embedded diagrams.

## Quick Start

```bash
# Generate complete Word document with all features
python tools/generate_tech_doc.py "OCMS 41"

# Generate without OLE attachments (faster, smaller file)
python tools/generate_tech_doc.py "OCMS 41" --no-ole

# Custom image compression
python tools/generate_tech_doc.py "OCMS 41" --max-width 800
```

## Requirements

```bash
pip install python-docx Pillow pywin32
```

| Package | Required For |
|---------|--------------|
| python-docx | Markdown to Word conversion |
| Pillow | Image compression |
| pywin32 | OLE object attachments (Windows only) |

**Additional Requirements:**
- Draw.io Desktop (for exporting diagrams)
- Microsoft Word (for OLE attachments)

## Tools Overview

### 1. generate_tech_doc.py (Main Script)

Complete pipeline that runs all steps automatically.

```bash
python tools/generate_tech_doc.py <folder> [options]
```

**Options:**
| Option | Description |
|--------|-------------|
| `--no-ole` | Skip OLE object attachments |
| `--max-width N` | Maximum image width in pixels (default: 1000) |
| `--skip-drawio` | Skip exporting drawio files |
| `--skip-compress` | Skip image compression |

**Examples:**
```bash
# Full generation
python tools/generate_tech_doc.py "OCMS 41"

# Without OLE (no Word automation needed)
python tools/generate_tech_doc.py "OCMS 41" --no-ole

# Smaller images
python tools/generate_tech_doc.py "OCMS 41" --max-width 600

# Skip drawio export (use existing PNGs)
python tools/generate_tech_doc.py "OCMS 41" --skip-drawio
```

---

### 2. drawio_to_png.py

Export draw.io diagrams to PNG images.

```bash
python tools/drawio_to_png.py <input.drawio> [output_folder]
```

**Examples:**
```bash
# Export to default images folder
python tools/drawio_to_png.py "OCMS 41/Section 1/diagram.drawio"

# Export to custom folder
python tools/drawio_to_png.py "OCMS 41/Section 1/diagram.drawio" "./exports"
```

**Requirements:**
- Draw.io Desktop installed at `C:\Program Files\draw.io\draw.io.exe`

---

### 3. compress_images.py

Compress PNG images to reduce file size.

```bash
python tools/compress_images.py <folder> [max_width]
```

**Examples:**
```bash
# Default compression (1000px max width)
python tools/compress_images.py "OCMS 41"

# Smaller images (800px max width)
python tools/compress_images.py "OCMS 41" 800

# Larger images for print (1400px max width)
python tools/compress_images.py "OCMS 41" 1400
```

**Notes:**
- Compresses all PNG files in `*/images/` subfolders
- Typically achieves 80-95% reduction
- Converts RGBA to RGB (removes transparency)

---

### 4. md_to_word.py

Convert markdown to Word document with proper formatting.

```bash
python tools/md_to_word.py <input.md> [output.docx]
```

**Examples:**
```bash
# Convert single file
python tools/md_to_word.py "OCMS 41/v1.0_Technical_Doc.md"

# Convert with custom output name
python tools/md_to_word.py "doc.md" "output.docx"

# Convert all files in directory
python tools/md_to_word.py "./docs" --dir
```

**Features:**
- Headers (H1-H6) with Arial font
- Tables with borders and shading
- Code blocks
- Bullet and numbered lists
- Inline images
- Header/footer with page numbers

---

### 5. add_ole_attachments.py

Add OLE object attachments to Word document.

```bash
python tools/add_ole_attachments.py <markdown.md>
```

**Example:**
```bash
python tools/add_ole_attachments.py "OCMS 41/v1.0_Technical_Doc.md"
```

**Requirements:**
- Microsoft Word installed
- pywin32 package
- Word document must already exist (run md_to_word.py first)

**How it works:**
1. Parses markdown to find image references
2. Opens corresponding .docx file in Word
3. Finds "NOTE: Due to page size limit" text
4. Inserts image as OLE Package object (clickable icon)

---

### 6. rename_sections.py

Rename section folders and update drawio tab names.

```bash
python tools/rename_sections.py <folder>
```

**Example:**
```bash
python tools/rename_sections.py "OCMS 41"
```

**What it does:**
- Renames Section folders (e.g., Section 2 -> Section 1)
- Renames drawio files to match
- Updates tab names inside drawio files
- Deletes old images folders

---

## Folder Structure

Expected project structure:

```
OCMS 41/
├── v1.0_OCMS_41_Technical_Doc.md    # Main markdown file
├── Section 1/
│   ├── OCMS 41 Section 1 Technical Flow.drawio
│   └── images/
│       ├── 1.2_Sync_Furnish_Cron.png
│       └── ...
├── Section 2/
│   ├── OCMS 41 Section 2 Technical Flow.drawio
│   └── images/
│       └── ...
└── ...
```

## Image Reference in Markdown

Use standard markdown image syntax:

```markdown
![Flow Diagram](./Section 1/images/1.2_Sync_Furnish_Cron.png)

NOTE: Due to page size limit, the full-sized image is appended.
```

The "NOTE:" line is used by add_ole_attachments.py to insert OLE objects.

## Troubleshooting

### Draw.io not found
Install Draw.io Desktop from: https://github.com/jgraph/drawio-desktop/releases

### OLE attachments fail
- Ensure Microsoft Word is installed
- Install pywin32: `pip install pywin32`
- Close Word if it's open before running the script

### Large file size
- Use `--max-width 600` for smaller images
- Skip OLE attachments with `--no-ole`

### XML parsing error in drawio
- Check for unescaped quotes in text elements
- Replace `"` with `&quot;` in the drawio XML
