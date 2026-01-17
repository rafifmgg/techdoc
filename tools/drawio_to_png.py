#!/usr/bin/env python
"""
Draw.io to PNG Converter
Converts each tab/page in a .drawio file to separate PNG images.

Uses Draw.io Desktop CLI for export.

Requirements:
    - Draw.io Desktop installed
    - Default path: C:\Program Files\draw.io\draw.io.exe

Usage:
    python drawio_to_png.py <input.drawio> [output_folder]
"""

import sys
import os
import re
import subprocess
import xml.etree.ElementTree as ET
from pathlib import Path


# Draw.io Desktop paths (Windows)
DRAWIO_PATHS = [
    r"C:\Program Files\draw.io\draw.io.exe",
    os.path.expandvars(r"%LOCALAPPDATA%\Programs\draw.io\draw.io.exe"),
    r"C:\Program Files (x86)\draw.io\draw.io.exe",
]


def find_drawio():
    """Find Draw.io executable."""
    for path in DRAWIO_PATHS:
        if os.path.exists(path):
            return path
    return None


def parse_drawio_pages(drawio_path):
    """Parse drawio file and extract page names."""
    tree = ET.parse(drawio_path)
    root = tree.getroot()

    pages = []
    for i, diagram in enumerate(root.findall('.//diagram')):
        page_name = diagram.get('name', 'Untitled')
        page_id = diagram.get('id', '')
        pages.append({
            'name': page_name,
            'id': page_id,
            'index': i
        })

    return pages


def sanitize_filename(name):
    """Convert page name to valid filename."""
    name = re.sub(r'[<>:"/\\|?*]', '_', name)
    name = name.replace(' ', '_')
    return name


def export_page(drawio_exe, drawio_path, page_index, output_path, scale=2):
    """Export a single page to PNG using Draw.io CLI."""
    cmd = [
        drawio_exe,
        "--export",
        "--format", "png",
        "--page-index", str(page_index),
        "--scale", str(scale),
        "--output", str(output_path),
        str(drawio_path)
    ]

    try:
        result = subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            timeout=60
        )

        if result.returncode == 0 and output_path.exists():
            return True
        else:
            if result.stderr:
                print(f"    Error: {result.stderr}")
            return False

    except subprocess.TimeoutExpired:
        print(f"    Timeout")
        return False
    except Exception as e:
        print(f"    Error: {e}")
        return False


def export_drawio_to_png(drawio_path, output_folder, scale=2):
    """Export all pages from drawio file to PNG images."""
    drawio_path = Path(drawio_path).resolve()
    output_folder = Path(output_folder)
    output_folder.mkdir(parents=True, exist_ok=True)

    if not drawio_path.exists():
        raise FileNotFoundError(f"File not found: {drawio_path}")

    # Find Draw.io executable
    drawio_exe = find_drawio()
    if not drawio_exe:
        print("ERROR: Draw.io Desktop not found!")
        print()
        print("Please install Draw.io Desktop from:")
        print("  https://github.com/jgraph/drawio-desktop/releases")
        print()
        print("Expected paths:")
        for path in DRAWIO_PATHS:
            print(f"  - {path}")
        return []

    print(f"Using: {drawio_exe}")
    print()

    # Parse pages
    pages = parse_drawio_pages(drawio_path)

    if not pages:
        print("No pages found in drawio file")
        return []

    print(f"Found {len(pages)} page(s) in {drawio_path.name}:")
    for p in pages:
        print(f"  {p['index']}: {p['name']}")
    print()

    exported_files = []

    for page in pages:
        page_name = page['name']
        page_index = page['index']
        safe_name = sanitize_filename(page_name)
        output_path = output_folder / f"{safe_name}.png"

        print(f"Exporting [{page_index}]: {page_name}...")

        success = export_page(drawio_exe, drawio_path, page_index, output_path, scale)

        if success:
            file_size = output_path.stat().st_size
            print(f"  -> {output_path.name} ({file_size:,} bytes)")
            exported_files.append(str(output_path))
        else:
            print(f"  -> Failed")

    print()
    print(f"Exported {len(exported_files)}/{len(pages)} file(s) to {output_folder}")

    return exported_files


def main():
    if len(sys.argv) < 2:
        print("Draw.io to PNG Converter")
        print("-" * 40)
        print()
        print("Usage:")
        print("  python drawio_to_png.py <input.drawio> [output_folder]")
        print()
        print("Examples:")
        print("  python drawio_to_png.py diagram.drawio")
        print("  python drawio_to_png.py diagram.drawio ./images")
        print()
        print("Requirements:")
        print("  Draw.io Desktop must be installed")
        sys.exit(1)

    input_path = sys.argv[1]
    output_folder = sys.argv[2] if len(sys.argv) > 2 else Path(input_path).parent / "images"

    print(f"Input:  {input_path}")
    print(f"Output: {output_folder}")
    print()

    try:
        export_drawio_to_png(input_path, output_folder)
    except Exception as e:
        print(f"Error: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()
