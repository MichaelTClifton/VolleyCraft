#!/usr/bin/env python3
"""Generate placeholder 16x16 PNG textures for VolleyCraft mod."""
import os
import struct
import zlib

def make_png(pixels_rgba):
    """Create a minimal PNG from a flat list of (R,G,B,A) tuples (16x16)."""
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

    ihdr = struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0)
    return (b'\x89PNG\r\n\x1a\n' +
            chunk(b'IHDR', ihdr) +
            chunk(b'IDAT', compressed) +
            chunk(b'IEND', b''))

def solid(r, g, b, a=255):
    return [(r, g, b, a)] * 256

def net_pole_texture():
    pixels = []
    for y in range(16):
        for x in range(16):
            if 6 <= x <= 9:
                pixels.append((120, 120, 130, 255))  # dark metal
            else:
                pixels.append((80, 80, 90, 255))  # darker metal
    return pixels

def net_segment_texture():
    pixels = []
    for y in range(16):
        for x in range(16):
            # Rope grid pattern
            if x % 4 == 0 or y % 4 == 0:
                pixels.append((220, 220, 200, 255))  # rope
            else:
                pixels.append((0, 0, 0, 60))  # semi-transparent gap
    return pixels

def volleyball_texture():
    pixels = []
    cx, cy, r = 7.5, 7.5, 7.0
    for y in range(16):
        for x in range(16):
            dx = x - cx
            dy = y - cy
            dist = (dx*dx + dy*dy) ** 0.5
            if dist > r:
                pixels.append((0, 0, 0, 0))  # transparent outside
            else:
                # Orange base with dark seam lines
                seam = (abs(dx) < 1.2 and dist < r) or (abs(dy) < 1.2 and dist < r)
                if seam:
                    pixels.append((40, 20, 10, 255))
                else:
                    shade = max(0, int(255 - (dist / r) * 60))
                    pixels.append((shade, int(shade * 0.4), 0, 255))
    return pixels

BASE = '/home/user/VolleyCraft/src/main/resources/assets/volleycraft/textures'
os.makedirs(f'{BASE}/block', exist_ok=True)
os.makedirs(f'{BASE}/item', exist_ok=True)

with open(f'{BASE}/block/net_pole.png', 'wb') as f:
    f.write(make_png(net_pole_texture()))
print('Generated net_pole.png')

with open(f'{BASE}/block/net_segment.png', 'wb') as f:
    f.write(make_png(net_segment_texture()))
print('Generated net_segment.png')

with open(f'{BASE}/item/volleyball.png', 'wb') as f:
    f.write(make_png(volleyball_texture()))
print('Generated volleyball.png')

print('All textures generated!')
