"""Build tightly packed native normal/POW art; no runtime rotation or scaling.

python client/tools/weapon_combat_assets.py --resource D:/gunny/Resource \
    --ffdec D:/gunny/GunnyNokia/tools/ffdec/ffdec.jar
"""
import argparse
from concurrent.futures import ThreadPoolExecutor
import hashlib
import io
import json
from pathlib import Path
import struct
import subprocess

from PIL import Image
from swf_bitmap import library, bitmap_refs, bitmap

ROOT = Path(__file__).resolve().parents[1]
PAIRS = {0: (101, 111), 1: (161, 171), 2: (341, 351), 3: (381, 391),
         4: (321, 331), 5: (121, 131), 7: (401, 411), 9: (141, 151)}


def native_projectiles(resource, number):
    path = next(p for p in (resource / 'image/bomb/blastout').glob('*.swf')
                if p.stem.lower() == f'blastout{number}')
    definitions, symbols = library(path)
    refs = bitmap_refs(symbols[f'bullet{number}'], definitions)
    frames = [bitmap(definitions[ref]) for ref in refs]
    assert frames and all(im is not None for im in frames)
    return frames, {'source': str(path.relative_to(resource)),
        'sha256': hashlib.sha256(path.read_bytes()).hexdigest(), 'symbol': f'bullet{number}', 'bitmap_ids': refs}


def render_impact(resource, ffdec, java, number):
    path = resource / f'image/bomb/bullet/bullet{number}.swf'
    definitions, symbols = library(path)
    sid = symbols[f'blastOutMovie{number}']
    count = struct.unpack_from('<H', definitions[sid][1], 2)[0]
    dest = ROOT / f'build/weapon-combat-source/{number}'
    files = sorted(dest.rglob('*.png'), key=lambda p: int(p.stem)) if dest.exists() else []
    if len(files) != count:
        result = subprocess.run([java, '-Djava.awt.headless=true', '-jar', str(ffdec),
            '-onerror', 'abort', '-selectid', str(sid), '-zoom', '0.6', '-ignorebackground',
            '-format', 'sprite:png', '-export', 'sprite', str(dest), str(path)],
            capture_output=True, text=True, timeout=90)
        if result.returncode:
            raise RuntimeError(result.stdout + result.stderr)
        files = sorted(dest.rglob('*.png'), key=lambda p: int(p.stem))
    assert len(files) == count
    frames = [Image.open(p).convert('RGBA') for p in files]
    visible = [i for i, frame in enumerate(frames) if frame.getbbox()]
    assert visible
    # Trim only the empty tail, then preserve all visible native poses across 25 ticks.
    chosen = [round(i * min(count - 1, visible[-1] + 1) / 24) for i in range(25)]
    return [frames[i] for i in chosen], {'source': str(path.relative_to(resource)),
        'sha256': hashlib.sha256(path.read_bytes()).hexdigest(), 'symbol': f'blastOutMovie{number}',
        'source_frames': count, 'sampled_source_frames': [i + 1 for i in chosen]}


