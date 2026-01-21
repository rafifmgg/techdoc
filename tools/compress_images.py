#!/usr/bin/env python
"""
Compress PNG images for Word document insertion.

Resizes images to a maximum width and converts to optimized PNG.
"""

import sys
from pathlib import Path
from PIL import Image

# Maximum width for images (in pixels)
MAX_WIDTH = 1200  # Good balance between quality and file size


def compress_image(input_path, output_path=None, max_width=MAX_WIDTH):
    """
    Compress and resize an image.

    Args:
        input_path: Path to input image
        output_path: Path to output image (defaults to overwrite input)
        max_width: Maximum width in pixels

    Returns:
        Tuple of (original_size, new_size) in bytes
    """
    input_path = Path(input_path)
    if output_path is None:
        output_path = input_path
    else:
        output_path = Path(output_path)

    original_size = input_path.stat().st_size

    # Open image
    with Image.open(input_path) as img:
        # Get original dimensions
        orig_width, orig_height = img.size

        # Calculate new dimensions if resize needed
        if orig_width > max_width:
            ratio = max_width / orig_width
            new_width = max_width
            new_height = int(orig_height * ratio)

            # Resize with high quality
            img = img.resize((new_width, new_height), Image.Resampling.LANCZOS)

        # Convert to RGB if necessary (for PNG with transparency)
        if img.mode in ('RGBA', 'P'):
            # Create white background
            background = Image.new('RGB', img.size, (255, 255, 255))
            if img.mode == 'RGBA':
                background.paste(img, mask=img.split()[3])
            else:
                background.paste(img)
            img = background

        # Save as optimized PNG
        img.save(output_path, 'PNG', optimize=True)

    new_size = output_path.stat().st_size
    return original_size, new_size


def compress_folder(folder_path, max_width=MAX_WIDTH):
    """Compress all PNG images in a folder and subfolders."""
    folder_path = Path(folder_path)

    total_original = 0
    total_new = 0
    count = 0

    # Find all PNG files
    png_files = list(folder_path.glob('**/images/*.png'))

    if not png_files:
        print("No PNG files found in images subfolders.")
        return

    print(f"Found {len(png_files)} PNG files to compress")
    print(f"Max width: {max_width}px")
    print()

    for png_file in sorted(png_files):
        try:
            original_size, new_size = compress_image(png_file, max_width=max_width)

            reduction = (1 - new_size / original_size) * 100
            original_kb = original_size / 1024
            new_kb = new_size / 1024

            print(f"  {png_file.name}: {original_kb:.0f}KB -> {new_kb:.0f}KB ({reduction:.0f}% reduction)")

            total_original += original_size
            total_new += new_size
            count += 1

        except Exception as e:
            print(f"  Error compressing {png_file.name}: {e}")

    print()
    print(f"Compressed {count} files")
    print(f"Total: {total_original/1024/1024:.1f}MB -> {total_new/1024/1024:.1f}MB")
    print(f"Overall reduction: {(1 - total_new/total_original)*100:.0f}%")


def main():
    if len(sys.argv) < 2:
        print("Compress PNG Images")
        print("-" * 40)
        print()
        print("Usage:")
        print("  python compress_images.py <folder> [max_width]")
        print()
        print("Examples:")
        print("  python compress_images.py \"OCMS 41\"")
        print("  python compress_images.py \"OCMS 41\" 800")
        print()
        print(f"Default max width: {MAX_WIDTH}px")
        sys.exit(1)

    folder_path = Path(sys.argv[1]).resolve()
    max_width = int(sys.argv[2]) if len(sys.argv) > 2 else MAX_WIDTH

    if not folder_path.exists():
        print(f"Error: Folder not found: {folder_path}")
        sys.exit(1)

    compress_folder(folder_path, max_width)


if __name__ == '__main__':
    main()
