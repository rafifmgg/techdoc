#!/usr/bin/env python
"""
Draw.io to PNG Converter
Converts each tab/page in a .drawio file to separate PNG images.

Uses Draw.io Desktop CLI to export to PDF first, then converts PDF to PNG.
This approach avoids cropping issues that occur with direct PNG export.

Requirements:
    - Draw.io Desktop installed
    - PyMuPDF (fitz) for PDF to PNG conversion: pip install PyMuPDF

Usage:
    python drawio_to_png.py <input.drawio> [output_folder]
"""

import sys
import os
import re
import subprocess
import xml.etree.ElementTree as ET
import tempfile
import shutil
from pathlib import Path

try:
    import fitz  # PyMuPDF
except ImportError:
    print("ERROR: PyMuPDF not installed. Run: pip install PyMuPDF")
    sys.exit(1)


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


def calculate_content_bounds(graph_model):
    """
    Calculate the bounding box of all content in a diagram.
    Handles nested elements by calculating absolute positions.
    """
    if graph_model is None:
        return None

    # Build a map of cell id -> cell element
    cells = {}
    for cell in graph_model.iter('mxCell'):
        cell_id = cell.get('id')
        if cell_id:
            cells[cell_id] = cell

    # Calculate absolute position for each cell
    def get_absolute_bounds(cell):
        """Get absolute x, y, width, height for a cell."""
        geom = cell.find('mxGeometry')
        if geom is None:
            return None

        x = float(geom.get('x', 0))
        y = float(geom.get('y', 0))
        width = float(geom.get('width', 0))
        height = float(geom.get('height', 0))

        # Check if geometry is relative (for edges)
        if geom.get('relative') == '1':
            return None  # Skip relative geometries (connection points)

        # Add parent offset recursively
        parent_id = cell.get('parent')
        if parent_id and parent_id in cells and parent_id not in ('0', '1'):
            parent_bounds = get_absolute_bounds(cells[parent_id])
            if parent_bounds:
                x += parent_bounds['x']
                y += parent_bounds['y']

        return {'x': x, 'y': y, 'width': width, 'height': height}

    min_x, min_y = float('inf'), float('inf')
    max_x, max_y = float('-inf'), float('-inf')

    for cell_id, cell in cells.items():
        if cell_id in ('0', '1'):  # Skip root cells
            continue

        bounds = get_absolute_bounds(cell)
        if bounds and (bounds['width'] > 0 or bounds['height'] > 0):
            min_x = min(min_x, bounds['x'])
            min_y = min(min_y, bounds['y'])
            max_x = max(max_x, bounds['x'] + bounds['width'])
            max_y = max(max_y, bounds['y'] + bounds['height'])

    if min_x == float('inf'):
        return None

    return {
        'min_x': min_x,
        'min_y': min_y,
        'max_x': max_x,
        'max_y': max_y,
        'width': max_x - min_x,
        'height': max_y - min_y
    }


def adjust_page_size(drawio_path, output_path, padding=200):
    """Create a copy of drawio file with adjusted page sizes to fit all content."""
    tree = ET.parse(drawio_path)
    root = tree.getroot()

    for diagram in root.findall('.//diagram'):
        graph_model = diagram.find('.//mxGraphModel')

        if graph_model is not None:
            bounds = calculate_content_bounds(graph_model)

            if bounds:
                # Calculate new page dimensions with padding
                new_width = int(bounds['max_x'] + padding)
                new_height = int(bounds['max_y'] + padding)

                # Ensure minimum dimensions
                new_width = max(new_width, 1000)
                new_height = max(new_height, 800)

                print(f"    Content bounds: ({bounds['min_x']:.0f}, {bounds['min_y']:.0f}) to ({bounds['max_x']:.0f}, {bounds['max_y']:.0f})")
                print(f"    New page size: {new_width} x {new_height}")

                # Update page dimensions
                graph_model.set('pageWidth', str(new_width))
                graph_model.set('pageHeight', str(new_height))

                # Also resize any swimlanes to encompass all content within their scope
                resize_swimlanes(graph_model, bounds, padding)

    # Write modified file
    tree.write(output_path, encoding='utf-8', xml_declaration=True)
    return output_path


def resize_swimlanes(graph_model, overall_bounds, padding=50):
    """Resize swimlanes to ensure they encompass all related content."""
    cells = {}
    for cell in graph_model.iter('mxCell'):
        cell_id = cell.get('id')
        if cell_id:
            cells[cell_id] = cell

    # Find swimlanes and resize them
    for cell_id, cell in cells.items():
        style = cell.get('style', '')
        if 'swimlane' in style:
            geom = cell.find('mxGeometry')
            if geom is not None:
                # Get current swimlane bounds
                sw_x = float(geom.get('x', 0))
                sw_y = float(geom.get('y', 0))
                sw_width = float(geom.get('width', 0))
                sw_height = float(geom.get('height', 0))

                # Calculate bounds of all children
                child_max_x = sw_x
                child_max_y = sw_y
                for child_id, child in cells.items():
                    if child.get('parent') == cell_id:
                        child_geom = child.find('mxGeometry')
                        if child_geom is not None and child_geom.get('relative') != '1':
                            cx = float(child_geom.get('x', 0))
                            cy = float(child_geom.get('y', 0))
                            cw = float(child_geom.get('width', 0))
                            ch = float(child_geom.get('height', 0))
                            child_max_x = max(child_max_x, sw_x + cx + cw)
                            child_max_y = max(child_max_y, sw_y + cy + ch)

                # Extend swimlane to include content below it (outside children)
                # This is for elements with parent="1" that are positioned below the swimlane
                new_height = max(sw_height, overall_bounds['max_y'] - sw_y + padding)
                new_width = max(sw_width, child_max_x - sw_x + padding)

                geom.set('width', str(int(new_width)))
                geom.set('height', str(int(new_height)))


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


