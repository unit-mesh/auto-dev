#!/usr/bin/env python3
"""
Convert PNG files to ICO format for Windows
Uses PIL/Pillow to create multi-resolution ICO
"""

import sys
from pathlib import Path

try:
    from PIL import Image
except ImportError:
    print("❌ Pillow not installed. Installing...")
    import subprocess
    subprocess.check_call([sys.executable, "-m", "pip", "install", "--user", "pillow"])
    from PIL import Image

def create_ico(output_path, *png_paths):
    """Create ICO file from multiple PNG files"""
    images = []
    sizes = []

    for png_path in png_paths:
        if Path(png_path).exists():
            img = Image.open(png_path)
            images.append(img)
            sizes.append(img.size)
            print(f"  Added: {png_path} ({img.size[0]}x{img.size[1]})")

    if not images:
        print("❌ No valid PNG files found")
        sys.exit(1)

    # Save as ICO with all sizes
    images[0].save(
        output_path,
        format='ICO',
        sizes=sizes,
        append_images=images[1:] if len(images) > 1 else None
    )

    print(f"✅ ICO created: {output_path}")

if __name__ == "__main__":
    script_dir = Path(__file__).parent
    resources_dir = script_dir / ".." / "src" / "jvmMain" / "resources"

    print("🪟 Creating Windows ICO file...")

    # PNG files in order of size (smallest to largest)
    png_files = [
        resources_dir / f"icon-{size}.png"
        for size in [16, 32, 48, 64, 128, 256]
    ]

    output_ico = resources_dir / "icon.ico"

    create_ico(str(output_ico), *[str(p) for p in png_files if p.exists()])
