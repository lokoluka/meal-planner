#!/usr/bin/env python3
"""Resize image to 512x512 PNG"""

from PIL import Image
import sys

def resize_image(input_path, output_path, size=(512, 512)):
    """Resize image to target size, maintaining aspect ratio with padding if needed."""
    img = Image.open(input_path)
    
    # Convert to RGBA for transparency support
    if img.mode != 'RGBA':
        img = img.convert('RGBA')
    
    # Calculate aspect ratios
    img_aspect = img.width / img.height
    target_aspect = size[0] / size[1]
    
    if img_aspect > target_aspect:
        # Image is wider, fit by height
        new_height = size[1]
        new_width = int(new_height * img_aspect)
    else:
        # Image is taller, fit by width
        new_width = size[0]
        new_height = int(new_width / img_aspect)
    
    # Resize
    img = img.resize((new_width, new_height), Image.Resampling.LANCZOS)
    
    # Create canvas with transparent background
    canvas = Image.new('RGBA', size, (0, 0, 0, 0))
    offset = ((size[0] - new_width) // 2, (size[1] - new_height) // 2)
    canvas.paste(img, offset, img)
    
    # Save
    canvas.save(output_path, 'PNG')
    print(f"✓ Resized to {size[0]}x{size[1]} and saved to: {output_path}")

if __name__ == '__main__':
    input_file = sys.argv[1] if len(sys.argv) > 1 else 'icon.png'
    output_file = sys.argv[2] if len(sys.argv) > 2 else 'icon-512x512.png'
    
    resize_image(input_file, output_file, (512, 512))
