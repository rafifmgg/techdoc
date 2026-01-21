#!/usr/bin/env python
"""
Generate Technical Documentation

Complete pipeline to generate Word document from markdown with:
1. Export drawio diagrams to PNG
2. Compress images for smaller file size
3. Convert markdown to Word
4. Add OLE object attachments (optional)

Usage:
    python generate_tech_doc.py <folder> [options]

Examples:
    python generate_tech_doc.py "OCMS 41"
    python generate_tech_doc.py "OCMS 41" --no-ole
    python generate_tech_doc.py "OCMS 41" --max-width 800
"""

import sys
import argparse
import subprocess
from pathlib import Path


def find_markdown_file(folder):
    """Find the main markdown file in the folder."""
    folder = Path(folder)
    md_files = list(folder.glob('*.md'))

    # Prefer files starting with 'v' (versioned) or containing 'Technical'
    for md_file in md_files:
        if md_file.name.startswith('v') or 'Technical' in md_file.name:
            return md_file

    # Return first md file found
    if md_files:
        return md_files[0]

    return None


def find_drawio_files(folder):
    """Find all drawio files in section subfolders."""
    folder = Path(folder)
    drawio_files = []

    # Look for Section folders
    for section_folder in sorted(folder.glob('Section *')):
        for drawio_file in section_folder.glob('*.drawio'):
            drawio_files.append(drawio_file)

    return drawio_files


def run_script(script_name, args, description):
    """Run a Python script with arguments."""
    script_path = Path(__file__).parent / script_name

    if not script_path.exists():
        print(f"  ERROR: Script not found: {script_path}")
        return False

    cmd = [sys.executable, str(script_path)] + args
    print(f"\n{'='*60}")
    print(f"Step: {description}")
    print(f"{'='*60}")

    try:
        result = subprocess.run(cmd, check=True)
        return True
    except subprocess.CalledProcessError as e:
        print(f"  ERROR: Script failed with exit code {e.returncode}")
        return False
    except Exception as e:
        print(f"  ERROR: {e}")
        return False


def main():
    parser = argparse.ArgumentParser(
        description='Generate Technical Documentation from markdown and drawio files',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  python generate_tech_doc.py "OCMS 41"
  python generate_tech_doc.py "OCMS 41" --no-ole
  python generate_tech_doc.py "OCMS 41" --max-width 800
  python generate_tech_doc.py "OCMS 41" --skip-drawio
        """
    )

    parser.add_argument('folder', help='Project folder containing markdown and drawio files')
    parser.add_argument('--no-ole', action='store_true', help='Skip adding OLE object attachments')
    parser.add_argument('--max-width', type=int, default=1000, help='Maximum image width in pixels (default: 1000)')
    parser.add_argument('--skip-drawio', action='store_true', help='Skip exporting drawio files (use existing PNGs)')
    parser.add_argument('--skip-compress', action='store_true', help='Skip image compression')

    args = parser.parse_args()

    folder = Path(args.folder).resolve()

    if not folder.exists():
        print(f"Error: Folder not found: {folder}")
        sys.exit(1)

    print("="*60)
    print("Generate Technical Documentation")
    print("="*60)
    print(f"Folder: {folder}")
    print(f"Max image width: {args.max_width}px")
    print(f"Add OLE attachments: {not args.no_ole}")

    # Find markdown file
    md_file = find_markdown_file(folder)
    if not md_file:
        print(f"\nError: No markdown file found in {folder}")
        sys.exit(1)
    print(f"Markdown file: {md_file.name}")

    # Find drawio files
    drawio_files = find_drawio_files(folder)
    print(f"Drawio files found: {len(drawio_files)}")

    # Step 1: Export drawio files to PNG
    if not args.skip_drawio and drawio_files:
        for drawio_file in drawio_files:
            success = run_script(
                'drawio_to_png.py',
                [str(drawio_file)],
                f"Exporting {drawio_file.name}"
            )
            if not success:
                print("Warning: Failed to export drawio file, continuing...")

    # Step 2: Compress images
    if not args.skip_compress:
        success = run_script(
            'compress_images.py',
            [str(folder), str(args.max_width)],
            "Compressing images"
        )
        if not success:
            print("Warning: Failed to compress images, continuing...")

    # Step 3: Convert markdown to Word
    success = run_script(
        'md_to_word.py',
        [str(md_file)],
        "Converting markdown to Word"
    )
    if not success:
        print("Error: Failed to convert markdown to Word")
        sys.exit(1)

    # Step 4: Add OLE attachments (optional)
    if not args.no_ole:
        success = run_script(
            'add_ole_attachments.py',
            [str(md_file)],
            "Adding OLE object attachments"
        )
        if not success:
            print("Warning: Failed to add OLE attachments")
            print("Note: This requires Microsoft Word to be installed")

    # Done
    docx_file = md_file.with_suffix('.docx')
    print("\n" + "="*60)
    print("COMPLETED!")
    print("="*60)
    print(f"Output: {docx_file}")

    if docx_file.exists():
        size_mb = docx_file.stat().st_size / 1024 / 1024
        print(f"Size: {size_mb:.1f} MB")


if __name__ == '__main__':
    main()
