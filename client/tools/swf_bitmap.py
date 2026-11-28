"""Decode bitmap-backed symbols from the native Gunny SWF resource libraries."""
from pathlib import Path
from PIL import Image, ImageDraw
import io
import struct
import zlib



def tags(data):
    offset = 0
    while offset + 2 <= len(data):
        header = struct.unpack_from('<H', data, offset)[0]
        offset += 2
        kind, length = header >> 6, header & 63
        if length == 63:
            length = struct.unpack_from('<I', data, offset)[0]
            offset += 4
        yield kind, data[offset:offset + length]
        offset += length
        if kind == 0:
            break


def library(path):
    packed = path.read_bytes()
    data = zlib.decompress(packed[8:]) if packed[:3] == b'CWS' else packed[8:]
    start = (5 + 4 * (data[0] >> 3) + 7) // 8 + 4
    definitions, symbols = {}, {}
    for kind, body in tags(data[start:]):
        if kind in (2, 20, 21, 22, 32, 35, 36, 39, 83):
            definitions[struct.unpack_from('<H', body)[0]] = (kind, body)
        if kind in (56, 76):
            pos = 2
            for _ in range(struct.unpack_from('<H', body)[0]):
                char_id = struct.unpack_from('<H', body, pos)[0]
                pos += 2
                end = body.index(0, pos)
                symbols[body[pos:end].decode('utf8', 'replace')] = char_id
                pos = end + 1
    return definitions, symbols


def bitmap(definition):
    kind, body = definition
    if kind == 21:
        return Image.open(io.BytesIO(body[2:])).convert('RGBA')
    if kind == 20:
        _, fmt, width, height = struct.unpack_from('<HBHH', body)
        if fmt == 5:
            return Image.frombytes('RGB',(width,height),zlib.decompress(body[7:]),'raw','XRGB').convert('RGBA')
        if fmt == 3:
            count=body[7]+1;raw=zlib.decompress(body[8:]);palette=raw[:count*3];indices=raw[count*3:];stride=(width+3)&~3
            pixels=bytes(value for y in range(height) for x in range(width) for value in palette[indices[y*stride+x]*3:indices[y*stride+x]*3+3])
            return Image.frombytes('RGB',(width,height),pixels).convert('RGBA')
    if kind == 35:
        _, alpha_offset = struct.unpack_from('<HI', body)
        image = Image.open(io.BytesIO(body[6:6 + alpha_offset])).convert('RGBA')
        image.putalpha(Image.frombytes('L', image.size, zlib.decompress(body[6 + alpha_offset:])))
        return image
    if kind == 36:
        _, fmt, width, height = struct.unpack_from('<HBHH', body)
        if fmt == 3:
            count = body[7] + 1
            raw = zlib.decompress(body[8:])
            palette = raw[:count * 4]
            indices = raw[count * 4:]
            stride = (width + 3) & ~3
            pixels = bytearray()
            for y in range(height):
                for x in range(width):
                    offset = indices[y * stride + x] * 4
                    pixels.extend(palette[offset:offset + 4])
            return Image.frombytes('RGBA', (width, height), bytes(pixels))
        if fmt == 5:
            return Image.frombytes('RGBA', (width, height), zlib.decompress(body[7:]), 'raw', 'ARGB')
    return None


def bitmap_refs(char_id, definitions, seen=None):
    seen = set() if seen is None else seen
    if char_id in seen or char_id not in definitions:
        return []
    seen.add(char_id)
    kind, body = definitions[char_id]
    if kind in (35, 36):
        return [char_id]
    refs = []
    if kind == 39:
        for tag, item in tags(body[4:]):
            if tag == 26 and item[0] & 2:
                refs.append(struct.unpack_from('<H', item, 3)[0])
            elif tag == 4:
                refs.append(struct.unpack_from('<H', item)[0])
    elif kind in (2, 22, 32, 83):
        # Audit candidate references, not a general SWF shape renderer.
        for candidate, (tag, _) in definitions.items():
            if tag in (35, 36) and any(bytes([fill]) + struct.pack('<H', candidate) in body[2:] for fill in (64, 65, 66, 67)):
                refs.append(candidate)
    found = []
    for ref in refs:
        for image_id in bitmap_refs(ref, definitions, seen):
            if image_id not in found:
                found.append(image_id)
    return found