def export_page_to_pdf(drawio_exe, drawio_path, page_index, output_path):
    """Export a single page to PDF using Draw.io CLI."""
    # Draw.io CLI uses 1-based page index for --page-index
    cmd = [
        drawio_exe,
        "--export",
        "--format", "pdf",
        "--page-index", str(page_index + 1),  # Convert 0-based to 1-based
        "--crop",  # Crop to content
        "--output", str(output_path),
        str(drawio_path)
    ]

    try:
        result = subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            timeout=120
        )

        if result.returncode == 0 and output_path.exists():
            return True
        else:
            if result.stderr:
                print(f"    PDF Export Error: {result.stderr}")
            return False

    except subprocess.TimeoutExpired:
        print(f"    Timeout during PDF export")
        return False
    except Exception as e:
        print(f"    PDF Export Error: {e}")
        return False


def pdf_to_png(pdf_path, png_path, dpi=300):
    """Convert PDF to PNG using PyMuPDF."""
    try:
        doc = fitz.open(pdf_path)
        if len(doc) == 0:
            print(f"    PDF has no pages")
            return False

        # Get the first page (each drawio page exports to single-page PDF)
        page = doc[0]

        # Calculate zoom factor for desired DPI (default PDF is 72 DPI)
        zoom = dpi / 72
        matrix = fitz.Matrix(zoom, zoom)

        # Render page to pixmap
        pixmap = page.get_pixmap(matrix=matrix, alpha=False)

        # Save as PNG
        pixmap.save(str(png_path))

        doc.close()
        return True

    except Exception as e:
        print(f"    PDF to PNG Error: {e}")
        return False


def export_page(drawio_exe, drawio_path, page_index, output_path, scale=2, border=50):
    """Export a single page to PNG via PDF intermediate format."""
    # Create temp PDF file
    temp_pdf = output_path.parent / f"_temp_{page_index}.pdf"

    try:
        # Step 1: Export to PDF
        if not export_page_to_pdf(drawio_exe, drawio_path, page_index, temp_pdf):
            return False

        # Step 2: Convert PDF to PNG (scale 2 = 144 DPI, scale 3 = 216 DPI)
        dpi = 72 * scale
        if not pdf_to_png(temp_pdf, output_path, dpi=dpi):
            return False

        return True

    finally:
        # Clean up temp PDF
        if temp_pdf.exists():
            temp_pdf.unlink()


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

    # Parse pages from original file
    pages = parse_drawio_pages(drawio_path)

    if not pages:
        print("No pages found in drawio file")
        return []

    print(f"Found {len(pages)} page(s) in {drawio_path.name}:")
    for p in pages:
        print(f"  {p['index']}: {p['name']}")
    print()

    # Create temp file with adjusted page sizes
    temp_dir = tempfile.mkdtemp()
    temp_drawio = Path(temp_dir) / "adjusted.drawio"

    try:
        print("Calculating content bounds and adjusting page sizes...")
        adjust_page_size(drawio_path, temp_drawio, padding=200)
        print()

        exported_files = []

        for page in pages:
            page_name = page['name']
            page_index = page['index']
            safe_name = sanitize_filename(page_name)
            output_path = output_folder / f"{safe_name}.png"

            print(f"Exporting [{page_index}]: {page_name}...")

            # Export from the adjusted temp file
            success = export_page(drawio_exe, temp_drawio, page_index, output_path, scale)

            if success:
                file_size = output_path.stat().st_size
                print(f"  -> {output_path.name} ({file_size:,} bytes)")
                exported_files.append(str(output_path))
            else:
                print(f"  -> Failed")

        print()
        print(f"Exported {len(exported_files)}/{len(pages)} file(s) to {output_folder}")

        return exported_files

    finally:
        # Clean up temp directory
        shutil.rmtree(temp_dir, ignore_errors=True)


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
        print("Features:")
        print("  - Uses PDF intermediate format to avoid cropping issues")
        print("  - Exports each tab/page as separate PNG")
        print()
        print("Requirements:")
        print("  - Draw.io Desktop must be installed")
        print("  - PyMuPDF: pip install PyMuPDF")
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
        import traceback
        traceback.print_exc()
        sys.exit(1)


if __name__ == "__main__":
    main()
