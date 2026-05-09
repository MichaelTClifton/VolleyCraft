#!/usr/bin/env python3
"""Generate placeholder 16x16 PNG textures for VolleyCraft mod."""
import os
import struct
import zlib


def make_png(pixels_rgba):
    """Create a minimal valid PNG from 256 (R,G,B,A) tuples (16x16)."""
    width = height = 16
    raw = b''
    for row in range(height):
        raw += b'\x00'  # filter type: None
        for col in range(width):
            r, g, b, a = pixels_rgba[row * width + col]
            raw += bytes([r, g, b, a])
    compressed = zlib.compress(raw, 9)

    def chunk(name, data):
        c = name + data
        return struct.pack('>I', len(data)) + c + struct.pack('>I', zlib.crc32(c) & 0xffffffff)

    ihdr_data = struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0)
    return (b'\x89PNG\r\n\x1a\n'
            + chunk(b'IHDR', ihdr_data)
            + chunk(b'IDAT', compressed)
            + chunk(b'IEND', b''))


def net_pole_texture():
    pixels = []
    for y in range(16):
        for x in range(16):
            if 6 <= x <= 9:
                # Highlight stripe (pole face)
                shade = 150 if (x == 6 or x == 9) else 130
                pixels.append((shade, shade, shade + 10, 255))
            else:
                pixels.append((80, 80, 90, 255))
    return pixels


def net_segment_texture():
    pixels = []
    for y in range(16):
        for x in range(16):
            # Rope grid: lines every 4 pixels
            if x % 4 == 0 or y % 4 == 0:
                pixels.append((210, 200, 170, 255))
            else:
                pixels.append((0, 0, 0, 30))  # nearly transparent gap
    return pixels


def volleyball_texture():
    pixels = []
    cx, cy, r = 7.5, 7.5, 7.0
    for y in range(16):
        for x in range(16):
            dx, dy = x - cx, y - cy
            dist = (dx * dx + dy * dy) ** 0.5
            if dist > r:
                pixels.append((0, 0, 0, 0))
            else:
                seam = abs(dx) < 1.3 or abs(dy) < 1.3 or abs(abs(dx) - abs(dy)) < 1.0
                if seam:
                    pixels.append((30, 15, 5, 255))
                else:
                    bright = max(0, int(240 - (dist / r) * 50))
                    pixels.append((bright, int(bright * 0.38), 0, 255))
    return pixels


BASE = os.path.join(os.path.dirname(__file__),
                    'src', 'main', 'resources', 'assets', 'volleycraft', 'textures')

for path in [f'{BASE}/block', f'{BASE}/item']:
    os.makedirs(path, exist_ok=True)

textures = [
    (f'{BASE}/block/net_pole.png', net_pole_texture()),
    (f'{BASE}/block/net_segment.png', net_segment_texture()),
    (f'{BASE}/item/volleyball.png', volleyball_texture()),
]

for filepath, pixels in textures:
    with open(filepath, 'wb') as f:
        f.write(make_png(pixels))
    print(f'Generated {os.path.basename(filepath)}')

print('All textures generated!')