def tile(im, side, limit, angle=0):
    im = im.crop(im.getbbox())
    ratio = limit / max(im.size)
    im = im.resize((max(1, round(im.width * ratio)), max(1, round(im.height * ratio))), Image.Resampling.LANCZOS)
    if angle:
        im = im.rotate(angle, resample=Image.Resampling.BICUBIC, expand=True)
    canvas = Image.new('RGBA', (side, side))
    canvas.alpha_composite(im, ((side - im.width) // 2, (side - im.height) // 2))
    return canvas


def projectiles(frames, weapon, powered, scale):
    side = (40 if powered else 32) * scale
    limit = (26 if powered else 24 if weapon == 9 else 22 if weapon in (4, 5) else 14) * scale
    return [tile(im, side, limit, angle * 22.5) for im in frames for angle in range(16)]


def held(frames, weapon, powered, scale):
    # The projectile sits on the same hand pivot in both firing modes.
    limit = (23 if powered else 22 if weapon in (4, 5) else 15) * scale
    base = tile(frames[0], 32 * scale, limit)
    return [base.rotate(angle * 90 / 7, resample=Image.Resampling.BICUBIC) for angle in range(8)]


def trails(shots):
    result = []
    for fade in range(3):
        for im in shots[:16]:
            im = im.copy()
            im.putalpha(im.getchannel('A').point(lambda a: round(a * (0.45, 0.23, 0.08)[fade])))
            result.append(im)
    return result


def impact_sections(normal, powered, scale):
    # One common physical scale per pair preserves the native larger POW silhouette.
    width = max(normal[0].width, powered[0].width)
    height = max(normal[0].height, powered[0].height)
    factor = min(92 / width, 92 / height) * scale
    result = []
    for frames in (normal, powered):
        cells = []
        for im in frames:
            im = im.resize((max(1, round(im.width * factor)), max(1, round(im.height * factor))), Image.Resampling.LANCZOS)
            cell = Image.new('RGBA', (96 * scale, 96 * scale))
            cell.alpha_composite(im, ((cell.width - im.width) // 2, (cell.height - im.height) // 2))
            cells.append(cell)
        result.append(cells)
    return result


def pack(sections, scale, dest):
    # Metadata is in logical pixels. Crop on that grid so 1x/2x use identical drawing code.
    unique, records, indices, keys = [], [], [], {}
    for section in sections:
        mapped = []
        for im in section:
            box = im.getbbox()
            if box:
                box = (box[0] // scale, box[1] // scale,
                       (box[2] + scale - 1) // scale, (box[3] + scale - 1) // scale)
                crop = im.crop(tuple(v * scale for v in box))
                dx, dy = box[0] - im.width // scale // 2, box[1] - im.height // scale // 2
                key = (crop.size, dx, dy, crop.tobytes())
            else:
                crop, dx, dy, key = None, 0, 0, None
            if key not in keys:
                keys[key] = len(records)
                records.append([0, 0, 0 if crop is None else crop.width // scale,
                                0 if crop is None else crop.height // scale, dx, dy])
                unique.append(crop)
            mapped.append(keys[key])
        indices.append(mapped)
    # Shelf pack the actual drawn regions, avoiding 25 full 96x96 cells per effect.
    # Keep E72 PNG dimensions below 1024, including the largest fruit explosion pack.
    atlas_w, atlas_h, shelves = 512, 0, []
    for index in sorted(range(len(records)), key=lambda i: (-records[i][3], -records[i][2])):
        record = records[index]
        w, h = record[2:4]
        candidates = [s for s in shelves if s[2] >= h and s[0] + w <= atlas_w]
        if candidates:
            shelf = min(candidates, key=lambda s: (s[2] - h, atlas_w - s[0] - w))
        else:
            shelf = [0, atlas_h, h]
            shelves.append(shelf)
            atlas_h += h
        record[0:2] = shelf[0], shelf[1]
        shelf[0] += w
    atlas = Image.new('RGBA', (atlas_w * scale, max(1, atlas_h) * scale))
    assert scale != 1 or max(atlas.size) <= 1024, atlas.size
    for im, record in zip(unique, records):
        if im is not None:
            atlas.alpha_composite(im, (record[0] * scale, record[1] * scale))
    encoded = io.BytesIO()
    atlas.save(encoded, format='PNG', optimize=True)
    with dest.open('wb') as out:
        out.write(b'GWC1')
        out.write(struct.pack('>B', len(indices)))
        for section in indices:
            out.write(struct.pack('>H', len(section)))
            out.write(struct.pack('>' + 'H' * len(section), *section))
        out.write(struct.pack('>H', len(records)))
        for record in records:
            out.write(struct.pack('>6h', *record))
        out.write(encoded.getvalue())
    return {'atlas_size': list(atlas.size), 'decoded_rgba_bytes': atlas.width * atlas.height * 4,
        'unique_regions': len(records), 'section_frames': [len(s) for s in sections],
        'packed_bytes': dest.stat().st_size, 'sha256': hashlib.sha256(dest.read_bytes()).hexdigest()}


def main():
    parser = argparse.ArgumentParser(__doc__)
    parser.add_argument('--resource', type=Path, required=True)
    parser.add_argument('--ffdec', type=Path, required=True)
    parser.add_argument('--java', default='java')
    args = parser.parse_args()
    numbers = sorted({n for pair in PAIRS.values() for n in pair})
    with ThreadPoolExecutor(max_workers=3) as pool:
        hits = dict(zip(numbers, pool.map(lambda n: render_impact(args.resource, args.ffdec, args.java, n), numbers)))
    report = {'format': 'GWC1', 'directions': 16, 'impact_ticks': 25,
        'sections': ['normal projectile', 'POW projectile', 'normal trail', 'POW trail',
                     'normal held', 'POW held', 'normal impact', 'POW impact'], 'weapons': []}
    for weapon, (normal, powered) in PAIRS.items():
        normal_shots, normal_meta = native_projectiles(args.resource, normal)
        pow_shots, pow_meta = native_projectiles(args.resource, powered)
        entry = {'weapon': weapon, 'normal': normal_meta, 'bpow': pow_meta,
            'normal_impact': hits[normal][1], 'bpow_impact': hits[powered][1], 'scales': {}}
        for scale in (1, 2):
            out = ROOT / f'assets{scale}'
            n = projectiles(normal_shots, weapon, False, scale)
            p = projectiles(pow_shots, weapon, True, scale)
            sections = [n, p, trails(n), trails(p), held(normal_shots, weapon, False, scale),
                        held(pow_shots, weapon, True, scale)] + impact_sections(hits[normal][0], hits[powered][0], scale)
            entry['scales'][str(scale)] = pack(sections, scale, out / f'weapon-combat-{weapon}.dat')
        report['weapons'].append(entry)
    target = ROOT / 'source-assets/weapon-combat'
    target.mkdir(exist_ok=True)
    (target / 'manifest.json').write_text(json.dumps(report, indent=2), encoding='utf-8')
    print(json.dumps({'weapons': len(PAIRS), 'decoded_bytes_by_scale': {
        str(s): sum(w['scales'][str(s)]['decoded_rgba_bytes'] for w in report['weapons']) for s in (1, 2)}}))


if __name__ == '__main__':
    main()
