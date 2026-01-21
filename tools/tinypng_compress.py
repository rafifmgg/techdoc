#!/usr/bin/env python
"""
TinyPNG Image Compression

Compress PNG images using TinyPNG API for best quality compression.
Keeps original dimensions, only optimizes file size.

Usage:
    python tinypng_compress.py <folder> [section]

Examples:
    python tinypng_compress.py "OCMS 41"           # All sections
    python tinypng_compress.py "OCMS 41" 1         # Section 1 only
    python tinypng_compress.py "OCMS 41" 2         # Section 2 only
"""

import sys
from pathlib import Path
import tinify

# TinyPNG API key
TINYPNG_API_KEY = "XhwrTX3RkXRBB0t9vjSS9BngKmBhZs8s"
tinify.key = TINYPNG_API_KEY


def compress_image(input_path):
    """Compress single image with TinyPNG."""
    input_path = Path(input_path)
    original_size = input_path.stat().st_size

    source = tinify.from_file(str(input_path))
    source.to_file(str(input_path))

    new_size = input_path.stat().st_size
    return original_size, new_size


def compress_section(folder_path, section_num=None):
    """Compress images in a specific section or all sections."""
    folder_path = Path(folder_path)

    if section_num:
        pattern = f'Section {section_num}/images/*.png'
    else:
        pattern = '**/images/*.png'

    png_files = sorted(folder_path.glob(pattern))

    if not png_files:
        print(f"No PNG files found.")
        return

    print(f"Found {len(png_files)} PNG files")
    print(f"Using TinyPNG API")
    print()

    total_original = 0
    total_new = 0

    for i, png_file in enumerate(png_files, 1):
        try:
            print(f"[{i}/{len(png_files)}] {png_file.name}...", end=" ", flush=True)
            original_size, new_size = compress_image(png_file)

            reduction = (1 - new_size / original_size) * 100
            print(f"{original_size//1024}KB -> {new_size//1024}KB ({reduction:.0f}%)")

            total_original += original_size
            total_new += new_size

        except Exception as e:
            print(f"Error: {e}")

    print()
    print(f"Total: {total_original/1024/1024:.1f}MB -> {total_new/1024/1024:.1f}MB")
    if total_original > 0:
        print(f"Reduction: {(1 - total_new/total_original)*100:.0f}%")


def main():
    if len(sys.argv) < 2:
        print("Usage: python tinypng_compress.py <folder> [section]")
        print("Example: python tinypng_compress.py \"OCMS 41\" 1")
        sys.exit(1)

    folder = sys.argv[1]
    section = sys.argv[2] if len(sys.argv) > 2 else None

    compress_section(folder, section)


if __name__ == '__main__':
    main()
